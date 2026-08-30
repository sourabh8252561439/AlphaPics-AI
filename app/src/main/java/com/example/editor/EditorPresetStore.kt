package com.example.editor

import android.content.Context
import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

@Immutable
data class EditorCustomPreset(
    val id: String,
    val name: String,
    val light: LightAdjustments,
    val color: ColorAdjustments,
    val filter: FilterAdjustment,
    val createdAtMillis: Long
) {
    fun applyTo(base: EditorState, intensity: Float = 100f): EditorState {
        val weight = intensity.coerceIn(0f, 100f) / 100f
        return base.copy(
            light = LightAdjustments(
                exposure = light.exposure * weight,
                brightness = light.brightness * weight,
                contrast = light.contrast * weight,
                highlights = light.highlights * weight,
                shadows = light.shadows * weight,
                whites = light.whites * weight,
                blacks = light.blacks * weight,
                gamma = light.gamma * weight
            ),
            color = ColorAdjustments(
                saturation = color.saturation * weight,
                vibrance = color.vibrance * weight,
                warmth = color.warmth * weight,
                tint = color.tint * weight
            ),
            filter = filter.copy(intensity = filter.intensity * weight)
        )
    }

    fun previewPreset(): FilterPreset {
        val base = FilterPresetCatalog.find(filter.presetId)
        val weight = filter.intensity.coerceIn(0f, 100f) / 100f
        return FilterPreset(
            id = id,
            label = name,
            exposure = light.exposure + base.exposure * weight,
            contrast = light.contrast + base.contrast * weight,
            saturation = color.saturation + base.saturation * weight,
            warmth = color.warmth + base.warmth * weight,
            tint = color.tint + base.tint * weight
        )
    }
}

@Immutable
data class EditorPresetLibrary(
    val favoriteIds: Set<String> = emptySet(),
    val customPresets: List<EditorCustomPreset> = emptyList()
)

/** Lightweight local persistence for favorite IDs and user-saved global look presets. */
object EditorPresetStore {

    private const val PREFERENCES_NAME = "alphapics_editor_presets_v1"
    private const val LIBRARY_KEY = "library"
    const val MAX_CUSTOM_PRESETS = 20

    fun load(context: Context): EditorPresetLibrary {
        val raw = preferences(context).getString(LIBRARY_KEY, null) ?: return EditorPresetLibrary()
        return runCatching { decode(raw) }.getOrElse { EditorPresetLibrary() }
    }

    fun toggleFavorite(context: Context, presetId: String): EditorPresetLibrary {
        if (presetId.isBlank()) return load(context)
        val current = load(context)
        val favorites = current.favoriteIds.toMutableSet().apply {
            if (!add(presetId)) remove(presetId)
        }
        return current.copy(favoriteIds = favorites).also { persist(context, it) }
    }

    fun saveCustom(
        context: Context,
        name: String,
        state: EditorState,
        id: String = "custom-${UUID.randomUUID()}",
        createdAtMillis: Long = System.currentTimeMillis()
    ): EditorPresetLibrary {
        val safeName = name.trim().take(32).ifBlank { "My look" }
        val current = load(context)
        val preset = EditorCustomPreset(
            id = id,
            name = safeName,
            light = state.light,
            color = state.color,
            filter = state.filter,
            createdAtMillis = createdAtMillis
        )
        val custom = (current.customPresets.filterNot { it.id == id } + preset)
            .takeLast(MAX_CUSTOM_PRESETS)
        val validIds = FilterPresetCatalog.Presets.mapTo(mutableSetOf()) { it.id }
            .apply { addAll(custom.map { it.id }) }
        val updated = current.copy(
            favoriteIds = (current.favoriteIds + id).filterTo(linkedSetOf()) { it in validIds },
            customPresets = custom
        )
        persist(context, updated)
        return updated
    }

    fun deleteCustom(context: Context, presetId: String): EditorPresetLibrary {
        val current = load(context)
        val updated = current.copy(
            favoriteIds = current.favoriteIds - presetId,
            customPresets = current.customPresets.filterNot { it.id == presetId }
        )
        persist(context, updated)
        return updated
    }

    internal fun clear(context: Context) {
        preferences(context).edit().clear().commit()
    }

    private fun persist(context: Context, library: EditorPresetLibrary) {
        preferences(context).edit().putString(LIBRARY_KEY, encode(library)).apply()
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private fun encode(library: EditorPresetLibrary): String = JSONObject().apply {
        put("favorites", JSONArray(library.favoriteIds.toList()))
        put("custom", JSONArray().apply {
            library.customPresets.forEach { preset ->
                put(JSONObject().apply {
                    put("id", preset.id)
                    put("name", preset.name)
                    put("created", preset.createdAtMillis)
                    put("light", encodeLight(preset.light))
                    put("color", encodeColor(preset.color))
                    put("filter", JSONObject().apply {
                        put("id", preset.filter.presetId)
                        put("intensity", preset.filter.intensity.toDouble())
                    })
                })
            }
        })
    }.toString()

    private fun decode(raw: String): EditorPresetLibrary {
        val root = JSONObject(raw)
        val favoritesArray = root.optJSONArray("favorites") ?: JSONArray()
        val favorites = buildSet {
            for (index in 0 until favoritesArray.length()) {
                favoritesArray.optString(index).takeIf(String::isNotBlank)?.let(::add)
            }
        }
        val customArray = root.optJSONArray("custom") ?: JSONArray()
        val custom = buildList {
            for (index in 0 until customArray.length()) {
                val item = customArray.optJSONObject(index) ?: continue
                val id = item.optString("id").takeIf(String::isNotBlank) ?: continue
                val light = item.optJSONObject("light")?.let(::decodeLight) ?: LightAdjustments()
                val color = item.optJSONObject("color")?.let(::decodeColor) ?: ColorAdjustments()
                val filterObject = item.optJSONObject("filter")
                add(
                    EditorCustomPreset(
                        id = id,
                        name = item.optString("name", "My look").take(32),
                        light = light,
                        color = color,
                        filter = FilterAdjustment(
                            presetId = filterObject?.optString("id", "original") ?: "original",
                            intensity = (filterObject?.optDouble("intensity", 100.0) ?: 100.0).toFloat()
                                .coerceIn(0f, 100f)
                        ),
                        createdAtMillis = item.optLong("created", 0L)
                    )
                )
            }
        }.takeLast(MAX_CUSTOM_PRESETS)
        return EditorPresetLibrary(favoriteIds = favorites, customPresets = custom)
    }

    private fun encodeLight(value: LightAdjustments) = JSONObject().apply {
        put("exposure", value.exposure.toDouble())
        put("brightness", value.brightness.toDouble())
        put("contrast", value.contrast.toDouble())
        put("highlights", value.highlights.toDouble())
        put("shadows", value.shadows.toDouble())
        put("whites", value.whites.toDouble())
        put("blacks", value.blacks.toDouble())
        put("gamma", value.gamma.toDouble())
    }

    private fun decodeLight(value: JSONObject) = LightAdjustments(
        exposure = value.float("exposure"),
        brightness = value.float("brightness"),
        contrast = value.float("contrast"),
        highlights = value.float("highlights"),
        shadows = value.float("shadows"),
        whites = value.float("whites"),
        blacks = value.float("blacks"),
        gamma = value.float("gamma")
    )

    private fun encodeColor(value: ColorAdjustments) = JSONObject().apply {
        put("saturation", value.saturation.toDouble())
        put("vibrance", value.vibrance.toDouble())
        put("warmth", value.warmth.toDouble())
        put("tint", value.tint.toDouble())
    }

    private fun decodeColor(value: JSONObject) = ColorAdjustments(
        saturation = value.float("saturation"),
        vibrance = value.float("vibrance"),
        warmth = value.float("warmth"),
        tint = value.float("tint")
    )

    private fun JSONObject.float(key: String): Float =
        optDouble(key, 0.0).toFloat().coerceIn(-100f, 100f)
}
