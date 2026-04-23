#!/usr/bin/env python3
"""
Gerador de leads e interações sintéticas para o pipeline de Lead Scoring.
Usa Faker para nomes/empresas realistas em português e publica no Kafka.

Uso:
    python generate_leads.py

Variáveis de ambiente:
    KAFKA_BOOTSTRAP_SERVERS  (default: localhost:9093)
    NUM_LEADS                (default: 50)
    INTERACTIONS_PER_LEAD    (default: 5)
"""

import json
import os
import random
import time
from datetime import datetime, timezone

from faker import Faker
from kafka import KafkaProducer

fake = Faker("pt_BR")

ROLES = [
    "CEO", "CTO", "Diretor de TI", "Diretor Comercial", "VP de Vendas",
    "Gerente de TI", "Gerente Comercial", "Analista de TI", "Analista Comercial",
]
SEGMENTS      = ["Manufatura", "Varejo", "Financeiro", "Saúde", "Educação", "Tecnologia", "Serviços"]
SOURCES       = ["site", "linkedin", "evento", "indicacao"]
INTERACTIONS  = ["EMAIL_OPEN", "PRICING_PAGE_VISIT", "DEMO_WATCHED", "SITE_VISIT"]
EMP_RANGES    = [(10, 49), (50, 200), (201, 1000), (1001, 5000)]
CRM_STAGES    = ["NOVO", "QUALIFICADO", "PROPOSTA", "NEGOCIACAO", "FECHADO"]


def ts() -> str:
    return datetime.now(timezone.utc).isoformat()


def generate_lead(lead_id: str) -> dict:
    lo, hi = random.choice(EMP_RANGES)
    return {
        "leadId":    lead_id,
        "name":      fake.name(),
        "role":      random.choice(ROLES),
        "company":   fake.company(),
        "employees": random.randint(lo, hi),
        "segment":   random.choice(SEGMENTS),
        "source":    random.choice(SOURCES),
        "timestamp": ts(),
    }


def generate_interaction(lead_id: str) -> dict:
    itype = random.choice(INTERACTIONS)
    count = random.randint(1, 3) if itype in ("EMAIL_OPEN", "SITE_VISIT") else 1
    return {"leadId": lead_id, "type": itype, "count": count, "timestamp": ts()}


def generate_crm_update(lead_id: str) -> dict:
    stage = random.choice(CRM_STAGES[1:])  # nunca NOVO para updates
    return {"leadId": lead_id, "stage": stage, "timestamp": ts()}


def main():
    bootstrap  = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9094")
    num_leads  = int(os.getenv("NUM_LEADS", "50"))
    num_inter  = int(os.getenv("INTERACTIONS_PER_LEAD", "5"))

    producer = KafkaProducer(
        bootstrap_servers=bootstrap,
        value_serializer=lambda v: json.dumps(v, ensure_ascii=False).encode("utf-8"),
        key_serializer=lambda k: k.encode("utf-8"),
        request_timeout_ms=10000,
        retries=3,
    )

    print(f"Bootstrap: {bootstrap}")
    print(f"Gerando {num_leads} leads com {num_inter} interações cada...\n")

    lead_ids = []
    for i in range(1, num_leads + 1):
        lead_id = f"GEN-{i:04d}"
        lead    = generate_lead(lead_id)
        producer.send("lead-events", key=lead_id, value=lead)
        lead_ids.append(lead_id)
        print(f"  lead-event  | {lead_id} | {lead['name']} | {lead['role']} @ {lead['company']}")
        time.sleep(0.03)

    producer.flush()
    print(f"\n✓ {num_leads} leads publicados em 'lead-events'\n")

    for lead_id in lead_ids:
        for _ in range(num_inter):
            interaction = generate_interaction(lead_id)
            producer.send("interaction-events", key=lead_id, value=interaction)
        time.sleep(0.02)

    producer.flush()
    print(f"✓ {num_leads * num_inter} interações publicadas em 'interaction-events'\n")

    # Gera alguns CRM updates aleatórios
    updates = random.sample(lead_ids, k=min(10, len(lead_ids)))
    for lead_id in updates:
        update = generate_crm_update(lead_id)
        producer.send("crm-updates", key=lead_id, value=update)
        print(f"  crm-update  | {lead_id} -> {update['stage']}")

    producer.flush()
    producer.close()
    print(f"\n✓ {len(updates)} CRM updates publicados em 'crm-updates'")
    print("\nConcluído.")


if __name__ == "__main__":
    main()
