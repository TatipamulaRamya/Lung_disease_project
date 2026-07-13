package com.respiratoryai.mobile.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.respiratoryai.mobile.BuildConfig
import com.respiratoryai.mobile.data.model.PredictionResponse
import com.respiratoryai.mobile.data.model.SeverityResponse
import com.respiratoryai.mobile.data.model.WindowPrediction
import com.respiratoryai.mobile.ui.theme.Coral
import com.respiratoryai.mobile.ui.theme.DeepTeal
import com.respiratoryai.mobile.ui.theme.ElectricBlue
import com.respiratoryai.mobile.ui.theme.InkBlue
import com.respiratoryai.mobile.ui.theme.Mint
import com.respiratoryai.mobile.ui.theme.NightBlue
import com.respiratoryai.mobile.ui.theme.RespiratoryAiTheme
import com.respiratoryai.mobile.ui.theme.RoyalBlue
import com.respiratoryai.mobile.ui.theme.SlateBlue
import com.respiratoryai.mobile.ui.theme.SurfaceBlue

@Composable
fun RespiratoryAiApp(
    uiState: AppUiState,
    onPickAudio: () -> Unit,
    onRecordClick: () -> Unit,
    onStopRecording: () -> Unit,
    onAnalyze: () -> Unit,
    onClearResult: () -> Unit,
    onDownloadReport: () -> Unit,
    onBackendUrlChange: (String) -> Unit,
    onSaveBackendUrl: () -> Unit,
    onResetBackendUrl: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        ) {
            DecorativeBackdrop()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AppChromeHeader()

                DiagnosticHeroCard(
                    isRecording = uiState.isRecording,
                    isAnalyzing = uiState.isAnalyzing,
                )

                CaptureStudioCard(
                    uiState = uiState,
                    onPickAudio = onPickAudio,
                    onRecordClick = onRecordClick,
                    onStopRecording = onStopRecording,
                    onAnalyze = onAnalyze,
                )

                if (BuildConfig.SHOW_CONNECTION_SETTINGS) {
                    ConnectionCard(
                        uiState = uiState,
                        onBackendUrlChange = onBackendUrlChange,
                        onSaveBackendUrl = onSaveBackendUrl,
                        onResetBackendUrl = onResetBackendUrl,
                    )
                }

                uiState.errorMessage?.let {
                    InfoCard(
                        title = "Needs attention",
                        body = it,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    )
                }

                if (uiState.result == null) {
                    EmptyStateCard(uiState = uiState)
                }

                uiState.result?.let { result ->
                    ResultOverviewCard(
                        result = result,
                        onClearResult = onClearResult,
                        onDownloadReport = onDownloadReport,
                    )
                    ProbabilityCard(result = result)
                    WindowInsightsCard(result = result)
                    HeatmapCard(result = result)
                }
            }
        }
    }
}

@Composable
private fun BoxScope.DecorativeBackdrop() {
    Box(
        modifier = Modifier
            .padding(top = 20.dp, end = 0.dp)
            .align(Alignment.TopEnd)
            .clip(CircleShape)
            .background(ElectricBlue.copy(alpha = 0.10f))
            .fillMaxWidth(0.52f)
            .height(250.dp),
    )
    Box(
        modifier = Modifier
            .padding(top = 360.dp)
            .offset(x = (-30).dp)
            .align(Alignment.TopStart)
            .clip(CircleShape)
            .background(Mint.copy(alpha = 0.18f))
            .fillMaxWidth(0.36f)
            .height(160.dp),
    )
}

@Composable
private fun AppChromeHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(RoyalBlue, ElectricBlue),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "RA",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "RESPIRATORY AI",
                    style = MaterialTheme.typography.labelLarge,
                    color = NightBlue,
                )
                Text(
                    text = "Respiratory Diagnostic Suite",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateBlue,
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "SESSION",
                style = MaterialTheme.typography.bodySmall,
                color = SlateBlue,
            )
            StatusPill(
                text = "ANDROID",
                background = MaterialTheme.colorScheme.secondaryContainer,
                textColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun DiagnosticHeroCard(
    isRecording: Boolean,
    isAnalyzing: Boolean,
) {
    val statusText = when {
        isRecording -> "Live recording"
        isAnalyzing -> "AI analysis in progress"
        else -> "AI-powered respiratory screening"
    }

    Card(
        shape = RoundedCornerShape(34.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(RoyalBlue, ElectricBlue, SurfaceBlue),
                    ),
                )
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(
                    text = statusText,
                    background = Color.White.copy(alpha = 0.12f),
                    textColor = Color.White.copy(alpha = 0.92f),
                )
                StatusPill(
                    text = "v0.1",
                    background = Color.White.copy(alpha = 0.08f),
                    textColor = Color.White.copy(alpha = 0.88f),
                )
            }

            Text(
                text = "Breath Sound\nAnalysis",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
            )

            Text(
                text = "Capture a breathing sound and send it to the AI backend for a quick respiratory screening summary.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.78f),
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
            ) {
                Column(
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(24.dp),
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "About Respiratory AI",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                    Text(
                        text = "Record or import a breathing sample, run the AI check, and review a clear summary on the device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.82f),
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.14f))

                    StepLine("1", "Record or import a breathing sample")
                    StepLine("2", "Tap Analyze to run the check")
                    StepLine("3", "Review the screening summary")
                }
            }
        }
    }
}

@Composable
private fun CaptureStudioCard(
    uiState: AppUiState,
    onPickAudio: () -> Unit,
    onRecordClick: () -> Unit,
    onStopRecording: () -> Unit,
    onAnalyze: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = "01",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "AUDIO CAPTURE",
                        style = MaterialTheme.typography.labelLarge,
                        color = NightBlue,
                    )
                    Text(
                        text = "Record or import a lung sound sample",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateBlue,
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = when {
                            uiState.isRecording -> "Recording live now"
                            uiState.selectedFileName != null -> uiState.selectedFileName
                            else -> "Spectral display activates after capture"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = NightBlue.copy(alpha = 0.78f),
                    )
                    Text(
                        text = uiState.infoMessage ?: "Capture will appear here once ready.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = SlateBlue,
                    )
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onPickAudio,
                    enabled = !uiState.isRecording && !uiState.isAnalyzing,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = NightBlue,
                    ),
                ) {
                    Text("Pick file")
                }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = if (uiState.isRecording) onStopRecording else onRecordClick,
                    enabled = !uiState.isAnalyzing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NightBlue,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(if (uiState.isRecording) "Stop" else "Record")
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onAnalyze,
                enabled = uiState.canAnalyze && !uiState.isRecording && !uiState.isAnalyzing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalBlue,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    disabledContentColor = SlateBlue,
                ),
            ) {
                Text(
                    if (uiState.isAnalyzing) {
                        "Running diagnosis..."
                    } else {
                        "Analyze"
                    },
                )
            }

            if (uiState.isAnalyzing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Text(
                text = "Tip: record in a quiet place and keep the phone close while capturing the sound.",
                style = MaterialTheme.typography.bodyMedium,
                color = SlateBlue,
            )
        }
    }
}

@Composable
private fun EmptyStateCard(
    uiState: AppUiState,
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (uiState.isRecording) "Recording in progress" else "Results dashboard",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "After analysis, the screening summary, confidence, and audio highlight will appear here.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConnectionCard(
    uiState: AppUiState,
    onBackendUrlChange: (String) -> Unit,
    onSaveBackendUrl: () -> Unit,
    onResetBackendUrl: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Connection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "You can change the backend URL here if the testing link changes later.",
                style = MaterialTheme.typography.bodyMedium,
                color = SlateBlue,
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.backendUrlInput,
                onValueChange = onBackendUrlChange,
                label = { Text("Backend URL") },
                supportingText = { Text("Current: ${uiState.backendUrl}") },
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onSaveBackendUrl,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalBlue,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Save URL")
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onResetBackendUrl,
                ) {
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
private fun StepLine(
    index: String,
    text: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = index,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.84f),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.82f),
        )
    }
}

@Composable
private fun ResultOverviewCard(
    result: PredictionResponse,
    onClearResult: () -> Unit,
    onDownloadReport: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = DeepTeal),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = result.prediction,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = result.filename,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.78f),
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onDownloadReport,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = NightBlue,
                        ),
                    ) {
                        Text("Download report")
                    }
                    OutlinedButton(onClick = onClearResult) {
                        Text("Clear")
                    }
                }
            }

            Text(
                text = "${"%.2f".format(result.confidence)}% confidence",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )

            LinearProgressIndicator(
                progress = { (result.confidence / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = Mint,
                trackColor = Color.White.copy(alpha = 0.18f),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SummaryTile(
                    modifier = Modifier.weight(1f),
                    label = "Severity",
                    value = result.severity.level,
                    containerColor = Color.White.copy(alpha = 0.12f),
                    labelColor = Color.White.copy(alpha = 0.72f),
                    valueColor = Color.White,
                )
                SummaryTile(
                    modifier = Modifier.weight(1f),
                    label = "Windows",
                    value = (result.windowsUsed ?: 1).toString(),
                    containerColor = Color.White.copy(alpha = 0.12f),
                    labelColor = Color.White.copy(alpha = 0.72f),
                    valueColor = Color.White,
                )
                SummaryTile(
                    modifier = Modifier.weight(1f),
                    label = "Aggregation",
                    value = result.aggregation ?: "mean",
                    containerColor = Color.White.copy(alpha = 0.12f),
                    labelColor = Color.White.copy(alpha = 0.72f),
                    valueColor = Color.White,
                )
            }

            InfoCard(
                title = "${result.severity.level} severity",
                body = result.severity.message,
                containerColor = Color.White.copy(alpha = 0.12f),
                titleColor = Color.White,
                bodyColor = Color.White.copy(alpha = 0.86f),
            )
        }
    }
}

@Composable
private fun ProbabilityCard(
    result: PredictionResponse,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "More details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            result.probabilities.entries
                .sortedByDescending { it.value }
                .forEachIndexed { index, entry ->
                    ProbabilityRow(
                        label = entry.key,
                        value = entry.value,
                        highlight = index == 0,
                    )
                    if (index < result.probabilities.size - 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                    }
                }
        }
    }
}

@Composable
private fun WindowInsightsCard(
    result: PredictionResponse,
) {
    val windows = result.windowPredictions
    if (windows.isEmpty()) {
        return
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Recording timeline",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            windows.forEachIndexed { index, window ->
                WindowRow(window = window)
                if (index < windows.size - 1) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                }
            }
        }
    }
}

@Composable
private fun HeatmapCard(
    result: PredictionResponse,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Audio highlight",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "This image shows the part of the recording that stood out the most.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HeatmapImage(
                dataUrl = result.heatmap,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ProbabilityRow(
    label: String,
    value: Double,
    highlight: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Medium,
            )
            Text(
                text = "${"%.2f".format(value)}%",
                style = MaterialTheme.typography.bodyLarge,
                color = if (highlight) SurfaceBlue else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }
        LinearProgressIndicator(
            progress = { (value / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = if (highlight) DeepTeal else MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.secondaryContainer,
        )
    }
}

@Composable
private fun WindowRow(
    window: WindowPrediction,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = window.prediction,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Window ${window.windowIndex + 1} · ${formatWindow(window)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusPill(
                text = "${"%.1f".format(window.confidence)}%",
                background = MaterialTheme.colorScheme.secondaryContainer,
                textColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun SummaryTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    labelColor: Color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f),
    valueColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(containerColor)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = labelColor,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = valueColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun StatusPill(
    text: String,
    background: Color,
    textColor: Color,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun InfoCard(
    title: String,
    body: String,
    containerColor: Color,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    bodyColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = bodyColor,
            )
        }
    }
}

@Composable
private fun HeatmapImage(
    dataUrl: String,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(dataUrl) {
        decodeDataUrl(dataUrl)
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Prediction heatmap",
            modifier = modifier
                .clip(RoundedCornerShape(20.dp)),
        )
    } else {
        Box(
            modifier = modifier
                .height(210.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Preview image not available",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun decodeDataUrl(dataUrl: String): android.graphics.Bitmap? {
    val base64Part = dataUrl.substringAfter("base64,", "")
    if (base64Part.isBlank()) {
        return null
    }
    val bytes = Base64.decode(base64Part, Base64.DEFAULT)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

private fun formatWindow(window: WindowPrediction?): String {
    if (window == null) {
        return "Single clip"
    }
    return "${"%.2f".format(window.startSec)}s - ${"%.2f".format(window.startSec + window.durationSec)}s"
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F7FA)
@Composable
private fun RespiratoryAiPreview() {
        RespiratoryAiTheme {
            RespiratoryAiApp(
                uiState = AppUiState(
                    selectedFileName = "breathing_sample.wav",
                    canAnalyze = true,
                    infoMessage = "Recording saved. You can analyze it now.",
                    backendUrl = "https://demo.example.com/",
                    backendUrlInput = "https://demo.example.com/",
                    result = PredictionResponse(
                    filename = "breathing_sample.wav",
                    prediction = "COPD",
                    confidence = 92.14,
                    probabilities = mapOf(
                        "COPD" to 92.14,
                        "Asthma" to 5.32,
                        "Pneumonia" to 1.21,
                        "Normal" to 1.33,
                    ),
                    severity = SeverityResponse(
                        level = "High",
                        message = "Chronic obstructive pattern detected; specialist evaluation recommended.",
                    ),
                    heatmap = "",
                    windowsUsed = 4,
                    aggregation = "mean_probability",
                    representativeWindow = WindowPrediction(
                        windowIndex = 2,
                        startSec = 6.0,
                        durationSec = 5.0,
                        prediction = "COPD",
                        confidence = 93.1,
                    ),
                    windowPredictions = listOf(
                        WindowPrediction(
                            windowIndex = 0,
                            startSec = 0.0,
                            durationSec = 5.0,
                            prediction = "Asthma",
                            confidence = 61.4,
                        ),
                        WindowPrediction(
                            windowIndex = 1,
                            startSec = 3.0,
                            durationSec = 5.0,
                            prediction = "COPD",
                            confidence = 81.7,
                        ),
                        WindowPrediction(
                            windowIndex = 2,
                            startSec = 6.0,
                            durationSec = 5.0,
                            prediction = "COPD",
                            confidence = 93.1,
                        ),
                    ),
                ),
            ),
            onPickAudio = {},
            onRecordClick = {},
            onStopRecording = {},
            onAnalyze = {},
            onClearResult = {},
            onDownloadReport = {},
            onBackendUrlChange = {},
            onSaveBackendUrl = {},
            onResetBackendUrl = {},
        )
    }
}
