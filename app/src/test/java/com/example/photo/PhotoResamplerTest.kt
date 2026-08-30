package com.example.photo

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
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class PhotoResamplerTest {

    @Test
    fun `multi pass downscale returns exact dimensions and progress`() {
        val source = Bitmap.createBitmap(512, 384, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.CYAN) }
        var checkpoints = 0
        var progress = 0f
        val result = PhotoResampler.resize(
            source,
            64,
            48,
            checkpoint = { checkpoints++ },
            onProgress = { progress = it }
        )

        assertEquals(64, result.width)
        assertEquals(48, result.height)
        assertEquals(Color.CYAN, result.getPixel(32, 24))
        assertTrue(checkpoints >= 3)
        assertEquals(1f, progress, 0.001f)
        source.recycle()
        result.recycle()
    }

    @Test
    fun `resampling preserves transparent pixels`() {
        val source = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.TRANSPARENT)
            setPixel(10, 10, Color.RED)
        }
        val result = PhotoResampler.resize(source, 40, 40)

        assertTrue(result.hasAlpha())
        assertEquals(0, Color.alpha(result.getPixel(0, 0)))
        source.recycle()
        result.recycle()
    }
}
