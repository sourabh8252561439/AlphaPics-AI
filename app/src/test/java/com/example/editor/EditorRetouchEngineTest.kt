package com.example.editor

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
@Config(sdk = [36])
class EditorRetouchEngineTest {

    @Test
    fun `local exposure changes only the painted region`() {
        val bitmap = solidBitmap(21, 21, 0xFF303030.toInt())

        EditorRetouchEngine.applyInPlace(
            bitmap,
            RetouchAdjustments(strokes = listOf(stroke(LocalRetouchMode.EXPOSURE)))
        )

        assertTrue(Color.red(bitmap.getPixel(10, 10)) > 0x30)
        assertEquals(0xFF303030.toInt(), bitmap.getPixel(0, 0))
    }

    @Test
    fun `red eye correction is selective for red dominant pixels`() {
        val bitmap = solidBitmap(15, 15, 0xFF805050.toInt()).apply {
            setPixel(7, 7, 0xFFE02020.toInt())
            setPixel(8, 7, 0xFF20C040.toInt())
        }

        EditorRetouchEngine.applyInPlace(
            bitmap,
            RetouchAdjustments(strokes = listOf(stroke(LocalRetouchMode.RED_EYE, size = 20f)))
        )

        assertTrue(Color.red(bitmap.getPixel(7, 7)) < 0xE0)
        assertEquals(0xFF20C040.toInt(), bitmap.getPixel(8, 7))
    }

    @Test
    fun `clone uses the captured normalized source offset`() {
        val bitmap = solidBitmap(20, 10, Color.BLACK).apply {
            for (y in 0 until height) for (x in 0..4) setPixel(x, y, Color.CYAN)
        }
        val cloneStroke = RetouchStroke(
            mode = LocalRetouchMode.CLONE,
            points = listOf(RetouchPoint(0.70f, 0.5f)),
            size = 24f,
            feather = 0f,
            strength = 100f,
            cloneSourceOffsetX = -50f,
            cloneSourceOffsetY = 0f
        )

        EditorRetouchEngine.applyInPlace(
            bitmap,
            RetouchAdjustments(strokes = listOf(cloneStroke))
        )

        assertEquals(Color.CYAN, bitmap.getPixel(13, 5))
    }

    @Test
    fun `blur and sharpen brushes perform real local spatial processing`() {
        val source = Bitmap.createBitmap(17, 17, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.BLACK)
            setPixel(8, 8, Color.WHITE)
        }
        val blurred = source.copy(Bitmap.Config.ARGB_8888, true)
        val sharpened = source.copy(Bitmap.Config.ARGB_8888, true)

        EditorRetouchEngine.applyInPlace(
            blurred,
            RetouchAdjustments(strokes = listOf(stroke(LocalRetouchMode.BLUR, size = 30f)))
        )
        EditorRetouchEngine.applyInPlace(
            sharpened,
            RetouchAdjustments(strokes = listOf(stroke(LocalRetouchMode.SHARPEN, size = 30f)))
        )

        assertTrue(Color.red(blurred.getPixel(8, 8)) < 255)
        assertNotEquals(source.getPixel(7, 8), blurred.getPixel(7, 8))
        assertEquals(255, Color.alpha(sharpened.getPixel(8, 8)))
    }

    @Test
    fun `erase mask restores the pre-retouch baseline`() {
        val original = 0xFF404040.toInt()
        val bitmap = solidBitmap(21, 21, original)
        val brighten = stroke(LocalRetouchMode.BRIGHTNESS, size = 28f)
        val erase = stroke(LocalRetouchMode.ERASE_MASK, size = 10f, strength = 100f)

        EditorRetouchEngine.applyInPlace(
            bitmap,
            RetouchAdjustments(strokes = listOf(brighten, erase))
        )

        assertEquals(original, bitmap.getPixel(10, 10))
        assertNotEquals(original, bitmap.getPixel(8, 10))
    }

    @Test
    fun `retouch preserves source alpha`() {
        val bitmap = solidBitmap(11, 11, 0x40305070)

        EditorRetouchEngine.applyInPlace(
            bitmap,
            RetouchAdjustments(strokes = listOf(stroke(LocalRetouchMode.TEMPERATURE, size = 30f)))
        )

        assertEquals(0x40, Color.alpha(bitmap.getPixel(5, 5)))
    }

    @Test
    fun `stroke model sanitizes controls coordinates and history bounds`() {
        val unsafe = RetouchStroke(
            mode = LocalRetouchMode.HEAL,
            points = List(600) { RetouchPoint(-2f, 3f) },
            size = 80f,
            feather = -10f,
            strength = 200f
        ).sanitized()
        var adjustments = RetouchAdjustments()
        repeat(140) { adjustments = adjustments.append(unsafe) }

        assertEquals(512, unsafe.points.size)
        assertEquals(RetouchPoint(0f, 1f), unsafe.points.first())
        assertEquals(30f, unsafe.size, 0.001f)
        assertEquals(0f, unsafe.feather, 0.001f)
        assertEquals(100f, unsafe.strength, 0.001f)
        assertEquals(128, adjustments.strokes.size)
        assertEquals(127, adjustments.removeLastStroke().strokes.size)
    }

    @Test
    fun `renderer reports progress and cancellation checkpoints`() {
        val bitmap = solidBitmap(20, 20, 0xFF606060.toInt())
        var progress = -1f
        var checkpoints = 0

        EditorRetouchEngine.applyInPlace(
            bitmap = bitmap,
            retouch = RetouchAdjustments(
                strokes = listOf(
                    stroke(LocalRetouchMode.HEAL),
                    stroke(LocalRetouchMode.SATURATION)
                )
            ),
            onProgress = { progress = it },
            checkpoint = { checkpoints++ }
        )

        assertEquals(1f, progress, 0.001f)
        assertTrue(checkpoints >= 2)
    }

    private fun stroke(
        mode: LocalRetouchMode,
        size: Float = 18f,
        strength: Float = 100f
    ): RetouchStroke = RetouchStroke(
        mode = mode,
        points = listOf(RetouchPoint(0.5f, 0.5f)),
        size = size,
        feather = 0f,
        strength = strength
    )

    private fun solidBitmap(width: Int, height: Int, color: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
}
