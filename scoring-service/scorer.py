import time
from pathlib import Path

import joblib
import numpy as np

MODEL_PATH = Path(__file__).parent / "model" / "xgboost_model.pkl"

FEATURE_NAMES = [
    "roleScore", "companySizeScore", "segmentFitScore", "sourceScore",
    "emailOpenScore", "pricingPageVisits", "demoWatched",
    "daysSinceLastContact", "crmStageScore",
]

_model = None


def _load_model():
    global _model
    if _model is None:
        if not MODEL_PATH.exists():
            import train
            train.train()
        _model = joblib.load(MODEL_PATH)
    return _model


def _feature_vector(features: dict) -> list[float]:
    epoch_ms = features.get("lastContactEpoch", 0)
    days_since = max(0.0, (time.time() * 1000 - epoch_ms) / (1000 * 86400))

    return [
        float(features.get("roleScore", 0)),
        float(features.get("companySizeScore", 0)),
        float(features.get("segmentFitScore", 0)),
        float(features.get("sourceScore", 0)),
        float(features.get("emailOpenScore", 0)),
        float(min(features.get("pricingPageVisits", 0), 10)),
        1.0 if features.get("demoWatched", False) else 0.0,
        days_since,
        float(features.get("crmStageScore", 10)),
    ]


def score_lead(features: dict) -> dict:
    model = _load_model()
    vector = _feature_vector(features)
    prob = float(model.predict_proba(np.array([vector]))[0][1])
    score = min(100, max(0, round(prob * 100)))

    label = "QUENTE" if score >= 80 else "MORNO" if score >= 50 else "FRIO"
    next_action = {
        "QUENTE": "Ligar hoje — alta prioridade",
        "MORNO":  "Enviar e-mail personalizado, agendar follow-up",
        "FRIO":   "Nurturing automático — não priorizar agora",
    }[label]

    # top 3 features por valor absoluto (exclui daysSinceLastContact — quanto menor melhor)
    named = dict(zip(FEATURE_NAMES, vector))
    top = sorted(
        [k for k in named if k != "daysSinceLastContact"],
        key=lambda k: named[k],
        reverse=True,
    )[:3]

    return {
        "leadId":      features.get("leadId"),
        "name":        features.get("name", ""),
        "score":       score,
        "scoreLabel":  label,
        "nextAction":  next_action,
        "topFeatures": top,
        "probability": round(prob, 4),
        "features": {
            "roleScore":          int(named["roleScore"]),
            "companySizeScore":   int(named["companySizeScore"]),
            "segmentFitScore":    int(named["segmentFitScore"]),
            "sourceScore":        int(named["sourceScore"]),
            "emailOpenScore":     int(named["emailOpenScore"]),
            "pricingPageVisits":  int(named["pricingPageVisits"]),
            "demoWatched":        bool(named["demoWatched"]),
            "daysSinceLastContact": round(named["daysSinceLastContact"], 1),
            "crmStageScore":      int(named["crmStageScore"]),
        },
    }
