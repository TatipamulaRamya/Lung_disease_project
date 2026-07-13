package com.respiratoryai.mobile.ui

import com.respiratoryai.mobile.data.model.PredictionResponse

data class AppUiState(
    val selectedFileName: String? = null,
    val canAnalyze: Boolean = false,
    val isRecording: Boolean = false,
    val isAnalyzing: Boolean = false,
    val infoMessage: String? = "Record audio or choose a file to get started.",
    val errorMessage: String? = null,
    val backendUrl: String = "",
    val backendUrlInput: String = "",
    val result: PredictionResponse? = null,
)
