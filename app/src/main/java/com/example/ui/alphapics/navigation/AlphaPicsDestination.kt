package com.example.ui.alphapics.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember

/**
 * Type-safe destination model for AlphaPics AI.
 * Represents all top-level destinations and feature workspaces in the application.
 */
sealed interface AlphaPicsDestination {
    /** Main AlphaPics AI discovery hub */
    data object Home : AlphaPicsDestination

    /** Enhancement workspace for AI Enhance, Restore, Upscale, Unblur, etc. */
    data class Enhance(
        val featureId: String,
        val initialModeId: String = "auto"
    ) : AlphaPicsDestination

    /** Device photo editor workspace for adjustments, crop, filters, detail, text */
    data class Editor(
        val initialToolId: String = "adjust"
    ) : AlphaPicsDestination

    /** Multi-photo collage workspace with local composition and high-resolution export. */
    data object Collage : AlphaPicsDestination

    /** Local Resize, Format Convert, Metadata/EXIF, and Image Info workspace. */
    data class PhotoUtilities(
        val initialTabId: String = "resize"
    ) : AlphaPicsDestination

    /** Honest placeholder screen for unintegrated cloud/AI routes */
    data class Placeholder(
        val feature: AlphaPicsFeature
    ) : AlphaPicsDestination

    /** Original protected image compressor workspace */
    data object Compressor : AlphaPicsDestination

    /** 20-photo batch compressor */
    data object Batch : AlphaPicsDestination

    /** Local multi-photo resize, convert, watermark, padding, logo, and preset workflow. */
    data object BatchStudio : AlphaPicsDestination

    /** Compression history log */
    data object History : AlphaPicsDestination

    /** AlphaPics application settings */
    data object Settings : AlphaPicsDestination
}

/**
 * State holder for AlphaPics navigation.
 * Maintains a back-stack of destinations and provides type-safe navigation methods.
 */
@Stable
class AlphaPicsNavState(
    initialDestination: AlphaPicsDestination = AlphaPicsDestination.Home
) {
    private val backStack = mutableStateListOf<AlphaPicsDestination>(initialDestination)

    val currentDestination: AlphaPicsDestination
        get() = backStack.lastOrNull() ?: AlphaPicsDestination.Home

    val canGoBack: Boolean
        get() = backStack.size > 1

    fun navigateTo(destination: AlphaPicsDestination) {
        if (currentDestination != destination) {
            backStack.add(destination)
        }
    }

    fun pop(): Boolean {
        return if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
            true
        } else {
            false
        }
    }

    fun popToRoot() {
        while (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    fun replace(destination: AlphaPicsDestination) {
        if (backStack.isNotEmpty()) {
            backStack[backStack.lastIndex] = destination
        } else {
            backStack.add(destination)
        }
    }
}

@Composable
fun rememberAlphaPicsNavState(
    initialDestination: AlphaPicsDestination = AlphaPicsDestination.Home
): AlphaPicsNavState {
    return remember { AlphaPicsNavState(initialDestination) }
}
