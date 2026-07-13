package com.respiratoryai.mobile.data.repository

import android.os.Build
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.respiratoryai.mobile.BuildConfig
import com.respiratoryai.mobile.data.api.PredictionApi
import com.respiratoryai.mobile.data.model.ApiErrorResponse
import com.respiratoryai.mobile.data.model.PredictionResponse
import java.io.File
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit

class PredictionRepository(
    private val api: PredictionApi,
) {
    suspend fun predict(file: File): PredictionResponse {
        val requestBody = file.asRequestBody("audio/*".toMediaType())
        val multipart = MultipartBody.Part.createFormData(
            name = "file",
            filename = file.name,
            body = requestBody,
        )
        return try {
            api.predict(multipart)
        } catch (exception: HttpException) {
            throw IllegalStateException(readableHttpError(exception), exception)
        } catch (exception: SocketTimeoutException) {
            throw IllegalStateException("The analysis took too long. Please try again with a shorter or clearer recording.", exception)
        }
    }

    companion object {
        fun create(baseUrl: String = defaultBaseUrl()): PredictionRepository {
            val json = Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                // First-run inference over a public tunnel can take noticeably longer.
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .callTimeout(120, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(normalizeBaseUrl(baseUrl))
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()

            return PredictionRepository(
                api = retrofit.create(PredictionApi::class.java),
            )
        }

        fun defaultBaseUrl(): String {
            return if (isProbablyEmulator()) {
                BuildConfig.EMULATOR_API_BASE_URL
            } else {
                BuildConfig.DEVICE_API_BASE_URL
            }
        }

        fun normalizeBaseUrl(baseUrl: String): String {
            val trimmed = baseUrl.trim()
            return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        }

        private fun readableHttpError(exception: HttpException): String {
            val fallback = when (exception.code()) {
                400 -> "That recording could not be analyzed. Try a clear WAV, MP3, M4A, AAC, OGG, FLAC, 3GP, or WEBM file."
                503 -> "The analysis server is currently unavailable. Please try again in a moment."
                else -> "The analysis request failed. Please try again."
            }

            val errorBody = exception.response()?.errorBody()?.string().orEmpty()
            if (errorBody.isBlank()) {
                return fallback
            }

            return try {
                Json { ignoreUnknownKeys = true }
                    .decodeFromString<ApiErrorResponse>(errorBody)
                    .error
                    ?.takeIf { it.isNotBlank() }
                    ?: fallback
            } catch (_: SerializationException) {
                fallback
            }
        }

        private fun isProbablyEmulator(): Boolean {
            return Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
                Build.MODEL.contains("sdk_gphone", ignoreCase = true) ||
                Build.MODEL.contains("Emulator", ignoreCase = true) ||
                Build.PRODUCT.contains("sdk", ignoreCase = true) ||
                Build.BRAND.contains("generic", ignoreCase = true) ||
                Build.DEVICE.contains("generic", ignoreCase = true)
        }
    }
}
