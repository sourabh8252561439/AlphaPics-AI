package com.example.batchstudio

import android.net.Uri
import com.example.batch.MAX_BATCH_ITEMS
import com.example.editor.ExportFormat
import com.example.editor.WatermarkAnchor
import com.example.photo.MAX_UTILITY_DIMENSION
import com.example.photo.MAX_UTILITY_PIXELS
import kotlin.math.min
import kotlin.math.roundToInt

enum class BatchStudioTool(val label: String) {
    RESIZE("Resize"),
    CONVERT("Convert"),
    WATERMARK("Watermark"),
    LOGO("Logo"),
    PADDING("Padding"),
    PRESET("Preset")
}

enum class BatchResizeMode { NONE, DIMENSIONS, PERCENTAGE }

enum class BatchOutputFormat(val label: String, val exportFormat: ExportFormat?) {
    KEEP("Keep type", null),
    JPEG("JPEG", ExportFormat.JPEG),
    PNG("PNG", ExportFormat.PNG),
    WEBP("WebP", ExportFormat.WEBP);

    fun resolve(sourceMimeType: String?): ExportFormat = exportFormat ?: when (sourceMimeType) {
        "image/png" -> ExportFormat.PNG
        "image/webp" -> ExportFormat.WEBP
        else -> ExportFormat.JPEG
    }
}

enum class BatchPlacement(val label: String) {
    TOP_LEFT("Top left"),
    TOP_RIGHT("Top right"),
    CENTER("Center"),
    BOTTOM_LEFT("Bottom left"),
    BOTTOM_RIGHT("Bottom right");

    fun asWatermarkAnchor(): WatermarkAnchor = when (this) {
        TOP_LEFT -> WatermarkAnchor.TOP_LEFT
        TOP_RIGHT -> WatermarkAnchor.TOP_RIGHT
        CENTER -> WatermarkAnchor.CENTER
        BOTTOM_LEFT -> WatermarkAnchor.BOTTOM_LEFT
        BOTTOM_RIGHT -> WatermarkAnchor.BOTTOM_RIGHT
    }
}

data class BatchStudioSettings(
    val resizeMode: BatchResizeMode = BatchResizeMode.NONE,
    val targetWidth: Int = 2048,
    val targetHeight: Int = 2048,
    val percentage: Float = 100f,
    val maintainAspectRatio: Boolean = true,
    val outputFormat: BatchOutputFormat = BatchOutputFormat.KEEP,
    val quality: Int = 94,
    val watermarkEnabled: Boolean = false,
    val watermarkText: String = "AlphaPics AI",
    val watermarkPlacement: BatchPlacement = BatchPlacement.BOTTOM_RIGHT,
    val watermarkScale: Float = 4f,
    val watermarkOpacity: Float = 55f,
    val logoEnabled: Boolean = false,
    val logoUri: Uri? = null,
    val logoPlacement: BatchPlacement = BatchPlacement.BOTTOM_RIGHT,
    val logoScale: Float = 18f,
    val logoOpacity: Float = 85f,
    val paddingEnabled: Boolean = false,
    val paddingPercent: Float = 8f,
    val paddingColorArgb: Long = 0xFF050814,
    val alignment: BatchPlacement = BatchPlacement.CENTER,
    val presetEnabled: Boolean = false,
    val presetId: String = "natural",
    val presetIntensity: Float = 100f
) {
    fun sanitized(): BatchStudioSettings = copy(
        targetWidth = targetWidth.coerceIn(1, MAX_UTILITY_DIMENSION),
        targetHeight = targetHeight.coerceIn(1, MAX_UTILITY_DIMENSION),
        percentage = percentage.coerceIn(1f, 400f),
        quality = quality.coerceIn(40, 100),
        watermarkText = watermarkText.take(80),
        watermarkScale = watermarkScale.coerceIn(1.5f, 12f),
        watermarkOpacity = watermarkOpacity.coerceIn(0f, 100f),
        logoScale = logoScale.coerceIn(5f, 50f),
        logoOpacity = logoOpacity.coerceIn(0f, 100f),
        paddingPercent = paddingPercent.coerceIn(0f, 50f),
        presetIntensity = presetIntensity.coerceIn(0f, 100f)
    )

    fun resolveContentDimensions(sourceWidth: Int, sourceHeight: Int): Pair<Int, Int> {
        require(sourceWidth > 0 && sourceHeight > 0) { "Source dimensions are invalid" }
        val safe = sanitized()
        val resolved = when (safe.resizeMode) {
            BatchResizeMode.NONE -> sourceWidth to sourceHeight
            BatchResizeMode.PERCENTAGE -> {
                val scale = safe.percentage / 100f
                (sourceWidth * scale).roundToInt().coerceAtLeast(1) to
                    (sourceHeight * scale).roundToInt().coerceAtLeast(1)
            }
            BatchResizeMode.DIMENSIONS -> if (safe.maintainAspectRatio) {
                val scale = min(
                    safe.targetWidth.toFloat() / sourceWidth,
                    safe.targetHeight.toFloat() / sourceHeight
                )
                (sourceWidth * scale).roundToInt().coerceAtLeast(1) to
                    (sourceHeight * scale).roundToInt().coerceAtLeast(1)
            } else {
                safe.targetWidth to safe.targetHeight
            }
        }
        validateBatchStudioDimensions(resolved.first, resolved.second)
        return resolved
    }

    fun resolveOutputDimensions(contentWidth: Int, contentHeight: Int): Pair<Int, Int> {
        if (!paddingEnabled || paddingPercent <= 0f) return contentWidth to contentHeight
        val factor = 1f + sanitized().paddingPercent / 50f
        val width = (contentWidth * factor).roundToInt().coerceAtLeast(contentWidth)
        val height = (contentHeight * factor).roundToInt().coerceAtLeast(contentHeight)
        validateBatchStudioDimensions(width, height)
        return width to height
    }

    val hasVisibleOperation: Boolean
        get() = resizeMode != BatchResizeMode.NONE ||
            outputFormat != BatchOutputFormat.KEEP ||
            watermarkEnabled || logoEnabled || paddingEnabled || presetEnabled
}

enum class BatchStudioItemStatus {
    QUEUED,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    CANCELLED
}

data class BatchStudioOutput(
    val uri: Uri,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val mimeType: String,
    val filename: String
)

data class BatchStudioItem(
    val id: String,
    val uri: Uri,
    val displayName: String,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val sizeBytes: Long?,
    val status: BatchStudioItemStatus = BatchStudioItemStatus.QUEUED,
    val progress: Float = 0f,
    val output: BatchStudioOutput? = null,
    val errorMessage: String? = null
)

data class BatchStudioSummary(
    val total: Int,
    val succeeded: Int,
    val failed: Int,
    val cancelled: Int
) {
    val completed: Int get() = succeeded + failed + cancelled

    companion object {
        fun from(items: List<BatchStudioItem>): BatchStudioSummary = BatchStudioSummary(
            total = items.size.coerceAtMost(MAX_BATCH_ITEMS),
            succeeded = items.count { it.status == BatchStudioItemStatus.SUCCEEDED },
            failed = items.count { it.status == BatchStudioItemStatus.FAILED },
            cancelled = items.count { it.status == BatchStudioItemStatus.CANCELLED }
        )
    }
}

fun validateBatchStudioDimensions(width: Int, height: Int) {
    require(width in 1..MAX_UTILITY_DIMENSION && height in 1..MAX_UTILITY_DIMENSION) {
        "Batch output must be between 1 and $MAX_UTILITY_DIMENSION pixels per side"
    }
    require(width.toLong() * height <= MAX_UTILITY_PIXELS) {
        "Batch output exceeds the safe 64-megapixel limit"
    }
}
