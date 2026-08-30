package com.example.editor

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class EditorGeometryEngineTest {

    @Test
    fun `neutral render is independent and pixel identical`() {
        val source = solidBitmap(4, 3, 0x80446688.toInt())

        val output = EditorGeometryEngine.render(source, TransformAdjustments())

        assertTrue(output !== source)
        assertEquals(source.width, output.width)
        assertEquals(source.height, output.height)
        assertEquals(source.getPixel(2, 1), output.getPixel(2, 1))
        assertFalse(source.isRecycled)
    }

    @Test
    fun `quarter rotation swaps output dimensions`() {
        val source = solidBitmap(6, 4, Color.RED)

        val output = EditorGeometryEngine.render(
            source,
            TransformAdjustments(rotationDegrees = 90)
        )

        assertEquals(4, output.width)
        assertEquals(6, output.height)
        assertTrue(output !== source)
    }

    @Test
    fun `horizontal flip returns independent bitmap with unchanged dimensions`() {
        val source = Bitmap.createBitmap(3, 1, Bitmap.Config.ARGB_8888).apply {
            setPixel(0, 0, Color.RED)
            setPixel(1, 0, Color.GREEN)
            setPixel(2, 0, Color.BLUE)
        }

        val output = EditorGeometryEngine.render(
            source,
            TransformAdjustments(flipHorizontal = true)
        )

        assertEquals(3, output.width)
        assertEquals(1, output.height)
        assertTrue(output !== source)
        assertFalse(source.isRecycled)
    }

    @Test
    fun `preset and social aspect ratios crop at full resolution`() {
        val source = solidBitmap(400, 300, Color.CYAN)

        val square = EditorGeometryEngine.render(
            source,
            TransformAdjustments(aspectId = "1:1")
        )
        val portrait = EditorGeometryEngine.render(
            source,
            TransformAdjustments(aspectId = "4:5")
        )
        val social = EditorGeometryEngine.render(
            source,
            TransformAdjustments(aspectId = "1.91:1")
        )

        assertEquals(300, square.width)
        assertEquals(300, square.height)
        assertEquals(240, portrait.width)
        assertEquals(300, portrait.height)
        assertEquals(400, social.width)
        assertEquals(209, social.height)
    }

    @Test
    fun `custom aspect ratio uses user width and height`() {
        val source = solidBitmap(400, 400, Color.MAGENTA)

        val output = EditorGeometryEngine.render(
            source,
            TransformAdjustments(
                aspectId = "custom",
                customAspectWidth = 16f,
                customAspectHeight = 9f
            )
        )

        assertEquals(400, output.width)
        assertEquals(225, output.height)
    }

    @Test
    fun `free crop uses all four normalized edges`() {
        val source = solidBitmap(10, 10, Color.YELLOW)

        val output = EditorGeometryEngine.render(
            source,
            TransformAdjustments(
                aspectId = "free",
                cropRect = NormalizedCropRect(left = 0.2f, top = 0.1f, right = 0.8f, bottom = 0.9f)
            )
        )

        assertEquals(6, output.width)
        assertEquals(8, output.height)
    }

    @Test
    fun `straighten keeps dimensions and auto fills opaque corners`() {
        val source = solidBitmap(60, 40, 0xFF556677.toInt())

        val output = EditorGeometryEngine.render(
            source,
            TransformAdjustments(straightenDegrees = 12f)
        )

        assertEquals(60, output.width)
        assertEquals(40, output.height)
        listOf(0 to 0, 59 to 0, 0 to 39, 59 to 39).forEach { (x, y) ->
            assertEquals(255, Color.alpha(output.getPixel(x, y)))
        }
    }

    @Test
    fun `perspective and manual lens corrections produce real local warp`() {
        val source = Bitmap.createBitmap(25, 25, Bitmap.Config.ARGB_8888).apply {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    setPixel(x, y, Color.rgb(x * 10, y * 10, (x + y) * 5))
                }
            }
        }

        val output = EditorGeometryEngine.render(
            source,
            TransformAdjustments(
                perspectiveHorizontal = 42f,
                perspectiveVertical = -28f,
                lensDistortion = 55f,
                geometryHorizontal = 18f,
                geometryVertical = -16f
            )
        )

        assertEquals(source.width, output.width)
        assertEquals(source.height, output.height)
        assertNotEquals(source.getPixel(2, 4), output.getPixel(2, 4))
        assertTrue(Color.alpha(output.getPixel(12, 12)) > 0)
    }

    @Test
    fun `geometry renderer reports completion and exposes cancellation checkpoints`() {
        val source = solidBitmap(32, 24, 0xFF334455.toInt())
        var progress = -1f
        var checkpoints = 0

        EditorGeometryEngine.render(
            source = source,
            transform = TransformAdjustments(lensDistortion = 20f),
            onProgress = { progress = it },
            checkpoint = { checkpoints += 1 }
        )

        assertEquals(1f, progress, 0.0001f)
        assertTrue(checkpoints >= 4)
    }

    @Test
    fun `transform reset clears geometry while grid remains non-exporting state`() {
        val session = EditorSession(
            EditorState(
                transform = TransformAdjustments(
                    aspectId = "3:4",
                    straightenDegrees = 5f,
                    perspectiveVertical = 20f,
                    lensDistortion = -30f,
                    grid = CropGrid.THIRDS
                )
            )
        )

        session.resetTransform()

        assertTrue(session.workingState.transform.isNeutral)
        assertEquals(CropGrid.OFF, session.workingState.transform.grid)
    }

    private fun solidBitmap(width: Int, height: Int, color: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
}
