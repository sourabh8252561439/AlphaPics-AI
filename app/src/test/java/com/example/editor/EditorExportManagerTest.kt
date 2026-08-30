package com.example.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
class EditorExportManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `editor export reads URI bounds and saves a readable full resolution image`() = runTest {
        val source = File(context.cacheDir, "editor_export_uri.png")
        val bitmap = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.MAGENTA)
        }
        FileOutputStream(source).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        try {
            val result = EditorExportManager.exportImage(
                context = context,
                sourceUri = Uri.fromFile(source),
                state = EditorState(),
                format = ExportFormat.PNG
            ).getOrThrow()

            assertEquals(64, result.width)
            assertEquals(48, result.height)
            val decoded = context.contentResolver.openInputStream(result.uri)?.use(BitmapFactory::decodeStream)
            assertNotNull(decoded)
            assertEquals(64, decoded?.width)
            assertEquals(48, decoded?.height)
            decoded?.recycle()
            context.contentResolver.delete(result.uri, null, null)
        } finally {
            source.delete()
        }
    }
}
