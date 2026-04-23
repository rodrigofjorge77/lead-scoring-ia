import json
import os

import redis

_r: redis.Redis | None = None


def _get_redis() -> redis.Redis:
    global _r
    if _r is None:
        _r = redis.Redis(
            host=os.getenv("REDIS_HOST", "localhost"),
            port=int(os.getenv("REDIS_PORT", "6379")),
            decode_responses=True,
        )
    return _r


def get_lead_features(lead_id: str) -> dict | None:
    raw = _get_redis().get(f"lead:{lead_id}")
    return json.loads(raw) if raw else None


def get_all_lead_ids() -> list[str]:
    keys = _get_redis().keys("lead:*")
    return [k.replace("lead:", "") for k in keys]
