package com.example.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorSpatialEngineTest {

    @Test
    fun `neutral spatial state preserves pixels exactly in an independent array`() {
        val source = intArrayOf(0x00112233, 0x7F445566, 0xFFFFFFFF.toInt(), 0xFF000000.toInt())

        val result = EditorSpatialEngine.processPixels(source, 2, 2, EditorState())

        assertTrue(source.contentEquals(result))
        assertTrue(source !== result)
    }

    @Test
    fun `sharpen increases contrast across a hard edge`() {
        val source = edgeImage(7, 5, 0xFF505050.toInt(), 0xFFA0A0A0.toInt())
        val result = EditorSpatialEngine.processPixels(
            source,
            7,
            5,
            EditorState(detail = DetailAdjustments(sharpen = 80f))
        )

        val darkEdge = result[2 + 2 * 7]
        val brightEdge = result[3 + 2 * 7]
        assertTrue(red(darkEdge) < 0x50)
        assertTrue(red(brightEdge) > 0xA0)
    }

    @Test
    fun `structure clarity and texture each produce a real local result`() {
        val source = edgeImage(7, 5, 0xFF606060.toInt(), 0xFFA0A0A0.toInt())
        val states = listOf(
            EditorState(detail = DetailAdjustments(structure = 70f)),
            EditorState(detail = DetailAdjustments(clarity = 70f)),
            EditorState(detail = DetailAdjustments(texture = 70f))
        )

        states.forEach { state ->
            val result = EditorSpatialEngine.processPixels(source, 7, 5, state)
            assertTrue(!source.contentEquals(result))
        }
    }

    @Test
    fun `noise reduction suppresses an isolated bright impulse`() {
        val source = IntArray(25) { 0xFF606060.toInt() }.also { it[12] = 0xFFFFFFFF.toInt() }
        val result = EditorSpatialEngine.processPixels(
            source,
            5,
            5,
            EditorState(detail = DetailAdjustments(noiseReduction = 100f))
        )

        assertTrue(red(result[12]) < 255)
        assertTrue(red(result[12]) > 0x60)
    }

    @Test
    fun `positive dehaze increases tonal and color separation`() {
        val source = intArrayOf(0xFF6D7378.toInt())
        val result = EditorSpatialEngine.processPixels(
            source,
            1,
            1,
            EditorState(detail = DetailAdjustments(dehaze = 70f))
        )[0]

        assertTrue(channelRange(result) > channelRange(source[0]))
        assertTrue(result != source[0])
    }

    @Test
    fun `vignette darkens corners while preserving the center`() {
        val source = IntArray(81) { 0xFFC0C0C0.toInt() }
        val result = EditorSpatialEngine.processPixels(
            source,
            9,
            9,
            EditorState(effects = EffectAdjustments(vignette = 100f))
        )

        assertEquals(0xC0, red(result[4 + 4 * 9]))
        assertTrue(red(result[0]) < red(result[4 + 4 * 9]))
    }

    @Test
    fun `grain is deterministic and visibly changes a flat image`() {
        val source = IntArray(49) { 0xFF808080.toInt() }
        val state = EditorState(effects = EffectAdjustments(grain = 65f))

        val first = EditorSpatialEngine.processPixels(source, 7, 7, state)
        val second = EditorSpatialEngine.processPixels(source, 7, 7, state)

        assertTrue(first.contentEquals(second))
        assertTrue(!source.contentEquals(first))
    }

    @Test
    fun `fade lifts black and softens white`() {
        val source = intArrayOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt())
        val result = EditorSpatialEngine.processPixels(
            source,
            2,
            1,
            EditorState(effects = EffectAdjustments(fade = 100f))
        )

        assertTrue(red(result[0]) > 0)
        assertTrue(red(result[1]) < 255)
    }

    @Test
    fun `gaussian blur spreads an impulse and lowers its center`() {
        val source = IntArray(81) { 0xFF000000.toInt() }.also { it[40] = 0xFFFFFFFF.toInt() }
        val result = EditorSpatialEngine.processPixels(
            source,
            9,
            9,
            EditorState(effects = EffectAdjustments(gaussianBlur = 100f))
        )

        assertTrue(red(result[40]) < 255)
        assertTrue(red(result[39]) > 0)
    }

    @Test
    fun `focus blur protects the center and blurs the outer image`() {
        val source = checkerboard(11, 11)
        val result = EditorSpatialEngine.processPixels(
            source,
            11,
            11,
            EditorState(effects = EffectAdjustments(focusBlur = 100f))
        )

        assertEquals(source[5 + 5 * 11], result[5 + 5 * 11])
        assertTrue(source[0] != result[0])
    }

    @Test
    fun `radial blur changes outer detail while protecting the center`() {
        val source = checkerboard(17, 17)
        val result = EditorSpatialEngine.processPixels(
            source,
            17,
            17,
            EditorState(effects = EffectAdjustments(radialBlur = 100f))
        )

        assertEquals(source[8 + 8 * 17], result[8 + 8 * 17])
        assertTrue(source.indices.any { index ->
            val x = index % 17
            val y = index / 17
            (x < 4 || x > 12 || y < 4 || y > 12) && source[index] != result[index]
        })
    }

    @Test
    fun `blur averages alpha without introducing dark color halos`() {
        val source = IntArray(25) { 0x00000000 }.also { it[12] = 0xFFFF0000.toInt() }
        val result = EditorSpatialEngine.processPixels(
            source,
            5,
            5,
            EditorState(effects = EffectAdjustments(gaussianBlur = 100f))
        )
        val neighbor = result[11]

        assertTrue(alpha(neighbor) > 0)
        assertTrue(red(neighbor) > 240)
        assertEquals(0, green(neighbor))
        assertEquals(0, blue(neighbor))
    }

    @Test
    fun `striped block rendering matches whole image rendering`() {
        val width = 13
        val height = 10
        val source = IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            0xFF000000.toInt() or ((x * 17) shl 16) or ((y * 21) shl 8) or ((x + y) * 9)
        }
        val state = EditorState(
            detail = DetailAdjustments(sharpen = 40f, clarity = 30f, noiseReduction = 20f),
            effects = EffectAdjustments(vignette = 35f, grain = 20f, gaussianBlur = 30f)
        )
        val expected = EditorSpatialEngine.processPixels(source, width, height, state)
        val plan = EditorSpatialEngine.createPlan(state)
        val actual = IntArray(source.size)

        var startY = 0
        while (startY < height) {
            val rows = minOf(3, height - startY)
            val sourceStartY = maxOf(0, startY - plan.requiredRadius)
            val sourceEndY = minOf(height, startY + rows + plan.requiredRadius)
            val block = source.copyOfRange(sourceStartY * width, sourceEndY * width)
            val output = IntArray(rows * width)
            EditorSpatialEngine.processBlock(
                block,
                sourceStartY,
                output,
                startY,
                rows,
                width,
                height,
                plan
            )
            output.copyInto(actual, startY * width)
            startY += rows
        }

        assertTrue(expected.contentEquals(actual))
    }

    private fun edgeImage(width: Int, height: Int, left: Int, right: Int): IntArray =
        IntArray(width * height) { index -> if (index % width < width / 2) left else right }

    private fun checkerboard(width: Int, height: Int): IntArray =
        IntArray(width * height) { index ->
            if ((index % width + index / width) % 2 == 0) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
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
