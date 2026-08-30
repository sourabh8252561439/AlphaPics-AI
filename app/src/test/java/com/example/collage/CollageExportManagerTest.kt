package com.example.collage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
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
class CollageExportManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `export rejects fewer than two photos before decoding`() = runTest {
        val result = CollageExportManager.export(
            context = context,
            sourceUris = listOf(Uri.parse("content://alphapics/one")),
            state = CollageState()
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("between 2 and 6"))
    }

    @Test
    fun `export rejects a layout with missing photos before decoding`() = runTest {
        val result = CollageExportManager.export(
            context = context,
            sourceUris = listOf(
                Uri.parse("content://alphapics/one"),
                Uri.parse("content://alphapics/two")
            ),
            state = CollageState(layoutId = CollageLayoutCatalog.FourGrid.id)
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("needs 4 photos"))
    }

    @Test
    fun `export decodes composites encodes and reads back a high resolution collage`() = runTest {
        val first = sourceFile("collage_export_blue.png", Color.BLUE)
        val second = sourceFile("collage_export_magenta.png", Color.MAGENTA)
        try {
            var lastProgress = 0f
            val result = CollageExportManager.export(
                context = context,
                sourceUris = listOf(Uri.fromFile(first), Uri.fromFile(second)),
                state = CollageState(outputLongEdge = 1024).ensurePhotoCount(2),
                format = ExportFormat.PNG,
                onProgress = { lastProgress = it }
            ).getOrThrow()

            assertEquals(1024, result.width)
            assertEquals(1024, result.height)
            assertEquals("image/png", result.mimeType)
            assertEquals(1f, lastProgress, 0.001f)
            val decoded = context.contentResolver.openInputStream(result.uri)?.use(BitmapFactory::decodeStream)
            assertNotNull(decoded)
            assertEquals(1024, decoded?.width)
            assertEquals(1024, decoded?.height)
            decoded?.recycle()
            context.contentResolver.delete(result.uri, null, null)
        } finally {
            first.delete()
            second.delete()
        }
    }

    private fun sourceFile(name: String, color: Int): File {
        val file = File(context.cacheDir, name)
        val bitmap = Bitmap.createBitmap(96, 128, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
        try {
            FileOutputStream(file).use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
        } finally {
            bitmap.recycle()
        }
        return file
    }
}
