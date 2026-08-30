package com.example.editor

import android.graphics.Bitmap
import android.graphics.Color
import java.util.LinkedHashMap
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/** Deterministic local retouch renderer. Strokes replay identically for preview and export. */
object EditorRetouchEngine {

    private const val ROW_CACHE_SIZE = 20

    fun applyInPlace(
        bitmap: Bitmap,
        retouch: RetouchAdjustments,
        onProgress: (Float) -> Unit = {},
        checkpoint: () -> Unit = {}
    ) {
        require(bitmap.isMutable) { "Retouch bitmap must be mutable" }
        if (retouch.strokes.isEmpty()) {
            onProgress(1f)
            return
        }

        val baseline = bitmap.copy(Bitmap.Config.ARGB_8888, false)
            ?: throw IllegalStateException("Unable to allocate retouch baseline")
        try {
            retouch.strokes.forEachIndexed { index, rawStroke ->
                checkpoint()
                val stroke = rawStroke.sanitized()
                if (stroke.points.isNotEmpty()) {
                    applyStroke(bitmap, baseline, stroke, checkpoint)
                }
                onProgress((index + 1f) / retouch.strokes.size.toFloat())
            }
        } finally {
            baseline.recycle()
        }
    }

    private fun applyStroke(
        bitmap: Bitmap,
        baseline: Bitmap,
        stroke: RetouchStroke,
        checkpoint: () -> Unit
    ) {
        val width = bitmap.width
        val height = bitmap.height
        val minimumDimension = min(width, height).coerceAtLeast(1)
        val radius = (stroke.size / 200f * minimumDimension).coerceAtLeast(1f)
        val points = stroke.points.map { point ->
            PixelPoint(point.x * (width - 1f), point.y * (height - 1f))
        }
        val minX = floor(points.minOf { it.x } - radius - 1f).toInt().coerceIn(0, width - 1)
        val maxX = floor(points.maxOf { it.x } + radius + 1f).toInt().coerceIn(0, width - 1)
        val minY = floor(points.minOf { it.y } - radius - 1f).toInt().coerceIn(0, height - 1)
        val maxY = floor(points.maxOf { it.y } + radius + 1f).toInt().coerceIn(0, height - 1)
        val source = bitmap.copy(Bitmap.Config.ARGB_8888, false)
            ?: throw IllegalStateException("Unable to allocate retouch stroke source")
        try {
            val sourceSampler = BitmapSampler(source, ROW_CACHE_SIZE)
            val baselineSampler = BitmapSampler(baseline, ROW_CACHE_SIZE)
            val outputRow = IntArray(width)
            val strength = stroke.strength / 100f
            for (y in minY..maxY) {
                if ((y - minY) % 8 == 0) checkpoint()
                bitmap.getPixels(outputRow, 0, width, 0, y, width, 1)
                for (x in minX..maxX) {
                    val coverage = strokeCoverage(
                        x = x + 0.5f,
                        y = y + 0.5f,
                        points = points,
                        radius = radius,
                        feather = stroke.feather / 100f
                    )
                    if (coverage <= 0f) continue
                    val original = sourceSampler.pixel(x, y)
                    val target = when (stroke.mode) {
                        LocalRetouchMode.HEAL -> healingPixel(sourceSampler, x, y, radius)
                        LocalRetouchMode.CLONE -> sourceSampler.bilinear(
                            x + stroke.cloneSourceOffsetX / 100f * width,
                            y + stroke.cloneSourceOffsetY / 100f * height
                        )
                        LocalRetouchMode.BLEMISH -> averagePixel(
                            sourceSampler,
                            x,
                            y,
                            (radius * 0.45f).toInt().coerceIn(1, 8)
                        )
                        LocalRetouchMode.RED_EYE -> correctRedEye(original)
                        LocalRetouchMode.BLUR -> averagePixel(
                            sourceSampler,
                            x,
                            y,
                            (radius * 0.30f).toInt().coerceIn(1, 6)
                        )
                        LocalRetouchMode.SHARPEN -> sharpenPixel(sourceSampler, x, y, original)
                        LocalRetouchMode.EXPOSURE -> exposurePixel(original, strength)
                        LocalRetouchMode.BRIGHTNESS -> brightnessPixel(original, strength)
                        LocalRetouchMode.SATURATION -> saturationPixel(original, strength)
                        LocalRetouchMode.TEMPERATURE -> temperaturePixel(original, strength)
                        LocalRetouchMode.ERASE_MASK -> baselineSampler.pixel(x, y)
                    }
                    val blendStrength = if (stroke.mode in setOf(
                            LocalRetouchMode.EXPOSURE,
                            LocalRetouchMode.BRIGHTNESS,
                            LocalRetouchMode.SATURATION,
                            LocalRetouchMode.TEMPERATURE
                        )
                    ) coverage else coverage * strength
                    outputRow[x] = mixPremultiplied(original, target, blendStrength.coerceIn(0f, 1f))
                }
                bitmap.setPixels(outputRow, 0, width, 0, y, width, 1)
            }
        } finally {
            source.recycle()
        }
    }

    private fun strokeCoverage(
        x: Float,
        y: Float,
        points: List<PixelPoint>,
        radius: Float,
        feather: Float
    ): Float {
        val distance = if (points.size == 1) {
            distance(x, y, points.first().x, points.first().y)
        } else {
            var best = Float.POSITIVE_INFINITY
            for (index in 0 until points.lastIndex) {
                best = min(best, distanceToSegment(x, y, points[index], points[index + 1]))
            }
            best
        }
        if (distance >= radius) return 0f
        val hardRadius = radius * (1f - feather.coerceIn(0f, 1f) * 0.88f)
        if (distance <= hardRadius || hardRadius >= radius) return 1f
        val fraction = ((radius - distance) / (radius - hardRadius)).coerceIn(0f, 1f)
        return fraction * fraction * (3f - 2f * fraction)
    }

    private fun healingPixel(sampler: BitmapSampler, x: Int, y: Int, radius: Float): Int {
        val offset = (radius * 1.35f).toInt().coerceAtLeast(2)
        val clone = sampler.pixelClamped(x - offset, y - offset)
        val local = averagePixel(sampler, x, y, (radius * 0.35f).toInt().coerceIn(1, 6))
        return mixPremultiplied(clone, local, 0.38f)
    }

    private fun sharpenPixel(sampler: BitmapSampler, x: Int, y: Int, center: Int): Int {
        val blurred = averagePixel(sampler, x, y, 1)
        fun sharpen(channel: (Int) -> Int): Int {
            val value = channel(center) + (channel(center) - channel(blurred)) * 1.25f
            return value.toInt().coerceIn(0, 255)
        }
        return Color.argb(
            Color.alpha(center),
            sharpen(Color::red),
            sharpen(Color::green),
            sharpen(Color::blue)
        )
    }

    private fun averagePixel(sampler: BitmapSampler, x: Int, y: Int, radius: Int): Int {
        var alphaSum = 0f
        var redSum = 0f
        var greenSum = 0f
        var blueSum = 0f
        var count = 0
        for (sampleY in y - radius..y + radius) {
            for (sampleX in x - radius..x + radius) {
                val pixel = sampler.pixelClamped(sampleX, sampleY)
                val alpha = Color.alpha(pixel) / 255f
                alphaSum += alpha
                redSum += Color.red(pixel) * alpha
                greenSum += Color.green(pixel) * alpha
                blueSum += Color.blue(pixel) * alpha
                count++
            }
        }
        if (count == 0 || alphaSum <= 0.0001f) return Color.TRANSPARENT
        val alpha = alphaSum / count.toFloat()
        return Color.argb(
            (alpha * 255f).toInt().coerceIn(0, 255),
            (redSum / alphaSum).toInt().coerceIn(0, 255),
            (greenSum / alphaSum).toInt().coerceIn(0, 255),
            (blueSum / alphaSum).toInt().coerceIn(0, 255)
        )
    }

    private fun correctRedEye(pixel: Int): Int {
        val red = Color.red(pixel)
        val green = Color.green(pixel)
        val blue = Color.blue(pixel)
        if (red < green * 1.18f || red < blue * 1.18f || red < 72) return pixel
        val neutralRed = ((green + blue) / 2f * 0.92f).toInt().coerceIn(0, 255)
        return Color.argb(Color.alpha(pixel), neutralRed, green, blue)
    }

    private fun exposurePixel(pixel: Int, strength: Float): Int {
        val multiplier = 2f.pow(strength.coerceIn(0f, 1f) * 1.35f)
        return mapRgb(pixel) { channel -> (channel * multiplier).toInt() }
    }

    private fun brightnessPixel(pixel: Int, strength: Float): Int =
        mapRgb(pixel) { channel -> channel + (strength * 72f).toInt() }

    private fun saturationPixel(pixel: Int, strength: Float): Int {
        val red = Color.red(pixel).toFloat()
        val green = Color.green(pixel).toFloat()
        val blue = Color.blue(pixel).toFloat()
        val luminance = red * 0.2126f + green * 0.7152f + blue * 0.0722f
        val factor = 1f + strength.coerceIn(0f, 1f) * 1.25f
        return Color.argb(
            Color.alpha(pixel),
            (luminance + (red - luminance) * factor).toInt().coerceIn(0, 255),
            (luminance + (green - luminance) * factor).toInt().coerceIn(0, 255),
            (luminance + (blue - luminance) * factor).toInt().coerceIn(0, 255)
        )
    }

    private fun temperaturePixel(pixel: Int, strength: Float): Int = Color.argb(
        Color.alpha(pixel),
        (Color.red(pixel) + strength * 52f).toInt().coerceIn(0, 255),
        (Color.green(pixel) + strength * 10f).toInt().coerceIn(0, 255),
        (Color.blue(pixel) - strength * 44f).toInt().coerceIn(0, 255)
    )

    private fun mapRgb(pixel: Int, transform: (Int) -> Int): Int = Color.argb(
        Color.alpha(pixel),
        transform(Color.red(pixel)).coerceIn(0, 255),
        transform(Color.green(pixel)).coerceIn(0, 255),
        transform(Color.blue(pixel)).coerceIn(0, 255)
    )

    private fun mixPremultiplied(first: Int, second: Int, amount: Float): Int {
        if (amount <= 0f) return first
        if (amount >= 1f) return second
        val inverse = 1f - amount
        val firstAlpha = Color.alpha(first) / 255f
        val secondAlpha = Color.alpha(second) / 255f
        val alpha = firstAlpha * inverse + secondAlpha * amount
        if (alpha <= 0.0001f) return Color.TRANSPARENT
        fun channel(firstValue: Int, secondValue: Int): Int =
            ((firstValue * firstAlpha * inverse + secondValue * secondAlpha * amount) / alpha)
                .toInt().coerceIn(0, 255)
        return Color.argb(
            (alpha * 255f).toInt().coerceIn(0, 255),
            channel(Color.red(first), Color.red(second)),
            channel(Color.green(first), Color.green(second)),
            channel(Color.blue(first), Color.blue(second))
        )
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val deltaX = x1 - x2
        val deltaY = y1 - y2
        return sqrt(deltaX * deltaX + deltaY * deltaY)
    }

    private fun distanceToSegment(x: Float, y: Float, start: PixelPoint, end: PixelPoint): Float {
        val deltaX = end.x - start.x
        val deltaY = end.y - start.y
        val lengthSquared = deltaX * deltaX + deltaY * deltaY
        if (lengthSquared <= 0.0001f) return distance(x, y, start.x, start.y)
        val fraction = (((x - start.x) * deltaX + (y - start.y) * deltaY) / lengthSquared)
            .coerceIn(0f, 1f)
        return distance(x, y, start.x + deltaX * fraction, start.y + deltaY * fraction)
    }

    private data class PixelPoint(val x: Float, val y: Float)

    private class BitmapSampler(private val bitmap: Bitmap, capacity: Int) {
        private val rows = object : LinkedHashMap<Int, IntArray>(capacity, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, IntArray>?): Boolean =
                size > capacity
        }

        fun pixel(x: Int, y: Int): Int = row(y)[x]

        fun pixelClamped(x: Int, y: Int): Int = pixel(
            x.coerceIn(0, bitmap.width - 1),
            y.coerceIn(0, bitmap.height - 1)
        )

        fun bilinear(x: Float, y: Float): Int {
            val safeX = x.coerceIn(0f, bitmap.width - 1f)
            val safeY = y.coerceIn(0f, bitmap.height - 1f)
            val x0 = floor(safeX).toInt()
            val y0 = floor(safeY).toInt()
            val x1 = min(x0 + 1, bitmap.width - 1)
            val y1 = min(y0 + 1, bitmap.height - 1)
            val top = mixPremultiplied(pixel(x0, y0), pixel(x1, y0), safeX - x0)
            val bottom = mixPremultiplied(pixel(x0, y1), pixel(x1, y1), safeX - x0)
            return mixPremultiplied(top, bottom, safeY - y0)
        }

        private fun row(y: Int): IntArray = rows.getOrPut(y) {
            IntArray(bitmap.width).also { bitmap.getPixels(it, 0, bitmap.width, 0, y, bitmap.width, 1) }
        }
    }
}
