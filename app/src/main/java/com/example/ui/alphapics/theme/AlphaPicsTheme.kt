package com.example.ui.alphapics.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.alphapics.motion.LocalAlphaPicsReducedMotion
import com.example.ui.alphapics.motion.rememberAlphaPicsReducedMotion

object AlphaPicsColors {
    val Void = Color(0xFF04050A)
    val DeepNavy = Color(0xFF070B16)
    val Navy = Color(0xFF0A1020)
    val Surface = Color(0xFF0D1425)
    val SurfaceRaised = Color(0xFF121B31)
    val SurfaceSoft = Color(0xFF171F35)
    val Border = Color(0xFF263453)
    val BorderSoft = Color(0xFF1A2742)
    val BorderFocus = Color(0xFF365A99)
    val ElectricBlue = Color(0xFF3478FF)
    val BrightBlue = Color(0xFF56A0FF)
    val Violet = Color(0xFF9A5CFF)
    val Purple = Color(0xFF6F4CFF)
    val Cyan = Color(0xFF52E2FF)
    val TextPrimary = Color(0xFFF7F9FF)
    val TextSecondary = Color(0xFFA9B4CC)
    val TextTertiary = Color(0xFF8996B2)
    val Success = Color(0xFF5DE0B8)
    val Warning = Color(0xFFFFB85C)
    val Danger = Color(0xFFFF7185)
}

object AlphaPicsGradients {
    val Brand = Brush.linearGradient(
        colors = listOf(
            AlphaPicsColors.Cyan,
            AlphaPicsColors.ElectricBlue,
            AlphaPicsColors.Violet
        )
    )

    val Hero = Brush.linearGradient(
        colors = listOf(
            Color(0xFF10264B),
            Color(0xFF151A39),
            Color(0xFF261440)
        )
    )

    val Background = Brush.verticalGradient(
        colors = listOf(
            AlphaPicsColors.Void,
            AlphaPicsColors.DeepNavy,
            Color(0xFF080713),
            AlphaPicsColors.Void
        )
    )
}

object AlphaPicsSpacing {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 24.dp
    val Xxl = 32.dp
    val Section = 40.dp
    val ScreenHorizontal = 18.dp
}

object AlphaPicsShapes {
    val Small = RoundedCornerShape(10.dp)
    val Medium = RoundedCornerShape(16.dp)
    val Card = RoundedCornerShape(22.dp)
    val Hero = RoundedCornerShape(28.dp)
    val Pill = RoundedCornerShape(100.dp)
}

private val AlphaPicsColorScheme = darkColorScheme(
    primary = AlphaPicsColors.ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF142E60),
    onPrimaryContainer = AlphaPicsColors.TextPrimary,
    secondary = AlphaPicsColors.Violet,
    onSecondary = Color.White,
    tertiary = AlphaPicsColors.Cyan,
    background = AlphaPicsColors.Void,
    onBackground = AlphaPicsColors.TextPrimary,
    surface = AlphaPicsColors.Surface,
    onSurface = AlphaPicsColors.TextPrimary,
    surfaceVariant = AlphaPicsColors.SurfaceRaised,
    onSurfaceVariant = AlphaPicsColors.TextSecondary,
    outline = AlphaPicsColors.Border
)

private val AlphaPicsTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 31.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 21.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.1.sp
    )
)

@Composable
fun AlphaPicsTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAlphaPicsReducedMotion provides rememberAlphaPicsReducedMotion()
    ) {
        MaterialTheme(
        colorScheme = AlphaPicsColorScheme,
        typography = AlphaPicsTypography,
        shapes = MaterialTheme.shapes.copy(
            small = AlphaPicsShapes.Small,
            medium = AlphaPicsShapes.Medium,
            large = AlphaPicsShapes.Card
        ),
        content = content
        )
    }
}
