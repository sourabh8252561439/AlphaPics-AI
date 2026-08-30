package com.example.compression

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.alphapics.theme.AlphaPicsColors
import kotlin.math.roundToInt

@Composable
fun CompressionModeSelector(
    mode: CompressionMode,
    onModeChange: (CompressionMode) -> Unit,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf(
            CompressionMode.TARGET_SIZE to "Target Size",
            CompressionMode.QUALITY to "Quality"
        ).forEach { (candidate, label) ->
            val selected = mode == candidate
            OutlinedButton(
                onClick = { onModeChange(candidate) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.5.dp,
                    if (selected) AlphaPicsColors.BrightBlue else {
                        if (isDarkMode) Color(0xFF4B5563) else Color(0xFFCBD5E1)
                    }
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (selected) {
                        AlphaPicsColors.ElectricBlue
                    } else {
                        Color.Transparent
                    },
                    contentColor = if (selected) {
                        Color.White
                    } else {
                        if (isDarkMode) Color(0xFFE5E7EB) else Color(0xFF334155)
                    }
                )
            ) {
                Text(label, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TargetSizeControl(
    state: TargetSizeInputState,
    onStateChange: (TargetSizeInputState) -> Unit,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var previouslyFocused by remember { mutableStateOf(false) }
    val textColor = if (isDarkMode) AlphaPicsColors.TextPrimary else Color(0xFF111827)
    val secondaryColor = if (isDarkMode) AlphaPicsColors.TextSecondary else Color(0xFF64748B)
    val cardColor = if (isDarkMode) AlphaPicsColors.SurfaceRaised else Color(0xFFF8FAFC)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(cardColor, RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (isDarkMode) AlphaPicsColors.BorderSoft else Color(0xFFE2E8F0),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = state.previewKilobytes?.let { "Target Size: $it KB" }
                ?: "Target Size",
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = state.text,
            onValueChange = { onStateChange(state.withText(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (previouslyFocused && !focusState.isFocused) {
                        onStateChange(state.commit().state)
                    }
                    previouslyFocused = focusState.isFocused
                }
                .testTag("target_size_input_box"),
            placeholder = { Text("e.g. 100") },
            suffix = { Text("KB", fontWeight = FontWeight.Bold) },
            singleLine = true,
            isError = state.validationMessage != null &&
                state.committedKilobytes == null,
            supportingText = {
                Text(
                    state.validationMessage
                        ?: "Supported range: ${TargetSizeRules.MIN_KB}-${TargetSizeRules.MAX_KB} KB"
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    onStateChange(state.commit().state)
                    focusManager.clearFocus()
                }
            )
        )

        Spacer(Modifier.height(8.dp))
        listOf(
            listOf("Passport • 20 KB" to 20, "Forms • 50 KB" to 50),
            listOf("100 KB" to 100, "500 KB" to 500)
        ).forEach { rowPresets ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowPresets.forEach { (label, kilobytes) ->
                    OutlinedButton(
                        onClick = { onStateChange(state.withPreset(kilobytes)) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Text(label, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Slider(
            value = state.sliderPosition,
            onValueChange = { onStateChange(state.withSliderPosition(it)) },
            onValueChangeFinished = {
                onStateChange(state.commit().state)
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = AlphaPicsColors.BrightBlue,
                activeTrackColor = AlphaPicsColors.ElectricBlue
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("target_size_slider")
        )
        Text(
            "Fine control for small file-size targets.",
            color = secondaryColor,
            fontSize = 11.sp
        )
    }
}

@Composable
fun QualityControl(
    value: Float,
    onValueChange: (Float) -> Unit,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor = if (isDarkMode) AlphaPicsColors.TextPrimary else Color(0xFF111827)
    val secondaryColor = if (isDarkMode) AlphaPicsColors.TextSecondary else Color(0xFF64748B)
    val cardColor = if (isDarkMode) AlphaPicsColors.SurfaceRaised else Color(0xFFF8FAFC)
    val displayedQuality = value.roundToInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(cardColor, RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (isDarkMode) AlphaPicsColors.BorderSoft else Color(0xFFE2E8F0),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            "Quality: $displayedQuality%",
            color = textColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = {
                onValueChange(
                    it.coerceIn(
                        QUALITY_MIN_PERCENT.toFloat(),
                        QUALITY_MAX_PERCENT.toFloat()
                    )
                )
            },
            valueRange = QUALITY_MIN_PERCENT.toFloat()..QUALITY_MAX_PERCENT.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = AlphaPicsColors.Violet,
                activeTrackColor = AlphaPicsColors.Violet
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quality_slider")
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(90, 85, 80, 70).forEach { preset ->
                OutlinedButton(
                    onClick = { onValueChange(preset.toFloat()) },
                    modifier = Modifier.weight(1f),
                    contentPadding = ButtonDefaults.ContentPadding
                ) {
                    Text("$preset%", fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Higher values keep more visual detail and usually create larger files.",
            color = secondaryColor,
            fontSize = 11.sp
        )
    }
}
