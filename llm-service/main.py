import os

import httpx
from fastapi import FastAPI, HTTPException

from ollama_client import generate, is_model_available
from prompt_templates import build_prompt
from redis_client import get_lead_features

SCORING_URL = os.getenv("SCORING_SERVICE_URL", "http://localhost:8081")

app = FastAPI(title="LLM Summary Service", version="1.0.0")


@app.get("/health")
async def health():
    ollama_ok = await is_model_available()
    return {
        "status": "ok",
        "ollama": "ready" if ollama_ok else "model not pulled — run: docker exec -it leadscoringia-ollama-1 ollama pull mistral",
    }


@app.get("/summary/{lead_id}")
async def get_summary(lead_id: str):
    # 1. features do Redis
    features = get_lead_features(lead_id)
    if features is None:
        raise HTTPException(status_code=404, detail=f"Lead '{lead_id}' não encontrado no Redis")

    # 2. score do scoring-service
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.get(f"{SCORING_URL}/score/{lead_id}")
            resp.raise_for_status()
            score_data = resp.json()
    except httpx.HTTPError as e:
        raise HTTPException(status_code=502, detail=f"Erro ao consultar scoring-service: {e}")

    # 3. gera resumo via Mistral
    if not await is_model_available():
        raise HTTPException(
            status_code=503,
            detail="Modelo Mistral não disponível. Execute: docker exec -it leadscoringia-ollama-1 ollama pull mistral",
        )

    prompt = build_prompt(
        features=features,
        score=score_data["score"],
        label=score_data["scoreLabel"],
        top_features=score_data["topFeatures"],
    )

    summary = await generate(prompt)

    return {
        "leadId":      lead_id,
        "name":        features.get("name"),
        "score":       score_data["score"],
        "scoreLabel":  score_data["scoreLabel"],
        "nextAction":  score_data["nextAction"],
        "topFeatures": score_data["topFeatures"],
        "summary":     summary,
    }


@app.get("/summary/batch/all")
async def get_all_summaries():
    """Gera resumo para todos os leads com score >= 50 (QUENTE ou MORNO)."""
    if not await is_model_available():
        raise HTTPException(
            status_code=503,
            detail="Modelo Mistral não disponível. Execute: docker exec -it leadscoringia-ollama-1 ollama pull mistral",
        )

    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            resp = await client.get(f"{SCORING_URL}/scores")
            resp.raise_for_status()
            all_scores = resp.json()["results"]
    except httpx.HTTPError as e:
        raise HTTPException(status_code=502, detail=f"Erro ao consultar scoring-service: {e}")

    prioritized = [r for r in all_scores if r["score"] >= 50]
    results = []

    for score_data in prioritized:
        lead_id  = score_data["leadId"]
        features = get_lead_features(lead_id)
        if not features:
            continue

        prompt  = build_prompt(features, score_data["score"], score_data["scoreLabel"], score_data["topFeatures"])
        summary = await generate(prompt)

        results.append({
            "leadId":     lead_id,
            "name":       features.get("name"),
            "score":      score_data["score"],
            "scoreLabel": score_data["scoreLabel"],
            "nextAction": score_data["nextAction"],
            "summary":    summary,
        })

    return {"summarized": len(results), "results": results}
