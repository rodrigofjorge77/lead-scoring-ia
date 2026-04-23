def build_prompt(features: dict, score: int, label: str, top_features: list[str]) -> str:
    demo = "sim" if features.get("demoWatched") else "não"
    emails_abertos = features.get("emailOpenScore", 0) // 10
    top_str = ", ".join(top_features)

    return (
        "Você é um assistente de vendas B2B.\n"
        f"Com base nas informações abaixo, gere um resumo de 2-3 linhas em português "
        f"para um vendedor, explicando por que este lead tem score {score}/100.\n"
        "Seja direto e mencione os pontos mais relevantes para conversão.\n\n"
        f"Lead: {features.get('name')}, {features.get('role')} "
        f"em {features.get('company')} ({features.get('employees')} funcionários)\n"
        f"Segmento: {features.get('segment')} | Origem: {features.get('source')}\n"
        f"Comportamento: visitou preços {features.get('pricingPageVisits', 0)}x, "
        f"abriu {emails_abertos} e-mails, assistiu demo: {demo}\n"
        f"Estágio CRM: {features.get('crmStage', 'NOVO')} | Label: {label}\n"
        f"Top features: {top_str}\n\n"
        "Resumo (2-3 linhas, em português):"
    )
