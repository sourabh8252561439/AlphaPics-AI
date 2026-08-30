package com.example.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PhotoUtilityModelTest {

    @Test
    fun `dimensions mode uses explicit width and height`() {
        val settings = ResizeSettings(targetWidth = 1200, targetHeight = 800)
        assertEquals(1200 to 800, settings.resolvedDimensions(4000, 3000))
    }

    @Test
    fun `percentage mode scales both axes`() {
        val settings = ResizeSettings(mode = ResizeMode.PERCENTAGE, percentage = 37.5f)
        assertEquals(1500 to 750, settings.resolvedDimensions(4000, 2000))
    }

    @Test
    fun `long edge preset preserves landscape and portrait aspect`() {
        assertEquals(
            2048 to 1024,
            ResizeSettings().withLongEdge(4000, 2000, 2048).resolvedDimensions(4000, 2000)
        )
        assertEquals(
            1024 to 2048,
            ResizeSettings().withLongEdge(2000, 4000, 2048).resolvedDimensions(2000, 4000)
        )
    }

    @Test
    fun `unsafe dimensions fail with an actionable limit`() {
        assertThrows(IllegalArgumentException::class.java) {
            ResizeSettings(targetWidth = 8192, targetHeight = 8192).resolvedDimensions(100, 100)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ResizeSettings(targetWidth = 9000, targetHeight = 100).resolvedDimensions(100, 100)
        }
    }

    @Test
    fun `utility tab lookup fails closed to resize`() {
        assertEquals(PhotoUtilityTab.CONVERT, PhotoUtilityTab.fromId("convert"))
        assertEquals(PhotoUtilityTab.RESIZE, PhotoUtilityTab.fromId("unknown"))
    }
}
