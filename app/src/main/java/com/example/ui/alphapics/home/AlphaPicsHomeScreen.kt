package com.example.ui.alphapics.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.example.ui.alphapics.components.AlphaPicsBackdrop
import com.example.ui.alphapics.components.AlphaPicsBrandHeader
import com.example.ui.alphapics.components.AlphaPicsEditPhotoCard
import com.example.ui.alphapics.components.AlphaPicsFeatureGrid
import com.example.ui.alphapics.components.AlphaPicsHeroCard
import com.example.ui.alphapics.components.AlphaPicsSectionHeading
import com.example.ui.alphapics.components.AlphaPicsToolGrid
import com.example.ui.alphapics.components.AlphaPicsToolSpec
import com.example.ui.alphapics.navigation.AlphaPicsAccent
import com.example.ui.alphapics.navigation.AlphaPicsFeature
import com.example.ui.alphapics.navigation.AlphaPicsFeatureCatalog
import com.example.ui.alphapics.navigation.AlphaPicsIcon
import com.example.ui.alphapics.theme.AlphaPicsSpacing
import com.example.ui.alphapics.theme.AlphaPicsTheme

@Composable
fun AlphaPicsHomeScreen(
    onOpenFeature: (AlphaPicsFeature) -> Unit,
    onChooseEnhancementPhoto: () -> Unit,
    onOpenEnhancementCamera: () -> Unit,
    onOpenCompressor: () -> Unit,
    onOpenBatch: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {}
) {
    val quickTools = remember(onOpenCompressor, onOpenBatch, onOpenHistory) {
        listOf(
            AlphaPicsToolSpec(
                id = "compress-image",
                title = "Compress",
                icon = AlphaPicsIcon.COMPRESS,
                accent = AlphaPicsAccent.BLUE,
                onClick = onOpenCompressor
            ),
            AlphaPicsToolSpec(
                id = "batch-compress",
                title = "Batch",
                icon = AlphaPicsIcon.BATCH,
                accent = AlphaPicsAccent.VIOLET,
                onClick = onOpenBatch
            ),
            AlphaPicsToolSpec(
                id = "compression-history",
                title = "History",
                icon = AlphaPicsIcon.HISTORY,
                accent = AlphaPicsAccent.CYAN,
                onClick = onOpenHistory
            )
        )
    }

    AlphaPicsTheme {
        AlphaPicsBackdrop(modifier = modifier) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = bottomBar
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .statusBarsPadding()
                        .padding(horizontal = AlphaPicsSpacing.ScreenHorizontal)
                        .testTag("alphapics_home")
                ) {
                    Spacer(Modifier.height(AlphaPicsSpacing.Md))
                    AlphaPicsBrandHeader(onOpenSettings = onOpenSettings)
                    Spacer(Modifier.height(AlphaPicsSpacing.Lg))
                    AlphaPicsHeroCard(
                        onChoosePhoto = onChooseEnhancementPhoto,
                        onOpenCamera = onOpenEnhancementCamera
                    )

                    Spacer(Modifier.height(AlphaPicsSpacing.Xl))
                    AlphaPicsSectionHeading(
                        eyebrow = "Enhancement shortcuts",
                        title = "Choose a focus"
                    )
                    Spacer(Modifier.height(AlphaPicsSpacing.Md))
                    AlphaPicsFeatureGrid(
                        features = AlphaPicsFeatureCatalog.HomeFeatures,
                        onFeatureClick = onOpenFeature
                    )

                    Spacer(Modifier.height(AlphaPicsSpacing.Xl))
                    AlphaPicsSectionHeading(
                        eyebrow = "Photo Editor",
                        title = "Refine the final look"
                    )
                    Spacer(Modifier.height(AlphaPicsSpacing.Md))
                    AlphaPicsEditPhotoCard(
                        onClick = { onOpenFeature(AlphaPicsFeatureCatalog.EditPhoto) }
                    )

                    Spacer(Modifier.height(AlphaPicsSpacing.Xl))
                    AlphaPicsSectionHeading(
                        eyebrow = "Quick Tools",
                        title = "Everyday essentials"
                    )
                    Spacer(Modifier.height(AlphaPicsSpacing.Md))
                    AlphaPicsToolGrid(tools = quickTools)
                    Spacer(Modifier.height(AlphaPicsSpacing.Xxl))
                }
            }
        }
    }
}
