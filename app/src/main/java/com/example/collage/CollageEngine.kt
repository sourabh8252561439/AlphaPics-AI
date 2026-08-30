package com.example.collage

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import com.example.editor.EditorOverlayEngine
import kotlin.math.max
import kotlin.math.min

/** Deterministic local collage compositor shared by interactive preview and high-resolution export. */
object CollageEngine {

    fun render(
        sources: List<Bitmap>,
        state: CollageState,
        outputWidth: Int,
        outputHeight: Int,
        backgroundImage: Bitmap? = null,
        allowIncomplete: Boolean = false,
        onProgress: (Float) -> Unit = {},
        checkpoint: () -> Unit = {}
    ): Bitmap {
        require(outputWidth > 0 && outputHeight > 0) { "Collage dimensions must be positive" }
        val minimumSources = if (allowIncomplete) 1 else 2
        require(sources.size in minimumSources..MAX_COLLAGE_PHOTOS) {
            "Choose between $minimumSources and $MAX_COLLAGE_PHOTOS photos"
        }
        sources.forEach { require(!it.isRecycled && it.width > 0 && it.height > 0) { "Invalid collage source" } }

        val safeState = state.ensurePhotoCount(sources.size)
        val preset = CollageLayoutCatalog.find(safeState.layoutId)
        val usedCount = if (preset.isFreestyle) sources.size else preset.requiredPhotos
        if (!allowIncomplete) require(sources.size >= usedCount) { "${preset.label} needs $usedCount photos" }
        val slots = CollageLayoutCatalog.slots(
            layoutId = safeState.layoutId,
            photoCount = usedCount,
            freestyleRects = safeState.freestyleRects
        ).take(usedCount)

        val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        drawBackground(canvas, outputWidth, outputHeight, safeState.background, backgroundImage)
        onProgress(0.08f)

        val minimumDimension = min(outputWidth, outputHeight).toFloat()
        val gap = safeState.spacing.coerceIn(0f, 12f) / 100f * minimumDimension
        val border = safeState.borderWidth.coerceIn(0f, 6f) / 100f * minimumDimension
        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

        val drawCount = min(sources.size, usedCount)
        slots.take(drawCount).forEachIndexed { index, normalizedSlot ->
            checkpoint()
            val slot = RectF(
                normalizedSlot.x * outputWidth + gap / 2f,
                normalizedSlot.y * outputHeight + gap / 2f,
                (normalizedSlot.x + normalizedSlot.width) * outputWidth - gap / 2f,
                (normalizedSlot.y + normalizedSlot.height) * outputHeight - gap / 2f
            )
            if (slot.width() <= 1f || slot.height() <= 1f) return@forEachIndexed
            val corner = safeState.cornerRadius.coerceIn(0f, 24f) / 100f * min(slot.width(), slot.height())
            val source = sources[index]
            val transform = safeState.photoTransforms[index].sanitized()

            val destination = coverRect(source, slot, transform)
            imagePaint.shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                setLocalMatrix(
                    Matrix().apply {
                        setRectToRect(
                            RectF(0f, 0f, source.width.toFloat(), source.height.toFloat()),
                            destination,
                            Matrix.ScaleToFit.FILL
                        )
                    }
                )
            }
            canvas.drawRoundRect(slot, corner, corner, imagePaint)
            imagePaint.shader = null

            if (border > 0f) {
                val inset = border / 2f
                canvas.drawRoundRect(
                    RectF(slot.left + inset, slot.top + inset, slot.right - inset, slot.bottom - inset),
                    corner,
                    corner,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = border
                        color = safeState.borderColorArgb.toInt()
                    }
                )
            }
            onProgress(0.08f + (index + 1f) / drawCount * 0.78f)
        }

        checkpoint()
        EditorOverlayEngine.applyInPlace(
            bitmap = output,
            overlays = safeState.overlays,
            checkpoint = checkpoint,
            onProgress = { overlayProgress -> onProgress(0.86f + overlayProgress * 0.14f) }
        )
        onProgress(1f)
        return output
    }

    private fun drawBackground(
        canvas: Canvas,
        width: Int,
        height: Int,
        background: CollageBackground,
        backgroundImage: Bitmap?
    ) {
        when (background.mode) {
            CollageBackgroundMode.SOLID -> canvas.drawColor(background.firstColorArgb.toInt())
            CollageBackgroundMode.GRADIENT -> canvas.drawRect(
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f,
                        0f,
                        width.toFloat(),
                        height.toFloat(),
                        background.firstColorArgb.toInt(),
                        background.secondColorArgb.toInt(),
                        Shader.TileMode.CLAMP
                    )
                }
            )
            CollageBackgroundMode.IMAGE -> {
                canvas.drawColor(background.firstColorArgb.toInt())
                if (backgroundImage != null && !backgroundImage.isRecycled) {
                    canvas.drawBitmap(
                        backgroundImage,
                        null,
                        coverRect(
                            source = backgroundImage,
                            target = RectF(0f, 0f, width.toFloat(), height.toFloat()),
                            transform = CollagePhotoTransform()
                        ),
                        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
                    )
                }
            }
        }
    }

    internal fun coverRect(
        source: Bitmap,
        target: RectF,
        transform: CollagePhotoTransform
    ): RectF {
        val safe = transform.sanitized()
        val scale = max(target.width() / source.width, target.height() / source.height) * safe.zoom
        val drawWidth = source.width * scale
        val drawHeight = source.height * scale
        val overflowX = max(0f, drawWidth - target.width())
        val overflowY = max(0f, drawHeight - target.height())
        val centerX = target.centerX() + safe.offsetX * overflowX / 2f
        val centerY = target.centerY() + safe.offsetY * overflowY / 2f
        return RectF(
            centerX - drawWidth / 2f,
            centerY - drawHeight / 2f,
            centerX + drawWidth / 2f,
            centerY + drawHeight / 2f
        )
    }
}
