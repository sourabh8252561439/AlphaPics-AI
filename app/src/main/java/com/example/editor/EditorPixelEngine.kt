package com.example.editor

import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Shared nonlinear tone and color renderer used by both preview and export.
 *
 * The engine deliberately operates on un-premultiplied ARGB channel values and always preserves
 * source alpha. All public adjustment values use the editor's -100..100 convention. Keeping this
 * implementation independent from Android Bitmap APIs makes the color math deterministic and
 * directly unit-testable.
 */
object EditorPixelEngine {

    fun processPixels(source: IntArray, state: EditorState): IntArray {
        val plan = createPlan(state)
        if (plan.isNeutral) return source.clone()

        return source.clone().also { pixels ->
            processPixelsInPlace(pixels, plan)
        }
    }

    fun processPixel(argb: Int, state: EditorState): Int {
        val plan = createPlan(state)
        return if (plan.isNeutral) argb else processPixel(argb, plan)
    }

    internal fun processPixelsInPlace(pixels: IntArray, state: EditorState) {
        val plan = createPlan(state)
        if (!plan.isNeutral) processPixelsInPlace(pixels, plan)
    }

    internal fun processPixelsInPlace(pixels: IntArray, plan: RenderPlan) {
        if (plan.isNeutral) return
        for (index in pixels.indices) {
            pixels[index] = processPixel(pixels[index], plan)
        }
    }

    private fun processPixel(argb: Int, settings: RenderPlan): Int {
        val alpha = argb ushr 24 and 0xFF
        var red = (argb ushr 16 and 0xFF) / 255f
        var green = (argb ushr 8 and 0xFF) / 255f
        var blue = (argb and 0xFF) / 255f

        // Manual white balance. Positive temperature adds warmth; positive tint adds magenta.
        val temperature = settings.temperature / 100f
        val tint = settings.tint / 100f
        red *= 1f + 0.20f * temperature + 0.07f * tint
        green *= 1f + 0.035f * temperature - 0.14f * tint
        blue *= 1f - 0.20f * temperature + 0.07f * tint

        // Exposure is multiplicative, expressed as a safe +/-2 stop range.
        val exposureMultiplier = 2f.pow(settings.exposure / 50f)
        red *= exposureMultiplier
        green *= exposureMultiplier
        blue *= exposureMultiplier

        val tonalLuminance = luminance(red, green, blue).coerceIn(0f, 1f)
        val shadowMask = 1f - smoothStep(0.05f, 0.62f, tonalLuminance)
        val highlightMask = smoothStep(0.38f, 0.95f, tonalLuminance)
        val blackMask = 1f - smoothStep(0f, 0.30f, tonalLuminance)
        val whiteMask = smoothStep(0.70f, 1f, tonalLuminance)

        val rangeOffset =
            (settings.shadows / 100f) * 0.34f * shadowMask +
                (settings.highlights / 100f) * 0.34f * highlightMask +
                (settings.blacks / 100f) * 0.24f * blackMask +
                (settings.whites / 100f) * 0.24f * whiteMask +
                (settings.brightness / 100f) * 0.28f

        red += rangeOffset
        green += rangeOffset
        blue += rangeOffset

        // Contrast pivots around perceptual middle gray.
        val contrastFactor = 2f.pow(settings.contrast / 100f)
        red = (red - 0.5f) * contrastFactor + 0.5f
        green = (green - 0.5f) * contrastFactor + 0.5f
        blue = (blue - 0.5f) * contrastFactor + 0.5f

        // Positive gamma brightens midtones; endpoints remain fixed.
        if (settings.gamma != 0f) {
            val exponent = 2f.pow(-settings.gamma / 100f)
            red = red.coerceIn(0f, 1f).pow(exponent)
            green = green.coerceIn(0f, 1f).pow(exponent)
            blue = blue.coerceIn(0f, 1f).pow(exponent)
        }

        // Saturation is global. Vibrance adapts to existing channel separation so already vivid
        // colors are protected and neutral pixels stay neutral.
        var colorLuminance = luminance(red, green, blue)
        val saturationFactor = max(0f, 1f + settings.saturation / 100f)
        red = colorLuminance + (red - colorLuminance) * saturationFactor
        green = colorLuminance + (green - colorLuminance) * saturationFactor
        blue = colorLuminance + (blue - colorLuminance) * saturationFactor

        colorLuminance = luminance(red, green, blue)
        val channelMax = max(red, max(green, blue))
        val channelMin = min(red, min(green, blue))
        val existingSeparation = (channelMax - channelMin).coerceIn(0f, 1f)
        val vibranceFactor = max(
            0f,
            1f + (settings.vibrance / 100f) * (1f - existingSeparation) * 0.85f
        )
        red = colorLuminance + (red - colorLuminance) * vibranceFactor
        green = colorLuminance + (green - colorLuminance) * vibranceFactor
        blue = colorLuminance + (blue - colorLuminance) * vibranceFactor

        if (!settings.hsl.isNeutral) {
            val adjusted = applyHsl(red, green, blue, settings.hsl)
            red = adjusted.red
            green = adjusted.green
            blue = adjusted.blue
        }

        if (!settings.colorMix.isNeutral) {
            red *= max(0f, 1f + settings.colorMix.red / 100f)
            green *= max(0f, 1f + settings.colorMix.green / 100f)
            blue *= max(0f, 1f + settings.colorMix.blue / 100f)
        }

        var graded = Rgb(red, green, blue)
        if (!settings.splitTone.isNeutral) {
            graded = applySplitTone(graded, settings.splitTone)
        }
        if (!settings.colorGrading.isNeutral) {
            graded = applyColorGrading(graded, settings.colorGrading)
        }
        red = graded.red
        green = graded.green
        blue = graded.blue

        val masterRed = settings.masterLut[toChannel(red)]
        val masterGreen = settings.masterLut[toChannel(green)]
        val masterBlue = settings.masterLut[toChannel(blue)]
        val curvedRed = settings.redLut[masterRed]
        val curvedGreen = settings.greenLut[masterGreen]
        val curvedBlue = settings.blueLut[masterBlue]

        return alpha shl 24 or
            (curvedRed shl 16) or
            (curvedGreen shl 8) or
            curvedBlue
    }

    internal fun createPlan(state: EditorState): RenderPlan {
        val preset = FilterPresetCatalog.find(state.filter.presetId)
        val presetWeight = state.filter.intensity.coerceIn(0f, 100f) / 100f

        return RenderPlan(
            exposure = clamp(state.light.exposure + preset.exposure * presetWeight),
            brightness = clamp(state.light.brightness),
            contrast = clamp(state.light.contrast + preset.contrast * presetWeight),
            highlights = clamp(state.light.highlights),
            shadows = clamp(state.light.shadows),
            whites = clamp(state.light.whites),
            blacks = clamp(state.light.blacks),
            gamma = clamp(state.light.gamma),
            saturation = clamp(state.color.saturation + preset.saturation * presetWeight),
            vibrance = clamp(state.color.vibrance),
            temperature = clamp(state.color.warmth + preset.warmth * presetWeight),
            tint = clamp(state.color.tint + preset.tint * presetWeight),
            hsl = state.hsl,
            colorMix = state.colorMix,
            splitTone = state.splitTone,
            colorGrading = state.colorGrading,
            masterLut = CurveEngine.buildLut(state.curves.master),
            redLut = CurveEngine.buildLut(state.curves.red),
            greenLut = CurveEngine.buildLut(state.curves.green),
            blueLut = CurveEngine.buildLut(state.curves.blue),
            curvesNeutral = state.curves.isNeutral
        )
    }

    private fun applySplitTone(rgb: Rgb, settings: SplitToneAdjustments): Rgb {
        val tonalLuminance = luminance(rgb.red, rgb.green, rgb.blue).coerceIn(0f, 1f)
        val pivot = 0.5f + settings.balance.coerceIn(-100f, 100f) / 100f * 0.20f
        val shadowWeight = 1f - smoothStep(pivot - 0.28f, pivot + 0.10f, tonalLuminance)
        val highlightWeight = smoothStep(pivot - 0.10f, pivot + 0.28f, tonalLuminance)
        var result = applyTonalGrade(
            rgb,
            hue = settings.shadowHue,
            saturation = settings.shadowSaturation,
            luminanceShift = 0f,
            weight = shadowWeight
        )
        result = applyTonalGrade(
            result,
            hue = settings.highlightHue,
            saturation = settings.highlightSaturation,
            luminanceShift = 0f,
            weight = highlightWeight
        )
        return result
    }

    private fun applyColorGrading(rgb: Rgb, settings: ColorGradingAdjustments): Rgb {
        val tonalLuminance = luminance(rgb.red, rgb.green, rgb.blue).coerceIn(0f, 1f)
        val shadowWeight = 1f - smoothStep(0.12f, 0.56f, tonalLuminance)
        val highlightWeight = smoothStep(0.44f, 0.88f, tonalLuminance)
        val midtoneWeight = (1f - max(shadowWeight, highlightWeight)).coerceIn(0f, 1f)
        var result = applyGradeRange(rgb, settings.shadows, shadowWeight)
        result = applyGradeRange(result, settings.midtones, midtoneWeight)
        result = applyGradeRange(result, settings.highlights, highlightWeight)
        return result
    }

    private fun applyGradeRange(rgb: Rgb, range: ColorGradeRange, weight: Float): Rgb =
        applyTonalGrade(
            rgb = rgb,
            hue = range.hue,
            saturation = range.saturation,
            luminanceShift = range.luminance,
            weight = weight
        )

    private fun applyTonalGrade(
        rgb: Rgb,
        hue: Float,
        saturation: Float,
        luminanceShift: Float,
        weight: Float
    ): Rgb {
        if (weight <= 0f || (saturation == 0f && luminanceShift == 0f)) return rgb
        val baseLuminance = luminance(rgb.red, rgb.green, rgb.blue)
        val tone = hslToRgb(hue, 1f, 0.5f)
        val neutralTone = Rgb(
            red = baseLuminance + (tone.red - 0.5f) * 0.78f,
            green = baseLuminance + (tone.green - 0.5f) * 0.78f,
            blue = baseLuminance + (tone.blue - 0.5f) * 0.78f
        )
        val tintAmount = saturation.coerceIn(0f, 100f) / 100f * weight * 0.46f
        val tinted = mixRgb(rgb, neutralTone, tintAmount)
        val offset = luminanceShift.coerceIn(-100f, 100f) / 100f * weight * 0.24f
        return tinted.copy(
            red = tinted.red + offset,
            green = tinted.green + offset,
            blue = tinted.blue + offset
        )
    }

    private fun mixRgb(first: Rgb, second: Rgb, amount: Float): Rgb {
        val safeAmount = amount.coerceIn(0f, 1f)
        val retained = 1f - safeAmount
        return Rgb(
            red = first.red * retained + second.red * safeAmount,
            green = first.green * retained + second.green * safeAmount,
            blue = first.blue * retained + second.blue * safeAmount
        )
    }

    private fun applyHsl(
        red: Float,
        green: Float,
        blue: Float,
        adjustments: HslAdjustments
    ): Rgb {
        val hsl = rgbToHsl(red.coerceIn(0f, 1f), green.coerceIn(0f, 1f), blue.coerceIn(0f, 1f))
        if (hsl.saturation <= 0.0001f) return Rgb(red, green, blue)

        var weightTotal = 0f
        var hueShift = 0f
        var saturationShift = 0f
        var luminanceShift = 0f
        for (channel in HslColorChannel.entries) {
            val distance = circularHueDistance(hsl.hue, channel.centerHue)
            val linearWeight = (1f - distance / HSL_INFLUENCE_DEGREES).coerceIn(0f, 1f)
            val weight = linearWeight * linearWeight
            if (weight == 0f) continue
            val adjustment = adjustments[channel]
            weightTotal += weight
            hueShift += adjustment.hue * weight
            saturationShift += adjustment.saturation * weight
            luminanceShift += adjustment.luminance * weight
        }

        if (weightTotal <= 0f) return Rgb(red, green, blue)
        val normalization = max(1f, weightTotal)
        val adjustedHue = wrapHue(
            hsl.hue + (hueShift / normalization / 100f) * MAX_HUE_SHIFT_DEGREES
        )
        val adjustedSaturation = (
            hsl.saturation * (1f + saturationShift / normalization / 100f)
            ).coerceIn(0f, 1f)
        val adjustedLuminance = (
            hsl.luminance + (luminanceShift / normalization / 100f) * MAX_LUMINANCE_SHIFT
            ).coerceIn(0f, 1f)
        return hslToRgb(adjustedHue, adjustedSaturation, adjustedLuminance)
    }

    private fun rgbToHsl(red: Float, green: Float, blue: Float): Hsl {
        val maximum = max(red, max(green, blue))
        val minimum = min(red, min(green, blue))
        val delta = maximum - minimum
        val luminance = (maximum + minimum) / 2f
        if (delta <= 0.0001f) return Hsl(0f, 0f, luminance)

        val saturation = delta / (1f - abs(2f * luminance - 1f)).coerceAtLeast(0.0001f)
        val hueSector = when (maximum) {
            red -> ((green - blue) / delta) % 6f
            green -> (blue - red) / delta + 2f
            else -> (red - green) / delta + 4f
        }
        return Hsl(wrapHue(hueSector * 60f), saturation.coerceIn(0f, 1f), luminance)
    }

    private fun hslToRgb(hue: Float, saturation: Float, luminance: Float): Rgb {
        val chroma = (1f - abs(2f * luminance - 1f)) * saturation
        val sector = wrapHue(hue) / 60f
        val secondary = chroma * (1f - abs(sector % 2f - 1f))
        val base = when (sector.toInt().coerceIn(0, 5)) {
            0 -> Rgb(chroma, secondary, 0f)
            1 -> Rgb(secondary, chroma, 0f)
            2 -> Rgb(0f, chroma, secondary)
            3 -> Rgb(0f, secondary, chroma)
            4 -> Rgb(secondary, 0f, chroma)
            else -> Rgb(chroma, 0f, secondary)
        }
        val match = luminance - chroma / 2f
        return Rgb(base.red + match, base.green + match, base.blue + match)
    }

    private fun circularHueDistance(first: Float, second: Float): Float {
        val direct = abs(first - second) % 360f
        return min(direct, 360f - direct)
    }

    private fun wrapHue(hue: Float): Float = ((hue % 360f) + 360f) % 360f

    private fun smoothStep(edge0: Float, edge1: Float, value: Float): Float {
        val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun luminance(red: Float, green: Float, blue: Float): Float =
        red * 0.2126f + green * 0.7152f + blue * 0.0722f

    private fun toChannel(value: Float): Int =
        (value.coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255)

    private fun clamp(value: Float): Float = value.coerceIn(-100f, 100f)

    internal data class RenderPlan(
        val exposure: Float,
        val brightness: Float,
        val contrast: Float,
        val highlights: Float,
        val shadows: Float,
        val whites: Float,
        val blacks: Float,
        val gamma: Float,
        val saturation: Float,
        val vibrance: Float,
        val temperature: Float,
        val tint: Float,
        val hsl: HslAdjustments,
        val colorMix: ColorMixAdjustments,
        val splitTone: SplitToneAdjustments,
        val colorGrading: ColorGradingAdjustments,
        val masterLut: IntArray,
        val redLut: IntArray,
        val greenLut: IntArray,
        val blueLut: IntArray,
        val curvesNeutral: Boolean
    ) {
        val isNeutral: Boolean
            get() = exposure == 0f && brightness == 0f && contrast == 0f &&
                highlights == 0f && shadows == 0f && whites == 0f && blacks == 0f &&
                gamma == 0f && saturation == 0f && vibrance == 0f &&
                temperature == 0f && tint == 0f && hsl.isNeutral && colorMix.isNeutral &&
                splitTone.isNeutral && colorGrading.isNeutral && curvesNeutral
    }

    private data class Hsl(val hue: Float, val saturation: Float, val luminance: Float)

    private data class Rgb(val red: Float, val green: Float, val blue: Float)

    private const val HSL_INFLUENCE_DEGREES = 45f
    private const val MAX_HUE_SHIFT_DEGREES = 30f
    private const val MAX_LUMINANCE_SHIFT = 0.35f
}
