package com.example.enhance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class EnhancementEngineTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `local enhancement reads a device URI and produces a real result`() = runTest {
        val source = File(context.cacheDir, "enhancement_uri.jpg")
        val bitmap = Bitmap.createBitmap(72, 54, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(32, 92, 178))
        }
        FileOutputStream(source).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bitmap.recycle()
        try {
            val result = EnhancementEngine.processPhoto(context, Uri.fromFile(source), "auto")
            assertTrue(result is EnhancementResult.Success)
            result as EnhancementResult.Success
            assertEquals(72, result.width)
            assertEquals(54, result.height)
            assertTrue(result.sizeBytes > 0)
        } finally {
            source.delete()
        }
    }

    @Test
    fun `catalog provides all 9 enhancement modes`() {
        assertEquals(9, EnhancementCatalog.Modes.size)
        val auto = EnhancementCatalog.find("auto")
        assertEquals("Auto", auto.label)
        assertTrue(auto.isLocalAvailable)

        val unblur = EnhancementCatalog.find("unblur")
        assertEquals("Unblur", unblur.label)
        assertTrue(unblur.isLocalAvailable)

        val face = EnhancementCatalog.find("face")
        assertEquals("Face", face.label)
        assertFalse(face.isLocalAvailable)

        val upscale = EnhancementCatalog.find("upscale")
        assertEquals("Upscale", upscale.label)
        assertFalse(upscale.isLocalAvailable)
    }

    @Test
    fun `catalog falls back safely to auto for unknown mode`() {
        val unknown = EnhancementCatalog.find("non_existent_mode")
        assertNotNull(unknown)
        assertEquals("auto", unknown.id)
        assertEquals("Auto", unknown.label)
    }

    @Test
    fun `histogram stats returns valid ranges`() {
        val stats = LocalAutoEnhancer.HistogramStats(
            minLuminance = 10,
            maxLuminance = 240,
            avgLuminance = 125f,
            shadowPercentage = 0.08f,
            highlightPercentage = 0.05f
        )
        assertEquals(10, stats.minLuminance)
        assertEquals(240, stats.maxLuminance)
        assertEquals(125f, stats.avgLuminance, 0.01f)
    }
}
