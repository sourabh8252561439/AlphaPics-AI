package com.example.ui.alphapics.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.alphapics.components.AlphaPicsBackdrop
import com.example.ui.alphapics.components.AlphaPicsBadge
import com.example.ui.alphapics.components.AlphaPicsBrandMark
import com.example.ui.alphapics.components.AlphaPicsGlyph
import com.example.ui.alphapics.components.AlphaPicsPrimaryAction
import com.example.ui.alphapics.components.accentColor
import com.example.ui.alphapics.navigation.AlphaPicsFeature
import com.example.ui.alphapics.theme.AlphaPicsColors
import com.example.ui.alphapics.theme.AlphaPicsShapes
import com.example.ui.alphapics.theme.AlphaPicsSpacing
import com.example.ui.alphapics.theme.AlphaPicsTheme

@Composable
fun AlphaPicsPlaceholderScreen(
    feature: AlphaPicsFeature,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                        .testTag("alphapics_placeholder_${feature.id}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(AlphaPicsSpacing.Md))
                    PlaceholderTopBar(onBack = onBack)
                    Spacer(Modifier.height(AlphaPicsSpacing.Section))
                    AlphaPicsGlyph(
                        icon = feature.icon,
                        accent = feature.accentColor(),
                        size = 76.dp
                    )
                    Spacer(Modifier.height(AlphaPicsSpacing.Xl))
                    AlphaPicsBadge(text = "Coming soon", accent = feature.accentColor())
                    Spacer(Modifier.height(AlphaPicsSpacing.Lg))
                    Text(
                        text = feature.title,
                        color = AlphaPicsColors.TextPrimary,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(AlphaPicsSpacing.Sm))
                    Text(
                        text = feature.description,
                        color = AlphaPicsColors.TextSecondary,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(AlphaPicsSpacing.Xxl))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = AlphaPicsColors.SurfaceRaised.copy(alpha = 0.88f),
                                shape = AlphaPicsShapes.Card
                            )
                            .border(1.dp, AlphaPicsColors.BorderSoft, AlphaPicsShapes.Card)
                            .padding(AlphaPicsSpacing.Xl),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "A better photo experience is on the way.",
                            color = AlphaPicsColors.TextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(AlphaPicsSpacing.Sm))
                        Text(
                            text = "This feature is not available yet. We'll add it when it is ready for your photos.",
                            color = AlphaPicsColors.TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(Modifier.height(AlphaPicsSpacing.Xxl))
                    AlphaPicsPrimaryAction(
                        label = "Back to Home",
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "placeholder_back_to_home"
                    )
                    Spacer(Modifier.height(AlphaPicsSpacing.Xxl))
                }
            }
        }
    }
}

@Composable
private fun PlaceholderTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(AlphaPicsColors.SurfaceRaised, AlphaPicsShapes.Medium)
                .border(1.dp, AlphaPicsColors.BorderSoft, AlphaPicsShapes.Medium)
                .clickable(role = Role.Button, onClick = onBack)
                .testTag("placeholder_back_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Home",
                tint = AlphaPicsColors.TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
        AlphaPicsBrandMark(size = 48.dp)
    }
}
