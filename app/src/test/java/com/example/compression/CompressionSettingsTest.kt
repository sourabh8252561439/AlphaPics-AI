package com.example.compression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompressionSettingsTest {

    @Test
    fun `target input starts empty without a zero`() {
        val state = TargetSizeInputState()
        assertEquals("", state.text)
        assertNull(state.committedKilobytes)
    }

    @Test
    fun `common target values remain exact`() {
        listOf("25", "30", "35", "50", "500").forEach { input ->
            val commit = TargetSizeInputState().withText(input).commit()
            assertTrue(commit.isValid)
            assertEquals(input, commit.state.text)
            assertEquals(input.toInt(), commit.state.committedKilobytes)
        }
    }

    @Test
    fun `leading zero is normalized only when committed`() {
        val draft = TargetSizeInputState().withText("025")
        assertEquals("025", draft.text)
        assertEquals("25", draft.commit().state.text)
    }

    @Test
    fun `non digits are removed without inserting a value`() {
        assertEquals("2550", TargetSizeInputState().withText("2a5.5 KB0").text)
        assertEquals("", TargetSizeInputState().withText("abc").text)
    }

    @Test
    fun `out of range values clamp at commit`() {
        val low = TargetSizeInputState().withText("2").commit()
        val high = TargetSizeInputState().withText("999999").commit()

        assertEquals(TargetSizeRules.MIN_KB, low.state.committedKilobytes)
        assertEquals(TargetSizeRules.MAX_KB, high.state.committedKilobytes)
        assertTrue(low.wasClamped)
        assertTrue(high.wasClamped)
    }

    @Test
    fun `empty target is invalid for processing`() {
        val validation = CompressionSettingsState().validateForProcessing()
        assertTrue(validation is SettingsValidation.Invalid)
    }

    @Test
    fun `slider preserves continuous position while synchronizing text`() {
        val position = 0.37123f
        val state = TargetSizeInputState().withSliderPosition(position)
        assertEquals(position, state.sliderPosition, 0.000001f)
        assertEquals(state.committedKilobytes.toString(), state.text)
    }

    @Test
    fun `preset synchronizes text committed value and slider`() {
        val state = TargetSizeInputState().withPreset(50)
        assertEquals("50", state.text)
        assertEquals(50, state.committedKilobytes)
        assertEquals(
            TargetSizeRules.kbToSliderPosition(50f),
            state.sliderPosition,
            0.000001f
        )
    }

    @Test
    fun `quality snapshot rounds display value only at processing`() {
        val state = CompressionSettingsState(
            mode = CompressionMode.QUALITY,
            qualitySliderValue = 84.6f
        )
        val validation = state.validateForProcessing() as SettingsValidation.Valid
        assertEquals(85, (validation.snapshot as CompressionSettingsSnapshot.Quality).percentage)
        assertFalse(validation.wasClamped)
    }

    @Test
    fun `unknown stored mode safely maps to supported default`() {
        assertEquals(CompressionMode.TARGET_SIZE, CompressionMode.fromStored("retired_mode"))
        assertEquals(CompressionMode.TARGET_SIZE, CompressionMode.fromStored(null))
    }
}
