package com.example.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorCurveEngineTest {

    @Test
    fun `identity curve maps every byte exactly`() {
        val lut = CurveEngine.buildLut(ToneCurve())

        for (value in 0..255) assertEquals(value, lut[value])
    }

    @Test
    fun `editing preserves endpoints and ordered control points`() {
        val added = CurveEngine.addPoint(ToneCurve(), 0.5f, 0.7f)
        val moved = CurveEngine.movePoint(added, 1, 0.65f, 0.8f)
        val deleted = CurveEngine.deletePoint(moved, 1)

        assertEquals(CurvePoint(0f, 0f), added.points.first())
        assertEquals(CurvePoint(1f, 1f), added.points.last())
        assertTrue(moved.points.zipWithNext().all { (left, right) -> left.x < right.x })
        assertEquals(ToneCurve.IdentityPoints, deleted.points)
    }

    @Test
    fun `smooth S curve remains monotone and fixes endpoints`() {
        val curve = ToneCurve(
            listOf(
                CurvePoint(0f, 0f),
                CurvePoint(0.25f, 0.15f),
                CurvePoint(0.75f, 0.85f),
                CurvePoint(1f, 1f)
            )
        )
        val lut = CurveEngine.buildLut(curve)

        assertEquals(0, lut.first())
        assertEquals(255, lut.last())
        assertTrue((0 until lut.lastIndex).all { index -> lut[index] <= lut[index + 1] })
        assertTrue(lut[64] < 64)
        assertTrue(lut[192] > 192)
    }

    @Test
    fun `red channel curve changes red without changing green or blue`() {
        val source = 0xFF4080C0.toInt()
        val lift = ToneCurve(
            listOf(CurvePoint(0f, 0f), CurvePoint(0.25f, 0.75f), CurvePoint(1f, 1f))
        )
        val result = EditorPixelEngine.processPixel(
            source,
            EditorState(curves = CurvesAdjustments(red = lift))
        )

        assertTrue(red(result) > red(source))
        assertEquals(green(source), green(result))
        assertEquals(blue(source), blue(result))
    }

    @Test
    fun `master curve changes all color channels`() {
        val source = 0xFF406080.toInt()
        val lift = ToneCurve(
            listOf(CurvePoint(0f, 0.15f), CurvePoint(1f, 1f))
        )
        val result = EditorPixelEngine.processPixel(
            source,
            EditorState(curves = CurvesAdjustments(master = lift))
        )

        assertTrue(red(result) > red(source))
        assertTrue(green(result) > green(source))
        assertTrue(blue(result) > blue(source))
    }

    private fun red(color: Int): Int = color ushr 16 and 0xFF
    private fun green(color: Int): Int = color ushr 8 and 0xFF
    private fun blue(color: Int): Int = color and 0xFF
}
