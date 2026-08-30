package com.example.editor

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EditorPresetStoreTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        EditorPresetStore.clear(context)
    }

    @After
    fun tearDown() {
        EditorPresetStore.clear(context)
    }

    @Test
    fun `custom preset persists global look and reapplies it`() {
        val source = EditorState(
            light = LightAdjustments(exposure = 18f, highlights = -22f, gamma = 12f),
            color = ColorAdjustments(saturation = 14f, vibrance = 20f, warmth = -8f),
            filter = FilterAdjustment("cinema", 65f)
        )

        EditorPresetStore.saveCustom(context, "Night look", source, id = "custom-night", createdAtMillis = 7L)
        val loaded = EditorPresetStore.load(context)
        val custom = loaded.customPresets.single()
        val applied = custom.applyTo(EditorState(detail = DetailAdjustments(sharpen = 20f)))
        val halfStrength = custom.applyTo(EditorState(), intensity = 50f)

        assertEquals("Night look", custom.name)
        assertEquals(source.light, custom.light)
        assertEquals(source.color, custom.color)
        assertEquals(source.filter, custom.filter)
        assertEquals(20f, applied.detail.sharpen, 0.001f)
        assertEquals(9f, halfStrength.light.exposure, 0.001f)
        assertEquals(7f, halfStrength.color.saturation, 0.001f)
        assertEquals(32.5f, halfStrength.filter.intensity, 0.001f)
        assertTrue("custom-night" in loaded.favoriteIds)
    }

    @Test
    fun `favorite toggles and custom delete remain local`() {
        var library = EditorPresetStore.toggleFavorite(context, "warm")
        assertTrue("warm" in library.favoriteIds)
        library = EditorPresetStore.toggleFavorite(context, "warm")
        assertFalse("warm" in library.favoriteIds)

        EditorPresetStore.saveCustom(context, "One", EditorState(), id = "custom-one")
        library = EditorPresetStore.deleteCustom(context, "custom-one")
        assertTrue(library.customPresets.isEmpty())
        assertFalse("custom-one" in library.favoriteIds)
    }

    @Test
    fun `custom library is bounded and corrupt storage falls back safely`() {
        repeat(EditorPresetStore.MAX_CUSTOM_PRESETS + 5) { index ->
            EditorPresetStore.saveCustom(context, "Look $index", EditorState(), id = "custom-$index")
        }
        assertEquals(EditorPresetStore.MAX_CUSTOM_PRESETS, EditorPresetStore.load(context).customPresets.size)

        context.getSharedPreferences("alphapics_editor_presets_v1", Context.MODE_PRIVATE)
            .edit().putString("library", "not-json").commit()
        assertEquals(EditorPresetLibrary(), EditorPresetStore.load(context))
    }
}
