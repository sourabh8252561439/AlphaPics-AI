package com.example.ui.alphapics

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.R
import com.example.editor.CropGrid
import com.example.editor.EditorState
import com.example.editor.EditorPresetStore
import com.example.editor.DrawOverlayStroke
import com.example.editor.LocalRetouchMode
import com.example.editor.OverlayAdjustments
import com.example.editor.OverlayShapeKind
import com.example.editor.OverlayStickerKind
import com.example.editor.OverlayToolMode
import com.example.editor.RetouchAdjustments
import com.example.editor.RetouchPoint
import com.example.editor.RetouchStroke
import com.example.editor.ShapeOverlay
import com.example.editor.StickerOverlay
import com.example.editor.TextOverlay
import com.example.editor.TransformAdjustments
import com.example.editor.WatermarkAdjustment
import com.example.ui.alphapics.editor.AlphaPicsEditorScreen
import com.example.ui.alphapics.enhance.AlphaPicsEnhancementWorkspace
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import androidx.test.core.app.ApplicationProvider

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class AlphaPicsWorkspaceScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun enhancement_workspace_selected_photo_shell() {
        composeTestRule.setContent {
            AlphaPicsEnhancementWorkspace(
                imageModel = R.drawable.alphapics_brand_logo,
                initialModeId = "auto",
                onBack = {},
                onChoosePhoto = {},
                onOpenCamera = {}
            )
        }

        composeTestRule.onNodeWithTag("enhancement_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("enhancement_mode_rail").assertIsDisplayed()
        composeTestRule.onNodeWithTag("enhancement_unavailable_state").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/enhancement_workspace.png"
        )
    }

    @Test
    fun editor_adjust_state() {
        captureEditorState("adjust", "editor_adjust.png")
    }

    @Test
    fun editor_hsl_state() {
        captureEditorAdjustTab("hsl", "editor_hsl.png")
    }

    @Test
    fun editor_curves_state() {
        captureEditorAdjustTab("curves", "editor_curves.png")
    }

    @Test
    fun editor_color_mix_state() {
        captureEditorAdjustTab("mix", "editor_color_mix.png")
    }

    @Test
    fun editor_split_tone_state() {
        captureEditorAdjustTab("split", "editor_split_tone.png")
    }

    @Test
    fun editor_color_grading_state() {
        captureEditorAdjustTab("grade", "editor_color_grading.png")
    }

    @Test
    fun editor_histogram_state() {
        captureEditorAdjustTab("histogram", "editor_histogram.png")
    }

    @Test
    fun editor_filters_state() {
        EditorPresetStore.clear(ApplicationProvider.getApplicationContext())
        captureEditorState("filters", "editor_filters.png")
    }

    @Test
    fun editor_preset_save_state() {
        EditorPresetStore.clear(ApplicationProvider.getApplicationContext())
        composeTestRule.setContent {
            AlphaPicsEditorScreen(
                imageModel = R.drawable.alphapics_brand_logo,
                initialToolId = "filters",
                onBack = {},
                onChoosePhoto = {},
                onOpenCamera = {}
            )
        }

        composeTestRule.onNodeWithTag("editor_preset_save_toggle").performClick()
        composeTestRule.onNodeWithTag("editor_preset_name").assertIsDisplayed()
        composeTestRule.onNodeWithTag("alphapics_editor_workspace").captureRoboImage(
            filePath = "src/test/screenshots/editor_preset_save.png"
        )
    }

    @Test
    fun editor_history_state() {
        composeTestRule.setContent {
            AlphaPicsEditorScreen(
                imageModel = R.drawable.alphapics_brand_logo,
                initialToolId = "filters",
                onBack = {},
                onChoosePhoto = {},
                onOpenCamera = {}
            )
        }

        composeTestRule.onNodeWithTag("editor_preset_warm").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Apply").performClick()
        composeTestRule.onNodeWithTag("editor_tool_history").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("editor_history_entry_1").performScrollTo()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("alphapics_editor_workspace").captureRoboImage(
            filePath = "src/test/screenshots/editor_history.png"
        )
    }

    @Test
    fun editor_crop_state() {
        captureEditorState("crop", "editor_crop.png")
    }

    @Test
    fun editor_crop_grid_state() {
        composeTestRule.setContent {
            AlphaPicsEditorScreen(
                imageModel = R.drawable.alphapics_brand_logo,
                initialToolId = "crop",
                onBack = {},
                onChoosePhoto = {},
                onOpenCamera = {},
                initialEditorState = EditorState(
                    transform = TransformAdjustments(
                        aspectId = "4:5",
                        grid = CropGrid.THIRDS
                    )
                )
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("alphapics_editor_workspace").captureRoboImage(
            filePath = "src/test/screenshots/editor_crop_grid.png"
        )
    }

    @Test
    fun editor_geometry_state() {
        captureEditorTransformTab("geometry", "editor_geometry.png")
    }

    @Test
    fun editor_lens_state() {
        captureEditorTransformTab("lens", "editor_lens.png")
    }

    @Test
    fun editor_retouch_state() {
        captureEditorState("retouch", "editor_retouch.png")
    }

    @Test
    fun editor_retouch_clone_state() {
        captureEditorWithState(
            toolId = "retouch",
            state = EditorState(
                retouch = RetouchAdjustments(activeMode = LocalRetouchMode.CLONE)
            ),
            fileName = "editor_retouch_clone.png"
        )
    }

    @Test
    fun editor_retouch_mask_state() {
        captureEditorWithState(
            toolId = "retouch",
            state = EditorState(
                retouch = RetouchAdjustments(
                    strokes = listOf(
                        RetouchStroke(
                            mode = LocalRetouchMode.BRIGHTNESS,
                            points = listOf(
                                RetouchPoint(0.32f, 0.42f),
                                RetouchPoint(0.50f, 0.50f),
                                RetouchPoint(0.68f, 0.57f)
                            ),
                            size = 12f,
                            feather = 50f,
                            strength = 70f
                        )
                    ),
                    showMask = true
                )
            ),
            fileName = "editor_retouch_mask.png"
        )
    }

    @Test
    fun editor_remove_state() {
        captureEditorState("remove", "editor_remove.png")
    }

    @Test
    fun editor_background_state() {
        captureEditorState("background", "editor_background.png")
    }

    @Test
    fun editor_detail_state() {
        captureEditorState("detail", "editor_detail.png")
    }

    @Test
    fun editor_effects_state() {
        composeTestRule.setContent {
            AlphaPicsEditorScreen(
                imageModel = R.drawable.alphapics_brand_logo,
                initialToolId = "detail",
                onBack = {},
                onChoosePhoto = {},
                onOpenCamera = {}
            )
        }

        composeTestRule.onNodeWithTag("editor_detail_tab_effects").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("editor_context_detail").assertIsDisplayed()
        composeTestRule.onNodeWithTag("alphapics_editor_workspace").captureRoboImage(
            filePath = "src/test/screenshots/editor_effects.png"
        )
    }

    @Test
    fun editor_text_state() {
        captureEditorOverlayState(
            toolId = "text",
            state = EditorState(
                overlays = OverlayAdjustments(
                    texts = listOf(TextOverlay(text = "ALPHAPICS", y = 0.36f, fontSize = 12f)),
                    textDraft = "ALPHAPICS",
                    textTemplate = TextOverlay(text = "ALPHAPICS", y = 0.36f, fontSize = 12f)
                )
            ),
            fileName = "editor_text.png"
        )
    }

    @Test
    fun editor_draw_state() {
        captureEditorOverlayState(
            toolId = "text",
            state = EditorState(
                overlays = OverlayAdjustments(
                    activeTool = OverlayToolMode.DRAW,
                    drawColorArgb = 0xFF26D9FF,
                    drawing = listOf(
                        DrawOverlayStroke(
                            points = listOf(RetouchPoint(0.28f, 0.66f), RetouchPoint(0.50f, 0.54f), RetouchPoint(0.72f, 0.66f)),
                            colorArgb = 0xFF26D9FF,
                            size = 3f
                        )
                    )
                )
            ),
            fileName = "editor_draw.png"
        )
    }

    @Test
    fun editor_shape_state() {
        val shape = ShapeOverlay(
            kind = OverlayShapeKind.ROUNDED_RECTANGLE,
            strokeArgb = 0xFF26D9FF,
            fillArgb = 0xFF2F7BFF,
            opacity = 72f
        )
        captureEditorOverlayState(
            toolId = "text",
            state = EditorState(
                overlays = OverlayAdjustments(
                    activeTool = OverlayToolMode.SHAPE,
                    shapes = listOf(shape),
                    shapeTemplate = shape
                )
            ),
            fileName = "editor_shape.png"
        )
    }

    @Test
    fun editor_sticker_state() {
        val sticker = StickerOverlay(
            kind = OverlayStickerKind.SPARKLE,
            colorArgb = 0xFF9B6CFF,
            scale = 24f,
            rotation = 18f
        )
        captureEditorOverlayState(
            toolId = "text",
            state = EditorState(
                overlays = OverlayAdjustments(
                    activeTool = OverlayToolMode.STICKER,
                    stickers = listOf(sticker),
                    stickerTemplate = sticker
                )
            ),
            fileName = "editor_sticker.png"
        )
    }

    @Test
    fun editor_frame_state() {
        captureEditorOverlayState(
            toolId = "text",
            state = EditorState(
                overlays = OverlayAdjustments(
                    activeTool = OverlayToolMode.FRAME,
                    frame = com.example.editor.FrameAdjustments(
                        borderEnabled = true,
                        borderColorArgb = 0xFFFFFFFF,
                        borderThickness = 5f,
                        cornerRadius = 8f,
                        presetId = "rounded"
                    )
                )
            ),
            fileName = "editor_frame.png"
        )
    }

    @Test
    fun editor_watermark_state() {
        captureEditorOverlayState(
            toolId = "text",
            state = EditorState(
                overlays = OverlayAdjustments(
                    activeTool = OverlayToolMode.WATERMARK,
                    watermark = WatermarkAdjustment(enabled = true, text = "AlphaPics AI")
                )
            ),
            fileName = "editor_watermark.png"
        )
    }

    private fun captureEditorState(toolId: String, fileName: String) {
        composeTestRule.setContent {
            AlphaPicsEditorScreen(
                imageModel = R.drawable.alphapics_brand_logo,
                initialToolId = toolId,
                onBack = {},
                onChoosePhoto = {},
                onOpenCamera = {}
            )
        }

        composeTestRule.onNodeWithTag("editor_context_$toolId").assertIsDisplayed()
        composeTestRule.onNodeWithTag("editor_tool_$toolId").assertIsDisplayed()
        composeTestRule.onNodeWithTag("alphapics_editor_workspace").captureRoboImage(
            filePath = "src/test/screenshots/$fileName"
        )
    }

    private fun captureEditorAdjustTab(tabId: String, fileName: String) {
        composeTestRule.setContent {
            AlphaPicsEditorScreen(
                imageModel = R.drawable.alphapics_brand_logo,
                initialToolId = "adjust",
                onBack = {},
                onChoosePhoto = {},
                onOpenCamera = {}
            )
        }

        val tabNode = composeTestRule.onNodeWithTag("editor_adjust_tab_$tabId")
        if (tabId in setOf("mix", "split", "grade", "histogram")) tabNode.performScrollTo()
        tabNode.performClick()
        composeTestRule.waitForIdle()
        if (tabId == "histogram") {
            composeTestRule.waitUntil(timeoutMillis = 10_000) {
                composeTestRule.onAllNodesWithTag("editor_histogram_chart")
                    .fetchSemanticsNodes().isNotEmpty()
            }
        }
        composeTestRule.onNodeWithTag("editor_context_adjust").assertIsDisplayed()
        composeTestRule.onNodeWithTag("alphapics_editor_workspace").captureRoboImage(
            filePath = "src/test/screenshots/$fileName"
        )
    }

    private fun captureEditorTransformTab(tabId: String, fileName: String) {
        composeTestRule.setContent {
            AlphaPicsEditorScreen(
                imageModel = R.drawable.alphapics_brand_logo,
                initialToolId = "crop",
                onBack = {},
                onChoosePhoto = {},
                onOpenCamera = {}
            )
        }

        composeTestRule.onNodeWithTag("editor_transform_tab_$tabId").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("editor_context_crop").assertIsDisplayed()
        composeTestRule.onNodeWithTag("alphapics_editor_workspace").captureRoboImage(
            filePath = "src/test/screenshots/$fileName"
        )
    }

    private fun captureEditorWithState(toolId: String, state: EditorState, fileName: String) {
        composeTestRule.setContent {
            AlphaPicsEditorScreen(
                imageModel = R.drawable.alphapics_brand_logo,
                initialToolId = toolId,
                onBack = {},
                onChoosePhoto = {},
                onOpenCamera = {},
                initialEditorState = state
            )
        }

        composeTestRule.waitForIdle()
        if (!state.overlays.isNeutral) {
            composeTestRule.waitUntil(timeoutMillis = 10_000) {
                composeTestRule.onAllNodesWithTag("editor_processed_preview")
                    .fetchSemanticsNodes().isNotEmpty()
            }
        }
        composeTestRule.onNodeWithTag("editor_context_$toolId").assertIsDisplayed()
        composeTestRule.onNodeWithTag("alphapics_editor_workspace").captureRoboImage(
            filePath = "src/test/screenshots/$fileName"
        )
    }

    private fun captureEditorOverlayState(toolId: String, state: EditorState, fileName: String) {
        composeTestRule.setContent {
            AlphaPicsEditorScreen(
                imageModel = R.drawable.alphapics_brand_logo,
                initialToolId = toolId,
                onBack = {},
                onChoosePhoto = {},
                onOpenCamera = {},
                initialEditorState = state
            )
        }

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithTag("editor_processed_preview")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("editor_context_$toolId").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/$fileName"
        )
    }
}
