package com.example.editor

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
@Config(sdk = [36])
class EditorHistogramEngineTest {

    @Test
    fun `solid color populates exact rgb and luminance bins`() {
        val color = Color.rgb(200, 100, 40)
        val bitmap = Bitmap.createBitmap(8, 6, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }

        val histogram = EditorHistogramEngine.analyze(bitmap)
        val luma = (54 * 200 + 183 * 100 + 19 * 40 + 128) shr 8

        assertEquals(48, histogram.sampledPixels)
        assertEquals(48, histogram.red[200])
        assertEquals(48, histogram.green[100])
        assertEquals(48, histogram.blue[40])
        assertEquals(48, histogram.luminance[luma])
    }

    @Test
    fun `transparent pixels are excluded`() {
        val bitmap = Bitmap.createBitmap(2, 1, Bitmap.Config.ARGB_8888).apply {
            setPixel(0, 0, Color.TRANSPARENT)
            setPixel(1, 0, Color.WHITE)
        }

        val histogram = EditorHistogramEngine.analyze(bitmap)

        assertEquals(1, histogram.sampledPixels)
        assertEquals(1, histogram.luminance[255])
    }

}
