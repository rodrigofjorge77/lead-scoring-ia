import json
import os
from datetime import datetime, timezone

from kafka import KafkaProducer

_producer: KafkaProducer | None = None


def _get_producer() -> KafkaProducer:
    global _producer
    if _producer is None:
        _producer = KafkaProducer(
            bootstrap_servers=os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9094"),
            value_serializer=lambda v: json.dumps(v, ensure_ascii=False).encode("utf-8"),
            key_serializer=lambda k: k.encode("utf-8"),
            request_timeout_ms=5000,
            retries=3,
        )
    return _producer


def publish_score(result: dict) -> None:
    payload = {**result, "timestamp": datetime.now(timezone.utc).isoformat()}
    _get_producer().send("score-updates", key=result["leadId"], value=payload)
    _get_producer().flush()
