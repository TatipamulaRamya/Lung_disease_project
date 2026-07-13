package com.respiratoryai.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PredictionResponse(
    val filename: String,
    val prediction: String,
    val confidence: Double,
    val probabilities: Map<String, Double>,
    val severity: SeverityResponse,
    val heatmap: String,
    @SerialName("windows_used")
    val windowsUsed: Int? = null,
    val aggregation: String? = null,
    @SerialName("window_overlap")
    val windowOverlap: Double? = null,
    @SerialName("representative_window")
    val representativeWindow: WindowPrediction? = null,
    @SerialName("window_predictions")
    val windowPredictions: List<WindowPrediction> = emptyList(),
)

@Serializable
data class SeverityResponse(
    val level: String,
    val message: String,
)

@Serializable
data class WindowPrediction(
    @SerialName("window_index")
    val windowIndex: Int,
    @SerialName("start_sec")
    val startSec: Double,
    @SerialName("duration_sec")
    val durationSec: Double,
    val prediction: String,
    val confidence: Double,
)
