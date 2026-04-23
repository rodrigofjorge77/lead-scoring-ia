from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI, HTTPException

from kafka_producer import publish_score
from redis_client import get_all_lead_ids, get_lead_features
from scorer import score_lead


@asynccontextmanager
async def lifespan(app: FastAPI):
    if not Path("model/xgboost_model.pkl").exists():
        print("Modelo não encontrado — treinando agora...")
        import train
        train.train()
    yield


app = FastAPI(title="Lead Scoring Service", version="1.0.0", lifespan=lifespan)


@app.get("/health")
def health():
    return {"status": "ok"}


@app.get("/score/{lead_id}")
def score(lead_id: str):
    features = get_lead_features(lead_id)
    if features is None:
        raise HTTPException(status_code=404, detail=f"Lead '{lead_id}' não encontrado no Redis")
    result = score_lead(features)
    publish_score(result)
    return result


@app.get("/scores")
def score_all():
    lead_ids = get_all_lead_ids()
    if not lead_ids:
        return {"scored": 0, "results": []}

    results = []
    for lead_id in lead_ids:
        features = get_lead_features(lead_id)
        if features:
            result = score_lead(features)
            publish_score(result)
            results.append(result)

    results.sort(key=lambda r: r["score"], reverse=True)
    return {"scored": len(results), "results": results}
