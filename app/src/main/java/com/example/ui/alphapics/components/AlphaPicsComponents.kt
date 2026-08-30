package com.example.ui.alphapics.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.alphapics.navigation.AlphaPicsAccent
import com.example.ui.alphapics.navigation.AlphaPicsFeature
import com.example.ui.alphapics.navigation.AlphaPicsIcon
import com.example.ui.alphapics.theme.AlphaPicsColors
import com.example.ui.alphapics.theme.AlphaPicsGradients
import com.example.ui.alphapics.theme.AlphaPicsShapes
import com.example.ui.alphapics.theme.AlphaPicsSpacing

@Composable
fun AlphaPicsBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AlphaPicsGradients.Background)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AlphaPicsColors.ElectricBlue.copy(alpha = 0.13f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.12f, size.height * 0.08f),
                    radius = size.width * 0.7f
                ),
                radius = size.width * 0.7f,
                center = Offset(size.width * 0.12f, size.height * 0.08f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AlphaPicsColors.Violet.copy(alpha = 0.09f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.94f, size.height * 0.46f),
                    radius = size.width * 0.62f
                ),
                radius = size.width * 0.62f,
                center = Offset(size.width * 0.94f, size.height * 0.46f)
            )
        }
        content()
    }
}

@Composable
fun AlphaPicsBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 52.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .background(AlphaPicsColors.SurfaceRaised, AlphaPicsShapes.Medium)
            .border(1.dp, AlphaPicsGradients.Brand, AlphaPicsShapes.Medium)
            .padding(3.dp)
            .clip(AlphaPicsShapes.Small),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = R.drawable.alphapics_brand_logo,
            contentDescription = "AlphaPics AI logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun AlphaPicsBrandHeader(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AlphaPicsBrandMark(size = 50.dp)
            Spacer(Modifier.width(AlphaPicsSpacing.Md))
            Column {
                Text(
                    text = "AlphaPics AI",
                    color = AlphaPicsColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "PHOTO ENHANCER + EDITOR",
                    color = AlphaPicsColors.TextTertiary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(AlphaPicsShapes.Medium)
                .background(AlphaPicsColors.SurfaceRaised.copy(alpha = 0.9f))
                .border(1.dp, AlphaPicsColors.BorderSoft, AlphaPicsShapes.Medium)
                .clickable(role = Role.Button, onClick = onOpenSettings)
                .testTag("home_settings"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = AlphaPicsColors.TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun AlphaPicsBadge(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(accent.copy(alpha = 0.12f), AlphaPicsShapes.Pill)
            .border(1.dp, accent.copy(alpha = 0.38f), AlphaPicsShapes.Pill)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = text.uppercase(),
            color = accent,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun AlphaPicsPrimaryAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val tagged = if (testTag == null) modifier else modifier.testTag(testTag)
    Box(
        modifier = tagged
            .heightIn(min = 56.dp)
            .background(AlphaPicsGradients.Brand, AlphaPicsShapes.Medium)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = AlphaPicsSpacing.Xl, vertical = AlphaPicsSpacing.Lg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AlphaPicsHeroCard(
    onChoosePhoto: () -> Unit,
    onOpenCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AlphaPicsGradients.Hero, AlphaPicsShapes.Hero)
            .border(1.dp, AlphaPicsColors.BorderFocus, AlphaPicsShapes.Hero)
            .padding(AlphaPicsSpacing.Lg)
            .testTag("alphapics_hero")
    ) {
        AlphaPicsBadge(
            text = "AI PHOTO ENHANCER",
            accent = AlphaPicsColors.Cyan
        )
        Spacer(Modifier.height(AlphaPicsSpacing.Md))
        Text(
            text = "Bring every photo back to its best",
            color = AlphaPicsColors.TextPrimary,
            style = MaterialTheme.typography.displaySmall
        )
        Spacer(Modifier.height(AlphaPicsSpacing.Xs))
        Text(
            text = "Choose a photo, then improve clarity, detail and tone.",
            color = AlphaPicsColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(AlphaPicsSpacing.Md))
        AlphaPicsPhotoEntry(
            onChoosePhoto = onChoosePhoto,
            onOpenCamera = onOpenCamera,
            compact = true
        )
    }
}

@Composable
fun AlphaPicsSectionHeading(
    eyebrow: String,
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = eyebrow.uppercase(),
            color = AlphaPicsColors.Cyan,
            style = MaterialTheme.typography.labelSmall
        )
        Spacer(Modifier.height(AlphaPicsSpacing.Xs))
        Text(
            text = title,
            color = AlphaPicsColors.TextPrimary,
            style = MaterialTheme.typography.titleLarge
        )
        if (!description.isNullOrBlank()) {
            Spacer(Modifier.height(AlphaPicsSpacing.Sm))
            Text(
                text = description,
                color = AlphaPicsColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun AlphaPicsFeatureGrid(
    features: List<AlphaPicsFeature>,
    onFeatureClick: (AlphaPicsFeature) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Sm)
    ) {
        features.forEach { feature ->
            AlphaPicsFeatureCard(
                feature = feature,
                onClick = { onFeatureClick(feature) }
            )
        }
    }
}

@Composable
private fun AlphaPicsFeatureCard(
    feature: AlphaPicsFeature,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = feature.accentColor()
    Column(
        modifier = modifier
            .width(84.dp)
            .heightIn(min = 96.dp)
            .background(
                Brush.linearGradient(
                    listOf(
                        AlphaPicsColors.SurfaceRaised.copy(alpha = 0.96f),
                        accent.copy(alpha = 0.09f)
                    )
                ),
                AlphaPicsShapes.Card
            )
            .border(1.dp, accent.copy(alpha = 0.25f), AlphaPicsShapes.Card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = AlphaPicsSpacing.Sm, vertical = AlphaPicsSpacing.Md)
            .testTag("alphapics_feature_${feature.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AlphaPicsGlyph(icon = feature.icon, accent = accent, size = 40.dp)
        Spacer(Modifier.height(AlphaPicsSpacing.Sm))
        Text(
            text = feature.title,
            color = AlphaPicsColors.TextPrimary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AlphaPicsEditPhotoCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF171E34), Color(0xFF20163D))
                ),
                AlphaPicsShapes.Card
            )
            .border(1.dp, AlphaPicsColors.Violet.copy(alpha = 0.34f), AlphaPicsShapes.Card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = AlphaPicsSpacing.Lg, vertical = AlphaPicsSpacing.Md)
            .testTag("edit_photo_entry"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Lg)
    ) {
        AlphaPicsGlyph(
            icon = AlphaPicsIcon.EDIT,
            accent = AlphaPicsColors.Violet,
            size = 44.dp
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Edit Photo",
                color = AlphaPicsColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(AlphaPicsSpacing.Xs))
            Text(
                text = "Adjust, retouch, crop and refine.",
                color = AlphaPicsColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = "›",
            color = AlphaPicsColors.Cyan,
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Immutable
data class AlphaPicsToolSpec(
    val id: String,
    val title: String,
    val icon: AlphaPicsIcon,
    val accent: AlphaPicsAccent,
    val onClick: () -> Unit
)

@Composable
fun AlphaPicsToolGrid(
    tools: List<AlphaPicsToolSpec>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Sm)
    ) {
        tools.forEach { tool ->
            val accent = accentColor(tool.accent)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 88.dp)
                    .background(AlphaPicsColors.SurfaceRaised.copy(alpha = 0.76f), AlphaPicsShapes.Medium)
                    .border(1.dp, AlphaPicsColors.BorderSoft, AlphaPicsShapes.Medium)
                    .clickable(role = Role.Button, onClick = tool.onClick)
                    .padding(horizontal = AlphaPicsSpacing.Sm, vertical = AlphaPicsSpacing.Md)
                    .testTag("alphapics_tool_${tool.id}"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AlphaPicsGlyph(icon = tool.icon, accent = accent, size = 34.dp)
                Spacer(Modifier.height(AlphaPicsSpacing.Xs))
                Text(
                    text = tool.title,
                    color = AlphaPicsColors.TextPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AlphaPicsGlyph(
    icon: AlphaPicsIcon,
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = 50.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .background(accent.copy(alpha = 0.12f), AlphaPicsShapes.Medium)
            .border(1.dp, accent.copy(alpha = 0.4f), AlphaPicsShapes.Medium)
            .padding(size * 0.22f),
        contentAlignment = Alignment.Center
    ) {
        AlphaPicsLineIcon(icon = icon, tint = accent, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun AlphaPicsLineIcon(
    icon: AlphaPicsIcon,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = size.minDimension * 0.075f
        val stroke = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(tint, Offset(w * x1, h * y1), Offset(w * x2, h * y2), strokeWidth, StrokeCap.Round)
        }

        when (icon) {
            AlphaPicsIcon.ENHANCE -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.08f, h * 0.18f),
                    size = Size(w * 0.72f, h * 0.68f),
                    cornerRadius = CornerRadius(w * 0.1f),
                    style = stroke
                )
                line(0.15f, 0.72f, 0.35f, 0.50f)
                line(0.35f, 0.50f, 0.52f, 0.66f)
                line(0.52f, 0.66f, 0.67f, 0.48f)
                drawCircle(tint, w * 0.07f, Offset(w * 0.60f, h * 0.36f), style = stroke)
                line(0.84f, 0.08f, 0.84f, 0.30f)
                line(0.73f, 0.19f, 0.95f, 0.19f)
            }
            AlphaPicsIcon.FACE, AlphaPicsIcon.RETOUCH -> {
                drawCircle(tint, w * 0.18f, Offset(w * 0.5f, h * 0.35f), style = stroke)
                drawArc(
                    color = tint,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(w * 0.16f, h * 0.48f),
                    size = Size(w * 0.68f, h * 0.46f),
                    style = stroke
                )
                if (icon == AlphaPicsIcon.RETOUCH) {
                    line(0.78f, 0.10f, 0.78f, 0.28f)
                    line(0.69f, 0.19f, 0.87f, 0.19f)
                }
            }
            AlphaPicsIcon.RESTORE -> {
                drawArc(
                    color = tint,
                    startAngle = 35f,
                    sweepAngle = 290f,
                    useCenter = false,
                    topLeft = Offset(w * 0.14f, h * 0.14f),
                    size = Size(w * 0.7f, h * 0.7f),
                    style = stroke
                )
                line(0.16f, 0.13f, 0.16f, 0.38f)
                line(0.16f, 0.13f, 0.39f, 0.13f)
                drawCircle(tint, w * 0.08f, Offset(w * 0.5f, h * 0.5f), style = stroke)
            }
            AlphaPicsIcon.UPSCALE, AlphaPicsIcon.RESIZE -> {
                line(0.12f, 0.40f, 0.12f, 0.12f)
                line(0.12f, 0.12f, 0.40f, 0.12f)
                line(0.88f, 0.60f, 0.88f, 0.88f)
                line(0.88f, 0.88f, 0.60f, 0.88f)
                line(0.38f, 0.38f, 0.16f, 0.16f)
                line(0.62f, 0.62f, 0.84f, 0.84f)
            }
            AlphaPicsIcon.BACKGROUND -> {
                drawCircle(tint, w * 0.13f, Offset(w * 0.43f, h * 0.34f), style = stroke)
                drawArc(
                    color = tint,
                    startAngle = 205f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(w * 0.12f, h * 0.46f),
                    size = Size(w * 0.62f, h * 0.38f),
                    style = stroke
                )
                line(0.76f, 0.14f, 0.76f, 0.86f)
                line(0.86f, 0.22f, 0.86f, 0.36f)
                line(0.86f, 0.54f, 0.86f, 0.68f)
            }
            AlphaPicsIcon.EDIT -> {
                line(0.12f, 0.24f, 0.88f, 0.24f)
                line(0.12f, 0.50f, 0.88f, 0.50f)
                line(0.12f, 0.76f, 0.88f, 0.76f)
                drawCircle(tint, w * 0.08f, Offset(w * 0.36f, h * 0.24f), style = stroke)
                drawCircle(tint, w * 0.08f, Offset(w * 0.65f, h * 0.50f), style = stroke)
                drawCircle(tint, w * 0.08f, Offset(w * 0.45f, h * 0.76f), style = stroke)
            }
            AlphaPicsIcon.ERASER -> {
                val path = Path().apply {
                    moveTo(w * 0.22f, h * 0.65f)
                    lineTo(w * 0.56f, h * 0.22f)
                    lineTo(w * 0.84f, h * 0.46f)
                    lineTo(w * 0.52f, h * 0.82f)
                    close()
                }
                drawPath(path, tint, style = stroke)
                line(0.22f, 0.65f, 0.52f, 0.82f)
                line(0.14f, 0.86f, 0.84f, 0.86f)
            }
            AlphaPicsIcon.RELIGHT -> {
                drawCircle(tint, w * 0.18f, Offset(w * 0.5f, h * 0.5f), style = stroke)
                line(0.50f, 0.06f, 0.50f, 0.20f)
                line(0.50f, 0.80f, 0.50f, 0.94f)
                line(0.06f, 0.50f, 0.20f, 0.50f)
                line(0.80f, 0.50f, 0.94f, 0.50f)
                line(0.18f, 0.18f, 0.28f, 0.28f)
                line(0.72f, 0.72f, 0.82f, 0.82f)
            }
            AlphaPicsIcon.CONVERT -> {
                drawArc(tint, 205f, 230f, false, Offset(w * 0.1f, h * 0.1f), Size(w * 0.62f, h * 0.62f), style = stroke)
                line(0.62f, 0.08f, 0.77f, 0.23f)
                line(0.77f, 0.23f, 0.56f, 0.25f)
                drawArc(tint, 25f, 230f, false, Offset(w * 0.28f, h * 0.28f), Size(w * 0.62f, h * 0.62f), style = stroke)
                line(0.38f, 0.92f, 0.23f, 0.77f)
                line(0.23f, 0.77f, 0.44f, 0.75f)
            }
            AlphaPicsIcon.COMPRESS -> {
                line(0.16f, 0.16f, 0.40f, 0.40f)
                line(0.84f, 0.16f, 0.60f, 0.40f)
                line(0.16f, 0.84f, 0.40f, 0.60f)
                line(0.84f, 0.84f, 0.60f, 0.60f)
                line(0.16f, 0.16f, 0.16f, 0.34f)
                line(0.16f, 0.16f, 0.34f, 0.16f)
                line(0.84f, 0.16f, 0.66f, 0.16f)
                line(0.84f, 0.16f, 0.84f, 0.34f)
            }
            AlphaPicsIcon.BATCH -> {
                drawRoundRect(tint, Offset(w * 0.10f, h * 0.18f), Size(w * 0.58f, h * 0.58f), CornerRadius(w * 0.08f), style = stroke)
                drawRoundRect(tint, Offset(w * 0.30f, h * 0.30f), Size(w * 0.58f, h * 0.58f), CornerRadius(w * 0.08f), style = stroke)
            }
            AlphaPicsIcon.HISTORY -> {
                drawArc(tint, 35f, 300f, false, Offset(w * 0.12f, h * 0.12f), Size(w * 0.76f, h * 0.76f), style = stroke)
                line(0.15f, 0.16f, 0.15f, 0.40f)
                line(0.15f, 0.16f, 0.38f, 0.16f)
                line(0.50f, 0.30f, 0.50f, 0.52f)
                line(0.50f, 0.52f, 0.67f, 0.62f)
            }
        }
    }
}

fun AlphaPicsFeature.accentColor(): Color = accentColor(accent)

private fun accentColor(accent: AlphaPicsAccent): Color = when (accent) {
    AlphaPicsAccent.BLUE -> AlphaPicsColors.BrightBlue
    AlphaPicsAccent.VIOLET -> AlphaPicsColors.Violet
    AlphaPicsAccent.CYAN -> AlphaPicsColors.Cyan
    AlphaPicsAccent.PURPLE -> Color(0xFFC173FF)
}
