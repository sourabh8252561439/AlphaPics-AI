package com.example.collage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollageModelTest {

    @Test
    fun `catalog accounts for two through six photo grids and freestyle`() {
        assertEquals(2, CollageLayoutCatalog.TwoSplit.requiredPhotos)
        assertEquals(3, CollageLayoutCatalog.ThreeFeature.slots.size)
        assertEquals(4, CollageLayoutCatalog.FourGrid.slots.size)
        assertEquals(5, CollageLayoutCatalog.FiveMosaic.slots.size)
        assertEquals(6, CollageLayoutCatalog.SixGrid.slots.size)
        assertTrue(CollageLayoutCatalog.Freestyle.isFreestyle)
    }

    @Test
    fun `photo state is bounded and supports transform swap`() {
        val state = CollageState().ensurePhotoCount(20)
            .updatePhotoTransform(0, CollagePhotoTransform(zoom = 8f, offsetX = -3f, offsetY = 2f))
            .updatePhotoTransform(1, CollagePhotoTransform(zoom = 2f, offsetX = 0.4f))
            .swapTransforms(0, 1)

        assertEquals(MAX_COLLAGE_PHOTOS, state.photoTransforms.size)
        assertEquals(2f, state.photoTransforms[0].zoom, 0.001f)
        assertEquals(4f, state.photoTransforms[1].zoom, 0.001f)
        assertEquals(-1f, state.photoTransforms[1].offsetX, 0.001f)
        assertEquals(1f, state.photoTransforms[1].offsetY, 0.001f)
    }

    @Test
    fun `freestyle frames remain inside canvas while moving and scaling`() {
        val initial = CollageLayoutCatalog.defaultFreestyle(2).first()
        val changed = initial.movedBy(2f, -2f).scaledBy(2f)

        assertTrue(changed.x >= 0f)
        assertTrue(changed.y >= 0f)
        assertTrue(changed.x + changed.width <= 1f)
        assertTrue(changed.y + changed.height <= 1f)
    }

    @Test
    fun `social collage aspects produce exact bounded dimensions`() {
        assertEquals(3072 to 3072, CollageState(aspectId = "1:1").outputDimensions())
        assertEquals(2457 to 3072, CollageState(aspectId = "4:5").outputDimensions())
        assertEquals(3072 to 1728, CollageState(aspectId = "16:9").outputDimensions())
        assertEquals(4096 to 4096, CollageState(outputLongEdge = 9000).outputDimensions())
    }
}
