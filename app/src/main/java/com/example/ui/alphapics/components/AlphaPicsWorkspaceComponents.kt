package com.example.ui.alphapics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.alphapics.navigation.AlphaPicsIcon
import com.example.ui.alphapics.theme.AlphaPicsColors
import com.example.ui.alphapics.theme.AlphaPicsShapes
import com.example.ui.alphapics.theme.AlphaPicsSpacing
import kotlin.math.roundToInt

@Composable
fun AlphaPicsWorkspaceTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(AlphaPicsColors.SurfaceRaised, AlphaPicsShapes.Medium)
                    .border(1.dp, AlphaPicsColors.BorderSoft, AlphaPicsShapes.Medium)
                    .clickable(role = Role.Button, onClick = onBack)
                    .testTag("workspace_back_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AlphaPicsColors.TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(AlphaPicsSpacing.Md))
            Column {
                Text(
                    text = title,
                    color = AlphaPicsColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = subtitle.uppercase(),
                    color = AlphaPicsColors.TextTertiary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        trailing()
    }
}

@Composable
fun AlphaPicsPhotoEntry(
    onChoosePhoto: () -> Unit,
    onOpenCamera: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AlphaPicsColors.Surface.copy(alpha = 0.92f), AlphaPicsShapes.Card)
            .border(1.dp, AlphaPicsColors.Cyan.copy(alpha = 0.28f), AlphaPicsShapes.Card)
            .padding(if (compact) AlphaPicsSpacing.Md else AlphaPicsSpacing.Lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AlphaPicsGlyph(
            icon = AlphaPicsIcon.ENHANCE,
            accent = AlphaPicsColors.Cyan,
            size = if (compact) 42.dp else 52.dp
        )
        Spacer(Modifier.height(AlphaPicsSpacing.Sm))
        Text(
            text = "Your photo starts here",
            color = AlphaPicsColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Choose from Gallery or Camera",
            color = AlphaPicsColors.TextTertiary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(AlphaPicsSpacing.Md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Sm)
        ) {
            Button(
                onClick = onChoosePhoto,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("workspace_choose_photo"),
                shape = AlphaPicsShapes.Medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlphaPicsColors.ElectricBlue,
                    contentColor = Color.White
                )
            ) {
                Text("Choose photo", style = MaterialTheme.typography.labelLarge)
            }
            OutlinedButton(
                onClick = onOpenCamera,
                modifier = Modifier
                    .weight(0.78f)
                    .heightIn(min = 48.dp)
                    .testTag("workspace_camera"),
                shape = AlphaPicsShapes.Medium,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    AlphaPicsColors.Cyan.copy(alpha = 0.72f)
                ),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlphaPicsColors.Cyan)
            ) {
                Text("Camera", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun AlphaPicsAvailabilityCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    badge: String = "Coming soon",
    accent: Color = AlphaPicsColors.Cyan
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AlphaPicsColors.SurfaceRaised.copy(alpha = 0.94f), AlphaPicsShapes.Medium)
            .border(1.dp, accent.copy(alpha = 0.25f), AlphaPicsShapes.Medium)
            .padding(horizontal = AlphaPicsSpacing.Lg, vertical = AlphaPicsSpacing.Md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = AlphaPicsColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(AlphaPicsSpacing.Sm))
            AlphaPicsBadge(text = badge, accent = accent)
        }
        Spacer(Modifier.height(AlphaPicsSpacing.Xs))
        Text(
            text = description,
            color = AlphaPicsColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun AlphaPicsLoadingState(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AlphaPicsColors.SurfaceRaised.copy(alpha = 0.96f), AlphaPicsShapes.Card)
            .border(1.dp, AlphaPicsColors.BorderSoft, AlphaPicsShapes.Card)
            .padding(AlphaPicsSpacing.Lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(36.dp)
                .testTag("alphapics_loading_indicator"),
            color = AlphaPicsColors.Cyan,
            trackColor = AlphaPicsColors.SurfaceSoft,
            strokeWidth = 3.dp
        )
        Spacer(Modifier.height(AlphaPicsSpacing.Md))
        Text(
            text = title,
            color = AlphaPicsColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(AlphaPicsSpacing.Xs))
        Text(
            text = description,
            color = AlphaPicsColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AlphaPicsErrorState(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AlphaPicsColors.SurfaceRaised.copy(alpha = 0.96f), AlphaPicsShapes.Card)
            .border(1.dp, AlphaPicsColors.Warning.copy(alpha = 0.42f), AlphaPicsShapes.Card)
            .padding(AlphaPicsSpacing.Lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AlphaPicsBadge(text = "Couldn’t open", accent = AlphaPicsColors.Warning)
        Spacer(Modifier.height(AlphaPicsSpacing.Md))
        Text(
            text = title,
            color = AlphaPicsColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(AlphaPicsSpacing.Xs))
        Text(
            text = description,
            color = AlphaPicsColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(AlphaPicsSpacing.Md))
        Button(
            onClick = onAction,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("alphapics_error_action"),
            shape = AlphaPicsShapes.Medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = AlphaPicsColors.ElectricBlue,
                contentColor = Color.White
            )
        ) {
            Text(actionLabel, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun AlphaPicsValueSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = -100f..100f,
    steps: Int = 0,
    valueFormatter: (Float) -> String = { it.roundToInt().toString() }
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = AlphaPicsColors.TextPrimary,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = valueFormatter(value),
                color = AlphaPicsColors.Cyan,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .semantics {
                    contentDescription = label
                    stateDescription = valueFormatter(value)
                },
            colors = SliderDefaults.colors(
                thumbColor = AlphaPicsColors.Cyan,
                activeTrackColor = AlphaPicsColors.ElectricBlue,
                inactiveTrackColor = AlphaPicsColors.Border
            )
        )
    }
}

@Composable
fun AlphaPicsContextActions(
    onCancel: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
    applyEnabled: Boolean = true,
    cancelLabel: String = "Cancel",
    applyLabel: String = "Apply"
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Sm)
    ) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
            shape = AlphaPicsShapes.Medium,
            border = androidx.compose.foundation.BorderStroke(1.dp, AlphaPicsColors.BorderFocus),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlphaPicsColors.TextPrimary)
        ) {
            Text(cancelLabel, style = MaterialTheme.typography.labelLarge)
        }
        Button(
            onClick = onApply,
            enabled = applyEnabled,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
            shape = AlphaPicsShapes.Medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = AlphaPicsColors.ElectricBlue,
                contentColor = Color.White,
                disabledContainerColor = AlphaPicsColors.SurfaceSoft,
                disabledContentColor = AlphaPicsColors.TextTertiary
            )
        ) {
            Text(applyLabel, style = MaterialTheme.typography.labelLarge)
        }
    }
}
