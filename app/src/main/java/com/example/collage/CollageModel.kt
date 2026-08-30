package com.example.collage

import androidx.compose.runtime.Immutable
import com.example.editor.OverlayAdjustments
import kotlin.math.max

const val MAX_COLLAGE_PHOTOS = 6

@Immutable
data class CollageRect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
) {
    fun sanitized(minimumSize: Float = 0.12f): CollageRect {
        val safeWidth = width.coerceIn(minimumSize, 1f)
        val safeHeight = height.coerceIn(minimumSize, 1f)
        return copy(
            x = x.coerceIn(0f, 1f - safeWidth),
            y = y.coerceIn(0f, 1f - safeHeight),
            width = safeWidth,
            height = safeHeight
        )
    }

    fun movedBy(dx: Float, dy: Float): CollageRect = copy(x = x + dx, y = y + dy).sanitized()

    fun scaledBy(factor: Float): CollageRect {
        val safeFactor = factor.coerceIn(0.5f, 2f)
        val centerX = x + width / 2f
        val centerY = y + height / 2f
        val newWidth = (width * safeFactor).coerceIn(0.12f, 1f)
        val newHeight = (height * safeFactor).coerceIn(0.12f, 1f)
        return CollageRect(
            x = centerX - newWidth / 2f,
            y = centerY - newHeight / 2f,
            width = newWidth,
            height = newHeight
        ).sanitized()
    }
}

@Immutable
data class CollagePhotoTransform(
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
) {
    fun sanitized(): CollagePhotoTransform = copy(
        zoom = zoom.coerceIn(1f, 4f),
        offsetX = offsetX.coerceIn(-1f, 1f),
        offsetY = offsetY.coerceIn(-1f, 1f)
    )
}

enum class CollageBackgroundMode {
    SOLID,
    GRADIENT,
    IMAGE
}

@Immutable
data class CollageBackground(
    val mode: CollageBackgroundMode = CollageBackgroundMode.SOLID,
    val firstColorArgb: Long = 0xFF070B14,
    val secondColorArgb: Long = 0xFF172448
)

@Immutable
data class CollageLayoutPreset(
    val id: String,
    val label: String,
    val requiredPhotos: Int,
    val slots: List<CollageRect>,
    val isFreestyle: Boolean = false
)

object CollageLayoutCatalog {
    val TwoSplit = CollageLayoutPreset(
        id = "two_split",
        label = "2 Split",
        requiredPhotos = 2,
        slots = listOf(
            CollageRect(0f, 0f, 0.5f, 1f),
            CollageRect(0.5f, 0f, 0.5f, 1f)
        )
    )
    val TwoStack = CollageLayoutPreset(
        id = "two_stack",
        label = "2 Stack",
        requiredPhotos = 2,
        slots = listOf(
            CollageRect(0f, 0f, 1f, 0.5f),
            CollageRect(0f, 0.5f, 1f, 0.5f)
        )
    )
    val ThreeFeature = CollageLayoutPreset(
        id = "three_feature",
        label = "3 Feature",
        requiredPhotos = 3,
        slots = listOf(
            CollageRect(0f, 0f, 0.58f, 1f),
            CollageRect(0.58f, 0f, 0.42f, 0.5f),
            CollageRect(0.58f, 0.5f, 0.42f, 0.5f)
        )
    )
    val ThreeRows = CollageLayoutPreset(
        id = "three_rows",
        label = "3 Rows",
        requiredPhotos = 3,
        slots = listOf(
            CollageRect(0f, 0f, 1f, 0.56f),
            CollageRect(0f, 0.56f, 0.5f, 0.44f),
            CollageRect(0.5f, 0.56f, 0.5f, 0.44f)
        )
    )
    val FourGrid = CollageLayoutPreset(
        id = "four_grid",
        label = "4 Grid",
        requiredPhotos = 4,
        slots = listOf(
            CollageRect(0f, 0f, 0.5f, 0.5f),
            CollageRect(0.5f, 0f, 0.5f, 0.5f),
            CollageRect(0f, 0.5f, 0.5f, 0.5f),
            CollageRect(0.5f, 0.5f, 0.5f, 0.5f)
        )
    )
    val FiveMosaic = CollageLayoutPreset(
        id = "five_mosaic",
        label = "5 Mosaic",
        requiredPhotos = 5,
        slots = listOf(
            CollageRect(0f, 0f, 0.5f, 0.52f),
            CollageRect(0.5f, 0f, 0.5f, 0.52f),
            CollageRect(0f, 0.52f, 1f / 3f, 0.48f),
            CollageRect(1f / 3f, 0.52f, 1f / 3f, 0.48f),
            CollageRect(2f / 3f, 0.52f, 1f / 3f, 0.48f)
        )
    )
    val SixGrid = CollageLayoutPreset(
        id = "six_grid",
        label = "6 Grid",
        requiredPhotos = 6,
        slots = buildList {
            repeat(2) { row ->
                repeat(3) { column ->
                    add(CollageRect(column / 3f, row / 2f, 1f / 3f, 0.5f))
                }
            }
        }
    )
    val Freestyle = CollageLayoutPreset(
        id = "freestyle",
        label = "Freestyle",
        requiredPhotos = 2,
        slots = emptyList(),
        isFreestyle = true
    )

    val Presets = listOf(TwoSplit, TwoStack, ThreeFeature, ThreeRows, FourGrid, FiveMosaic, SixGrid, Freestyle)

    fun find(id: String): CollageLayoutPreset = Presets.firstOrNull { it.id == id } ?: TwoSplit

    fun slots(layoutId: String, photoCount: Int, freestyleRects: List<CollageRect>): List<CollageRect> {
        val preset = find(layoutId)
        if (!preset.isFreestyle) return preset.slots
        val safeCount = photoCount.coerceIn(2, MAX_COLLAGE_PHOTOS)
        return freestyleRects.take(safeCount) + defaultFreestyle(safeCount).drop(freestyleRects.size)
    }

    fun defaultFreestyle(photoCount: Int): List<CollageRect> {
        val templates = listOf(
            CollageRect(0.06f, 0.08f, 0.56f, 0.46f),
            CollageRect(0.39f, 0.42f, 0.55f, 0.49f),
            CollageRect(0.49f, 0.06f, 0.43f, 0.35f),
            CollageRect(0.07f, 0.57f, 0.40f, 0.35f),
            CollageRect(0.26f, 0.27f, 0.48f, 0.40f),
            CollageRect(0.58f, 0.58f, 0.34f, 0.31f)
        )
        return templates.take(photoCount.coerceIn(0, MAX_COLLAGE_PHOTOS))
    }
}

@Immutable
data class CollageState(
    val layoutId: String = CollageLayoutCatalog.TwoSplit.id,
    val photoTransforms: List<CollagePhotoTransform> = emptyList(),
    val freestyleRects: List<CollageRect> = emptyList(),
    val spacing: Float = 1.5f,
    val cornerRadius: Float = 2f,
    val borderWidth: Float = 0f,
    val borderColorArgb: Long = 0xFFFFFFFF,
    val aspectId: String = "1:1",
    val background: CollageBackground = CollageBackground(),
    val overlays: OverlayAdjustments = OverlayAdjustments(),
    val outputLongEdge: Int = 3072
) {
    fun ensurePhotoCount(photoCount: Int): CollageState {
        val count = photoCount.coerceIn(0, MAX_COLLAGE_PHOTOS)
        return copy(
            photoTransforms = photoTransforms.take(count) +
                List(max(0, count - photoTransforms.size)) { CollagePhotoTransform() },
            freestyleRects = freestyleRects.take(count) +
                CollageLayoutCatalog.defaultFreestyle(count).drop(freestyleRects.size)
        )
    }

    fun updatePhotoTransform(index: Int, transform: CollagePhotoTransform): CollageState {
        if (index !in photoTransforms.indices) return this
        return copy(photoTransforms = photoTransforms.toMutableList().apply { this[index] = transform.sanitized() })
    }

    fun updateFreestyleRect(index: Int, rect: CollageRect): CollageState {
        if (index !in freestyleRects.indices) return this
        return copy(freestyleRects = freestyleRects.toMutableList().apply { this[index] = rect.sanitized() })
    }

    fun swapTransforms(first: Int, second: Int): CollageState {
        if (first !in photoTransforms.indices || second !in photoTransforms.indices || first == second) return this
        return copy(photoTransforms = photoTransforms.toMutableList().apply {
            val temporary = this[first]
            this[first] = this[second]
            this[second] = temporary
        })
    }

    val aspectRatio: Float
        get() = when (aspectId) {
            "4:5" -> 4f / 5f
            "3:4" -> 3f / 4f
            "9:16" -> 9f / 16f
            "16:9" -> 16f / 9f
            "4:3" -> 4f / 3f
            else -> 1f
        }

    fun outputDimensions(): Pair<Int, Int> {
        val longEdge = outputLongEdge.coerceIn(1024, 4096)
        return if (aspectRatio >= 1f) {
            longEdge to (longEdge / aspectRatio).toInt().coerceAtLeast(1)
        } else {
            (longEdge * aspectRatio).toInt().coerceAtLeast(1) to longEdge
        }
    }
}
