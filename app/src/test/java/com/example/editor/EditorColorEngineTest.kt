package com.example.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class EditorColorEngineTest {

    @Test
    fun `default state builds identity color matrix`() {
        val state = EditorState()
        val matrix = EditorColorEngine.buildColorMatrix(state)
        val values = matrix.values

        assertNotNull(values)
        assertEquals(20, values.size)

        // Diagonal elements should be ~1f
        assertEquals(1f, values[0], 0.05f) // R -> R
        assertEquals(1f, values[6], 0.05f) // G -> G
        assertEquals(1f, values[12], 0.05f) // B -> B
        assertEquals(1f, values[18], 0.05f) // A -> A

        // Offsets should be 0f
        assertEquals(0f, values[4], 0.05f)
        assertEquals(0f, values[9], 0.05f)
        assertEquals(0f, values[14], 0.05f)
        assertEquals(0f, values[19], 0.05f)
    }

    @Test
    fun `positive exposure increases color matrix offset`() {
        val state = EditorState(light = LightAdjustments(exposure = 50f))
        val matrix = EditorColorEngine.buildColorMatrix(state)
        val values = matrix.values

        // Offset values should be positive
        assertEquals(64f, values[4], 5f)
        assertEquals(64f, values[9], 5f)
        assertEquals(64f, values[14], 5f)
    }

    @Test
    fun `desaturation drops to standard luminance weights`() {
        val state = EditorState(color = ColorAdjustments(saturation = -100f))
        val matrix = EditorColorEngine.buildColorMatrix(state)
        val values = matrix.values

        // In pure grayscale with neutral contrast, all rows share luminance weights (0.213, 0.715, 0.072)
        assertEquals(0.213f, values[0], 0.05f)
        assertEquals(0.715f, values[1], 0.05f)
        assertEquals(0.072f, values[2], 0.05f)
    }

    @Test
    fun `warmth adjustment biases red channel up and blue channel down`() {
        val state = EditorState(color = ColorAdjustments(warmth = 50f))
        val matrix = EditorColorEngine.buildColorMatrix(state)
        val values = matrix.values

        // Red offset is increased, Blue offset is decreased
        val redOffset = values[4]
        val blueOffset = values[14]
        assertEquals(22.5f, redOffset, 5f)
        assertEquals(-22.5f, blueOffset, 5f)
    }
}
