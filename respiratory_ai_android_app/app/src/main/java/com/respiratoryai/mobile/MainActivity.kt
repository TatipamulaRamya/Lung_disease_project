package com.respiratoryai.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewmodel.compose.viewModel
import com.respiratoryai.mobile.reporting.PredictionReportExporter
import com.respiratoryai.mobile.ui.MainViewModel
import com.respiratoryai.mobile.ui.RespiratoryAiApp
import com.respiratoryai.mobile.ui.theme.RespiratoryAiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RespiratoryAiTheme {
                val viewModel: MainViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()
                val context = LocalContext.current
                var pendingReportBytes by remember { mutableStateOf<ByteArray?>(null) }
                var pendingReportName by remember { mutableStateOf("respiratory_report.pdf") }

                val requestPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    if (granted) {
                        viewModel.startRecording()
                    } else {
                        viewModel.onMicrophonePermissionDenied()
                    }
                }

                val pickAudioLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument(),
                ) { uri ->
                    if (uri != null) {
                        runCatching {
                            context.contentResolver.takePersistableUriPermission(
                                uri,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                            )
                        }
                        val document = DocumentFile.fromSingleUri(context, uri)
                        viewModel.onAudioPicked(
                            uri = uri,
                            displayName = document?.name,
                            mimeType = context.contentResolver.getType(uri) ?: document?.type,
                        )
                    }
                }

                val saveReportLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("application/pdf"),
                ) { uri ->
                    val bytes = pendingReportBytes
                    if (uri != null && bytes != null) {
                        runCatching {
                            context.contentResolver.openOutputStream(uri)?.use { output ->
                                output.write(bytes)
                            } ?: error("Could not open the selected save location.")
                        }.onSuccess {
                            viewModel.onReportSaved(pendingReportName)
                        }.onFailure { throwable ->
                            viewModel.onActionError(throwable.message ?: "Couldn't save the report.")
                        }
                    }
                    pendingReportBytes = null
                }

                RespiratoryAiApp(
                    uiState = uiState,
                    onPickAudio = {
                        pickAudioLauncher.launch(arrayOf("audio/*"))
                    },
                    onRecordClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO,
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            viewModel.startRecording()
                        } else {
                            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onStopRecording = viewModel::stopRecording,
                    onAnalyze = viewModel::analyzeSelectedAudio,
                    onClearResult = viewModel::clearResult,
                    onDownloadReport = {
                        val result = uiState.result
                        if (result == null) {
                            viewModel.onActionError("Run an analysis before downloading a report.")
                        } else {
                            runCatching {
                                pendingReportBytes = PredictionReportExporter.createPdf(result)
                                pendingReportName = PredictionReportExporter.defaultFileName(result.filename)
                                saveReportLauncher.launch(pendingReportName)
                            }.onFailure { throwable ->
                                viewModel.onActionError(throwable.message ?: "Couldn't create the report.")
                            }
                        }
                    },
                    onBackendUrlChange = viewModel::onBackendUrlChanged,
                    onSaveBackendUrl = viewModel::saveBackendUrl,
                    onResetBackendUrl = viewModel::resetBackendUrl,
                )
            }
        }
    }
}
