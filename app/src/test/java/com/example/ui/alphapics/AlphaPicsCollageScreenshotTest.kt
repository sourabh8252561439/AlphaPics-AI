package com.example.ui.alphapics

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.collage.CollageLayoutCatalog
import com.example.collage.CollageState
import com.example.editor.OverlayAdjustments
import com.example.editor.OverlayStickerKind
import com.example.editor.StickerOverlay
import com.example.editor.TextOverlay
import com.example.ui.alphapics.collage.AlphaPicsCollageScreen
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
class AlphaPicsCollageScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun collage_empty_state() {
        composeTestRule.setContent { AlphaPicsCollageScreen(onBack = {}) }

        composeTestRule.onNodeWithTag("collage_empty").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/collage_empty.png"
        )
    }

    @Test
    fun collage_four_photo_grid() {
        setCollageContent(photoCount = 4)

        composeTestRule.onNodeWithTag("collage_layout_four_grid").assertIsDisplayed()
        captureWorkspace("collage_grid.png")
    }

    @Test
    fun collage_canvas_controls() {
        setCollageContent(photoCount = 3)

        composeTestRule.onNodeWithTag("collage_panel_canvas").performClick()
        composeTestRule.onNodeWithTag("collage_background_gradient").performClick()
        composeTestRule.onNodeWithTag("collage_aspect_4_5").performClick()
        waitForPreview()
        captureWorkspace("collage_canvas.png")
    }

    @Test
    fun collage_freestyle_controls() {
        setCollageContent(photoCount = 3)

        composeTestRule.onNodeWithTag("collage_layout_freestyle").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("collage_freestyle_mode").performClick()
        waitForPreview()
        captureWorkspace("collage_freestyle.png")
    }

    @Test
    fun collage_text_and_sticker_controls() {
        val decoratedState = CollageState(
            layoutId = CollageLayoutCatalog.TwoSplit.id,
            overlays = OverlayAdjustments(
                texts = listOf(TextOverlay(text = "ALPHAPICS", fontSize = 10f)),
                stickers = listOf(
                    StickerOverlay(
                        kind = OverlayStickerKind.STAR,
                        x = 0.76f,
                        y = 0.30f,
                        scale = 14f,
                        colorArgb = 0xFFFFC34D
                    )
                )
            )
        ).ensurePhotoCount(2)
        setCollageContent(photoCount = 2, initialState = decoratedState)

        composeTestRule.onNodeWithTag("collage_panel_decorate").performClick()
        composeTestRule.onNodeWithTag("collage_decorate_sticker").performClick()
        waitForPreview()
        captureWorkspace("collage_decorate.png")
    }

    @Test
    fun collage_export_controls() {
        setCollageContent(
            photoCount = 2,
            initialState = CollageState(outputLongEdge = 4096).ensurePhotoCount(2)
        )

        composeTestRule.onNodeWithTag("collage_open_export").performClick()
        composeTestRule.onNodeWithTag("collage_export").assertIsDisplayed()
        captureWorkspace("collage_export.png")
    }

    @Test
    fun collage_undo_and_redo_restore_document_states() {
        setCollageContent(photoCount = 3)

        composeTestRule.onNodeWithTag("collage_panel_canvas").performClick()
        composeTestRule.onNodeWithTag("collage_aspect_4_5").performClick()
        composeTestRule.onNodeWithTag("collage_aspect_4_5").assertIsSelected()
        composeTestRule.onNodeWithTag("collage_undo").performClick()

        composeTestRule.onNodeWithTag("collage_aspect_1_1").assertIsSelected()
        composeTestRule.onNodeWithTag("collage_redo").performClick()
        composeTestRule.onNodeWithTag("collage_aspect_4_5").assertIsSelected()
    }

    private fun setCollageContent(photoCount: Int, initialState: CollageState? = null) {
        val photos = buildPhotos(photoCount)
        composeTestRule.setContent {
            if (initialState == null) {
                AlphaPicsCollageScreen(onBack = {}, initialImageModels = photos)
            } else {
                AlphaPicsCollageScreen(
                    onBack = {},
                    initialImageModels = photos,
                    initialState = initialState
                )
            }
        }
        waitForPreview()
    }

    private fun waitForPreview() {
        composeTestRule.waitUntil(timeoutMillis = 12_000) {
            composeTestRule.onAllNodesWithTag("collage_rendered_preview")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
    }

    private fun captureWorkspace(fileName: String) {
        composeTestRule.onNodeWithTag("alphapics_collage_workspace").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/$fileName")
    }

    private fun buildPhotos(count: Int): List<Bitmap> {
        val colors = listOf(
            Color.rgb(31, 91, 255),
            Color.rgb(135, 68, 255),
            Color.rgb(16, 190, 219),
            Color.rgb(244, 120, 58),
            Color.rgb(226, 58, 122),
            Color.rgb(54, 188, 108)
        )
        return List(count) { index ->
            Bitmap.createBitmap(260, 340, Bitmap.Config.ARGB_8888).apply {
                val canvas = Canvas(this)
                canvas.drawColor(colors[index])
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    alpha = 190
                }
                canvas.drawCircle(78f + index * 9f, 92f, 54f, paint)
                paint.alpha = 120
                canvas.drawRect(30f, 205f, 230f, 290f, paint)
            }
        }
    }
}
