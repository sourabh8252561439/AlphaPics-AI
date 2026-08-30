package com.example.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class EditorBitmapRendererTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `preview loader accepts a readable device URI`() = runTest {
        val file = File(context.cacheDir, "editor_preview_uri.png")
        val bitmap = Bitmap.createBitmap(48, 32, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.CYAN)
        }
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        try {
            val preview = EditorPreviewRenderer.loadSource(context, Uri.fromFile(file)).getOrThrow()
            assertEquals(48, preview.width)
            assertEquals(32, preview.height)
            preview.recycle()
        } finally {
            file.delete()
        }
    }

    @Test
    fun `render creates independent bitmap with matching dimensions`() {
        val source = Bitmap.createBitmap(3, 2, Bitmap.Config.ARGB_8888).apply {
            eraseColor(0xFF556677.toInt())
        }

        val result = EditorBitmapRenderer.renderToneColor(
            source,
            EditorState(light = LightAdjustments(exposure = 20f))
        )

        assertTrue(result !== source)
        assertEquals(3, result.width)
        assertEquals(2, result.height)
        assertEquals(0xFF, result.getPixel(0, 0) ushr 24 and 0xFF)
        assertTrue((result.getPixel(0, 0) ushr 16 and 0xFF) > 0x55)
        assertFalse(source.isRecycled)
    }

    @Test
    fun `in-place renderer preserves transparent alpha`() {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply {
            setPixel(0, 0, 0x40336699)
        }

        EditorBitmapRenderer.applyToneColorInPlace(
            bitmap,
            EditorState(
                light = LightAdjustments(gamma = 40f),
                color = ColorAdjustments(saturation = 50f, warmth = 30f)
            )
        )

        assertEquals(0x40, bitmap.getPixel(0, 0) ushr 24 and 0xFF)
    }

    @Test
    fun `full renderer applies spatial effects without changing bitmap dimensions`() {
        val bitmap = Bitmap.createBitmap(7, 7, Bitmap.Config.ARGB_8888).apply {
            eraseColor(0xFF606060.toInt())
            setPixel(3, 3, 0xFFFFFFFF.toInt())
        }

        EditorBitmapRenderer.applyAllInPlace(
            bitmap,
            EditorState(
                detail = DetailAdjustments(sharpen = 25f, noiseReduction = 30f),
                effects = EffectAdjustments(vignette = 40f, grain = 15f, gaussianBlur = 20f)
            )
        )

        assertEquals(7, bitmap.width)
        assertEquals(7, bitmap.height)
        assertTrue(bitmap.getPixel(0, 0) != 0xFF606060.toInt())
        assertFalse(bitmap.isRecycled)
    }
}
