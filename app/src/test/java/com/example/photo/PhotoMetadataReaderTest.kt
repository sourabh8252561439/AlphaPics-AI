package com.example.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
class PhotoMetadataReaderTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `reader reports dimensions format file size and real transparency`() = runTest {
        val file = File(context.cacheDir, "metadata_transparent.png")
        val bitmap = Bitmap.createBitmap(40, 24, Bitmap.Config.ARGB_8888).apply {
            setHasAlpha(true)
            eraseColor(Color.BLUE)
            for (y in 2..8) for (x in 2..8) setPixel(x, y, Color.TRANSPARENT)
        }
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        try {
            val result = PhotoMetadataReader.read(context, Uri.fromFile(file)).getOrThrow()
            assertEquals(40, result.width)
            assertEquals(24, result.height)
            assertEquals("PNG", result.formatLabel)
            assertTrue(result.sizeBytes.orZero() > 0)
            assertTrue(result.hasTransparency)
            assertTrue(result.colorDescription.isNotBlank())
        } finally {
            file.delete()
        }
    }

    @Test
    fun `reader exposes common EXIF and oriented dimensions`() = runTest {
        val file = File(context.cacheDir, "metadata_oriented.jpg")
        val bitmap = Bitmap.createBitmap(80, 40, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bitmap.recycle()
        ExifInterface(file.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            setAttribute(ExifInterface.TAG_MAKE, "AlphaPics Test Camera")
            setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "2026:08:29 13:45:00")
            saveAttributes()
        }
        try {
            val result = PhotoMetadataReader.read(context, Uri.fromFile(file)).getOrThrow()
            assertEquals(40, result.width)
            assertEquals(80, result.height)
            assertEquals("Rotated 90°", result.orientationLabel)
            assertEquals("AlphaPics Test Camera", result.exif["Camera make"])
            assertEquals("2026:08:29 13:45:00", result.dateLabel)
        } finally {
            file.delete()
        }
    }

    private fun Long?.orZero(): Long = this ?: 0L
}
