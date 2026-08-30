package com.example.batchstudio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchStudioModelTest {

    @Test
    fun `same box preserves landscape and portrait aspect ratios`() {
        val settings = BatchStudioSettings(
            resizeMode = BatchResizeMode.DIMENSIONS,
            targetWidth = 1200,
            targetHeight = 1200,
            maintainAspectRatio = true
        )

        assertEquals(1200 to 600, settings.resolveContentDimensions(4000, 2000))
        assertEquals(600 to 1200, settings.resolveContentDimensions(2000, 4000))
    }

    @Test
    fun `exact dimensions can intentionally use a free ratio`() {
        val settings = BatchStudioSettings(
            resizeMode = BatchResizeMode.DIMENSIONS,
            targetWidth = 900,
            targetHeight = 700,
            maintainAspectRatio = false
        )

        assertEquals(900 to 700, settings.resolveContentDimensions(4000, 2000))
    }

    @Test
    fun `percentage and padding resolve deterministic dimensions`() {
        val settings = BatchStudioSettings(
            resizeMode = BatchResizeMode.PERCENTAGE,
            percentage = 50f,
            paddingEnabled = true,
            paddingPercent = 10f
        )

        val content = settings.resolveContentDimensions(1000, 600)
        assertEquals(500 to 300, content)
        assertEquals(600 to 360, settings.resolveOutputDimensions(content.first, content.second))
    }

    @Test
    fun `unsafe padded output fails before processing`() {
        assertThrows(IllegalArgumentException::class.java) {
            BatchStudioSettings(
                resizeMode = BatchResizeMode.DIMENSIONS,
                targetWidth = 8192,
                targetHeight = 4096,
                maintainAspectRatio = false,
                paddingEnabled = true,
                paddingPercent = 20f
            ).let { settings ->
                val content = settings.resolveContentDimensions(100, 100)
                settings.resolveOutputDimensions(content.first, content.second)
            }
        }
    }

    @Test
    fun `keep type resolves supported formats and falls back honestly to JPEG`() {
        assertEquals(com.example.editor.ExportFormat.PNG, BatchOutputFormat.KEEP.resolve("image/png"))
        assertEquals(com.example.editor.ExportFormat.WEBP, BatchOutputFormat.KEEP.resolve("image/webp"))
        assertEquals(com.example.editor.ExportFormat.JPEG, BatchOutputFormat.KEEP.resolve("image/heic"))
        assertTrue(BatchStudioSettings(outputFormat = BatchOutputFormat.PNG).hasVisibleOperation)
    }
}
