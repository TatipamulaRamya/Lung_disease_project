from app.services.audio import AudioFeatureSummary
from app.services.inference import InferenceResult
from app.services.reporting import build_diagnostic_report


def test_report_contains_key_sections() -> None:
    features = AudioFeatureSummary(
        extraction_backend="librosa",
        duration_seconds=7.2,
        sample_rate=22050,
        mfcc_mean=[0.0] * 13,
        chroma_mean=[0.0] * 12,
        spectral_centroid=987.1,
        zero_crossing_rate=0.0412,
        rms_energy=0.021,
    )
    inference = InferenceResult(
        model_used="baseline",
        predicted_condition="COPD",
        confidence=0.71,
        severity="Moderate",
        urgent_alert=False,
        explanation=[],
        differentials=["Asthma", "Pneumonia"],
        recommendations=["Routine clinician review recommended."],
    )

    report = build_diagnostic_report("demo.wav", features, inference)

    assert "Respiratory Screening Report" in report
    assert "Predicted condition: COPD" in report
    assert "Severity: Moderate" in report
