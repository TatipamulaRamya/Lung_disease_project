package com.respiratoryai.mobile.reporting

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Base64
import com.respiratoryai.mobile.data.model.PredictionResponse
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object PredictionReportExporter {
    fun createPdf(result: PredictionResponse): ByteArray {
        val document = PdfDocument()
        var pageNumber = 1
        var page = document.startPage(newPageInfo(pageNumber))
        var canvas = page.canvas
        var y = MARGIN

        fun startNewPage() {
            document.finishPage(page)
            pageNumber += 1
            page = document.startPage(newPageInfo(pageNumber))
            canvas = page.canvas
            y = MARGIN
        }

        fun ensureSpace(height: Float) {
            if (y + height > PAGE_HEIGHT - MARGIN) {
                startNewPage()
            }
        }

        fun drawBlock(
            text: String,
            paint: Paint,
            topPadding: Float = 0f,
            bottomPadding: Float = 0f,
        ) {
            val lines = wrapText(text, paint, PAGE_WIDTH - (MARGIN * 2))
            val lineHeight = paint.textSize + LINE_SPACING
            ensureSpace(topPadding + (lines.size * lineHeight) + bottomPadding)
            y += topPadding
            lines.forEach { line ->
                canvas.drawText(line, MARGIN, y, paint)
                y += lineHeight
            }
            y += bottomPadding
        }

        val recommendation = recommendationFor(result.prediction, result.confidence)

        drawBlock("Respiratory AI Screening Report", TITLE_PAINT)
        y += 10f
        drawBlock("Generated: ${timestamp()}", BODY_PAINT)
        drawBlock("Sample: ${result.filename}", BODY_PAINT, bottomPadding = 10f)

        drawBlock("Summary", SECTION_PAINT, topPadding = 6f)
        drawBlock("Primary result: ${result.prediction}", EMPHASIS_PAINT)
        drawBlock("Confidence: ${"%.2f".format(result.confidence)}%", BODY_PAINT)
        drawBlock("Severity: ${result.severity.level}", BODY_PAINT)
        drawBlock(result.severity.message, BODY_PAINT)
        result.representativeWindow?.let { window ->
            drawBlock(
                "Representative window: ${"%.2f".format(window.startSec)}s to ${"%.2f".format(window.startSec + window.durationSec)}s",
                BODY_PAINT,
            )
        }
        y += 10f

        drawBlock("Class probabilities", SECTION_PAINT, topPadding = 6f)
        result.probabilities.entries
            .sortedByDescending { it.value }
            .forEach { entry ->
                drawBlock("${entry.key}: ${"%.2f".format(entry.value)}%", BODY_PAINT)
            }
        y += 10f

        drawBlock("Recommendations", SECTION_PAINT, topPadding = 6f)
        drawBlock(recommendation.summary, EMPHASIS_PAINT)
        recommendation.steps.forEach { step ->
            drawBlock("- $step", BODY_PAINT)
        }
        y += 10f

        drawBlock("General guidance", SECTION_PAINT, topPadding = 6f)
        generalRecommendations().forEach { step ->
            drawBlock("- $step", BODY_PAINT)
        }

        val heatmap = decodeDataUrl(result.heatmap)
        if (heatmap != null) {
            val heatmapIntro = "The image below shows the part of the recording that stood out the most during model analysis."
            val introLines = wrapText(heatmapIntro, BODY_PAINT, PAGE_WIDTH - (MARGIN * 2))
            val introHeight = SECTION_PAINT.textSize + LINE_SPACING + (introLines.size * (BODY_PAINT.textSize + LINE_SPACING))
            val minImageHeight = 120f
            if (y + 12f + introHeight + minImageHeight > PAGE_HEIGHT - MARGIN) {
                startNewPage()
            } else {
                y += 12f
            }

            drawBlock("Audio highlight", SECTION_PAINT, topPadding = 6f)
            drawBlock(heatmapIntro, BODY_PAINT)

            val availableWidth = PAGE_WIDTH - (MARGIN * 2)
            val availableHeight = max(PAGE_HEIGHT - y - MARGIN, 120f)
            val scale = min(availableWidth / heatmap.width, availableHeight / heatmap.height)
            val targetWidth = heatmap.width * scale
            val targetHeight = heatmap.height * scale
            ensureSpace(targetHeight)
            canvas.drawBitmap(
                heatmap,
                null,
                android.graphics.RectF(
                    MARGIN,
                    y,
                    MARGIN + targetWidth,
                    y + targetHeight,
                ),
                IMAGE_PAINT,
            )
            y += targetHeight
        }

        document.finishPage(page)

        return ByteArrayOutputStream().use { output ->
            document.writeTo(output)
            document.close()
            output.toByteArray()
        }
    }

    fun defaultFileName(sourceName: String): String {
        val sanitized = sourceName
            .substringBeforeLast('.')
            .replace(Regex("[^A-Za-z0-9_-]+"), "_")
            .trim('_')
            .ifBlank { "respiratory_sample" }
        return "${sanitized}_report.pdf"
    }

    private fun drawSectionTitle(
        canvas: android.graphics.Canvas,
        text: String,
        top: Float,
    ): Float {
        return drawWrappedText(canvas, text, SECTION_PAINT, top + 6f)
    }

    private fun drawWrappedText(
        canvas: android.graphics.Canvas,
        text: String,
        paint: Paint,
        top: Float,
    ): Float {
        var y = top
        wrapText(text, paint, PAGE_WIDTH - (MARGIN * 2)).forEach { line ->
            canvas.drawText(line, MARGIN, y, paint)
            y += paint.textSize + LINE_SPACING
        }
        return y
    }

    private fun wrapText(
        text: String,
        paint: Paint,
        maxWidth: Float,
    ): List<String> {
        if (text.isBlank()) {
            return listOf("")
        }

        val lines = mutableListOf<String>()
        val paragraphs = text.split('\n')
        paragraphs.forEach { paragraph ->
            val words = paragraph.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.isEmpty()) {
                lines += ""
            } else {
                var current = words.first()
                words.drop(1).forEach { word ->
                    val candidate = "$current $word"
                    if (paint.measureText(candidate) <= maxWidth) {
                        current = candidate
                    } else {
                        lines += current
                        current = word
                    }
                }
                lines += current
            }
        }
        return lines
    }

    private fun decodeDataUrl(dataUrl: String): Bitmap? {
        val base64Part = dataUrl.substringAfter("base64,", "")
        if (base64Part.isBlank()) {
            return null
        }
        val bytes = Base64.decode(base64Part, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun timestamp(): String {
        return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
    }

    private fun newPageInfo(pageNumber: Int): PdfDocument.PageInfo {
        return PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
    }

    private fun recommendationFor(
        prediction: String,
        confidence: Double,
    ): RecommendationContent {
        val normalizedPrediction = prediction.trim().lowercase(Locale.US)
        val severityBand = when {
            confidence <= 50.0 -> "low"
            confidence <= 80.0 -> "moderate"
            else -> "high"
        }

        return when (normalizedPrediction) {
            "normal" -> when (severityBand) {
                "low" -> RecommendationContent(
                    summary = "Breathing appears normal, but confidence is low.",
                    steps = listOf(
                        "Try recording again in a quiet environment.",
                        "Ensure proper microphone placement.",
                    ),
                )
                "moderate" -> RecommendationContent(
                    summary = "No abnormal lung patterns detected.",
                    steps = listOf(
                        "Maintain a healthy lifestyle.",
                        "Avoid pollution and smoking exposure.",
                    ),
                )
                else -> RecommendationContent(
                    summary = "Breathing is normal with high confidence.",
                    steps = listOf(
                        "Continue regular health monitoring.",
                        "No immediate medical action required.",
                    ),
                )
            }
            "asthma" -> when (severityBand) {
                "low" -> RecommendationContent(
                    summary = "Possible mild asthma symptoms detected.",
                    steps = listOf(
                        "Re-record audio and monitor symptoms.",
                        "Avoid allergens like dust, smoke, and cold air.",
                    ),
                )
                "moderate" -> RecommendationContent(
                    summary = "Signs of asthma detected.",
                    steps = listOf(
                        "Use prescribed inhalers if available.",
                        "Consult a doctor if symptoms persist.",
                    ),
                )
                else -> RecommendationContent(
                    summary = "Strong indication of asthma.",
                    steps = listOf(
                        "Immediate medical consultation recommended.",
                        "Avoid physical exertion and triggers.",
                    ),
                )
            }
            "copd" -> when (severityBand) {
                "low" -> RecommendationContent(
                    summary = "Early signs of COPD may be present.",
                    steps = listOf(
                        "Avoid smoking and polluted environments.",
                        "Monitor breathing regularly.",
                    ),
                )
                "moderate" -> RecommendationContent(
                    summary = "COPD symptoms detected.",
                    steps = listOf(
                        "Seek medical advice for proper diagnosis.",
                        "Follow breathing exercises and medication if prescribed.",
                    ),
                )
                else -> RecommendationContent(
                    summary = "Severe COPD symptoms detected.",
                    steps = listOf(
                        "Immediate medical attention required.",
                        "Oxygen support may be needed in critical cases.",
                    ),
                )
            }
            "pneumonia" -> when (severityBand) {
                "low" -> RecommendationContent(
                    summary = "Possible early signs of infection.",
                    steps = listOf(
                        "Monitor symptoms like cough or fever.",
                        "Stay hydrated and rest.",
                    ),
                )
                "moderate" -> RecommendationContent(
                    summary = "Likely pneumonia detected.",
                    steps = listOf(
                        "Consult a doctor immediately.",
                        "Medical tests like X-ray may be required.",
                    ),
                )
                else -> RecommendationContent(
                    summary = "Strong indication of pneumonia.",
                    steps = listOf(
                        "Urgent hospitalization may be required.",
                        "Immediate treatment is critical.",
                    ),
                )
            }
            else -> RecommendationContent(
                summary = "Clinical review is recommended for this result.",
                steps = listOf(
                    "Repeat the recording in a quiet environment if needed.",
                    "Discuss the result with a qualified doctor.",
                ),
            )
        }
    }

    private fun generalRecommendations(): List<String> {
        return listOf(
            "Ensure a clean and noise-free recording.",
            "Use a good quality microphone or stethoscope when available.",
            "Do not rely only on AI; always confirm with a doctor.",
            "Regular monitoring is important.",
        )
    }

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 42f
    private const val LINE_SPACING = 8f

    private val TITLE_PAINT = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF0A1B4D.toInt()
        textSize = 24f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val SECTION_PAINT = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1550E6.toInt()
        textSize = 17f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val EMPHASIS_PAINT = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF081942.toInt()
        textSize = 15f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val BODY_PAINT = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1B2B57.toInt()
        textSize = 13f
    }

    private val IMAGE_PAINT = Paint(Paint.ANTI_ALIAS_FLAG)

    private data class RecommendationContent(
        val summary: String,
        val steps: List<String>,
    )
}
