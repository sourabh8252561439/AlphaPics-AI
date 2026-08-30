package com.example.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorPixelEngineTest {

    @Test
    fun `neutral state preserves every argb channel exactly`() {
        val source = intArrayOf(
            0x00112233,
            0x7FABCDEF,
            0xFFFFFFFF.toInt(),
            0xFF000000.toInt()
        )

        val result = EditorPixelEngine.processPixels(source, EditorState())

        assertTrue(source.contentEquals(result))
        assertTrue(source !== result)
    }

    @Test
    fun `positive exposure increases midtone luminance`() {
        val source = 0xFF606060.toInt()
        val result = EditorPixelEngine.processPixel(
            source,
            EditorState(light = LightAdjustments(exposure = 35f))
        )

        assertTrue(red(result) > red(source))
        assertEquals(red(result), green(result))
        assertEquals(green(result), blue(result))
    }

    @Test
    fun `highlights target bright pixels more than dark pixels`() {
        val dark = 0xFF303030.toInt()
        val bright = 0xFFD0D0D0.toInt()
        val state = EditorState(light = LightAdjustments(highlights = 50f))

        val darkResult = EditorPixelEngine.processPixel(dark, state)
        val brightResult = EditorPixelEngine.processPixel(bright, state)

        assertTrue(red(brightResult) - red(bright) > red(darkResult) - red(dark))
    }

    @Test
    fun `shadows target dark pixels more than bright pixels`() {
        val dark = 0xFF303030.toInt()
        val bright = 0xFFD0D0D0.toInt()
        val state = EditorState(light = LightAdjustments(shadows = 50f))

        val darkResult = EditorPixelEngine.processPixel(dark, state)
        val brightResult = EditorPixelEngine.processPixel(bright, state)

        assertTrue(red(darkResult) - red(dark) > red(brightResult) - red(bright))
    }

    @Test
    fun `positive gamma brightens midtones while retaining endpoints`() {
        val black = 0xFF000000.toInt()
        val mid = 0xFF808080.toInt()
        val white = 0xFFFFFFFF.toInt()
        val state = EditorState(light = LightAdjustments(gamma = 50f))

        assertEquals(0, red(EditorPixelEngine.processPixel(black, state)))
        assertTrue(red(EditorPixelEngine.processPixel(mid, state)) > red(mid))
        assertEquals(255, red(EditorPixelEngine.processPixel(white, state)))
    }

    @Test
    fun `temperature warms red and cools blue channels`() {
        val neutral = 0xFF808080.toInt()
        val result = EditorPixelEngine.processPixel(
            neutral,
            EditorState(color = ColorAdjustments(warmth = 60f))
        )

        assertTrue(red(result) > red(neutral))
        assertTrue(blue(result) < blue(neutral))
    }

    @Test
    fun `full desaturation produces luminance gray`() {
        val source = 0xFFC04020.toInt()
        val result = EditorPixelEngine.processPixel(
            source,
            EditorState(color = ColorAdjustments(saturation = -100f))
        )

        assertTrue(kotlin.math.abs(red(result) - green(result)) <= 1)
        assertTrue(kotlin.math.abs(green(result) - blue(result)) <= 1)
    }

    @Test
    fun `vibrance leaves neutral gray stable and increases muted color separation`() {
        val gray = 0xFF777777.toInt()
        val muted = 0xFF8A786A.toInt()
        val state = EditorState(color = ColorAdjustments(vibrance = 70f))

        val grayResult = EditorPixelEngine.processPixel(gray, state)
        val mutedResult = EditorPixelEngine.processPixel(muted, state)

        assertTrue(kotlin.math.abs(red(gray) - red(grayResult)) <= 1)
        assertTrue(channelRange(mutedResult) > channelRange(muted))
    }

    @Test
    fun `red HSL saturation targets muted red without changing blue`() {
        val mutedRed = 0xFF9A665E.toInt()
        val blueTarget = 0xFF5E669A.toInt()
        val adjustment = HslAdjustments().update(
            HslColorChannel.RED,
            HslChannelAdjustment(saturation = 80f)
        )
        val state = EditorState(hsl = adjustment)

        val redResult = EditorPixelEngine.processPixel(mutedRed, state)
        val blueResult = EditorPixelEngine.processPixel(blueTarget, state)

        assertTrue(channelRange(redResult) > channelRange(mutedRed))
        assertEquals(blueTarget, blueResult)
    }

    @Test
    fun `red HSL hue rotates the selected color`() {
        val source = 0xFFCC453D.toInt()
        val state = EditorState(
            hsl = HslAdjustments().update(
                HslColorChannel.RED,
                HslChannelAdjustment(hue = 100f)
            )
        )

        val result = EditorPixelEngine.processPixel(source, state)

        assertTrue(green(result) > green(source))
        assertTrue(result != source)
    }

    @Test
    fun `red HSL luminance targets red more than blue`() {
        val redTarget = 0xFFB04A42.toInt()
        val blueTarget = 0xFF424AB0.toInt()
        val state = EditorState(
            hsl = HslAdjustments().update(
                HslColorChannel.RED,
                HslChannelAdjustment(luminance = 60f)
            )
        )

        val redResult = EditorPixelEngine.processPixel(redTarget, state)
        val blueResult = EditorPixelEngine.processPixel(blueTarget, state)

        assertTrue(red(redResult) > red(redTarget))
        assertEquals(blueTarget, blueResult)
    }

    @Test
    fun `all eight HSL channels retain independent adjustments`() {
        val adjustments = HslColorChannel.entries.fold(HslAdjustments()) { state, channel ->
            state.update(channel, HslChannelAdjustment(hue = channel.ordinal + 1f))
        }

        assertTrue(!adjustments.isNeutral)
        HslColorChannel.entries.forEach { channel ->
            assertEquals(channel.ordinal + 1f, adjustments[channel].hue)
        }
    }

    @Test
    fun `RGB color mix changes only the selected channel gain`() {
        val source = 0xFF406080.toInt()
        val result = EditorPixelEngine.processPixel(
            source,
            EditorState(colorMix = ColorMixAdjustments(red = 100f))
        )

        assertTrue(red(result) > red(source))
        assertEquals(green(source), green(result))
        assertEquals(blue(source), blue(result))
    }

    @Test
    fun `split toning targets blue shadows without tinting bright highlights`() {
        val shadow = 0xFF303030.toInt()
        val highlight = 0xFFD8D8D8.toInt()
        val state = EditorState(
            splitTone = SplitToneAdjustments(
                shadowHue = 220f,
                shadowSaturation = 100f,
                highlightSaturation = 0f
            )
        )

        val shadowResult = EditorPixelEngine.processPixel(shadow, state)
        val highlightResult = EditorPixelEngine.processPixel(highlight, state)

        assertTrue(blue(shadowResult) > red(shadowResult))
        assertTrue(channelRange(shadowResult) > channelRange(highlightResult))
    }

    @Test
    fun `split toning targets warm highlights independently`() {
        val source = 0xFFD8D8D8.toInt()
        val result = EditorPixelEngine.processPixel(
            source,
            EditorState(
                splitTone = SplitToneAdjustments(
                    shadowSaturation = 0f,
                    highlightHue = 35f,
                    highlightSaturation = 100f
                )
            )
        )

        assertTrue(red(result) > blue(result))
        assertTrue(green(result) > blue(result))
    }

    @Test
    fun `midtone color grading adds the selected hue to middle gray`() {
        val source = 0xFF808080.toInt()
        val grading = ColorGradingAdjustments().update(
            ColorGradeRegion.MIDTONES,
            ColorGradeRange(hue = 120f, saturation = 100f)
        )
        val result = EditorPixelEngine.processPixel(source, EditorState(colorGrading = grading))

        assertTrue(green(result) > red(result))
        assertTrue(green(result) > blue(result))
    }

    @Test
    fun `grading luminance honors the selected tonal range`() {
        val dark = 0xFF303030.toInt()
        val bright = 0xFFD0D0D0.toInt()
        val grading = ColorGradingAdjustments().update(
            ColorGradeRegion.SHADOWS,
            ColorGradeRange(hue = 220f, luminance = 60f)
        )
        val state = EditorState(colorGrading = grading)

        val darkLift = red(EditorPixelEngine.processPixel(dark, state)) - red(dark)
        val brightLift = red(EditorPixelEngine.processPixel(bright, state)) - red(bright)

        assertTrue(darkLift > brightLift)
    }

    @Test
    fun `all color grading regions retain independent settings`() {
        val grading = ColorGradeRegion.entries.fold(ColorGradingAdjustments()) { state, region ->
            state.update(region, ColorGradeRange(hue = region.ordinal * 90f, saturation = 30f))
        }

        assertTrue(!grading.isNeutral)
        ColorGradeRegion.entries.forEach { region ->
            assertEquals(region.ordinal * 90f, grading[region].hue)
            assertEquals(30f, grading[region].saturation)
        }
    }

    @Test
    fun `processing always preserves source alpha`() {
        val source = 0x4A336699
        val result = EditorPixelEngine.processPixel(
            source,
            EditorState(
                light = LightAdjustments(exposure = 80f, contrast = 40f, gamma = -30f),
                color = ColorAdjustments(saturation = 50f, warmth = -40f, tint = 25f),
                colorMix = ColorMixAdjustments(red = 10f),
                splitTone = SplitToneAdjustments(shadowSaturation = 25f),
                colorGrading = ColorGradingAdjustments(
                    midtones = ColorGradeRange(hue = 120f, saturation = 20f)
                ),
                hsl = HslAdjustments(red = HslChannelAdjustment(hue = 25f)),
                curves = CurvesAdjustments(
                    master = ToneCurve(listOf(CurvePoint(0f, 0f), CurvePoint(0.5f, 0.6f), CurvePoint(1f, 1f)))
                )
            )
        )

        assertEquals(alpha(source), alpha(result))
    }

    private fun alpha(color: Int): Int = color ushr 24 and 0xFF
    private fun red(color: Int): Int = color ushr 16 and 0xFF
    private fun green(color: Int): Int = color ushr 8 and 0xFF
    private fun blue(color: Int): Int = color and 0xFF

    private fun channelRange(color: Int): Int {
        val channels = listOf(red(color), green(color), blue(color))
        return channels.max() - channels.min()
    }
}
