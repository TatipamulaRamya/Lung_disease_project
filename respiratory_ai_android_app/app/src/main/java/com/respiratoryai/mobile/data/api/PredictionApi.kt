package com.respiratoryai.mobile.data.api

import com.respiratoryai.mobile.data.model.PredictionResponse
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface PredictionApi {
    @Multipart
    @POST("predict")
    suspend fun predict(
        @Part file: MultipartBody.Part,
    ): PredictionResponse
}
