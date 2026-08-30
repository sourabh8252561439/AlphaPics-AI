package com.example.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import java.util.LinkedHashMap
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Shared full-resolution geometry renderer used by both preview and export. */
object EditorGeometryEngine {

    private const val ROW_CACHE_SIZE = 24

    /** Returns an independent mutable bitmap and never recycles [source]. */
    fun render(
        source: Bitmap,
        transform: TransformAdjustments,
        onProgress: (Float) -> Unit = {},
        checkpoint: () -> Unit = {}
    ): Bitmap {
        checkpoint()
        onProgress(0f)
        var current = applyQuarterTurnAndFlips(source, transform)
        onProgress(0.15f)
        if (transform.straightenDegrees != 0f) {
            checkpoint()
            current = replaceIntermediate(
                original = source,
                previous = current,
                next = applyStraighten(current, transform.straightenDegrees)
            )
        }
        onProgress(0.30f)
        if (hasWarp(transform)) {
            current = replaceIntermediate(
                original = source,
                previous = current,
                next = applyWarp(current, transform, checkpoint) { warpProgress ->
                    onProgress(0.30f + warpProgress * 0.55f)
                }
            )
        }
        onProgress(0.85f)
        if (!transform.cropRect.isFull || transform.targetAspectRatio != null) {
            checkpoint()
            current = replaceIntermediate(
                original = source,
                previous = current,
                next = applyCrop(current, transform)
            )
        }
        onProgress(0.95f)
        if (current === source) {
            return source.copy(Bitmap.Config.ARGB_8888, true).also { onProgress(1f) }
                ?: throw IllegalStateException("Unable to allocate geometry output")
        }
        val result = if (current.isMutable && current.config == Bitmap.Config.ARGB_8888) {
            current
        } else {
            current.copy(Bitmap.Config.ARGB_8888, true).also { mutable ->
                if (mutable == null) throw IllegalStateException("Unable to allocate geometry output")
                current.recycle()
            } ?: throw IllegalStateException("Unable to allocate geometry output")
        }
        onProgress(1f)
        return result
    }

    private fun replaceIntermediate(original: Bitmap, previous: Bitmap, next: Bitmap): Bitmap {
        if (previous !== original && previous !== next) previous.recycle()
        return next
    }

    private fun applyQuarterTurnAndFlips(
        source: Bitmap,
        transform: TransformAdjustments
    ): Bitmap {
        val rotation = ((transform.rotationDegrees % 360) + 360) % 360
        if (rotation == 0 && !transform.flipHorizontal && !transform.flipVertical) return source
        val matrix = Matrix().apply {
            if (transform.flipHorizontal || transform.flipVertical) {
                postScale(
                    if (transform.flipHorizontal) -1f else 1f,
                    if (transform.flipVertical) -1f else 1f
                )
            }
            if (rotation != 0) postRotate(rotation.toFloat())
        }
        return Bitmap.createBitmap(
            source,
            0,
            0,
            source.width,
            source.height,
            matrix,
            true
        )
    }

    private fun applyStraighten(source: Bitmap, requestedDegrees: Float): Bitmap {
        val degrees = requestedDegrees.coerceIn(-15f, 15f)
        if (degrees == 0f) return source
        val width = source.width
        val height = source.height
        val radians = Math.toRadians(abs(degrees).toDouble())
        val cosine = cos(radians).toFloat()
        val sine = sin(radians).toFloat()
        val fillScale = max(
            cosine + height.toFloat() / width.toFloat() * sine,
            cosine + width.toFloat() / height.toFloat() * sine
        )
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { output ->
            Canvas(output).apply {
                val centerX = width / 2f
                val centerY = height / 2f
                translate(centerX, centerY)
                scale(fillScale, fillScale)
                rotate(degrees)
                translate(-centerX, -centerY)
                drawBitmap(source, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            }
        }
    }

    private fun hasWarp(transform: TransformAdjustments): Boolean =
        transform.perspectiveHorizontal != 0f || transform.perspectiveVertical != 0f ||
            transform.lensDistortion != 0f || transform.geometryHorizontal != 0f ||
            transform.geometryVertical != 0f

    private fun applyWarp(
        source: Bitmap,
        transform: TransformAdjustments,
        checkpoint: () -> Unit,
        onProgress: (Float) -> Unit
    ): Bitmap {
        val width = source.width
        val height = source.height
        val perspectiveX = transform.perspectiveHorizontal.coerceIn(-100f, 100f) / 100f
        val perspectiveY = transform.perspectiveVertical.coerceIn(-100f, 100f) / 100f
        val distortion = transform.lensDistortion.coerceIn(-100f, 100f) / 100f
        val geometryX = transform.geometryHorizontal.coerceIn(-100f, 100f) / 100f
        val geometryY = transform.geometryVertical.coerceIn(-100f, 100f) / 100f
        val safeZoom = 1f + abs(distortion) * 0.55f + abs(perspectiveX) * 0.25f +
            abs(perspectiveY) * 0.25f + abs(geometryX) * 0.18f + abs(geometryY) * 0.18f
        val sampler = CachedRowSampler(source, ROW_CACHE_SIZE)
        val row = IntArray(width)
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (y in 0 until height) {
            if (y % 8 == 0) checkpoint()
            val normalizedY = if (height == 1) 0f else y.toFloat() / (height - 1f) * 2f - 1f
            for (x in 0 until width) {
                val normalizedX = if (width == 1) 0f else x.toFloat() / (width - 1f) * 2f - 1f
                var sourceX = normalizedX / safeZoom
                var sourceY = normalizedY / safeZoom

                sourceX /= (1f + perspectiveX * sourceY * 0.35f).coerceAtLeast(0.35f)
                sourceY /= (1f + perspectiveY * sourceX * 0.35f).coerceAtLeast(0.35f)

                val radiusSquared = sourceX * sourceX + sourceY * sourceY
                val radialFactor = 1f + distortion * 0.45f * radiusSquared
                sourceX *= radialFactor
                sourceY *= radialFactor
                sourceX /= 1f + geometryX * 0.35f
                sourceY /= 1f + geometryY * 0.35f

                val bitmapX = (sourceX + 1f) * 0.5f * (width - 1f)
                val bitmapY = (sourceY + 1f) * 0.5f * (height - 1f)
                row[x] = sampler.bilinear(bitmapX, bitmapY)
            }
            output.setPixels(row, 0, width, 0, y, width, 1)
            if (y % 8 == 0 || y == height - 1) onProgress((y + 1f) / height.toFloat())
        }
        return output
    }

    private fun applyCrop(source: Bitmap, transform: TransformAdjustments): Bitmap {
        val crop = transform.cropRect.sanitized()
        val initialLeft = floor(crop.left * source.width).toInt().coerceIn(0, source.width - 1)
        val initialTop = floor(crop.top * source.height).toInt().coerceIn(0, source.height - 1)
        val initialRight = ceil(crop.right * source.width).toInt().coerceIn(initialLeft + 1, source.width)
        val initialBottom = ceil(crop.bottom * source.height).toInt().coerceIn(initialTop + 1, source.height)
        var cropWidth = initialRight - initialLeft
        var cropHeight = initialBottom - initialTop
        var startX = initialLeft
        var startY = initialTop

        transform.targetAspectRatio?.let { targetRatio ->
            val currentRatio = cropWidth.toFloat() / cropHeight.toFloat()
            if (currentRatio > targetRatio) {
                val targetWidth = (cropHeight * targetRatio).toInt().coerceIn(1, cropWidth)
                startX += (cropWidth - targetWidth) / 2
                cropWidth = targetWidth
            } else if (currentRatio < targetRatio) {
                val targetHeight = (cropWidth / targetRatio).toInt().coerceIn(1, cropHeight)
                startY += (cropHeight - targetHeight) / 2
                cropHeight = targetHeight
            }
        }

        if (startX == 0 && startY == 0 && cropWidth == source.width && cropHeight == source.height) {
            return source
        }
        return Bitmap.createBitmap(source, startX, startY, cropWidth, cropHeight)
    }

    /** Small bounded row cache keeps full-resolution warps from allocating a second pixel array. */
    private class CachedRowSampler(
        private val bitmap: Bitmap,
        private val capacity: Int
    ) {
        private val rows = object : LinkedHashMap<Int, IntArray>(capacity, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, IntArray>?): Boolean =
                size > capacity
        }

        fun bilinear(x: Float, y: Float): Int {
            if (x < 0f || y < 0f || x > bitmap.width - 1f || y > bitmap.height - 1f) {
                return Color.TRANSPARENT
            }
            val x0 = floor(x).toInt()
            val y0 = floor(y).toInt()
            val x1 = min(x0 + 1, bitmap.width - 1)
            val y1 = min(y0 + 1, bitmap.height - 1)
            val xFraction = x - x0
            val yFraction = y - y0
            val top = interpolatePremultiplied(pixel(x0, y0), pixel(x1, y0), xFraction)
            val bottom = interpolatePremultiplied(pixel(x0, y1), pixel(x1, y1), xFraction)
            return interpolatePremultiplied(top, bottom, yFraction)
        }

        private fun pixel(x: Int, y: Int): Int = row(y)[x]

        private fun row(y: Int): IntArray = rows.getOrPut(y) {
            IntArray(bitmap.width).also { bitmap.getPixels(it, 0, bitmap.width, 0, y, bitmap.width, 1) }
        }

        private fun interpolatePremultiplied(first: Int, second: Int, fraction: Float): Int {
            if (fraction <= 0f) return first
            if (fraction >= 1f) return second
            val inverse = 1f - fraction
            val firstAlpha = Color.alpha(first) / 255f
            val secondAlpha = Color.alpha(second) / 255f
            val alpha = firstAlpha * inverse + secondAlpha * fraction
            if (alpha <= 0.0001f) return Color.TRANSPARENT
            fun channel(firstChannel: Int, secondChannel: Int): Int {
                val premultiplied = firstChannel * firstAlpha * inverse +
                    secondChannel * secondAlpha * fraction
                return (premultiplied / alpha).toInt().coerceIn(0, 255)
            }
            return Color.argb(
                (alpha * 255f).toInt().coerceIn(0, 255),
                channel(Color.red(first), Color.red(second)),
                channel(Color.green(first), Color.green(second)),
                channel(Color.blue(first), Color.blue(second))
            )
        }
    }
}
