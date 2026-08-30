package com.example.editor

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
@Config(sdk = [36])
class EditorHistogramSamplingTest {

    @Test
    fun `large input uses bounded regular sampling and checkpoints`() {
        val bitmap = Bitmap.createBitmap(40, 30, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.GRAY)
        }
        var checkpoints = 0

        val histogram = EditorHistogramEngine.analyze(bitmap, maxSamples = 50) { checkpoints++ }

        assertTrue(histogram.sampledPixels <= 100)
        assertTrue(histogram.sampledPixels > 0)
        assertTrue(checkpoints >= 2)
        assertEquals(1f, histogram.normalized(histogram.red)[Color.red(Color.GRAY)], 0.001f)
    }
}
