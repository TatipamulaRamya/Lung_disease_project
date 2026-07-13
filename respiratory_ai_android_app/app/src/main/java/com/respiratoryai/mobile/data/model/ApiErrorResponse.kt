package com.respiratoryai.mobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorResponse(
    val error: String? = null,
)
