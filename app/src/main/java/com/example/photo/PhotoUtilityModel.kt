package com.example.photo

import androidx.compose.runtime.Immutable
import com.example.editor.ExportFormat
import kotlin.math.roundToInt

const val MAX_UTILITY_DIMENSION = 8192
const val MAX_UTILITY_PIXELS = 64_000_000L

enum class PhotoUtilityTab(val id: String, val label: String) {
    RESIZE("resize", "Resize"),
    CONVERT("convert", "Convert"),
    INFO("info", "Info");

    companion object {
        fun fromId(id: String?): PhotoUtilityTab = entries.firstOrNull { it.id == id } ?: RESIZE
    }
}

enum class ResizeMode { DIMENSIONS, PERCENTAGE }

enum class MetadataPolicy { REMOVE, PRESERVE_SAFE }

@Immutable
data class ResizeSettings(
    val mode: ResizeMode = ResizeMode.DIMENSIONS,
    val targetWidth: Int = 0,
    val targetHeight: Int = 0,
    val percentage: Float = 100f,
    val maintainAspectRatio: Boolean = true
) {
    fun resolvedDimensions(sourceWidth: Int, sourceHeight: Int): Pair<Int, Int> {
        require(sourceWidth > 0 && sourceHeight > 0) { "Source dimensions are invalid" }
        val resolved = if (mode == ResizeMode.PERCENTAGE) {
            val scale = percentage.coerceIn(1f, 400f) / 100f
            (sourceWidth * scale).roundToInt().coerceAtLeast(1) to
                (sourceHeight * scale).roundToInt().coerceAtLeast(1)
        } else {
            val width = targetWidth.takeIf { it > 0 } ?: sourceWidth
            val height = targetHeight.takeIf { it > 0 } ?: sourceHeight
            width to height
        }
        validateOutputDimensions(resolved.first, resolved.second)
        return resolved
    }

    fun withLongEdge(sourceWidth: Int, sourceHeight: Int, longEdge: Int): ResizeSettings {
        require(sourceWidth > 0 && sourceHeight > 0)
        val edge = longEdge.coerceIn(1, MAX_UTILITY_DIMENSION)
        val ratio = sourceWidth.toFloat() / sourceHeight
        val dimensions = if (sourceWidth >= sourceHeight) {
            edge to (edge / ratio).roundToInt().coerceAtLeast(1)
        } else {
            (edge * ratio).roundToInt().coerceAtLeast(1) to edge
        }
        return copy(
            mode = ResizeMode.DIMENSIONS,
            targetWidth = dimensions.first,
            targetHeight = dimensions.second,
            maintainAspectRatio = true
        )
    }

    fun withOriginalDimensions(sourceWidth: Int, sourceHeight: Int): ResizeSettings = copy(
        mode = ResizeMode.DIMENSIONS,
        targetWidth = sourceWidth,
        targetHeight = sourceHeight,
        percentage = 100f
    )
}

@Immutable
data class PhotoUtilitySettings(
    val resize: ResizeSettings = ResizeSettings(),
    val format: ExportFormat = ExportFormat.JPEG,
    val quality: Int = 94,
    val metadataPolicy: MetadataPolicy = MetadataPolicy.REMOVE
)

@Immutable
data class PhotoMetadata(
    val displayName: String,
    val mimeType: String,
    val formatLabel: String,
    val sizeBytes: Long?,
    val rawWidth: Int,
    val rawHeight: Int,
    val width: Int,
    val height: Int,
    val orientationLabel: String,
    val orientationValue: Int,
    val dateLabel: String?,
    val hasTransparency: Boolean,
    val colorDescription: String,
    val exif: Map<String, String>
) {
    val megapixels: Double get() = width.toDouble() * height / 1_000_000.0
    val aspectRatio: Double get() = width.toDouble() / height.coerceAtLeast(1)
}

fun validateOutputDimensions(width: Int, height: Int) {
    require(width in 1..MAX_UTILITY_DIMENSION && height in 1..MAX_UTILITY_DIMENSION) {
        "Width and height must be between 1 and $MAX_UTILITY_DIMENSION pixels"
    }
    require(width.toLong() * height <= MAX_UTILITY_PIXELS) {
        "This output exceeds the safe 64-megapixel limit"
    }
}
