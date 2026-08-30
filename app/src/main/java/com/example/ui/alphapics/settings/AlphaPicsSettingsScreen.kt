package com.example.ui.alphapics.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.alphapics.components.AlphaPicsBackdrop
import com.example.ui.alphapics.components.AlphaPicsBrandMark
import com.example.ui.alphapics.components.AlphaPicsGlyph
import com.example.ui.alphapics.navigation.AlphaPicsAccent
import com.example.ui.alphapics.navigation.AlphaPicsIcon
import com.example.ui.alphapics.theme.AlphaPicsColors
import com.example.ui.alphapics.theme.AlphaPicsShapes
import com.example.ui.alphapics.theme.AlphaPicsSpacing
import com.example.ui.alphapics.theme.AlphaPicsTheme
import com.example.util.AlphaPicsStorageManager
import kotlinx.coroutines.launch

@Composable
fun AlphaPicsSettingsScreen(
    isDarkMode: Boolean,
    versionName: String,
    onBack: () -> Unit,
    onOpenAppearance: () -> Unit,
    onCustomerSupport: () -> Unit,
    onRate: () -> Unit,
    onShare: () -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var cacheText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val bytes = AlphaPicsStorageManager.getCacheSizeBytes(context)
        cacheText = AlphaPicsStorageManager.formatBytes(bytes)
    }

    AlphaPicsTheme {
        AlphaPicsBackdrop(modifier = modifier) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = AlphaPicsSpacing.ScreenHorizontal)
                        .testTag("alphapics_settings")
                ) {
                    Spacer(Modifier.height(AlphaPicsSpacing.Md))
                    SettingsTopBar(onBack = onBack)
                    Spacer(Modifier.height(AlphaPicsSpacing.Section))

                    SettingsSection(title = "Appearance") {
                        SettingsRow(
                            title = "Theme",
                            description = if (isDarkMode) "Dark appearance" else "Light appearance",
                            icon = AlphaPicsIcon.RELIGHT,
                            accent = AlphaPicsAccent.BLUE,
                            onClick = onOpenAppearance,
                            testTag = "settings_appearance"
                        )
                    }

                    Spacer(Modifier.height(AlphaPicsSpacing.Xl))
                    SettingsSection(title = "Studio & Storage") {
                        SettingsRow(
                            title = "Clear Cache",
                            description = if (cacheText.isNotEmpty()) "Cache size: $cacheText • Free up temporary files" else "Free up temporary cache files",
                            icon = AlphaPicsIcon.CONVERT,
                            accent = AlphaPicsAccent.CYAN,
                            onClick = {
                                coroutineScope.launch {
                                    val freed = AlphaPicsStorageManager.clearCache(context)
                                    cacheText = AlphaPicsStorageManager.formatBytes(0L)
                                    Toast.makeText(context, "Cleared ${AlphaPicsStorageManager.formatBytes(freed)} temporary cache", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(AlphaPicsSpacing.Xl))
                    SettingsSection(title = "Support") {
                        SettingsRow(
                            title = "Customer Support",
                            description = "Get help or tell us what could be better.",
                            icon = AlphaPicsIcon.FACE,
                            accent = AlphaPicsAccent.CYAN,
                            onClick = onCustomerSupport
                        )
                        SettingsDivider()
                        SettingsRow(
                            title = "Rate AlphaPics AI",
                            description = "Share your experience on Google Play.",
                            icon = AlphaPicsIcon.ENHANCE,
                            accent = AlphaPicsAccent.VIOLET,
                            onClick = onRate
                        )
                        SettingsDivider()
                        SettingsRow(
                            title = "Share AlphaPics AI",
                            description = "Send the app to friends and family.",
                            icon = AlphaPicsIcon.EDIT,
                            accent = AlphaPicsAccent.BLUE,
                            onClick = onShare
                        )
                    }

                    Spacer(Modifier.height(AlphaPicsSpacing.Xl))
                    SettingsSection(title = "Legal") {
                        SettingsRow(
                            title = "Privacy Policy",
                            description = "See how the app handles information.",
                            icon = AlphaPicsIcon.RESTORE,
                            accent = AlphaPicsAccent.CYAN,
                            onClick = onPrivacy
                        )
                        SettingsDivider()
                        SettingsRow(
                            title = "Terms of Service",
                            description = "Read the terms for using AlphaPics AI.",
                            icon = AlphaPicsIcon.EDIT,
                            accent = AlphaPicsAccent.PURPLE,
                            onClick = onTerms
                        )
                    }

                    Spacer(Modifier.height(AlphaPicsSpacing.Xl))
                    SettingsSection(title = "About") {
                        SettingsRow(
                            title = "AlphaPics AI",
                            description = "Photo Enhancer + Editor  •  Version $versionName",
                            icon = AlphaPicsIcon.ENHANCE,
                            accent = AlphaPicsAccent.BLUE,
                            onClick = null
                        )
                    }
                    Spacer(Modifier.height(AlphaPicsSpacing.Xxl))
                }
            }
        }
    }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(AlphaPicsColors.SurfaceRaised, AlphaPicsShapes.Medium)
                    .border(1.dp, AlphaPicsColors.BorderSoft, AlphaPicsShapes.Medium)
                    .clickable(role = Role.Button, onClick = onBack)
                    .testTag("settings_back_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Home",
                    tint = AlphaPicsColors.TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.size(AlphaPicsSpacing.Md))
            Column {
                Text(
                    text = "Settings",
                    color = AlphaPicsColors.TextPrimary,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "ALPHAPICS AI",
                    color = AlphaPicsColors.TextTertiary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        AlphaPicsBrandMark(size = 48.dp)
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Text(
        text = title.uppercase(),
        color = AlphaPicsColors.Cyan,
        style = MaterialTheme.typography.labelSmall
    )
    Spacer(Modifier.height(AlphaPicsSpacing.Sm))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AlphaPicsColors.SurfaceRaised.copy(alpha = 0.88f), AlphaPicsShapes.Card)
            .border(1.dp, AlphaPicsColors.BorderSoft, AlphaPicsShapes.Card)
            .padding(vertical = AlphaPicsSpacing.Xs),
        content = content
    )
}

@Composable
private fun SettingsRow(
    title: String,
    description: String,
    icon: AlphaPicsIcon,
    accent: AlphaPicsAccent,
    onClick: (() -> Unit)?,
    testTag: String? = null
) {
    val accentColor = when (accent) {
        AlphaPicsAccent.BLUE -> AlphaPicsColors.BrightBlue
        AlphaPicsAccent.VIOLET -> AlphaPicsColors.Violet
        AlphaPicsAccent.CYAN -> AlphaPicsColors.Cyan
        AlphaPicsAccent.PURPLE -> AlphaPicsColors.Purple
    }
    val baseModifier = Modifier
        .fillMaxWidth()
        .let { if (testTag == null) it else it.testTag(testTag) }
        .let { if (onClick == null) it else it.clickable(role = Role.Button, onClick = onClick) }
        .padding(horizontal = AlphaPicsSpacing.Lg, vertical = AlphaPicsSpacing.Md)

    Row(
        modifier = baseModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Md)
    ) {
        AlphaPicsGlyph(icon = icon, accent = accentColor, size = 44.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = AlphaPicsColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(AlphaPicsSpacing.Xs))
            Text(
                text = description,
                color = AlphaPicsColors.TextTertiary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (onClick != null) {
            Text(
                text = "›",
                color = AlphaPicsColors.TextTertiary,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AlphaPicsSpacing.Lg)
            .height(1.dp)
            .background(AlphaPicsColors.BorderSoft)
    )
}
