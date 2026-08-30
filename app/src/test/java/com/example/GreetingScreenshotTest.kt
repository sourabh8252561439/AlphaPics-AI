package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun alphaPics_photo_workflow_screenshots() {
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = true) {
        ImageCompressorApp(isDarkMode = true, onThemeChange = {})
      }
    }

    composeTestRule.onNodeWithTag("alphapics_home").assertIsDisplayed()
    composeTestRule.onNodeWithTag("alphapics_hero").assertIsDisplayed()
    composeTestRule.onNodeWithText("Bring every photo back to its best").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_settings").assertIsDisplayed()
    composeTestRule.onNodeWithTag("alphapics_tool_compress-image").fetchSemanticsNode()
    composeTestRule.onNodeWithTag("alphapics_tool_batch-compress").fetchSemanticsNode()
    composeTestRule.onNodeWithTag("alphapics_tool_compression-history").fetchSemanticsNode()
    assertEquals(0, composeTestRule.onAllNodesWithText("AI Image").fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithText("Text-to-Video").fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithText("Create").fetchSemanticsNodes().size)

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")

    composeTestRule.onNodeWithTag("alphapics_feature_ai-enhance").performClick()
    composeTestRule.onNodeWithTag("alphapics_enhancement_workspace").assertIsDisplayed()
    composeTestRule.onNodeWithTag("enhancement_canvas").assertIsDisplayed()
    composeTestRule.onNodeWithTag("enhancement_mode_rail").assertIsDisplayed()
    composeTestRule.onNodeWithText("COMING SOON").assertIsDisplayed()
    assertEquals(0, composeTestRule.onAllNodesWithText("PHASE 1").fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithText("Foundation ready").fetchSemanticsNodes().size)
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/enhance.png")
    composeTestRule.onNodeWithTag("workspace_back_button").performClick()

    composeTestRule.onNodeWithTag("alphapics_tool_compression-history").performScrollTo()
    composeTestRule.onNodeWithTag("edit_photo_entry").assertIsDisplayed()
    composeTestRule.onNodeWithText("Everyday essentials").assertIsDisplayed()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/home_tools.png")

    composeTestRule.onNodeWithTag("home_settings").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("alphapics_settings").assertIsDisplayed()
    composeTestRule.onNodeWithText("APPEARANCE").assertIsDisplayed()
    composeTestRule.onNodeWithText("SUPPORT").assertIsDisplayed()
    composeTestRule.onNodeWithText("LEGAL").assertIsDisplayed()
    composeTestRule.onNodeWithText("ABOUT").assertIsDisplayed()
    assertEquals(
      0,
      composeTestRule.onAllNodesWithText("Compression History").fetchSemanticsNodes().size
    )
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/settings.png")

    composeTestRule.onNodeWithTag("settings_back_button").performClick()
    composeTestRule.onNodeWithTag("alphapics_tool_compress-image")
      .performScrollTo()
      .performClick()
    composeTestRule.onNodeWithText("Compress Photo").assertIsDisplayed()
    composeTestRule.onNodeWithText("Choose how to compress").assertIsDisplayed()
    composeTestRule.onNodeWithTag("compressor_back_button").assertIsDisplayed()
    assertEquals(0, composeTestRule.onAllNodesWithTag("menu_button").fetchSemanticsNodes().size)
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/compressor.png")

    composeTestRule.onNodeWithTag("compressor_back_button").performClick()
    composeTestRule.onNodeWithTag("alphapics_tool_batch-compress")
      .performScrollTo()
      .performClick()
    composeTestRule.onNodeWithText("Batch Compress").assertIsDisplayed()
    composeTestRule.onNodeWithText("Build a photo batch").assertIsDisplayed()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/batch.png")

    composeTestRule.onNodeWithContentDescription("Back to Home").performClick()
    composeTestRule.onNodeWithTag("alphapics_tool_compression-history")
      .performScrollTo()
      .performClick()
    composeTestRule.onNodeWithText("History").assertIsDisplayed()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
        .onAllNodesWithText("Your recent results will appear here")
        .fetchSemanticsNodes()
        .isNotEmpty()
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/history.png")
  }
}
