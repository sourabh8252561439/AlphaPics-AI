package com.example.collage

import android.graphics.Bitmap
import android.graphics.Color
import com.example.editor.OverlayAdjustments
import com.example.editor.OverlayStickerKind
import com.example.editor.StickerOverlay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class CollageEngineTest {

    @Test
    fun `two split composites exact sources without stretching or network work`() {
        val red = solid(40, 60, Color.RED)
        val blue = solid(80, 40, Color.BLUE)

        val result = CollageEngine.render(
            sources = listOf(red, blue),
            state = CollageState(spacing = 0f, cornerRadius = 0f),
            outputWidth = 200,
            outputHeight = 100
        )

        assertEquals(Color.RED, result.getPixel(25, 50))
        assertEquals(Color.BLUE, result.getPixel(175, 50))
        red.recycle()
        blue.recycle()
        result.recycle()
    }

    @Test
    fun `spacing rounded corners border and gradient expose real background pixels`() {
        val white = solid(40, 40, Color.WHITE)
        val state = CollageState(
            spacing = 8f,
            cornerRadius = 20f,
            borderWidth = 2f,
            borderColorArgb = 0xFFFF0000,
            background = CollageBackground(
                mode = CollageBackgroundMode.GRADIENT,
                firstColorArgb = 0xFF001122,
                secondColorArgb = 0xFF334455
            )
        )

        val result = CollageEngine.render(listOf(white, white), state, 160, 100)

        assertNotEquals(Color.WHITE, result.getPixel(80, 50))
        assertNotEquals(Color.WHITE, result.getPixel(4, 4))
        val hasRedBorder = (0 until 14).any { x ->
            (0 until result.height).any { y -> Color.red(result.getPixel(x, y)) > 200 }
        }
        assertTrue(hasRedBorder)
        white.recycle()
        result.recycle()
    }

    @Test
    fun `freestyle uses movable frames and renders safe built in stickers`() {
        val cyan = solid(60, 60, Color.CYAN)
        val yellow = solid(60, 60, Color.YELLOW)
        val state = CollageState(
            layoutId = CollageLayoutCatalog.Freestyle.id,
            overlays = OverlayAdjustments(
                stickers = listOf(
                    StickerOverlay(
                        kind = OverlayStickerKind.STAR,
                        x = 0.5f,
                        y = 0.5f,
                        scale = 22f,
                        colorArgb = 0xFFFF00FF
                    )
                )
            )
        ).ensurePhotoCount(2).updateFreestyleRect(0, CollageRect(0f, 0f, 0.45f, 0.45f))

        val result = CollageEngine.render(listOf(cyan, yellow), state, 180, 180)

        assertEquals(Color.CYAN, result.getPixel(20, 20))
        assertNotEquals(state.background.firstColorArgb.toInt(), result.getPixel(90, 90))
        cyan.recycle()
        yellow.recycle()
        result.recycle()
    }

    @Test
    fun `render checkpoints and progress complete for six photo grid`() {
        val sources = List(6) { index -> solid(24, 24, Color.rgb(index * 30, 80, 140)) }
        var checkpoints = 0
        var lastProgress = 0f

        val result = CollageEngine.render(
            sources = sources,
            state = CollageState(layoutId = CollageLayoutCatalog.SixGrid.id).ensurePhotoCount(6),
            outputWidth = 180,
            outputHeight = 120,
            checkpoint = { checkpoints++ },
            onProgress = { lastProgress = it }
        )

        assertTrue(checkpoints >= 7)
        assertEquals(1f, lastProgress, 0.001f)
        sources.forEach(Bitmap::recycle)
        result.recycle()
    }

    private fun solid(width: Int, height: Int, color: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
}
