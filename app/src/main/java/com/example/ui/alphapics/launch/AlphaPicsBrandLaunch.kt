package com.example.ui.alphapics.launch

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.alphapics.components.AlphaPicsBackdrop
import com.example.ui.alphapics.motion.AlphaPicsLinearEasing
import com.example.ui.alphapics.motion.AlphaPicsMotion
import com.example.ui.alphapics.motion.LocalAlphaPicsReducedMotion
import com.example.ui.alphapics.theme.AlphaPicsColors
import com.example.ui.alphapics.theme.AlphaPicsSpacing
import com.example.ui.alphapics.theme.AlphaPicsTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Cold-start brand reveal: dark cinematic surface, lens/logo scale-in, wordmark rise,
 * and a single light sweep. Targets ~0.9s before handing over to Home (crossfaded by
 * the caller). Pure UI over the normal startup path — it never blocks initialization.
 */
@Composable
fun AlphaPicsBrandLaunch(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlphaPicsTheme {
        val view = LocalView.current
        SideEffect {
            // Keep status icons light during the reveal so they never disappear
            // against the dark surface.
            (view.context as? Activity)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }

        val reducedMotion = LocalAlphaPicsReducedMotion.current
        val logoProgress = remember { Animatable(0f) }
        val wordmarkProgress = remember { Animatable(0f) }
        val sweepProgress = remember { Animatable(0f) }

        LaunchedEffect(reducedMotion) {
            if (reducedMotion) {
                logoProgress.snapTo(1f)
                wordmarkProgress.snapTo(1f)
                sweepProgress.snapTo(1f)
                delay(200)
            } else {
                launch {
                    logoProgress.animateTo(1f, tween(360, easing = AlphaPicsMotion.EasingEnter))
                }
                delay(110)
                launch {
                    wordmarkProgress.animateTo(1f, tween(340, easing = AlphaPicsMotion.EasingEnter))
                }
                delay(320)
                sweepProgress.animateTo(1f, tween(430, easing = AlphaPicsLinearEasing))
                delay(140)
            }
            onFinished()
        }

        AlphaPicsBackdrop(
            modifier = modifier
                .fillMaxSize()
                .testTag("alphapics_brand_launch")
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val logoFraction = logoProgress.value
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = logoFraction
                            scaleX = 0.92f + 0.08f * logoFraction
                            scaleY = 0.92f + 0.08f * logoFraction
                        }
                        .size(88.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(AlphaPicsColors.SurfaceRaised)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = R.drawable.alphapics_brand_logo,
                        contentDescription = "AlphaPics AI",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.height(AlphaPicsSpacing.Lg))

                val wordmarkFraction = wordmarkProgress.value
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer {
                        alpha = wordmarkFraction
                        translationY = (1f - wordmarkFraction) * 14f
                    }
                ) {
                    Text(
                        text = "AlphaPics AI",
                        color = AlphaPicsColors.TextPrimary,
                        style = MaterialTheme.typography.displaySmall
                    )
                    Spacer(Modifier.height(AlphaPicsSpacing.Xs))
                    Text(
                        text = "PHOTO ENHANCER + EDITOR",
                        color = AlphaPicsColors.TextTertiary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(Modifier.height(AlphaPicsSpacing.Lg))

                val sweepFraction = sweepProgress.value
                Canvas(
                    modifier = Modifier
                        .width(160.dp)
                        .height(2.dp)
                        .graphicsLayer { alpha = wordmarkFraction }
                ) {
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(
                                AlphaPicsColors.ElectricBlue.copy(alpha = 0.12f),
                                AlphaPicsColors.Violet.copy(alpha = 0.12f)
                            )
                        ),
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = size.height,
                        cap = StrokeCap.Round
                    )
                    if (sweepFraction > 0f) {
                        val segmentWidth = size.width * 0.45f
                        val travel = size.width + segmentWidth
                        val segmentStart = sweepFraction * travel - segmentWidth
                        withTransform({ translate(left = segmentStart, top = 0f) }) {
                            drawLine(
                                brush = Brush.horizontalGradient(
                                    0f to Color.Transparent,
                                    0.5f to AlphaPicsColors.Cyan,
                                    1f to Color.Transparent,
                                    startX = 0f,
                                    endX = segmentWidth
                                ),
                                start = Offset(0f, size.height / 2f),
                                end = Offset(segmentWidth, size.height / 2f),
                                strokeWidth = size.height,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}
