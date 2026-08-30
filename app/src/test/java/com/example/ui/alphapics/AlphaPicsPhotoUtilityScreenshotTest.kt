package com.example.ui.alphapics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import com.example.R
import com.example.ui.alphapics.photo.AlphaPicsPhotoUtilityScreen
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class AlphaPicsPhotoUtilityScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val temporaryFiles = mutableListOf<File>()

    @After
    fun cleanUp() {
        temporaryFiles.forEach(File::delete)
    }

    @Test
    fun photo_utility_empty_state() {
        composeTestRule.setContent {
            AlphaPicsPhotoUtilityScreen(
                imageModel = null,
                initialTabId = "resize",
                onBack = {},
                onChoosePhoto = {},
                onOpenCamera = {}
            )
        }

        composeTestRule.onNodeWithTag("photo_utility_empty").assertIsDisplayed()
        capture("photo_utility_empty.png")
    }

    @Test
    fun photo_utility_resize_workspace() {
        setPhotoContent("resize")

        composeTestRule.onNodeWithTag("resize_mode_dimensions").assertIsSelected()
        composeTestRule.onNodeWithTag("resize_aspect_lock").assertIsSelected()
        capture("photo_utility_resize.png")
    }

    @Test
    fun photo_utility_percentage_workspace() {
        setPhotoContent("resize")

        composeTestRule.onNodeWithTag("resize_mode_percentage").performClick()
        composeTestRule.onNodeWithTag("resize_mode_percentage").assertIsSelected()
        composeTestRule.waitForIdle()
        capture("photo_utility_percentage.png")
    }

    @Test
    fun photo_utility_convert_workspace() {
        setPhotoContent("convert")

        composeTestRule.onNodeWithTag("metadata_preserve").performClick()
        composeTestRule.onNodeWithTag("metadata_preserve").assertIsSelected()
        capture("photo_utility_convert.png")
    }

    @Test
    fun photo_utility_info_workspace() {
        setPhotoContent("info")

        composeTestRule.onNodeWithTag("photo_info_rows").assertIsDisplayed()
        capture("photo_utility_info.png")
    }

    @Test
    fun photo_utility_undo_and_redo_restore_settings() {
        setPhotoContent("resize")

        composeTestRule.onNodeWithTag("resize_mode_percentage").performClick()
        composeTestRule.onNodeWithTag("resize_mode_percentage").assertIsSelected()
        composeTestRule.onNodeWithTag("photo_utility_undo").performClick()
        composeTestRule.onNodeWithTag("resize_mode_dimensions").assertIsSelected()
        composeTestRule.onNodeWithTag("photo_utility_redo").performClick()
        composeTestRule.onNodeWithTag("resize_mode_percentage").assertIsSelected()
    }

    private fun setPhotoContent(initialTabId: String) {
        val uri = Uri.fromFile(createPhotoFile("photo_utility_$initialTabId.jpg"))
        composeTestRule.setContent {
            AlphaPicsPhotoUtilityScreen(
                imageModel = uri,
                initialTabId = initialTabId,
                onBack = {},
                onChoosePhoto = {},
                onOpenCamera = {}
            )
        }
        composeTestRule.waitUntil(timeoutMillis = 12_000) {
            composeTestRule.onAllNodesWithTag("photo_utility_preview")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
    }

    private fun createPhotoFile(name: String): File {
        val file = File(context.cacheDir, name).also(temporaryFiles::add)
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.alphapics_brand_logo)
        FileOutputStream(file).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 96, output))
        }
        bitmap.recycle()
        ExifInterface(file.absolutePath).apply {
            setAttribute(ExifInterface.TAG_MAKE, "AlphaPics Studio")
            setAttribute(ExifInterface.TAG_MODEL, "Local Device")
            setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "2026:08:29 14:30:00")
            setAttribute(ExifInterface.TAG_F_NUMBER, "2.0")
            setAttribute(ExifInterface.TAG_FOCAL_LENGTH, "35/1")
            saveAttributes()
        }
        return file
    }

    private fun capture(fileName: String) {
        composeTestRule.onNodeWithTag("alphapics_photo_utility").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/$fileName")
    }
}
