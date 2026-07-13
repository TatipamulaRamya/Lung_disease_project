from __future__ import annotations

from app.services.audio import AudioFeatureSummary
from app.services.inference import InferenceResult


def build_diagnostic_report(file_name: str, features: AudioFeatureSummary, inference: InferenceResult) -> str:
    alert_text = "Urgent follow-up recommended." if inference.urgent_alert else "Routine clinician review recommended."

    return (
        f"Respiratory Screening Report\n"
        f"File: {file_name}\n"
        f"Model: {inference.model_used}\n"
        f"Predicted condition: {inference.predicted_condition}\n"
        f"Confidence: {inference.confidence:.0%}\n"
        f"Severity: {inference.severity}\n"
        f"Alert: {alert_text}\n\n"
        f"Audio summary\n"
        f"- Backend: {features.extraction_backend}\n"
        f"- Duration: {features.duration_seconds:.2f}s\n"
        f"- Sample rate: {features.sample_rate} Hz\n"
        f"- Spectral centroid: {features.spectral_centroid:.4f}\n"
        f"- Zero crossing rate: {features.zero_crossing_rate:.6f}\n"
        f"- RMS energy: {features.rms_energy:.6f}\n\n"
        f"Clinical interpretation\n"
        f"- Primary label: {inference.predicted_condition}\n"
        f"- Severity estimate: {inference.severity}\n"
        f"- Differential labels to review: {', '.join(inference.differentials)}\n"
        f"- Recommended next action: {inference.recommendations[-1]}\n"
    )
