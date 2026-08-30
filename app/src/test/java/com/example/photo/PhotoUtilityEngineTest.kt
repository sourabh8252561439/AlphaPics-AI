package com.example.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import com.example.editor.ExportFormat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PhotoUtilityEngineTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `resize and PNG conversion save exact readable dimensions`() = runTest {
        val source = sourceFile("utility_source.png", Bitmap.CompressFormat.PNG, Color.MAGENTA)
        try {
            var progress = 0f
            val result = PhotoUtilityEngine.export(
                context,
                Uri.fromFile(source),
                PhotoUtilitySettings(
                    resize = ResizeSettings(targetWidth = 96, targetHeight = 64, maintainAspectRatio = false),
                    format = ExportFormat.PNG,
                    metadataPolicy = MetadataPolicy.REMOVE
                ),
                onProgress = { progress = it }
            ).getOrThrow()

            assertEquals(96, result.width)
            assertEquals(64, result.height)
            assertEquals("image/png", result.mimeType)
            assertEquals(1f, progress, 0.001f)
            val decoded = context.contentResolver.openInputStream(result.uri)?.use(BitmapFactory::decodeStream)
            assertNotNull(decoded)
            assertEquals(96, decoded?.width)
            assertEquals(64, decoded?.height)
            decoded?.recycle()
            context.contentResolver.delete(result.uri, null, null)
        } finally {
            source.delete()
        }
    }

    @Test
    fun `safe JPEG preservation copies camera fields and normalizes orientation`() = runTest {
        val source = sourceFile("utility_exif.jpg", Bitmap.CompressFormat.JPEG, Color.rgb(40, 100, 180))
        ExifInterface(source.absolutePath).apply {
            setAttribute(ExifInterface.TAG_MAKE, "AlphaPics Camera")
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }
        try {
            val result = PhotoUtilityEngine.export(
                context,
                Uri.fromFile(source),
                PhotoUtilitySettings(
                    resize = ResizeSettings(targetWidth = 120, targetHeight = 160),
                    format = ExportFormat.JPEG,
                    metadataPolicy = MetadataPolicy.PRESERVE_SAFE
                )
            ).getOrThrow()

            val exif = context.contentResolver.openInputStream(result.uri)?.use(::ExifInterface)
            assertEquals("AlphaPics Camera", exif?.getAttribute(ExifInterface.TAG_MAKE))
            assertEquals(
                ExifInterface.ORIENTATION_NORMAL,
                exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
            )
            context.contentResolver.delete(result.uri, null, null)
        } finally {
            source.delete()
        }
    }

    @Test
    fun `preserve metadata is rejected for non JPEG formats`() = runTest {
        val source = sourceFile("utility_invalid.png", Bitmap.CompressFormat.PNG, Color.YELLOW)
        try {
            val result = PhotoUtilityEngine.export(
                context,
                Uri.fromFile(source),
                PhotoUtilitySettings(
                    resize = ResizeSettings(targetWidth = 32, targetHeight = 32),
                    format = ExportFormat.PNG,
                    metadataPolicy = MetadataPolicy.PRESERVE_SAFE
                )
            )
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("JPEG"))
        } finally {
            source.delete()
        }
    }

    @Test
    fun `WebP conversion saves exact readable dimensions`() = runTest {
        val source = sourceFile("utility_webp_source.png", Bitmap.CompressFormat.PNG, Color.CYAN)
        try {
            val result = PhotoUtilityEngine.export(
                context,
                Uri.fromFile(source),
                PhotoUtilitySettings(
                    resize = ResizeSettings(targetWidth = 80, targetHeight = 60, maintainAspectRatio = false),
                    format = ExportFormat.WEBP,
                    quality = 88,
                    metadataPolicy = MetadataPolicy.REMOVE
                )
            ).getOrThrow()

            assertEquals(80, result.width)
            assertEquals(60, result.height)
            assertEquals("image/webp", result.mimeType)
            val decoded = context.contentResolver.openInputStream(result.uri)?.use(BitmapFactory::decodeStream)
            assertNotNull(decoded)
            assertEquals(80, decoded?.width)
            assertEquals(60, decoded?.height)
            decoded?.recycle()
            context.contentResolver.delete(result.uri, null, null)
        } finally {
            source.delete()
        }
    }

    @Test
    fun `remove metadata omits source camera fields from JPEG output`() = runTest {
        val source = sourceFile("utility_remove_exif.jpg", Bitmap.CompressFormat.JPEG, Color.BLUE)
        ExifInterface(source.absolutePath).apply {
            setAttribute(ExifInterface.TAG_MAKE, "Private Camera")
            setAttribute(ExifInterface.TAG_MODEL, "Private Model")
            saveAttributes()
        }
        try {
            val result = PhotoUtilityEngine.export(
                context,
                Uri.fromFile(source),
                PhotoUtilitySettings(
                    resize = ResizeSettings(targetWidth = 160, targetHeight = 120),
                    format = ExportFormat.JPEG,
                    metadataPolicy = MetadataPolicy.REMOVE
                )
            ).getOrThrow()

            val exif = context.contentResolver.openInputStream(result.uri)?.use(::ExifInterface)
            assertEquals(null, exif?.getAttribute(ExifInterface.TAG_MAKE))
            assertEquals(null, exif?.getAttribute(ExifInterface.TAG_MODEL))
            context.contentResolver.delete(result.uri, null, null)
        } finally {
            source.delete()
        }
    }

    private fun sourceFile(name: String, format: Bitmap.CompressFormat, color: Int): File {
        val file = File(context.cacheDir, name)
        val bitmap = Bitmap.createBitmap(160, 120, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
        FileOutputStream(file).use { bitmap.compress(format, 96, it) }
        bitmap.recycle()
        return file
    }
}
