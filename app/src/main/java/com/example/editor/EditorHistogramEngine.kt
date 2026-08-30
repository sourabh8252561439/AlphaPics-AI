package com.example.editor

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.runtime.Immutable
import kotlin.math.max
import kotlin.math.sqrt

@Immutable
data class EditorHistogram(
    val luminance: List<Int>,
    val red: List<Int>,
    val green: List<Int>,
    val blue: List<Int>,
    val sampledPixels: Int
) {
    init {
        require(luminance.size == BIN_COUNT)
        require(red.size == BIN_COUNT)
        require(green.size == BIN_COUNT)
        require(blue.size == BIN_COUNT)
        require(sampledPixels >= 0)
    }

    fun normalized(channel: List<Int>): List<Float> {
        val peak = channel.maxOrNull()?.coerceAtLeast(1) ?: 1
        return channel.map { it.toFloat() / peak.toFloat() }
    }

    companion object {
        const val BIN_COUNT = 256
    }
}

/** Bounded local RGB/luminance histogram analysis for the interactive editor preview. */
object EditorHistogramEngine {

    private const val DEFAULT_MAX_SAMPLES = 262_144

    fun analyze(
        bitmap: Bitmap,
        maxSamples: Int = DEFAULT_MAX_SAMPLES,
        checkpoint: () -> Unit = {}
    ): EditorHistogram {
        require(bitmap.width > 0 && bitmap.height > 0) { "Histogram bitmap must not be empty" }
        require(maxSamples > 0) { "maxSamples must be positive" }

        val pixelCount = bitmap.width.toLong() * bitmap.height.toLong()
        val stride = max(1, sqrt(pixelCount.toDouble() / maxSamples.toDouble()).toInt())
        val luminance = IntArray(EditorHistogram.BIN_COUNT)
        val red = IntArray(EditorHistogram.BIN_COUNT)
        val green = IntArray(EditorHistogram.BIN_COUNT)
        val blue = IntArray(EditorHistogram.BIN_COUNT)
        val row = IntArray(bitmap.width)
        var sampled = 0

        var y = 0
        while (y < bitmap.height) {
            if (y % max(1, stride * 16) == 0) checkpoint()
            bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
            var x = 0
            while (x < bitmap.width) {
                val pixel = row[x]
                if (Color.alpha(pixel) > 0) {
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)
                    val luma = ((54 * r + 183 * g + 19 * b + 128) shr 8).coerceIn(0, 255)
                    red[r]++
                    green[g]++
                    blue[b]++
                    luminance[luma]++
                    sampled++
                }
                x += stride
            }
            y += stride
        }

        checkpoint()
        return EditorHistogram(
            luminance = luminance.toList(),
            red = red.toList(),
            green = green.toList(),
            blue = blue.toList(),
            sampledPixels = sampled
        )
    }
}
