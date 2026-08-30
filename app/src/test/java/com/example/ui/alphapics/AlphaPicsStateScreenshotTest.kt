package com.example.ui.alphapics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.example.ui.alphapics.components.AlphaPicsBackdrop
import com.example.ui.alphapics.components.AlphaPicsErrorState
import com.example.ui.alphapics.components.AlphaPicsLoadingState
import com.example.ui.alphapics.components.AlphaPicsPhotoEntry
import com.example.ui.alphapics.components.AlphaPicsWorkspaceTopBar
import com.example.ui.alphapics.theme.AlphaPicsSpacing
import com.example.ui.alphapics.theme.AlphaPicsTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class AlphaPicsStateScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun photo_empty_state() {
        setStateFixture(title = "Choose a photo") {
            AlphaPicsPhotoEntry(
                onChoosePhoto = {},
                onOpenCamera = {},
                modifier = Modifier.fillMaxWidth()
            )
        }

        composeTestRule.onNodeWithTag("workspace_choose_photo").assertIsDisplayed()
        capture("state_empty.png")
    }

    @Test
    fun photo_loading_state() {
        setStateFixture(title = "Preparing photo") {
            AlphaPicsLoadingState(
                title = "Opening photo",
                description = "Preparing the original photo for a safe workspace preview.",
                modifier = Modifier.fillMaxWidth()
            )
        }

        composeTestRule.onNodeWithTag("alphapics_loading_indicator").assertIsDisplayed()
        capture("state_loading.png")
    }

    @Test
    fun photo_error_state() {
        setStateFixture(title = "Photo unavailable") {
            AlphaPicsErrorState(
                title = "This photo couldn’t be opened",
                description = "Choose another photo or try the camera again. Nothing was changed.",
                actionLabel = "Choose another photo",
                onAction = {},
                modifier = Modifier.fillMaxWidth()
            )
        }

        composeTestRule.onNodeWithTag("alphapics_error_action").assertIsDisplayed()
        capture("state_error.png")
    }

    private fun setStateFixture(
        title: String,
        content: @androidx.compose.runtime.Composable () -> Unit
    ) {
        composeTestRule.setContent {
            AlphaPicsTheme {
                AlphaPicsBackdrop {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(horizontal = AlphaPicsSpacing.ScreenHorizontal)
                            .testTag("state_fixture")
                    ) {
                        Spacer(Modifier.height(AlphaPicsSpacing.Sm))
                        AlphaPicsWorkspaceTopBar(
                            title = title,
                            subtitle = "AlphaPics AI state",
                            onBack = {}
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 96.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            content()
                        }
                    }
                }
            }
        }
    }

    private fun capture(fileName: String) {
        composeTestRule.onNodeWithTag("state_fixture").captureRoboImage(
            filePath = "src/test/screenshots/$fileName"
        )
    }
}
