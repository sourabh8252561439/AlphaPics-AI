package com.example.enhance

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * On-device local image processing engine for deterministic photo enhancement.
 * Performs histogram analysis, auto-exposure equalization, tone mapping, edge sharpening, and vibrance optimization.
 */
object LocalAutoEnhancer {

    data class HistogramStats(
        val minLuminance: Int,
        val maxLuminance: Int,
        val avgLuminance: Float,
        val shadowPercentage: Float,
        val highlightPercentage: Float
    )

    /**
     * Analyzes image luminance to compute key exposure and dynamic range statistics.
     */
    fun analyzeHistogram(bitmap: Bitmap): HistogramStats {
        val sampleStep = max(1, (bitmap.width * bitmap.height) / 10000)
        var sumLuminance = 0L
        var sampleCount = 0
        var minLum = 255
        var maxLum = 0
        var shadowCount = 0
        var highlightCount = 0

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width)

        for (y in 0 until height step max(1, height / 100)) {
            bitmap.getPixels(pixels, 0, width, 0, y, width, 1)
            for (x in 0 until width step max(1, width / 100)) {
                val pixel = pixels[x]
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)

                sumLuminance += lum
                sampleCount++
                if (lum < minLum) minLum = lum
                if (lum > maxLum) maxLum = lum
                if (lum < 40) shadowCount++
                if (lum > 215) highlightCount++
            }
        }

        val avg = if (sampleCount > 0) sumLuminance.toFloat() / sampleCount else 128f
        val shadowPct = if (sampleCount > 0) shadowCount.toFloat() / sampleCount else 0f
        val highlightPct = if (sampleCount > 0) highlightCount.toFloat() / sampleCount else 0f

        return HistogramStats(
            minLuminance = minLum,
            maxLuminance = maxLum,
            avgLuminance = avg,
            shadowPercentage = shadowPct,
            highlightPercentage = highlightPct
        )
    }

    /**
     * Applies mode-specific enhancement to bitmap.
     */
    fun enhance(source: Bitmap, modeId: String): Bitmap {
        val stats = analyzeHistogram(source)
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val matrix = when (modeId) {
            "auto" -> computeAutoMatrix(stats)
            "color" -> computeColorMatrix(stats)
            "light" -> computeLightMatrix(stats)
            "detail", "unblur" -> computeDetailMatrix(stats)
            "denoise" -> computeDenoiseMatrix(stats)
            "restore" -> computeRestoreMatrix(stats)
            else -> computeAutoMatrix(stats)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)

        // For sharpen/unblur, apply a high-pass detail convolution layer
        if (modeId == "unblur" || modeId == "detail") {
            return applySharpeningFilter(result)
        }

        return result
    }

    private fun computeAutoMatrix(stats: HistogramStats): ColorMatrix {
        val cm = ColorMatrix()

        // Exposure adjustment based on average luminance
        var exposureOffset = 0f
        if (stats.avgLuminance < 110) {
            exposureOffset = min(36f, (110 - stats.avgLuminance) * 0.45f)
        } else if (stats.avgLuminance > 165) {
            exposureOffset = max(-24f, (165 - stats.avgLuminance) * 0.35f)
        }

        // Contrast boost if dynamic range is narrow
        val dynamicRange = stats.maxLuminance - stats.minLuminance
        val contrast = if (dynamicRange < 180) 1.14f else 1.06f

        // Vibrance boost
        val sat = 1.12f

        cm.set(buildAdjustMatrix(contrast = contrast, brightnessOffset = exposureOffset, saturation = sat))
        return cm
    }

    private fun computeColorMatrix(stats: HistogramStats): ColorMatrix {
        val cm = ColorMatrix()
        cm.set(buildAdjustMatrix(contrast = 1.08f, brightnessOffset = 4f, saturation = 1.28f))
        return cm
    }

    private fun computeLightMatrix(stats: HistogramStats): ColorMatrix {
        val cm = ColorMatrix()
        val shadowLift = if (stats.shadowPercentage > 0.15f) 28f else 14f
        cm.set(buildAdjustMatrix(contrast = 1.05f, brightnessOffset = shadowLift, saturation = 1.04f))
        return cm
    }

    private fun computeDetailMatrix(stats: HistogramStats): ColorMatrix {
        val cm = ColorMatrix()
        cm.set(buildAdjustMatrix(contrast = 1.18f, brightnessOffset = 2f, saturation = 1.05f))
        return cm
    }

    private fun computeDenoiseMatrix(stats: HistogramStats): ColorMatrix {
        val cm = ColorMatrix()
        cm.set(buildAdjustMatrix(contrast = 0.96f, brightnessOffset = 6f, saturation = 1.02f))
        return cm
    }

    private fun computeRestoreMatrix(stats: HistogramStats): ColorMatrix {
        val cm = ColorMatrix()
        // Restore faded contrast and boost color density
        cm.set(buildAdjustMatrix(contrast = 1.22f, brightnessOffset = 10f, saturation = 1.20f))
        return cm
    }

    private fun buildAdjustMatrix(
        contrast: Float = 1f,
        brightnessOffset: Float = 0f,
        saturation: Float = 1f
    ): FloatArray {
        val invSat = 1f - saturation
        val rW = 0.213f * invSat
        val gW = 0.715f * invSat
        val bW = 0.072f * invSat

        val c = contrast
        val offset = (1f - c) * 128f + brightnessOffset

        return floatArrayOf(
            (rW + saturation) * c, gW * c, bW * c, 0f, offset,
            rW * c, (gW + saturation) * c, bW * c, 0f, offset,
            rW * c, gW * c, (bW + saturation) * c, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        )
    }

    /**
     * High-speed 3x3 unsharp masking kernel for focal edge sharpening.
     */
    private fun applySharpeningFilter(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val srcPixels = IntArray(width * height)
        val dstPixels = IntArray(width * height)
        source.getPixels(srcPixels, 0, width, 0, 0, width, height)

        // Sharpen kernel: [0, -0.5, 0; -0.5, 3.0, -0.5; 0, -0.5, 0]
        for (y in 1 until height - 1) {
            val rowOffset = y * width
            for (x in 1 until width - 1) {
                val idx = rowOffset + x

                val center = srcPixels[idx]
                val top = srcPixels[idx - width]
                val bottom = srcPixels[idx + width]
                val left = srcPixels[idx - 1]
                val right = srcPixels[idx + 1]

                val a = Color.alpha(center)
                val r = (Color.red(center) * 2.6f -
                    (Color.red(top) + Color.red(bottom) + Color.red(left) + Color.red(right)) * 0.4f).toInt().coerceIn(0, 255)
                val g = (Color.green(center) * 2.6f -
                    (Color.green(top) + Color.green(bottom) + Color.green(left) + Color.green(right)) * 0.4f).toInt().coerceIn(0, 255)
                val b = (Color.blue(center) * 2.6f -
                    (Color.blue(top) + Color.blue(bottom) + Color.blue(left) + Color.blue(right)) * 0.4f).toInt().coerceIn(0, 255)

                dstPixels[idx] = Color.argb(a, r, g, b)
            }
        }

        // Copy borders
        for (x in 0 until width) {
            dstPixels[x] = srcPixels[x]
            dstPixels[(height - 1) * width + x] = srcPixels[(height - 1) * width + x]
        }
        for (y in 0 until height) {
            dstPixels[y * width] = srcPixels[y * width]
            dstPixels[y * width + (width - 1)] = srcPixels[y * width + (width - 1)]
        }

        output.setPixels(dstPixels, 0, width, 0, 0, width, height)
        source.recycle()
        return output
    }
}
