"""
Treina o modelo XGBoost com dados sintéticos.
Em produção, substituir generate_synthetic_data() por dados reais de conversão do DuckDB.
"""
import numpy as np
import joblib
from pathlib import Path
from xgboost import XGBClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import roc_auc_score


def generate_synthetic_data(n: int = 1000) -> tuple[np.ndarray, np.ndarray]:
    rng = np.random.default_rng(42)

    role_scores         = rng.choice([30, 70, 85, 90, 95], size=n, p=[0.30, 0.20, 0.10, 0.20, 0.20])
    company_size_scores = rng.choice([20, 50, 80, 90],     size=n, p=[0.20, 0.30, 0.30, 0.20])
    segment_fit_scores  = rng.choice([50, 60, 80, 90],     size=n, p=[0.20, 0.30, 0.30, 0.20])
    source_scores       = rng.choice([50, 60, 70, 90],     size=n, p=[0.30, 0.20, 0.30, 0.20])
    email_open_scores   = rng.integers(0, 101, size=n).astype(float)
    pricing_visits      = rng.integers(0, 11,  size=n).astype(float)
    demo_watched        = rng.choice([0, 1],   size=n, p=[0.70, 0.30]).astype(float)
    days_since_contact  = rng.exponential(7,   size=n)
    crm_stage_scores    = rng.choice([10, 40, 80, 95, 100], size=n, p=[0.30, 0.20, 0.20, 0.20, 0.10])

    X = np.column_stack([
        role_scores, company_size_scores, segment_fit_scores, source_scores,
        email_open_scores, pricing_visits, demo_watched, days_since_contact, crm_stage_scores,
    ])

    # label gerado por regra determinística + ruído
    latent = (
        role_scores * 0.20
        + company_size_scores * 0.10
        + segment_fit_scores * 0.10
        + source_scores * 0.05
        + email_open_scores * 0.10
        + pricing_visits * 5.0
        + demo_watched * 20.0
        + np.maximum(0, 10 - days_since_contact) * 2.0
        + crm_stage_scores * 0.25
    )
    prob = 1 / (1 + np.exp(-(latent - 60) / 15))
    y = (rng.random(n) < prob).astype(int)

    return X, y


def train(save_path: str = "model/xgboost_model.pkl") -> None:
    X, y = generate_synthetic_data(1000)
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

    model = XGBClassifier(
        n_estimators=100,
        max_depth=4,
        learning_rate=0.1,
        random_state=42,
        eval_metric="logloss",
        verbosity=0,
    )
    model.fit(X_train, y_train)

    auc = roc_auc_score(y_test, model.predict_proba(X_test)[:, 1])
    print(f"Modelo treinado — AUC: {auc:.3f} | conversões: {y.sum()}/{len(y)}")

    Path(save_path).parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(model, save_path)
    print(f"Modelo salvo em: {save_path}")


if __name__ == "__main__":
    train()
