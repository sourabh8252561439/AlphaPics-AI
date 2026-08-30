package com.example.batchstudio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
class BatchStudioEngineTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `combined resize padding preset and watermark saves readable output`() = runTest {
        val source = createBitmapFile("batch_studio_source.png", 160, 120, Color.rgb(30, 95, 190))
        try {
            var progress = 0f
            val result = BatchStudioEngine.processItem(
                context = context,
                sourceUri = Uri.fromFile(source),
                sourceMimeType = "image/png",
                sourceName = source.name,
                settings = BatchStudioSettings(
                    resizeMode = BatchResizeMode.DIMENSIONS,
                    targetWidth = 80,
                    targetHeight = 60,
                    maintainAspectRatio = false,
                    outputFormat = BatchOutputFormat.PNG,
                    paddingEnabled = true,
                    paddingPercent = 10f,
                    watermarkEnabled = true,
                    watermarkText = "LOCAL",
                    presetEnabled = true,
                    presetId = "natural"
                ),
                onProgress = { progress = it }
            ).getOrThrow()

            assertEquals(96, result.width)
            assertEquals(72, result.height)
            assertEquals("image/png", result.mimeType)
            assertEquals(1f, progress, 0.001f)
            val decoded = context.contentResolver.openInputStream(result.uri)?.use(BitmapFactory::decodeStream)
            assertNotNull(decoded)
            assertEquals(96, decoded?.width)
            assertEquals(72, decoded?.height)
            assertTrue(decoded!!.hasMultipleColors())
            decoded.recycle()
            context.contentResolver.delete(result.uri, null, null)
        } finally {
            source.delete()
        }
    }

    @Test
    fun `logo layer is rendered at the selected placement`() = runTest {
        val source = createBitmapFile("batch_logo_source.png", 100, 100, Color.BLUE)
        val logo = createBitmapFile("batch_logo.png", 30, 30, Color.RED)
        try {
            val result = BatchStudioEngine.processItem(
                context,
                Uri.fromFile(source),
                "image/png",
                source.name,
                BatchStudioSettings(
                    outputFormat = BatchOutputFormat.PNG,
                    logoEnabled = true,
                    logoUri = Uri.fromFile(logo),
                    logoPlacement = BatchPlacement.BOTTOM_RIGHT,
                    logoScale = 20f,
                    logoOpacity = 100f
                )
            ).getOrThrow()

            val decoded = context.contentResolver.openInputStream(result.uri)?.use(BitmapFactory::decodeStream)
            assertNotNull(decoded)
            val logoPixel = decoded!!.getPixel(88, 88)
            assertTrue(Color.red(logoPixel) > 200)
            assertTrue(Color.blue(logoPixel) < 80)
            decoded.recycle()
            context.contentResolver.delete(result.uri, null, null)
        } finally {
            source.delete()
            logo.delete()
        }
    }

    private fun createBitmapFile(name: String, width: Int, height: Int, color: Int): File {
        val file = File(context.cacheDir, name)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file
    }

    private fun Bitmap.hasMultipleColors(): Boolean {
        val first = getPixel(0, 0)
        for (y in 0 until height step maxOf(1, height / 12)) {
            for (x in 0 until width step maxOf(1, width / 12)) {
                if (getPixel(x, y) != first) return true
            }
        }
        return false
    }
}
