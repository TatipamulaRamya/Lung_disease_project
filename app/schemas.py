from typing import Any

from pydantic import BaseModel, Field


class AnalysisResponse(BaseModel):
    file_name: str
    model_used: str
    predicted_condition: str
    confidence: float = Field(ge=0.0, le=1.0)
    severity: str
    urgent_alert: bool
    explanation: list[dict[str, Any]]
    feature_summary: dict[str, Any]
    recommendations: list[str]
    report: str
    differentials: list[str]
