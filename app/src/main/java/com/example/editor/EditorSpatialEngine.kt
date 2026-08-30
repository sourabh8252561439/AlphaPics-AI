package com.example.editor

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Deterministic local detail and effects processor.
 *
 * The block API lets Android bitmap rendering keep only a narrow source stripe in managed
 * memory. Every sample is clamped to the original image bounds, alpha is averaged in
 * premultiplied form for blur safety, and procedural grain is stable for a given pixel.
 */
object EditorSpatialEngine {

    fun processPixels(
        source: IntArray,
        width: Int,
        height: Int,
        state: EditorState
    ): IntArray {
        require(width > 0 && height > 0) { "Image dimensions must be positive" }
        require(source.size == width * height) { "Pixel count does not match dimensions" }
        val plan = createPlan(state)
        if (plan.isNeutral) return source.clone()
        return IntArray(source.size).also { output ->
            processBlock(
                source = source,
                sourceStartY = 0,
                output = output,
                outputStartY = 0,
                outputRowCount = height,
                width = width,
                height = height,
                plan = plan
            )
        }
    }

    internal fun createPlan(state: EditorState): SpatialRenderPlan {
        val detail = state.detail
        val effects = state.effects
        val gaussian = effects.gaussianBlur.coerceIn(0f, 100f)
        val focus = effects.focusBlur.coerceIn(0f, 100f)
        val radial = effects.radialBlur.coerceIn(0f, 100f)
        val blurStrength = max(gaussian, focus)
        val blurRadius = if (blurStrength == 0f) 0 else {
            (1f + blurStrength / 100f * (MAX_BLUR_RADIUS - 1)).roundToInt()
        }
        val detailRadius = if (detail.isNeutral) 0 else DETAIL_RADIUS
        val radialRadius = if (radial == 0f) 0 else MAX_RADIAL_DISTANCE

        return SpatialRenderPlan(
            sharpen = detail.sharpen.coerceIn(0f, 100f),
            structure = detail.structure.coerceIn(-100f, 100f),
            clarity = detail.clarity.coerceIn(-100f, 100f),
            texture = detail.texture.coerceIn(-100f, 100f),
            noiseReduction = detail.noiseReduction.coerceIn(0f, 100f),
            dehaze = detail.dehaze.coerceIn(-100f, 100f),
            vignette = effects.vignette.coerceIn(-100f, 100f),
            grain = effects.grain.coerceIn(0f, 100f),
            fade = effects.fade.coerceIn(0f, 100f),
            gaussianBlur = gaussian,
            focusBlur = focus,
            radialBlur = radial,
            blurRadius = blurRadius,
            gaussianWeights = gaussianWeights(blurRadius),
            requiredRadius = max(detailRadius, max(blurRadius, radialRadius))
        )
    }

    internal fun processBlock(
        source: IntArray,
        sourceStartY: Int,
        output: IntArray,
        outputStartY: Int,
        outputRowCount: Int,
        width: Int,
        height: Int,
        plan: SpatialRenderPlan
    ) {
        require(output.size >= width * outputRowCount)
        if (plan.isNeutral) {
            repeat(outputRowCount) { row ->
                val sourceOffset = (outputStartY + row - sourceStartY) * width
                source.copyInto(output, row * width, sourceOffset, sourceOffset + width)
            }
            return
        }

        val reader = BlockPixelReader(source, sourceStartY, width, height)
        for (row in 0 until outputRowCount) {
            val y = outputStartY + row
            for (x in 0 until width) {
                output[row * width + x] = processPixel(reader, x, y, width, height, plan)
            }
        }
    }

    private fun processPixel(
        reader: BlockPixelReader,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        plan: SpatialRenderPlan
    ): Int {
        val original = fromArgb(reader[x, y])
        var result = original

        val needsFineDetail = plan.sharpen != 0f || plan.texture != 0f ||
            plan.noiseReduction != 0f
        val fineMean = if (needsFineDetail) average(reader, x, y, 1) else original

        if (plan.noiseReduction != 0f) {
            val colorDistance = (
                abs(original.red - fineMean.red) +
                    abs(original.green - fineMean.green) +
                    abs(original.blue - fineMean.blue)
                ) / 3f
            val edgeProtection = 0.15f +
                (1f - smoothStep(0.06f, 0.30f, colorDistance)) * 0.85f
            val blend = plan.noiseReduction / 100f * 0.88f * edgeProtection
            result = mix(result, fineMean, blend)
        }

        if (plan.sharpen != 0f) {
            result = addHighPass(result, original, fineMean, plan.sharpen / 100f * 1.25f)
        }
        if (plan.texture != 0f) {
            result = addHighPass(result, original, fineMean, plan.texture / 100f * 0.58f)
        }

        val needsBroadDetail = plan.structure != 0f || plan.clarity != 0f
        if (needsBroadDetail) {
            val broadMean = average(reader, x, y, DETAIL_RADIUS)
            if (plan.structure != 0f) {
                result = addHighPass(
                    result,
                    original,
                    broadMean,
                    plan.structure / 100f * 0.78f
                )
            }
            if (plan.clarity != 0f) {
                val localContrast = original.luminance - broadMean.luminance
                val amount = plan.clarity / 100f * 1.05f
                result = result.copy(
                    red = result.red + localContrast * amount,
                    green = result.green + localContrast * amount,
                    blue = result.blue + localContrast * amount
                )
            }
        }

        if (plan.dehaze != 0f) {
            val amount = plan.dehaze / 100f
            val contrast = 1f + amount * 0.72f
            var red = (result.red - 0.5f) * contrast + 0.5f - amount * 0.018f
            var green = (result.green - 0.5f) * contrast + 0.5f - amount * 0.018f
            var blue = (result.blue - 0.5f) * contrast + 0.5f - amount * 0.018f
            val luminance = luminance(red, green, blue)
            val saturation = max(0f, 1f + amount * 0.30f)
            red = luminance + (red - luminance) * saturation
            green = luminance + (green - luminance) * saturation
            blue = luminance + (blue - luminance) * saturation
            result = result.copy(red = red, green = green, blue = blue)
        }

        if (plan.blurRadius > 0) {
            val blurred = gaussianAverage(reader, x, y, plan.blurRadius, plan.gaussianWeights)
            val gaussianBlend = plan.gaussianBlur / 100f
            val focusBlend = plan.focusBlur / 100f * focusEdgeMask(x, y, width, height)
            val combinedBlend = 1f - (1f - gaussianBlend) * (1f - focusBlend)
            result = mix(result, blurred, combinedBlend)
        }

        if (plan.radialBlur != 0f) {
            val radial = radialAverage(reader, x, y, width, height, plan.radialBlur / 100f)
            val edgeMask = focusEdgeMask(x, y, width, height)
            result = mix(result, radial, plan.radialBlur / 100f * edgeMask * 0.86f)
        }

        if (plan.fade != 0f) {
            val amount = plan.fade / 100f
            result = result.copy(
                red = result.red * (1f - 0.20f * amount) + 0.13f * amount,
                green = result.green * (1f - 0.20f * amount) + 0.13f * amount,
                blue = result.blue * (1f - 0.20f * amount) + 0.13f * amount
            )
        }

        if (plan.vignette != 0f) {
            val edge = vignetteEdgeMask(x, y, width, height)
            val amount = plan.vignette / 100f
            val factor = if (amount >= 0f) {
                1f - edge * amount * 0.68f
            } else {
                1f + edge * -amount * 0.42f
            }
            result = result.copy(
                red = result.red * factor,
                green = result.green * factor,
                blue = result.blue * factor
            )
        }

        if (plan.grain != 0f) {
            val amount = plan.grain / 100f
            val midtoneWeight = 0.45f + (1f - abs(result.luminance * 2f - 1f)) * 0.55f
            val noise = stableNoise(x, y) * amount * 0.115f * midtoneWeight
            result = result.copy(
                red = result.red + noise,
                green = result.green + noise,
                blue = result.blue + noise
            )
        }

        return toArgb(result)
    }

    private fun addHighPass(
        base: Pixel,
        source: Pixel,
        mean: Pixel,
        amount: Float
    ): Pixel = base.copy(
        red = base.red + (source.red - mean.red) * amount,
        green = base.green + (source.green - mean.green) * amount,
        blue = base.blue + (source.blue - mean.blue) * amount
    )

    private fun average(reader: BlockPixelReader, x: Int, y: Int, radius: Int): Pixel {
        var alphaSum = 0f
        var premultipliedRed = 0f
        var premultipliedGreen = 0f
        var premultipliedBlue = 0f
        var count = 0
        for (sampleY in y - radius..y + radius) {
            for (sampleX in x - radius..x + radius) {
                val pixel = fromArgb(reader[sampleX, sampleY])
                alphaSum += pixel.alpha
                premultipliedRed += pixel.red * pixel.alpha
                premultipliedGreen += pixel.green * pixel.alpha
                premultipliedBlue += pixel.blue * pixel.alpha
                count++
            }
        }
        if (alphaSum <= 0.0001f) return Pixel(0f, 0f, 0f, 0f)
        return Pixel(
            alpha = alphaSum / count.toFloat(),
            red = premultipliedRed / alphaSum,
            green = premultipliedGreen / alphaSum,
            blue = premultipliedBlue / alphaSum
        )
    }

    private fun gaussianAverage(
        reader: BlockPixelReader,
        x: Int,
        y: Int,
        radius: Int,
        weights: FloatArray
    ): Pixel {
        var alphaSum = 0f
        var premultipliedRed = 0f
        var premultipliedGreen = 0f
        var premultipliedBlue = 0f
        var totalWeight = 0f
        for (offsetY in -radius..radius) {
            val weightY = weights[offsetY + radius]
            for (offsetX in -radius..radius) {
                val weight = weightY * weights[offsetX + radius]
                val pixel = fromArgb(reader[x + offsetX, y + offsetY])
                val alphaWeight = pixel.alpha * weight
                alphaSum += alphaWeight
                premultipliedRed += pixel.red * alphaWeight
                premultipliedGreen += pixel.green * alphaWeight
                premultipliedBlue += pixel.blue * alphaWeight
                totalWeight += weight
            }
        }
        if (alphaSum <= 0.0001f || totalWeight <= 0f) return Pixel(0f, 0f, 0f, 0f)
        return Pixel(
            alpha = alphaSum / totalWeight,
            red = premultipliedRed / alphaSum,
            green = premultipliedGreen / alphaSum,
            blue = premultipliedBlue / alphaSum
        )
    }

    private fun gaussianWeights(radius: Int): FloatArray {
        if (radius <= 0) return FloatArray(0)
        val sigma = max(0.8f, radius / 2f)
        val denominator = 2f * sigma * sigma
        val weights = FloatArray(radius * 2 + 1) { index ->
            val distance = (index - radius).toFloat()
            exp((-(distance * distance) / denominator).toDouble()).toFloat()
        }
        val sum = weights.sum().coerceAtLeast(0.0001f)
        for (index in weights.indices) weights[index] /= sum
        return weights
    }

    private fun radialAverage(
        reader: BlockPixelReader,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        strength: Float
    ): Pixel {
        val centerX = (width - 1) / 2f
        val centerY = (height - 1) / 2f
        val deltaX = x - centerX
        val deltaY = y - centerY
        val length = sqrt(deltaX * deltaX + deltaY * deltaY)
        if (length <= 0.0001f) return fromArgb(reader[x, y])
        val directionX = deltaX / length
        val directionY = deltaY / length
        val distance = MAX_RADIAL_DISTANCE * strength
        var alphaSum = 0f
        var premultipliedRed = 0f
        var premultipliedGreen = 0f
        var premultipliedBlue = 0f
        repeat(RADIAL_SAMPLES) { sample ->
            val fraction = sample / (RADIAL_SAMPLES - 1f)
            val sampleX = (x - directionX * distance * fraction).roundToInt()
            val sampleY = (y - directionY * distance * fraction).roundToInt()
            val pixel = fromArgb(reader[sampleX, sampleY])
            alphaSum += pixel.alpha
            premultipliedRed += pixel.red * pixel.alpha
            premultipliedGreen += pixel.green * pixel.alpha
            premultipliedBlue += pixel.blue * pixel.alpha
        }
        if (alphaSum <= 0.0001f) return Pixel(0f, 0f, 0f, 0f)
        return Pixel(
            alpha = alphaSum / RADIAL_SAMPLES,
            red = premultipliedRed / alphaSum,
            green = premultipliedGreen / alphaSum,
            blue = premultipliedBlue / alphaSum
        )
    }

    private fun focusEdgeMask(x: Int, y: Int, width: Int, height: Int): Float {
        val normalizedX = (x + 0.5f - width / 2f) / (width / 2f).coerceAtLeast(1f)
        val normalizedY = (y + 0.5f - height / 2f) / (height / 2f).coerceAtLeast(1f)
        val ellipticalRadius = sqrt(normalizedX * normalizedX + normalizedY * normalizedY * 1.35f)
        return smoothStep(0.34f, 0.96f, ellipticalRadius)
    }

    private fun vignetteEdgeMask(x: Int, y: Int, width: Int, height: Int): Float {
        val normalizedX = (x + 0.5f - width / 2f) / (width / 2f).coerceAtLeast(1f)
        val normalizedY = (y + 0.5f - height / 2f) / (height / 2f).coerceAtLeast(1f)
        val cornerNormalizedRadius = sqrt(normalizedX * normalizedX + normalizedY * normalizedY) / SQRT_TWO
        return smoothStep(0.34f, 1f, cornerNormalizedRadius)
    }

    private fun stableNoise(x: Int, y: Int): Float {
        var value = x * 374761393 + y * 668265263 + 0x1B873593
        value = (value xor (value ushr 13)) * 1274126177
        value = value xor (value ushr 16)
        return ((value and 0xFFFF) / 32767.5f) - 1f
    }

    private fun mix(first: Pixel, second: Pixel, amount: Float): Pixel {
        val safeAmount = amount.coerceIn(0f, 1f)
        val retained = 1f - safeAmount
        return Pixel(
            alpha = first.alpha * retained + second.alpha * safeAmount,
            red = first.red * retained + second.red * safeAmount,
            green = first.green * retained + second.green * safeAmount,
            blue = first.blue * retained + second.blue * safeAmount
        )
    }

    private fun fromArgb(argb: Int): Pixel = Pixel(
        alpha = (argb ushr 24 and 0xFF) / 255f,
        red = (argb ushr 16 and 0xFF) / 255f,
        green = (argb ushr 8 and 0xFF) / 255f,
        blue = (argb and 0xFF) / 255f
    )

    private fun toArgb(pixel: Pixel): Int =
        (toChannel(pixel.alpha) shl 24) or
            (toChannel(pixel.red) shl 16) or
            (toChannel(pixel.green) shl 8) or
            toChannel(pixel.blue)

    private fun toChannel(value: Float): Int =
        (value.coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255)

    private fun luminance(red: Float, green: Float, blue: Float): Float =
        red * 0.2126f + green * 0.7152f + blue * 0.0722f

    private fun smoothStep(edge0: Float, edge1: Float, value: Float): Float {
        val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    internal data class SpatialRenderPlan(
        val sharpen: Float,
        val structure: Float,
        val clarity: Float,
        val texture: Float,
        val noiseReduction: Float,
        val dehaze: Float,
        val vignette: Float,
        val grain: Float,
        val fade: Float,
        val gaussianBlur: Float,
        val focusBlur: Float,
        val radialBlur: Float,
        val blurRadius: Int,
        val gaussianWeights: FloatArray,
        val requiredRadius: Int
    ) {
        val isNeutral: Boolean
            get() = sharpen == 0f && structure == 0f && clarity == 0f && texture == 0f &&
                noiseReduction == 0f && dehaze == 0f && vignette == 0f && grain == 0f &&
                fade == 0f && gaussianBlur == 0f && focusBlur == 0f && radialBlur == 0f
    }

    private class BlockPixelReader(
        private val pixels: IntArray,
        private val startY: Int,
        private val width: Int,
        private val height: Int
    ) {
        operator fun get(x: Int, y: Int): Int {
            val safeX = x.coerceIn(0, width - 1)
            val safeY = y.coerceIn(0, height - 1)
            val localY = safeY - startY
            check(localY >= 0 && localY * width + safeX < pixels.size) {
                "Spatial source stripe does not cover the requested sample"
            }
            return pixels[localY * width + safeX]
        }
    }

    private data class Pixel(
        val alpha: Float,
        val red: Float,
        val green: Float,
        val blue: Float
    ) {
        val luminance: Float
            get() = EditorSpatialEngine.luminance(red, green, blue)
    }

    private const val DETAIL_RADIUS = 2
    private const val MAX_BLUR_RADIUS = 6
    private const val MAX_RADIAL_DISTANCE = 8
    private const val RADIAL_SAMPLES = 7
    private const val SQRT_TWO = 1.41421356f
}
