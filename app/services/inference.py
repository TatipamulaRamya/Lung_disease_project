from __future__ import annotations

from dataclasses import dataclass

from app.services.audio import AudioFeatureSummary, build_explanation_regions


@dataclass
class InferenceResult:
    model_used: str
    predicted_condition: str
    confidence: float
    severity: str
    urgent_alert: bool
    explanation: list[dict[str, object]]
    differentials: list[str]
    recommendations: list[str]


class RespiratoryModelService:
    """
    Heuristic baseline that keeps the full product flow working until a trained
    CNN/ResNet/VGG/Inception/CNN-LSTM model is wired in.
    """

    def predict(self, features: AudioFeatureSummary) -> InferenceResult:
        pneumonia_score = 0.35
        asthma_score = 0.35
        copd_score = 0.35

        if features.spectral_centroid < 1200:
            pneumonia_score += 0.25
        if features.zero_crossing_rate > 0.08:
            asthma_score += 0.25
        if 0.03 <= features.zero_crossing_rate <= 0.08:
            copd_score += 0.2
        if features.rms_energy < 0.04:
            pneumonia_score += 0.15
        if features.duration_seconds > 12:
            copd_score += 0.1
        if features.extraction_backend != "librosa":
            asthma_score -= 0.05
            pneumonia_score -= 0.05
            copd_score -= 0.05

        scores = {
            "Pneumonia": pneumonia_score,
            "Asthma": asthma_score,
            "COPD": copd_score,
        }
        predicted_condition = max(scores, key=scores.get)
        confidence = min(max(scores[predicted_condition], 0.45), 0.93)
        severity = self._severity_for(confidence)
        urgent_alert = severity == "High"

        differentials = [label for label, _ in sorted(scores.items(), key=lambda item: item[1], reverse=True) if label != predicted_condition]
        recommendations = self._recommendations_for(predicted_condition, urgent_alert)

        return InferenceResult(
            model_used="Baseline respiratory classifier (swap with CNN / ResNet-50 / VGG16 / Inception / CNN-LSTM)",
            predicted_condition=predicted_condition,
            confidence=round(confidence, 2),
            severity=severity,
            urgent_alert=urgent_alert,
            explanation=build_explanation_regions(features.duration_seconds, predicted_condition),
            differentials=differentials,
            recommendations=recommendations,
        )

    @staticmethod
    def _severity_for(confidence: float) -> str:
        if confidence >= 0.8:
            return "High"
        if confidence >= 0.65:
            return "Moderate"
        return "Low"

    @staticmethod
    def _recommendations_for(predicted_condition: str, urgent_alert: bool) -> list[str]:
        recommendations = [
            "Repeat the recording in a quiet environment to improve signal quality.",
            "Review the AI result alongside patient symptoms, pulse oximetry, and temperature.",
        ]
        if predicted_condition == "Asthma":
            recommendations.append("Check for wheeze triggers and compare with bronchodilator response if available.")
        elif predicted_condition == "Pneumonia":
            recommendations.append("Correlate with fever, cough history, and imaging when clinically appropriate.")
        else:
            recommendations.append("Compare with smoking history and chronic breathlessness for COPD risk review.")

        if urgent_alert:
            recommendations.append("Flag for urgent clinician assessment because the estimated severity is high.")

        return recommendations
