package com.example.enhance

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class EnhancementMode(
    val id: String,
    val label: String,
    val description: String,
    val isLocalAvailable: Boolean = false,
    val tokenCost: Int = 1
)

object EnhancementCatalog {
    val Auto = EnhancementMode(
        id = "auto",
        label = "Auto",
        description = "Balanced intelligent exposure, contrast, and vibrance optimization on-device.",
        isLocalAvailable = true,
        tokenCost = 1
    )
    val Face = EnhancementMode(
        id = "face",
        label = "Face",
        description = "Natural portrait recovery and facial clarity.",
        isLocalAvailable = false,
        tokenCost = 1
    )
    val Unblur = EnhancementMode(
        id = "unblur",
        label = "Unblur",
        description = "Edge refinement and focal clarity enhancement.",
        isLocalAvailable = true,
        tokenCost = 1
    )
    val Denoise = EnhancementMode(
        id = "denoise",
        label = "Denoise",
        description = "Smooth sensor grain while preserving structural edges.",
        isLocalAvailable = true,
        tokenCost = 1
    )
    val Restore = EnhancementMode(
        id = "restore",
        label = "Restore",
        description = "Repair faded tones and restore dynamic range.",
        isLocalAvailable = true,
        tokenCost = 1
    )
    val Color = EnhancementMode(
        id = "color",
        label = "Color",
        description = "Intelligent white balance and color vibrancy optimization.",
        isLocalAvailable = true,
        tokenCost = 1
    )
    val Light = EnhancementMode(
        id = "light",
        label = "Light",
        description = "Shadow recovery and dynamic highlight tone balancing.",
        isLocalAvailable = true,
        tokenCost = 1
    )
    val Detail = EnhancementMode(
        id = "detail",
        label = "Detail",
        description = "Micro-contrast and fine texture amplification.",
        isLocalAvailable = true,
        tokenCost = 1
    )
    val Upscale = EnhancementMode(
        id = "upscale",
        label = "Upscale",
        description = "Super-resolution synthesis for ultra high-definition prints.",
        isLocalAvailable = false,
        tokenCost = 2
    )

    val Modes = listOf(
        Auto,
        Face,
        Unblur,
        Denoise,
        Restore,
        Color,
        Light,
        Detail,
        Upscale
    )

    fun find(id: String): EnhancementMode = Modes.firstOrNull { it.id == id } ?: Auto
}

sealed interface EnhancementResult {
    data class Success(
        val outputUri: Uri,
        val width: Int,
        val height: Int,
        val sizeBytes: Long,
        val processingTimeMs: Long,
        val modeId: String
    ) : EnhancementResult

    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : EnhancementResult

    data class TokenRequired(
        val requiredTokens: Int,
        val availableTokens: Int
    ) : EnhancementResult

    data class ProviderNotConfigured(
        val providerName: String,
        val modeId: String
    ) : EnhancementResult
}
