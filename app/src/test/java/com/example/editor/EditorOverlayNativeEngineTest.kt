package com.example.editor

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Native-only compositing contract kept isolated from deterministic bitmap tests. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class EditorOverlayNativeEngineTest {

    @Test
    fun `draw eraser removes only the drawing layer`() {
        val base = 0xFF203040.toInt()
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply {
            eraseColor(base)
        }
        val line = DrawOverlayStroke(
            points = listOf(RetouchPoint(0.2f, 0.5f), RetouchPoint(0.8f, 0.5f)),
            colorArgb = 0xFFFFFFFF,
            size = 8f
        )
        val eraser = DrawOverlayStroke(
            points = listOf(RetouchPoint(0.5f, 0.42f), RetouchPoint(0.5f, 0.58f)),
            size = 12f,
            eraser = true
        )

        EditorOverlayEngine.applyInPlace(
            bitmap,
            OverlayAdjustments(drawing = listOf(line, eraser))
        )

        assertNotEquals(base, bitmap.getPixel(30, 50))
        assertEquals(base, bitmap.getPixel(50, 50))
    }
}
