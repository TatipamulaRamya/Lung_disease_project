package com.respiratoryai.mobile.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.respiratoryai.mobile.data.model.PredictionResponse
import com.respiratoryai.mobile.data.repository.PredictionRepository
import com.respiratoryai.mobile.recording.WavAudioRecorder
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val recorder = WavAudioRecorder()
    private val preferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val defaultBackendUrl = PredictionRepository.defaultBaseUrl()

    private val _uiState = MutableStateFlow(
        AppUiState(
            backendUrl = preferences.getString(KEY_BACKEND_URL, defaultBackendUrl) ?: defaultBackendUrl,
            backendUrlInput = preferences.getString(KEY_BACKEND_URL, defaultBackendUrl) ?: defaultBackendUrl,
        ),
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var selectedAudioFile: File? = null

    fun onBackendUrlChanged(value: String) {
        _uiState.update {
            it.copy(backendUrlInput = value)
        }
    }

    fun saveBackendUrl() {
        val rawValue = _uiState.value.backendUrlInput.trim()
        val normalizedUrl = runCatching { PredictionRepository.normalizeBaseUrl(rawValue) }.getOrNull()

        if (normalizedUrl.isNullOrBlank() || !(normalizedUrl.startsWith("http://") || normalizedUrl.startsWith("https://"))) {
            _uiState.update {
                it.copy(
                    errorMessage = "Enter a valid backend URL starting with http:// or https://",
                )
            }
            return
        }

        preferences.edit().putString(KEY_BACKEND_URL, normalizedUrl).apply()
        _uiState.update {
            it.copy(
                backendUrl = normalizedUrl,
                backendUrlInput = normalizedUrl,
                infoMessage = "Backend URL updated to $normalizedUrl",
                errorMessage = null,
            )
        }
    }

    fun resetBackendUrl() {
        preferences.edit().putString(KEY_BACKEND_URL, defaultBackendUrl).apply()
        _uiState.update {
            it.copy(
                backendUrl = defaultBackendUrl,
                backendUrlInput = defaultBackendUrl,
                infoMessage = "Reset to the packaged backend URL.",
                errorMessage = null,
            )
        }
    }

    fun onAudioPicked(
        uri: Uri,
        displayName: String?,
        mimeType: String?,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val file = copyUriToCache(uri, displayName, mimeType)
                selectedAudioFile = file
                _uiState.update {
                    it.copy(
                        selectedFileName = file.name,
                        canAnalyze = true,
                        infoMessage = "File selected: ${file.name}",
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        errorMessage = throwable.message ?: "Unable to open that audio file.",
                    )
                }
            }
        }
    }

    fun startRecording() {
        if (_uiState.value.isRecording) {
            return
        }

        val outputFile = File(
            getApplication<Application>().cacheDir,
            "resp_recording_${timestamp()}.wav",
        )

        val started = recorder.start(outputFile)
        if (started) {
            selectedAudioFile = null
            _uiState.update {
                it.copy(
                    isRecording = true,
                    canAnalyze = false,
                    selectedFileName = null,
                    result = null,
                    infoMessage = "Recording... tap Stop when you're done.",
                    errorMessage = null,
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    errorMessage = "Couldn't start recording on this device.",
                )
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch(Dispatchers.IO) {
            val file = recorder.stop()
            if (file != null && file.exists() && file.length() > 44L) {
                selectedAudioFile = file
                _uiState.update {
                    it.copy(
                        isRecording = false,
                        selectedFileName = file.name,
                        canAnalyze = true,
                        infoMessage = "Recording saved. You can analyze it now.",
                        errorMessage = null,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isRecording = false,
                        canAnalyze = false,
                        errorMessage = "Recording finished, but the audio could not be used.",
                    )
                }
            }
        }
    }

    fun analyzeSelectedAudio() {
        val file = selectedAudioFile ?: return
        val backendUrl = _uiState.value.backendUrl

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isAnalyzing = true,
                    result = null,
                    infoMessage = "Checking your audio...",
                    errorMessage = null,
                )
            }

            runCatching { PredictionRepository.create(backendUrl).predict(file) }
                .onSuccess { result ->
                    handlePredictionSuccess(result)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            errorMessage = throwable.message ?: "Something went wrong while checking the audio.",
                        )
                    }
                }
        }
    }

    fun onMicrophonePermissionDenied() {
        _uiState.update {
            it.copy(
                errorMessage = "Please allow microphone access to record audio.",
            )
        }
    }

    fun clearResult() {
        _uiState.update {
            it.copy(
                result = null,
                infoMessage = "You can record or upload another sample.",
                errorMessage = null,
            )
        }
    }

    fun onReportSaved(fileName: String) {
        _uiState.update {
            it.copy(
                errorMessage = null,
                infoMessage = "Report saved as $fileName",
            )
        }
    }

    fun onActionError(message: String) {
        _uiState.update {
            it.copy(
                errorMessage = message,
            )
        }
    }

    override fun onCleared() {
        recorder.release()
        super.onCleared()
    }

    private fun handlePredictionSuccess(result: PredictionResponse) {
        _uiState.update {
            it.copy(
                isAnalyzing = false,
                result = result,
                infoMessage = "Your result is ready.",
                errorMessage = null,
            )
        }
    }

    private fun copyUriToCache(
        uri: Uri,
        displayName: String?,
        mimeType: String?,
    ): File {
        val app = getApplication<Application>()
        val ext = inferExtension(displayName, mimeType)
        val target = File(app.cacheDir, "picked_${timestamp()}.$ext")
        app.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IOException("Could not read the selected audio file.")
        return target
    }

    private fun inferExtension(
        displayName: String?,
        mimeType: String?,
    ): String {
        val extensionFromName = displayName
            ?.substringAfterLast('.', "")
            ?.trim()
            ?.lowercase(Locale.US)
            .orEmpty()

        if (extensionFromName.isNotBlank()) {
            return normalizeExtension(extensionFromName)
        }

        val extensionFromMime = mimeType
            ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            ?.trim()
            ?.lowercase(Locale.US)
            .orEmpty()

        return normalizeExtension(extensionFromMime.ifBlank { "wav" })
    }

    private fun normalizeExtension(extension: String): String {
        return when (extension.removePrefix(".")) {
            "x-wav", "wav" -> "wav"
            "mpeg", "mpga", "mp3" -> "mp3"
            "aac", "x-aac" -> "aac"
            "3gpp", "3gp" -> "3gp"
            "m4a", "mp4a-latm" -> "m4a"
            "mp4" -> "mp4"
            "ogg" -> "ogg"
            "flac" -> "flac"
            "webm" -> "webm"
            else -> extension.removePrefix(".")
        }
    }

    private fun timestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    }

    private companion object {
        const val PREFS_NAME = "respiratory_ai_settings"
        const val KEY_BACKEND_URL = "backend_url"
    }
}
