package com.example.editor

import androidx.compose.ui.graphics.ColorMatrix
import kotlin.math.max
import kotlin.math.min

/**
 * Math and Matrix computation engine for AlphaPics device photo editing.
 * Transforms non-destructive adjustment parameters into hardware-accelerated ColorMatrix representations.
 */
object EditorColorEngine {

    /**
     * Builds a composite 4x5 ColorMatrix representing all active light, color, and filter adjustments.
     */
    fun buildColorMatrix(state: EditorState): ColorMatrix {
        val preset = FilterPresetCatalog.find(state.filter.presetId)
        val filterWeight = (state.filter.intensity.coerceIn(0f, 100f)) / 100f

        // Combine light & filter values
        val totalExposure = state.light.exposure + (preset.exposure * filterWeight)
        val totalBrightness = state.light.brightness
        val totalContrast = state.light.contrast + (preset.contrast * filterWeight)
        val totalHighlights = state.light.highlights
        val totalShadows = state.light.shadows
        val totalWhites = state.light.whites
        val totalBlacks = state.light.blacks

        // Combine color & filter values
        val totalSaturation = state.color.saturation + state.color.vibrance * 0.5f + (preset.saturation * filterWeight)
        val totalWarmth = state.color.warmth + (preset.warmth * filterWeight)
        val totalTint = state.color.tint + (preset.tint * filterWeight)

        return computeMatrix(
            exposure = totalExposure,
            brightness = totalBrightness,
            contrast = totalContrast,
            highlights = totalHighlights,
            shadows = totalShadows,
            whites = totalWhites,
            blacks = totalBlacks,
            saturation = totalSaturation,
            warmth = totalWarmth,
            tint = totalTint
        )
    }

    /**
     * Internal matrix builder blending tonal range adjustments, contrast, saturation, and white balance.
     */
    fun computeMatrix(
        exposure: Float = 0f,
        brightness: Float = 0f,
        contrast: Float = 0f,
        highlights: Float = 0f,
        shadows: Float = 0f,
        whites: Float = 0f,
        blacks: Float = 0f,
        saturation: Float = 0f,
        warmth: Float = 0f,
        tint: Float = 0f
    ): ColorMatrix {
        val safeContrast = min(100f, max(-100f, contrast))
        val safeSaturation = min(100f, max(-100f, saturation))
        val safeExposure = min(100f, max(-100f, exposure))
        val safeBrightness = min(100f, max(-100f, brightness))
        val safeWarmth = min(100f, max(-100f, warmth))
        val safeTint = min(100f, max(-100f, tint))
        val safeHighlights = min(100f, max(-100f, highlights))
        val safeShadows = min(100f, max(-100f, shadows))
        val safeWhites = min(100f, max(-100f, whites))
        val safeBlacks = min(100f, max(-100f, blacks))

        // Contrast multiplier
        val c = 1f + safeContrast / 100f

        // Saturation multiplier
        val s = max(0f, 1f + safeSaturation / 100f)
        val inverseSaturation = 1f - s
        val redWeight = 0.213f * inverseSaturation
        val greenWeight = 0.715f * inverseSaturation
        val blueWeight = 0.072f * inverseSaturation

        // Tonal offset calculation (exposure + brightness + highlights + shadows + whites + blacks)
        val tonalOffset = safeExposure * 1.28f + safeBrightness * 1.28f +
            (safeHighlights * 0.64f) + (safeShadows * 0.64f) +
            (safeWhites * 0.8f) + (safeBlacks * 0.8f)

        val baseOffset = (1f - c) * 128f + tonalOffset
        val warmOffset = safeWarmth * 0.45f
        val tintOffset = safeTint * 0.45f

        return ColorMatrix(
            floatArrayOf(
                (redWeight + s) * c, greenWeight * c, blueWeight * c, 0f, baseOffset + warmOffset + (tintOffset * 0.5f),
                redWeight * c, (greenWeight + s) * c, blueWeight * c, 0f, baseOffset - (tintOffset * 0.5f),
                redWeight * c, greenWeight * c, (blueWeight + s) * c, 0f, baseOffset - warmOffset + (tintOffset * 0.5f),
                0f, 0f, 0f, 1f, 0f
            )
        )
    }
}
