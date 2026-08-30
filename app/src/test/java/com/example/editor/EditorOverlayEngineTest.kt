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
class EditorOverlayEngineTest {

    @Test
    fun `text overlay draws real pixels with configured styling`() {
        val bitmap = solidBitmap(180, 120, Color.BLACK)

        EditorOverlayEngine.applyInPlace(
            bitmap,
            OverlayAdjustments(
                texts = listOf(
                    TextOverlay(
                        text = "AlphaPics",
                        fontSize = 14f,
                        colorArgb = 0xFFFFFFFF,
                        backgroundArgb = 0xFF204080,
                        outlineArgb = 0xFF000000
                    )
                )
            )
        )

        assertTrue(countChanged(bitmap, Color.BLACK) > 100)
    }

    @Test
    fun `all shape kinds render locally`() {
        val bitmap = solidBitmap(160, 120, Color.BLACK)
        val shapes = OverlayShapeKind.entries.mapIndexed { index, kind ->
            ShapeOverlay(
                kind = kind,
                x = 0.18f + index * 0.16f,
                y = 0.5f,
                width = 0.14f,
                height = 0.22f,
                strokeArgb = 0xFFFFFFFF,
                fillArgb = if (kind in setOf(
                        OverlayShapeKind.RECTANGLE,
                        OverlayShapeKind.ROUNDED_RECTANGLE,
                        OverlayShapeKind.CIRCLE
                    )
                ) 0xFF3060A0 else null
            )
        }

        EditorOverlayEngine.applyInPlace(bitmap, OverlayAdjustments(shapes = shapes))

        assertTrue(countChanged(bitmap, Color.BLACK) > 150)
    }

    @Test
    fun `built in stickers support transform and flip`() {
        val bitmap = solidBitmap(150, 100, Color.BLACK)
        val stickers = OverlayStickerKind.entries.mapIndexed { index, kind ->
            StickerOverlay(
                kind = kind,
                x = 0.25f + index * 0.25f,
                y = 0.5f,
                scale = 20f,
                rotation = index * 18f,
                flipHorizontal = index == 1,
                colorArgb = 0xFF40C8FF
            )
        }

        EditorOverlayEngine.applyInPlace(bitmap, OverlayAdjustments(stickers = stickers))

        assertTrue(countChanged(bitmap, Color.BLACK) > 100)
    }

    @Test
    fun `watermark anchor places text near selected corner`() {
        val bitmap = solidBitmap(200, 120, Color.BLACK)

        EditorOverlayEngine.applyInPlace(
            bitmap,
            OverlayAdjustments(
                watermark = WatermarkAdjustment(
                    enabled = true,
                    text = "MARK",
                    anchor = WatermarkAnchor.BOTTOM_RIGHT,
                    scale = 10f,
                    opacity = 100f,
                    padding = 2f
                )
            )
        )

        var changedInBottomRight = 0
        for (y in 60 until 120) for (x in 100 until 200) {
            if (bitmap.getPixel(x, y) != Color.BLACK) changedInBottomRight++
        }
        assertTrue(changedInBottomRight > 10)
    }

    @Test
    fun `frame applies border and rounded transparent corners`() {
        val bitmap = solidBitmap(100, 80, 0xFF405060.toInt())

        EditorOverlayEngine.applyInPlace(
            bitmap,
            OverlayAdjustments(
                frame = FrameAdjustments(
                    borderEnabled = true,
                    borderColorArgb = 0xFFFFFFFF,
                    borderThickness = 5f,
                    cornerRadius = 15f
                )
            )
        )

        assertNotEquals(0xFF405060.toInt(), bitmap.getPixel(50, 2))
        assertTrue(Color.alpha(bitmap.getPixel(0, 0)) < 255)
    }

    @Test
    fun `overlay model caps items and sanitizes controls`() {
        var overlays = OverlayAdjustments()
        repeat(80) {
            overlays = overlays.addText(
                TextOverlay(text = "x", x = -1f, y = 2f, fontSize = 80f, opacity = 140f)
            )
        }
        repeat(150) {
            overlays = overlays.addDrawing(
                DrawOverlayStroke(points = listOf(RetouchPoint(-1f, 4f)), size = 20f)
            )
        }

        assertEquals(64, overlays.texts.size)
        assertEquals(128, overlays.drawing.size)
        assertEquals(0f, overlays.texts.last().x, 0.001f)
        assertEquals(1f, overlays.texts.last().y, 0.001f)
        assertEquals(24f, overlays.texts.last().fontSize, 0.001f)
        assertEquals(12f, overlays.drawing.last().size, 0.001f)
    }

    @Test
    fun `drawing undo redo is bounded and new stroke clears redo`() {
        val first = DrawOverlayStroke(points = listOf(RetouchPoint(0.2f, 0.2f)))
        val second = DrawOverlayStroke(points = listOf(RetouchPoint(0.8f, 0.8f)))
        val initial = OverlayAdjustments().addDrawing(first).addDrawing(second)

        val undone = initial.undoDrawing()
        assertEquals(1, undone.drawing.size)
        assertEquals(1, undone.drawingRedo.size)

        val restored = undone.redoDrawing()
        assertEquals(2, restored.drawing.size)
        assertTrue(restored.drawingRedo.isEmpty())

        val replaced = undone.addDrawing(first.copy(colorArgb = 0xFF00FFFF))
        assertTrue(replaced.drawingRedo.isEmpty())
    }

    private fun countChanged(bitmap: Bitmap, original: Int): Int {
        var count = 0
        for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
            if (bitmap.getPixel(x, y) != original) count++
        }
        return count
    }

    private fun solidBitmap(width: Int, height: Int, color: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
}
