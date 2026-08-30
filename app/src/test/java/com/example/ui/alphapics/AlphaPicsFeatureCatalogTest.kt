package com.example.ui.alphapics

import com.example.ui.alphapics.navigation.AlphaPicsFeatureCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AlphaPicsFeatureCatalogTest {
    @Test
    fun `every published feature route resolves to the same catalog entry`() {
        val published = AlphaPicsFeatureCatalog.PhotoEnhancement

        published.forEach { feature ->
            assertSame(feature, AlphaPicsFeatureCatalog.find(feature.id))
        }
        assertEquals(published.size, published.map { it.id }.toSet().size)
        assertTrue(published.containsAll(AlphaPicsFeatureCatalog.HomeFeatures))
    }

    @Test
    fun `consumer catalog stays photo only`() {
        val forbiddenTerms = listOf(
            "text to image",
            "image-to-image",
            "avatar",
            "product photo",
            "text to video",
            "image to video",
            "generative fill"
        )

        AlphaPicsFeatureCatalog.PhotoEnhancement.forEach { feature ->
            val consumerCopy = "${feature.id} ${feature.title} ${feature.description}".lowercase()
            forbiddenTerms.forEach { forbidden ->
                assertFalse("Unexpected '$forbidden' in ${feature.id}", consumerCopy.contains(forbidden))
            }
        }
    }

    @Test
    fun `unknown routes fail closed instead of opening an unrelated feature`() {
        assertNull(AlphaPicsFeatureCatalog.find(null))
        assertNull(AlphaPicsFeatureCatalog.find("unknown-feature"))
    }
}
