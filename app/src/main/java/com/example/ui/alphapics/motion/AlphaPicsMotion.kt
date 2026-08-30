package com.example.ui.alphapics.motion

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

/**
 * AlphaPics motion tokens. Fast, subtle, premium: entrances finish well under a second,
 * press feedback is minimal, and nothing loops indefinitely on static surfaces.
 */
object AlphaPicsMotion {
    val DurationFast = 150
    val DurationStandard = 240
    val DurationEmphasized = 420
    const val EntranceStaggerMs = 55L
    const val EntranceRiseDp = 24f

    /** Confident deceleration for anything entering the screen. */
    val EasingEnter: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Gentle acceleration for anything leaving or shrinking. */
    val EasingExit: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
}

/**
 * True when the user (or the system) disabled animations system-wide. All AlphaPics
 * motion must snap to its final state instead of animating when this is set.
 */
val LocalAlphaPicsReducedMotion = staticCompositionLocalOf { false }

@Composable
fun rememberAlphaPicsReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * Staggered fade + rise entrance. Runs once per composition; when [enabled] is false
 * (screenshot tests, default callers) or motion is reduced, the modifier is a no-op so
 * content renders in its final state immediately.
 */
@Composable
fun Modifier.alphaPicsEntrance(order: Int, enabled: Boolean): Modifier {
    if (!enabled || LocalAlphaPicsReducedMotion.current) return this
    val progress = remember(order) { Animatable(0f) }
    LaunchedEffect(order) {
        delay(order * AlphaPicsMotion.EntranceStaggerMs)
        progress.animateTo(
            1f,
            tween(AlphaPicsMotion.DurationEmphasized, easing = AlphaPicsMotion.EasingEnter)
        )
    }
    val fraction = progress.value
    return graphicsLayer {
        alpha = fraction
        translationY = (1f - fraction) * AlphaPicsMotion.EntranceRiseDp
    }
}

/**
 * Press feedback for primary AlphaPics cards. Create the interaction source with
 * [rememberAlphaPicsPressInteraction], apply [Modifier.alphaPicsPressScale] BEFORE the
 * card background so the whole surface scales, then pass the same source to
 * `clickable(interactionSource, indication, role, onClick)` so the ripple still draws
 * above the background. When motion is reduced the scale snaps, keeping the
 * interaction fully functional.
 */
@Composable
fun rememberAlphaPicsPressInteraction(): MutableInteractionSource =
    remember { MutableInteractionSource() }

@Composable
fun Modifier.alphaPicsPressScale(interactionSource: MutableInteractionSource): Modifier {
    val reducedMotion = LocalAlphaPicsReducedMotion.current
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) 0.97f else 1f,
        animationSpec = if (reducedMotion) snap() else tween(AlphaPicsMotion.DurationFast),
        label = "alphaPicsPressScale"
    )
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Single-source crossfade spec honoring reduced motion. */
@Composable
fun rememberAlphaPicsFadeSpec(durationMs: Int = AlphaPicsMotion.DurationStandard) =
    if (LocalAlphaPicsReducedMotion.current) snap() else tween<Float>(
        durationMs,
        easing = AlphaPicsMotion.EasingEnter
    )

/** Re-exported so callers can build one-shot sweeps without extra imports. */
val AlphaPicsLinearEasing = LinearEasing
