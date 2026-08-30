package com.example

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.geometry.center
import androidx.compose.foundation.layout.aspectRatio
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import android.app.Activity
import com.example.ads.BannerAdView
import com.example.ads.DailyTokenManager
import com.example.ads.InterstitialAdManager
import com.example.ads.RewardedAdManager
import com.example.compression.CompressionMode
import com.example.compression.CompressionSettingsState
import com.example.compression.ImageCompressionOutcome
import com.example.compression.ImageCompressionProcessor
import com.example.compression.QualityControl
import com.example.compression.SettingsValidation
import com.example.compression.TargetSizeControl
import com.example.compression.TargetSizeInputState
import com.example.history.CompressionHistoryDatabase
import com.example.history.CompressionHistoryEntity
import androidx.compose.animation.Crossfade
import com.example.ui.alphapics.home.AlphaPicsHomeScreen
import com.example.ui.alphapics.editor.AlphaPicsEditorScreen
import com.example.ui.alphapics.collage.AlphaPicsCollageScreen
import com.example.ui.alphapics.photo.AlphaPicsPhotoUtilityScreen
import com.example.ui.alphapics.enhance.AlphaPicsEnhancementWorkspace
import com.example.ui.alphapics.motion.rememberAlphaPicsFadeSpec
import com.example.ui.alphapics.navigation.AlphaPicsDestination
import com.example.ui.alphapics.navigation.AlphaPicsFeature
import com.example.ui.alphapics.navigation.AlphaPicsFeatureCatalog
import com.example.ui.alphapics.navigation.rememberAlphaPicsNavState
import com.example.ui.alphapics.placeholder.AlphaPicsPlaceholderScreen
import com.example.ui.alphapics.settings.AlphaPicsSettingsScreen
import com.example.ui.alphapics.theme.AlphaPicsColors
import androidx.core.view.WindowCompat

fun Modifier.dashedBorder(
    strokeWidth: androidx.compose.ui.unit.Dp,
    color: Color,
    cornerRadius: androidx.compose.ui.unit.Dp,
    dashLength: androidx.compose.ui.unit.Dp = 8.dp,
    gapLength: androidx.compose.ui.unit.Dp = 6.dp
) = this.drawBehind {
    val strokeWidthPx = strokeWidth.toPx()
    val cornerRadiusPx = cornerRadius.toPx()
    val dashLengthPx = dashLength.toPx()
    val gapLengthPx = gapLength.toPx()
    
    val pathEffect = PathEffect.dashPathEffect(
        floatArrayOf(dashLengthPx, gapLengthPx),
        0f
    )
    
    val rect = Rect(
        offset = Offset(strokeWidthPx / 2, strokeWidthPx / 2),
        size = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
    )
    
    val roundRect = RoundRect(rect, CornerRadius(cornerRadiusPx, cornerRadiusPx))
    val path = Path().apply {
        addRoundRect(roundRect)
    }
    
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidthPx,
            pathEffect = pathEffect
        )
    )
}

private val compressionSettingsSaver = listSaver<CompressionSettingsState, String>(
    save = { state ->
        listOf(
            state.mode.name,
            state.qualitySliderValue.toString(),
            state.targetSize.text,
            state.targetSize.sliderPosition.toString(),
            state.targetSize.committedKilobytes?.toString().orEmpty(),
            state.targetSize.validationMessage.orEmpty()
        )
    },
    restore = { values ->
        CompressionSettingsState(
            mode = CompressionMode.fromStored(values.getOrNull(0)),
            qualitySliderValue = values.getOrNull(1)?.toFloatOrNull() ?: 80f,
            targetSize = TargetSizeInputState(
                text = values.getOrNull(2).orEmpty(),
                sliderPosition = values.getOrNull(3)?.toFloatOrNull()
                    ?: TargetSizeInputState().sliderPosition,
                committedKilobytes = values.getOrNull(4)?.toIntOrNull(),
                validationMessage = values.getOrNull(5)?.ifBlank { null }
            )
        )
    }
)

// Custom ImageVectors for Camera and Palette to avoid large dependency overhead
val CameraIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Camera",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 18f)
            quadToRelative(1.25f, 0f, 2.125f, -0.875f)
            reflectiveQuadTo(15f, 15f)
            quadToRelative(0f, -1.25f, -0.875f, -2.125f)
            reflectiveQuadTo(12f, 12f)
            quadToRelative(-1.25f, 0f, -2.125f, 0.875f)
            reflectiveQuadTo(9f, 15f)
            quadToRelative(0f, 1.25f, 0.875f, 2.125f)
            reflectiveQuadTo(12f, 18f)
            close()
            moveTo(4f, 21f)
            quadToRelative(-0.825f, 0f, -1.413f, -0.587f)
            reflectiveQuadTo(2f, 19f)
            verticalLineTo(8f)
            quadToRelative(0f, -0.825f, 0.587f, -1.413f)
            reflectiveQuadTo(4f, 6f)
            horizontalLineTo(7.15f)
            lineTo(9f, 4f)
            horizontalLineTo(15f)
            lineTo(16.85f, 6f)
            horizontalLineTo(20f)
            quadToRelative(0.825f, 0f, 1.413f, 0.588f)
            reflectiveQuadTo(22f, 8f)
            verticalLineTo(19f)
            quadToRelative(0f, 0.825f, -0.587f, 1.413f)
            reflectiveQuadTo(20f, 21f)
            horizontalLineTo(4f)
            close()
        }
    }.build()

val PaletteIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Palette",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 22f)
            quadToRelative(-4.15f, 0f, -7.075f, -2.925f)
            reflectiveQuadTo(2f, 12f)
            quadToRelative(0f, -4.15f, 2.925f, -7.075f)
            reflectiveQuadTo(12f, 2f)
            quadToRelative(4.15f, 0f, 7.075f, 2.925f)
            reflectiveQuadTo(22f, 12f)
            quadToRelative(0f, 0.825f, -0.587f, 1.413f)
            reflectiveQuadTo(20f, 14f)
            horizontalLineTo(18f)
            quadToRelative(-0.825f, 0f, -1.413f, 0.588f)
            reflectiveQuadTo(16f, 16f)
            quadToRelative(0f, 0.825f, 0.588f, 1.413f)
            reflectiveQuadTo(17f, 18.5f)
            quadToRelative(0f, 1.45f, -1.025f, 2.475f)
            reflectiveQuadTo(13.5f, 22f)
            horizontalLineTo(12f)
            close()
            moveTo(6.5f, 11f)
            quadToRelative(0.625f, 0f, 1.063f, -0.438f)
            reflectiveQuadTo(8f, 9.5f)
            quadToRelative(0f, -0.625f, -0.438f, -1.063f)
            reflectiveQuadTo(6.5f, 8f)
            quadToRelative(-0.625f, 0f, -1.063f, 0.438f)
            reflectiveQuadTo(5f, 9.5f)
            quadToRelative(0f, 0.625f, 0.438f, 1.063f)
            reflectiveQuadTo(6.5f, 11f)
            close()
            moveTo(9.5f, 7f)
            quadToRelative(0.625f, 0f, 1.063f, -0.438f)
            reflectiveQuadTo(11f, 5.5f)
            quadToRelative(0f, -0.625f, -0.438f, -1.063f)
            reflectiveQuadTo(9.5f, 4f)
            quadToRelative(-0.625f, 0f, -1.063f, 0.438f)
            reflectiveQuadTo(8f, 5.5f)
            quadToRelative(0f, 0.625f, 0.438f, 1.063f)
            reflectiveQuadTo(9.5f, 7f)
            close()
            moveTo(14.5f, 7f)
            quadToRelative(0.625f, 0f, 1.063f, -0.438f)
            reflectiveQuadTo(16f, 5.5f)
            quadToRelative(0f, -0.625f, -0.438f, -1.063f)
            reflectiveQuadTo(14.5f, 4f)
            quadToRelative(-0.625f, 0f, -1.063f, 0.438f)
            reflectiveQuadTo(13f, 5.5f)
            quadToRelative(0f, 0.625f, 0.438f, 1.063f)
            reflectiveQuadTo(14.5f, 7f)
            close()
            moveTo(17.5f, 11f)
            quadToRelative(0.625f, 0f, 1.063f, -0.438f)
            reflectiveQuadTo(19f, 9.5f)
            quadToRelative(0f, -0.625f, -0.438f, -1.063f)
            reflectiveQuadTo(17.5f, 8f)
            quadToRelative(-0.625f, 0f, -1.063f, 0.438f)
            reflectiveQuadTo(15f, 9.5f)
            quadToRelative(0f, 0.625f, 0.438f, 1.063f)
            reflectiveQuadTo(17.5f, 11f)
            close()
        }
    }.build()

val HeadsetIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Headset",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 2f)
            quadToRelative(-3.75f, 0f, -6.375f, 2.625f)
            reflectiveQuadTo(3f, 11f)
            verticalLineToRelative(7f)
            quadToRelative(0f, 1.25f, 0.875f, 2.125f)
            reflectiveQuadTo(6f, 21f)
            horizontalLineToRelative(3f)
            verticalLineToRelative(-7f)
            horizontalLineTo(5f)
            verticalLineToRelative(-3f)
            quadToRelative(0f, -2.925f, 2.038f, -4.963f)
            reflectiveQuadTo(12f, 4f)
            quadToRelative(2.925f, 0f, 4.963f, 2.038f)
            reflectiveQuadTo(19f, 11f)
            verticalLineToRelative(3f)
            horizontalLineToRelative(-4f)
            verticalLineToRelative(7f)
            horizontalLineToRelative(3f)
            quadToRelative(1.25f, 0f, 2.125f, -0.875f)
            reflectiveQuadTo(21f, 18f)
            verticalLineToRelative(-7f)
            quadToRelative(0f, -3.75f, -2.625f, -6.375f)
            reflectiveQuadTo(12f, 2f)
            close()
        }
    }.build()

val ThumbUpIcon: ImageVector
    get() = ImageVector.Builder(
        name = "ThumbUp",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(2f, 21f)
            verticalLineToRelative(-9f)
            horizontalLineToRelative(3f)
            verticalLineToRelative(9f)
            close()
            moveTo(20f, 10f)
            horizontalLineToRelative(-5.5f)
            lineToRelative(0.9f, -4.5f)
            lineToRelative(0.05f, -0.4f)
            quadToRelative(0f, -0.5f, -0.3f, -0.8f)
            lineToRelative(-0.85f, -0.85f)
            lineToRelative(-4.5f, 4.5f)
            quadToRelative(-0.3f, 0.3f, -0.4f, 0.7f)
            reflectiveQuadTo(9.3f, 9.3f)
            verticalLineTo(19f)
            horizontalLineToRelative(8.1f)
            quadToRelative(0.75f, 0f, 1.35f, -0.45f)
            reflectiveQuadTo(19.65f, 17.35f)
            lineToRelative(1.25f, -5.85f)
            quadToRelative(0.1f, -0.3f, 0.1f, -0.6f)
            quadToRelative(0f, -0.825f, -0.587f, -1.413f)
            reflectiveQuadTo(19f, 9f)
            close()
        }
    }.build()

val CustomShareIcon: ImageVector
    get() = ImageVector.Builder(
        name = "ShareIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(18f, 16.08f)
            quadToRelative(-0.76f, 0f, -1.35f, 0.34f)
            lineToRelative(-7.05f, -4.11f)
            quadToRelative(0.08f, -0.34f, 0.08f, -0.69f)
            quadToRelative(0f, -0.35f, -0.08f, -0.69f)
            lineToRelative(6.93f, -4.05f)
            quadToRelative(0.61f, 0.58f, 1.42f, 0.58f)
            quadToRelative(1.04f, 0f, 1.77f, -0.73f)
            reflectiveQuadTo(20f, 4.75f)
            quadToRelative(0f, -1.04f, -0.73f, -1.77f)
            reflectiveQuadTo(17.5f, 2.25f)
            quadToRelative(-1.04f, 0f, -1.77f, 0.73f)
            reflectiveQuadTo(15f, 4.75f)
            quadToRelative(0f, 0.35f, 0.08f, 0.69f)
            lineToRelative(-6.93f, 4.05f)
            quadToRelative(-0.61f, -0.58f, -1.42f, -0.58f)
            quadToRelative(-1.04f, 0f, -1.77f, 0.73f)
            reflectiveQuadTo(4.25f, 11.37f)
            quadToRelative(0f, 1.04f, 0.73f, 1.77f)
            reflectiveQuadTo(6.75f, 13.87f)
            quadToRelative(0.81f, 0f, 1.42f, -0.58f)
            lineToRelative(7.05f, 4.11f)
            quadToRelative(-0.08f, 0.34f, -0.08f, 0.69f)
            quadToRelative(0f, 1.04f, 0.73f, 1.77f)
            reflectiveQuadTo(17.5f, 21.58f)
            quadToRelative(1.04f, 0f, 1.77f, -0.73f)
            reflectiveQuadTo(20f, 19.08f)
            quadToRelative(0f, -1.04f, -0.73f, -1.77f)
            reflectiveQuadTo(17.5f, 16.58f)
            close()
        }
    }.build()

val ShieldCheckIcon: ImageVector
    get() = ImageVector.Builder(
        name = "ShieldCheck",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 22f)
            quadToRelative(-4.15f, -1.25f, -6.075f, -4.925f)
            reflectiveQuadTo(4f, 9.15f)
            verticalLineTo(4.5f)
            lineToRelative(8f, -3f)
            lineToRelative(8f, 3f)
            verticalLineTo(9.15f)
            quadToRelative(0f, 4.275f, -1.925f, 7.95f)
            reflectiveQuadTo(12f, 22f)
            close()
            moveTo(11f, 15f)
            lineToRelative(4.25f, -4.25f)
            lineToRelative(-1.425f, -1.425f)
            lineTo(11f, 12.15f)
            lineToRelative(-1.825f, -1.825f)
            lineToRelative(-1.425f, 1.425f)
            close()
        }
    }.build()

val DocumentIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Document",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(14f, 2f)
            horizontalLineTo(6f)
            quadToRelative(-0.825f, 0f, -1.413f, 0.587f)
            reflectiveQuadTo(4f, 4f)
            verticalLineToRelative(16f)
            quadToRelative(0f, 0.825f, 0.587f, 1.413f)
            reflectiveQuadTo(6f, 22f)
            horizontalLineToRelative(12f)
            quadToRelative(0.825f, 0f, 1.413f, -0.587f)
            reflectiveQuadTo(20f, 20f)
            verticalLineTo(8f)
            lineTo(14f, 2f)
            close()
            moveTo(13f, 9f)
            verticalLineTo(3.5f)
            lineTo(18.5f, 9f)
            horizontalLineTo(13f)
            close()
        }
    }.build()

val InfoIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Info",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 22f)
            quadToRelative(-4.15f, 0f, -7.075f, -2.925f)
            reflectiveQuadTo(2f, 12f)
            quadToRelative(0f, -4.15f, 2.925f, -7.075f)
            reflectiveQuadTo(12f, 2f)
            quadToRelative(4.15f, 0f, 7.075f, 2.925f)
            reflectiveQuadTo(22f, 12f)
            quadToRelative(0f, 4.15f, -2.925f, 7.075f)
            reflectiveQuadTo(12f, 22f)
            close()
            moveTo(11f, 17f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(-6f)
            horizontalLineToRelative(-2f)
            close()
            moveTo(11f, 9f)
            horizontalLineToRelative(2f)
            verticalLineTo(7f)
            horizontalLineToRelative(-2f)
            close()
        }
    }.build()

val CustomFilledStarIcon: ImageVector
    get() = ImageVector.Builder(
        name = "CustomFilledStar",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 17.27f)
            lineTo(18.18f, 21f)
            lineTo(16.54f, 13.97f)
            lineTo(22f, 9.24f)
            lineTo(14.81f, 8.63f)
            lineTo(12f, 2f)
            lineTo(9.19f, 8.63f)
            lineTo(2f, 9.24f)
            lineTo(7.46f, 13.97f)
            lineTo(5.82f, 21f)
            close()
        }
    }.build()

val CustomOutlinedStarIcon: ImageVector
    get() = ImageVector.Builder(
        name = "CustomOutlinedStar",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(22f, 9.24f)
            lineToRelative(-7.19f, -0.62f)
            lineTo(12f, 2f)
            lineTo(9.19f, 8.63f)
            lineTo(2f, 9.24f)
            lineToRelative(5.46f, 4.73f)
            lineTo(5.82f, 21f)
            lineTo(12f, 17.27f)
            lineTo(18.18f, 21f)
            lineToRelative(-1.63f, -7.03f)
            lineTo(22f, 9.24f)
            close()
            moveTo(12f, 15.4f)
            lineToRelative(-3.76f, 2.27f)
            lineToRelative(1f, -4.28f)
            lineToRelative(-3.32f, -2.88f)
            lineToRelative(4.38f, -0.38f)
            lineTo(12f, 6.1f)
            lineToRelative(1.71f, 4.04f)
            lineToRelative(4.38f, 0.38f)
            lineToRelative(-3.32f, 2.88f)
            lineToRelative(1f, 4.28f)
            lineTo(12f, 15.4f)
            close()
        }
    }.build()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDarkMode by remember { mutableStateOf(true) }
            MyApplicationTheme(darkTheme = isDarkMode, dynamicColor = false) {
                ImageCompressorApp(
                    isDarkMode = isDarkMode,
                    onThemeChange = { isDarkMode = it }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCompressorApp(
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State Variables
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageName by remember { mutableStateOf<String?>(null) }
    var originalSizeInBytes by remember { mutableStateOf<Long?>(null) }
    
    var compressionSettings by rememberSaveable(stateSaver = compressionSettingsSaver) {
        mutableStateOf(CompressionSettingsState())
    }
    
    // Compression Results
    var compressedSizeInBytes by remember { mutableStateOf<Long?>(null) }
    var isCompressing by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }
    var compressedImageUri by remember { mutableStateOf<Uri?>(null) }
    var compressedMimeType by remember { mutableStateOf("image/jpeg") }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var lastOutcome by remember { mutableStateOf<ImageCompressionOutcome?>(null) }
    var showDailyLimitDialog by rememberSaveable { mutableStateOf(false) }

    val historyDao = remember {
        CompressionHistoryDatabase.getInstance(context).historyDao()
    }

    fun shareCompressedImage(uri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = compressedMimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share compressed photo"))
    }

    // Helper: Format file size
    fun formatFileSize(sizeInBytes: Long): String {
        if (sizeInBytes < 1024) return "$sizeInBytes B"
        val kb = sizeInBytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.1f MB", mb)
    }

    // Launch Image Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            imageName = null
            isSaved = false
            compressedSizeInBytes = null
            compressedImageUri = null
            resultMessage = null
            lastOutcome = null
            
            // Try to extract name and size
            try {
                // Get display name
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        imageName = cursor.getString(nameIndex)
                    }
                }
                if (imageName == null) {
                    imageName = "Selected photo"
                }

                // Get size
                var size = -1L
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { fd ->
                    size = fd.length
                }
                if (size <= 0) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        size = 0
                        val buffer = ByteArray(8192)
                        var bytesRead = stream.read(buffer)
                        while (bytesRead != -1) {
                            size += bytesRead
                            bytesRead = stream.read(buffer)
                        }
                    }
                }
                originalSizeInBytes = size
            } catch (_: Exception) {
                originalSizeInBytes = null
            }
        }
    }

    var isThemeDialogOpen by remember { mutableStateOf(false) }
    var isRateUsDialogOpen by remember { mutableStateOf(false) }
    var isSupportDialogOpen by remember { mutableStateOf(false) }
    var selectedRating by remember { mutableStateOf(5) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    val navState = rememberAlphaPicsNavState()

    fun navigateToFeature(feature: AlphaPicsFeature) {
        val dest = when (feature.id) {
            AlphaPicsFeatureCatalog.AiEnhance.id,
            AlphaPicsFeatureCatalog.RestorePhoto.id,
            AlphaPicsFeatureCatalog.Upscale.id -> {
                val initialModeId = when (feature.id) {
                    AlphaPicsFeatureCatalog.RestorePhoto.id -> "restore"
                    AlphaPicsFeatureCatalog.Upscale.id -> "upscale"
                    else -> "auto"
                }
                AlphaPicsDestination.Enhance(feature.id, initialModeId)
            }
            AlphaPicsFeatureCatalog.EditPhoto.id,
            AlphaPicsFeatureCatalog.RemoveBackground.id -> {
                val initialToolId = if (feature.id == AlphaPicsFeatureCatalog.RemoveBackground.id) {
                    "background"
                } else {
                    "adjust"
                }
                AlphaPicsDestination.Editor(initialToolId)
            }
            AlphaPicsFeatureCatalog.Resize.id -> AlphaPicsDestination.PhotoUtilities("resize")
            AlphaPicsFeatureCatalog.Convert.id -> AlphaPicsDestination.PhotoUtilities("convert")
            else -> AlphaPicsDestination.Placeholder(feature)
        }
        navState.navigateTo(dest)
    }

    fun createTempImageUri(ctx: Context): Uri? {
        val resolver = ctx.contentResolver
        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val details = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "temp_camera_photo_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        return try {
            resolver.insert(imageCollection, details)
        } catch (e: Exception) {
            null
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            val uri = tempCameraUri!!
            selectedImageUri = uri
            isSaved = false
            compressedSizeInBytes = null
            compressedImageUri = null
            resultMessage = null
            lastOutcome = null
            imageName = "Camera Photo"
            
            try {
                var size = -1L
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { fd ->
                    size = fd.length
                }
                if (size <= 0) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        size = 0
                        val buffer = ByteArray(8192)
                        var bytesRead = stream.read(buffer)
                        while (bytesRead != -1) {
                            size += bytesRead
                            bytesRead = stream.read(buffer)
                        }
                    }
                }
                originalSizeInBytes = size
            } catch (_: Exception) {
                originalSizeInBytes = null
            }
        }
    }

    fun openCameraForWorkspace() {
        val uri = createTempImageUri(context)
        if (uri != null) {
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Error creating photo file", Toast.LENGTH_SHORT).show()
        }
    }

    fun performCompression() {
        val uri = selectedImageUri
        if (uri == null) {
            Toast.makeText(context, "Please choose a photo first", Toast.LENGTH_SHORT).show()
            return
        }

        val validation = compressionSettings.validateForProcessing()
        compressionSettings = validation.state
        if (validation is SettingsValidation.Invalid) {
            resultMessage = validation.message
            Toast.makeText(context, validation.message, Toast.LENGTH_LONG).show()
            return
        }
        validation as SettingsValidation.Valid
        if (validation.wasClamped) {
            validation.state.targetSize.validationMessage?.let { message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }

        // Hard gate before any compression or gallery write can begin.
        if (!DailyTokenManager.hasAvailableToken(context)) {
            showDailyLimitDialog = true
            return
        }

        val settingsSnapshot = validation.snapshot
        val originalNameSnapshot = imageName ?: "Selected photo"
        isCompressing = true
        isSaved = false
        resultMessage = null
        lastOutcome = null

        coroutineScope.launch(Dispatchers.Default) {
            val outcome = ImageCompressionProcessor.process(
                context = context,
                uri = uri,
                settings = settingsSnapshot
            )

            val savedArtifact = when (outcome) {
                is ImageCompressionOutcome.Compressed -> outcome.artifact
                is ImageCompressionOutcome.TargetNotReached -> outcome.artifact
                is ImageCompressionOutcome.Skipped,
                is ImageCompressionOutcome.Failed -> null
            }
            if (savedArtifact != null) {
                try {
                    historyDao.insert(
                        CompressionHistoryEntity(
                            timestampMillis = System.currentTimeMillis(),
                            originalFileName = originalNameSnapshot,
                            originalSizeBytes = savedArtifact.originalSizeBytes,
                            finalSizeBytes = savedArtifact.finalSizeBytes,
                            originalWidth = savedArtifact.originalWidth,
                            originalHeight = savedArtifact.originalHeight,
                            finalWidth = savedArtifact.finalWidth,
                            finalHeight = savedArtifact.finalHeight,
                            inputFormat = savedArtifact.inputFormat,
                            outputFormat = savedArtifact.outputFormat,
                            compressionMode = savedArtifact.settings.historyValue,
                            settingValue = savedArtifact.settings.displayLabel,
                            targetReached = savedArtifact.targetReached,
                            outputUriString = savedArtifact.outputUri.toString()
                        )
                    )
                } catch (_: Throwable) {
                    // A history write failure does not invalidate a validated gallery output.
                }
            }

            withContext(Dispatchers.Main) {
                isCompressing = false
                lastOutcome = outcome
                when (outcome) {
                    is ImageCompressionOutcome.Compressed -> {
                        val artifact = outcome.artifact
                        originalSizeInBytes = artifact.originalSizeBytes
                        compressedImageUri = artifact.outputUri
                        compressedMimeType = artifact.mimeType
                        compressedSizeInBytes = artifact.finalSizeBytes
                        isSaved = true
                        resultMessage = "Compressed and saved • ${artifact.settings.displayLabel}"
                        Toast.makeText(context, "Compressed & saved to Gallery", Toast.LENGTH_LONG).show()
                        DailyTokenManager.consumeAfterSuccessfulCompression(context)
                        (context as? Activity)?.let {
                            InterstitialAdManager.onSuccessfulCompression(it)
                        }
                    }

                    is ImageCompressionOutcome.TargetNotReached -> {
                        val artifact = outcome.artifact
                        originalSizeInBytes = artifact.originalSizeBytes
                        compressedImageUri = artifact.outputUri
                        compressedMimeType = artifact.mimeType
                        compressedSizeInBytes = artifact.finalSizeBytes
                        isSaved = true
                        resultMessage = outcome.reason
                        Toast.makeText(context, outcome.reason, Toast.LENGTH_LONG).show()
                        DailyTokenManager.consumeAfterSuccessfulCompression(context)
                        (context as? Activity)?.let {
                            InterstitialAdManager.onSuccessfulCompression(it)
                        }
                    }

                    is ImageCompressionOutcome.Skipped -> {
                        originalSizeInBytes = outcome.originalSizeBytes
                        compressedImageUri = null
                        compressedSizeInBytes = null
                        isSaved = false
                        resultMessage = "Skipped — ${outcome.reason}"
                        Toast.makeText(context, outcome.reason, Toast.LENGTH_LONG).show()
                    }

                    is ImageCompressionOutcome.Failed -> {
                        compressedImageUri = null
                        compressedSizeInBytes = null
                        isSaved = false
                        resultMessage = "Failed — ${outcome.reason}"
                        Toast.makeText(context, outcome.reason, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    if (showDailyLimitDialog) {
        AlertDialog(
            onDismissRequest = { showDailyLimitDialog = false },
            title = {
                Text("Daily Free Limit Reached!")
            },
            text = {
                Text(
                    "To continue compressing without a premium account, watch 1 short video " +
                        "advertisement to claim 1 Free Compression Token."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val activity = context as? Activity
                        if (activity == null) {
                            Toast.makeText(
                                context,
                                "Unable to open rewarded advertisement right now.",
                                Toast.LENGTH_LONG
                            ).show()
                            return@TextButton
                        }

                        showDailyLimitDialog = false
                        try {
                            RewardedAdManager.show(
                                activity = activity,
                                onRewardEarned = {
                                    try {
                                        val balance = DailyTokenManager.grantRewardToken(context)
                                        if (balance > 0) {
                                            // Re-enter the normal compression path. The newly granted token
                                            // passes the same gate and is consumed only after a saved output.
                                            performCompression()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Unable to grant the compression token. Please try again.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    } catch (_: Throwable) {
                                        Toast.makeText(
                                            context,
                                            "Unable to grant the compression token. Please try again.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                onCancelled = {
                                    Toast.makeText(
                                        context,
                                        "Transaction Cancelled: You must watch the complete video advertisement to receive your 1 Free Token.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                },
                                onUnavailable = {
                                    Toast.makeText(
                                        context,
                                        "Rewarded advertisement is not ready yet. Please try again.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    showDailyLimitDialog = true
                                }
                            )
                        } catch (_: Throwable) {
                            Toast.makeText(
                                context,
                                "Rewarded advertisement is not ready yet. Please try again.",
                                Toast.LENGTH_LONG
                            ).show()
                            showDailyLimitDialog = true
                        }
                    }
                ) {
                    Text("Watch Ad")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDailyLimitDialog = false }) {
                    Text("Not Now")
                }
            }
        )
    }

    val currentBgColor = if (isDarkMode) AlphaPicsColors.Void else Color(0xFFF6F8FC)
    val currentTextColor = if (isDarkMode) AlphaPicsColors.TextPrimary else Color(0xFF101729)
    val currentSecondaryTextColor = if (isDarkMode) AlphaPicsColors.TextSecondary else Color(0xFF5C667B)
    val currentContainerBgColor = if (isDarkMode) AlphaPicsColors.SurfaceRaised else Color.White
    val currentBorderColor = if (isDarkMode) AlphaPicsColors.BorderSoft else Color(0xFFDCE2ED)
    val cardBorderColor = currentBorderColor
    val cardBgColor = currentContainerBgColor

    val isAlphaPicsSurface = navState.currentDestination !is AlphaPicsDestination.Compressor

    SideEffect {
        (context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = !isAlphaPicsSurface && !isDarkMode
            }
        }
    }

    BackHandler(enabled = navState.canGoBack) {
        navState.pop()
    }

    Crossfade(
        targetState = navState.currentDestination,
        animationSpec = rememberAlphaPicsFadeSpec(),
        label = "AlphaPicsNavigationCrossfade"
    ) { destination ->
        when (destination) {
            is AlphaPicsDestination.Enhance -> {
                AlphaPicsEnhancementWorkspace(
                    imageModel = selectedImageUri,
                    initialModeId = destination.initialModeId,
                    onBack = { navState.pop() },
                    onChoosePhoto = { imagePickerLauncher.launch("image/*") },
                    onOpenCamera = { openCameraForWorkspace() }
                )
            }

            is AlphaPicsDestination.Editor -> {
                AlphaPicsEditorScreen(
                    imageModel = selectedImageUri,
                    initialToolId = destination.initialToolId,
                    onBack = { navState.pop() },
                    onChoosePhoto = { imagePickerLauncher.launch("image/*") },
                    onOpenCamera = { openCameraForWorkspace() },
                    onOpenCollage = { navState.navigateTo(AlphaPicsDestination.Collage) },
                    onOpenPhotoUtilities = {
                        navState.navigateTo(AlphaPicsDestination.PhotoUtilities("resize"))
                    }
                )
            }

            AlphaPicsDestination.Collage -> {
                AlphaPicsCollageScreen(onBack = { navState.pop() })
            }

            is AlphaPicsDestination.PhotoUtilities -> {
                AlphaPicsPhotoUtilityScreen(
                    imageModel = selectedImageUri,
                    initialTabId = destination.initialTabId,
                    onBack = { navState.pop() },
                    onChoosePhoto = { imagePickerLauncher.launch("image/*") },
                    onOpenCamera = { openCameraForWorkspace() }
                )
            }

            is AlphaPicsDestination.Placeholder -> {
                AlphaPicsPlaceholderScreen(
                    feature = destination.feature,
                    onBack = { navState.pop() }
                )
            }

            AlphaPicsDestination.Batch -> {
                com.example.batch.BatchScreen(
                    isDarkMode = isDarkMode,
                    onBack = { navState.pop() },
                    onOpenStudio = { navState.navigateTo(AlphaPicsDestination.BatchStudio) }
                )
            }

            AlphaPicsDestination.BatchStudio -> {
                com.example.ui.alphapics.batchstudio.AlphaPicsBatchStudioScreen(
                    onBack = { navState.pop() },
                    onOpenBatchCompress = { navState.pop() }
                )
            }

            AlphaPicsDestination.History -> {
                com.example.history.HistoryScreen(
                    isDarkMode = isDarkMode,
                    onBack = { navState.pop() }
                )
            }

            AlphaPicsDestination.Settings -> {
                AlphaPicsSettingsScreen(
                    isDarkMode = isDarkMode,
                    versionName = BuildConfig.VERSION_NAME,
                    onBack = { navState.pop() },
                    onOpenAppearance = { isThemeDialogOpen = true },
                    onCustomerSupport = { isSupportDialogOpen = true },
                    onRate = { isRateUsDialogOpen = true },
                    onShare = {
                        try {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Check out AlphaPics AI for photo enhancement, editing and smart compression."
                                )
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, null))
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Unable to share at this moment",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onPrivacy = {
                        try {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.freeprivacypolicy.com/live/5e73e0f2-1cc8-4321-b62b-5bbf43663e01")
                            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            // The screen remains usable if no browser can handle the link.
                        }
                    },
                    onTerms = {
                        try {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.termsfeed.com/live/69d5c146-19c7-405c-b2e0-fd657f8563fa")
                            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            // The screen remains usable if no browser can handle the link.
                        }
                    }
                )
            }

            AlphaPicsDestination.Compressor -> {
                Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = currentBgColor,
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF070A12))
                        .statusBarsPadding()
                        .height(72.dp)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF20C7FF), Color(0xFF7A5CFF), Color.Transparent)
                                )
                            )
                            .align(Alignment.BottomCenter)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { navState.pop() },
                            modifier = Modifier.testTag("compressor_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Home",
                                tint = Color.White
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Compress Photo",
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                text = "ALPHAPICS AI  •  QUICK TOOL",
                                color = Color(0xFF9AA6BE),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(currentBgColor)
                        .navigationBarsPadding()
                ) {
                    BannerAdView(modifier = Modifier.fillMaxWidth())
                }
            }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(currentBgColor)
        ) {
            // Main body inside column with scrollable layout (above the ad banner)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Choose how to compress",
                    color = currentTextColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 400.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Set a file-size goal or keep direct control of photo quality.",
                    color = currentSecondaryTextColor,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 400.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // The product exposes only the two explicit user-controlled modes.
                TabSwitcher(
                    mode = compressionSettings.mode,
                    onModeChange = {
                        compressionSettings = compressionSettings.copy(mode = it)
                        isSaved = false
                        compressedSizeInBytes = null
                        compressedImageUri = null
                        resultMessage = null
                        lastOutcome = null
                    },
                    isDarkMode = isDarkMode,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // High-fidelity image layout section
                if (selectedImageUri != null && compressedImageUri != null) {
                    // Before-and-after interactive slider
                    BeforeAfterSlider(
                        originalUri = selectedImageUri!!,
                        compressedUri = compressedImageUri!!,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.4f)
                            .widthIn(max = 400.dp)
                    )
                } else {
                    // Regular photo container (empty state or selected but not yet compressed)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.6f)
                            .widthIn(max = 400.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(currentContainerBgColor)
                            .then(
                                if (selectedImageUri == null) {
                                    Modifier.dashedBorder(
                                        strokeWidth = 1.5.dp,
                                        color = if (isDarkMode) AlphaPicsColors.Cyan.copy(alpha = 0.45f) else Color(0xFFD1D5DB),
                                        cornerRadius = 24.dp,
                                        dashLength = 8.dp,
                                        gapLength = 6.dp
                                    )
                                } else {
                                    Modifier
                                        .border(
                                            BorderStroke(
                                                1.5.dp,
                                                if (isDarkMode) {
                                                    Brush.linearGradient(listOf(AlphaPicsColors.Cyan, AlphaPicsColors.Violet))
                                                } else {
                                                    SolidColor(Color(0xFFD1D5DB))
                                                }
                                            ),
                                            RoundedCornerShape(24.dp)
                                        )
                                        .clickable { imagePickerLauncher.launch("image/*") }
                                }
                            )
                            .testTag("choose_photo_container"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "Selected Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Transparent overlay at bottom showing name
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = imageName ?: "Selected photo",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 1
                                    )
                                }
                            }
                        } else {
                            // High-fidelity premium upload visual
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(AlphaPicsColors.ElectricBlue.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = CameraIcon,
                                        contentDescription = "Upload Photo",
                                        tint = AlphaPicsColors.Cyan,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                
                                // Beautifully spaced horizontal Row containing two visually distinct action buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left Button: Solid primary blue rectangle reading "TAP TO CHOOSE PHOTO"
                                    Button(
                                        onClick = { imagePickerLauncher.launch("image/*") },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AlphaPicsColors.ElectricBlue,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .testTag("tap_to_choose_button")
                                    ) {
                                        Text(
                                            text = "CHOOSE PHOTO",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            letterSpacing = 0.2.sp
                                        )
                                    }

                                    // Right Button: Camera button with clean white background, sleek blue outline, small blue camera icon and text "CAMERA" next to it
                                    Button(
                                        onClick = {
                                            val uri = createTempImageUri(context)
                                            if (uri != null) {
                                                tempCameraUri = uri
                                                cameraLauncher.launch(uri)
                                            } else {
                                                Toast.makeText(context, "Error creating photo file", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDarkMode) AlphaPicsColors.SurfaceRaised else Color.White,
                                            contentColor = if (isDarkMode) AlphaPicsColors.Cyan else AlphaPicsColors.ElectricBlue
                                        ),
                                        border = BorderStroke(
                                            1.5.dp,
                                            if (isDarkMode) AlphaPicsColors.Cyan.copy(alpha = 0.72f) else AlphaPicsColors.ElectricBlue
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                                        modifier = Modifier
                                            .weight(0.9f)
                                            .testTag("camera_button")
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = CameraIcon,
                                                contentDescription = "Camera",
                                                tint = if (isDarkMode) AlphaPicsColors.Cyan else AlphaPicsColors.ElectricBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "CAMERA",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                letterSpacing = 0.2.sp
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Photos up to 50 MB",
                                    color = currentSecondaryTextColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // High-Fidelity Stats Grid
                val originalPrimaryText: String
                val originalSubText: String
                val sizeBytes = originalSizeInBytes
                if (sizeBytes != null && sizeBytes > 0) {
                    val kb = sizeBytes / 1024.0
                    val mb = kb / 1024.0
                    if (kb >= 1024.0) {
                        originalPrimaryText = String.format(Locale.US, "%.1f MB", mb)
                        originalSubText = String.format(Locale.US, "(%d KB)", kb.toInt())
                    } else {
                        originalPrimaryText = String.format(Locale.US, "%d KB", kb.toInt())
                        originalSubText = String.format(Locale.US, "(%.2f MB)", mb)
                    }
                } else {
                    originalPrimaryText = "-- MB"
                    originalSubText = "—"
                }

                val compressedPrimaryText: String
                val compressedSubText: String
                val compSizeBytes = compressedSizeInBytes
                if (compSizeBytes != null && compSizeBytes > 0) {
                    val kb = compSizeBytes / 1024.0
                    val mb = kb / 1024.0
                    if (kb >= 1024.0) {
                        compressedPrimaryText = String.format(Locale.US, "%.1f MB", mb)
                        compressedSubText = String.format(Locale.US, "(%d KB)", kb.toInt())
                    } else {
                        compressedPrimaryText = String.format(Locale.US, "%d KB", kb.toInt())
                        compressedSubText = String.format(Locale.US, "(%.2f MB)", mb)
                    }
                } else {
                    compressedPrimaryText = "-- KB"
                    compressedSubText = "—"
                }

                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Original Size Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (isDarkMode) currentContainerBgColor else Color(0xFFF5F6F9), RoundedCornerShape(16.dp))
                            .border(BorderStroke(1.dp, currentBorderColor), RoundedCornerShape(16.dp))
                            .clickable {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(originalPrimaryText))
                                Toast.makeText(context, "Copied original size: $originalPrimaryText", Toast.LENGTH_SHORT).show()
                            }
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "ORIGINAL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = currentSecondaryTextColor,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = originalPrimaryText,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = if (selectedImageUri == null) currentSecondaryTextColor else (if (isDarkMode) Color.White else currentTextColor),
                                modifier = Modifier.testTag("original_file_size_text")
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = originalSubText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (selectedImageUri == null) (if (isDarkMode) currentSecondaryTextColor.copy(alpha = 0.6f) else currentSecondaryTextColor) else (if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF4B5563))
                            )
                        }
                    }

                    // Compressed Size Card (shows if compressed, else show a placeholder)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (isDarkMode) currentContainerBgColor else Color(0xFFF5F6F9), RoundedCornerShape(16.dp))
                            .border(
                                BorderStroke(
                                    1.dp, 
                                    if (compressedSizeInBytes != null && isDarkMode) Color(0xFF10B981) else currentBorderColor
                                ), 
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(compressedPrimaryText))
                                Toast.makeText(context, "Copied compressed size: $compressedPrimaryText", Toast.LENGTH_SHORT).show()
                            }
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "COMPRESSED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (compressedSizeInBytes != null) (if (isDarkMode) Color(0xFF10B981) else Color(0xFF047857)) else currentSecondaryTextColor,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = compressedPrimaryText,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = if (compressedSizeInBytes != null) (if (isDarkMode) Color(0xFF10B981) else Color(0xFF047857)) else currentSecondaryTextColor,
                                modifier = Modifier.testTag("compressed_file_size_text")
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = compressedSubText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (compressedSizeInBytes != null) {
                                    if (isDarkMode) Color(0xFF34D399) else Color(0xFF065F46)
                                } else {
                                    if (isDarkMode) currentSecondaryTextColor.copy(alpha = 0.6f) else currentSecondaryTextColor
                                }
                            )
                        }
                    }
                }

                // If compression completed, show saving percentage badge
                if (compressedSizeInBytes != null && originalSizeInBytes != null && originalSizeInBytes!! > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val pctReduction = ((originalSizeInBytes!! - compressedSizeInBytes!!).toFloat() / originalSizeInBytes!!.toFloat() * 100).toInt().coerceAtLeast(0)
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .border(BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "COMPRESSION SAVED $pctReduction% OF FILE SIZE",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Settings controls share the same pure input state and processing snapshot.
                when (compressionSettings.mode) {
                    CompressionMode.TARGET_SIZE -> TargetSizeControl(
                        state = compressionSettings.targetSize,
                        onStateChange = { targetState ->
                            compressionSettings = compressionSettings.copy(targetSize = targetState)
                            isSaved = false
                            compressedSizeInBytes = null
                            compressedImageUri = null
                            resultMessage = null
                            lastOutcome = null
                        },
                        isDarkMode = isDarkMode,
                        modifier = Modifier.fillMaxWidth()
                    )
                    CompressionMode.QUALITY -> QualityControl(
                        value = compressionSettings.qualitySliderValue,
                        onValueChange = { quality ->
                            compressionSettings = compressionSettings.withQuality(quality)
                            isSaved = false
                            compressedSizeInBytes = null
                            compressedImageUri = null
                            resultMessage = null
                            lastOutcome = null
                        },
                        isDarkMode = isDarkMode,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Loading/Processing Feedback
                AnimatedVisibility(
                    visible = isCompressing,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        LinearProgressIndicator(
                            color = AlphaPicsColors.ElectricBlue,
                            trackColor = currentBorderColor,
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Compressing your photo...",
                            fontSize = 13.sp,
                            color = currentSecondaryTextColor
                        )
                    }
                }

                // Soft green text displaying "Compressed: 12 KB (Saved 95%)"
                if (compressedSizeInBytes != null && originalSizeInBytes != null && originalSizeInBytes!! > 0) {
                    val pctReduction = ((originalSizeInBytes!! - compressedSizeInBytes!!).toFloat() / originalSizeInBytes!!.toFloat() * 100).toInt().coerceAtLeast(0)
                    val formattedCompressedSize = formatFileSize(compressedSizeInBytes!!)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.12f))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Compressed: $formattedCompressedSize (Saved $pctReduction%)",
                            color = Color(0xFF10B981), // soft green
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }

                resultMessage?.let { message ->
                    val outcomeColor = when (lastOutcome) {
                        is ImageCompressionOutcome.Compressed -> Color(0xFF10B981)
                        is ImageCompressionOutcome.TargetNotReached -> Color(0xFFF97316)
                        is ImageCompressionOutcome.Skipped -> Color(0xFFD97706)
                        is ImageCompressionOutcome.Failed -> Color(0xFFDC2626)
                        null -> Color(0xFF64748B)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(outcomeColor.copy(alpha = 0.12f))
                            .border(BorderStroke(1.dp, outcomeColor.copy(alpha = 0.45f)), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = message,
                            color = outcomeColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Row for main Action button and optionally share button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { performCompression() },
                        enabled = selectedImageUri != null && !isCompressing && !isSaved,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AlphaPicsColors.ElectricBlue,
                            contentColor = Color.White,
                            disabledContainerColor = if (isDarkMode) Color(0xFF1F2937).copy(alpha = 0.6f) else Color(0xFFE0E2E6),
                            disabledContentColor = if (isDarkMode) Color.White.copy(alpha = 0.35f) else Color(0xFF4B5563)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (selectedImageUri != null) 2.dp else 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("compress_action_button")
                    ) {
                        Text(
                            text = when {
                                isCompressing -> "COMPRESSING..."
                                isSaved -> "SAVED TO GALLERY"
                                else -> "COMPRESS PHOTO"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    if (isSaved && compressedImageUri != null) {
                        IconButton(
                            onClick = { shareCompressedImage(compressedImageUri!!) },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(AlphaPicsColors.ElectricBlue.copy(alpha = 0.15f))
                                .border(BorderStroke(1.5.dp, AlphaPicsColors.BrightBlue), RoundedCornerShape(16.dp))
                                .testTag("share_image_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share compressed photo",
                                tint = AlphaPicsColors.BrightBlue
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
            AlphaPicsDestination.Home -> {
                AlphaPicsHomeScreen(
                    onOpenFeature = { feature ->
                        navigateToFeature(feature)
                    },
                    onChooseEnhancementPhoto = {
                        navState.navigateTo(AlphaPicsDestination.Enhance(AlphaPicsFeatureCatalog.AiEnhance.id, "auto"))
                        imagePickerLauncher.launch("image/*")
                    },
                    onOpenEnhancementCamera = {
                        navState.navigateTo(AlphaPicsDestination.Enhance(AlphaPicsFeatureCatalog.AiEnhance.id, "auto"))
                        openCameraForWorkspace()
                    },
                    onOpenCompressor = {
                        navState.navigateTo(AlphaPicsDestination.Compressor)
                    },
                    onOpenBatch = {
                        navState.navigateTo(AlphaPicsDestination.Batch)
                    },
                    onOpenHistory = {
                        navState.navigateTo(AlphaPicsDestination.History)
                    },
                    onOpenSettings = {
                        navState.navigateTo(AlphaPicsDestination.Settings)
                    }
                )
            }
        }
    }

    if (isThemeDialogOpen) {
        AlertDialog(
            onDismissRequest = { isThemeDialogOpen = false },
            title = {
                Text(
                    text = "Appearance",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = currentTextColor
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (!isDarkMode) AlphaPicsColors.BrightBlue.copy(alpha = 0.12f)
                                else Color.Transparent
                            )
                            .clickable {
                                onThemeChange(false)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !isDarkMode,
                            onClick = { onThemeChange(false) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AlphaPicsColors.BrightBlue,
                                unselectedColor = if (isDarkMode) Color.Gray else Color.DarkGray
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Light",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = currentTextColor
                        )
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isDarkMode) AlphaPicsColors.BrightBlue.copy(alpha = 0.12f)
                                else Color.Transparent
                            )
                            .clickable {
                                onThemeChange(true)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isDarkMode,
                            onClick = { onThemeChange(true) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AlphaPicsColors.BrightBlue,
                                unselectedColor = if (isDarkMode) Color.Gray else Color.DarkGray
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Dark",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = currentTextColor
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { isThemeDialogOpen = false }
                ) {
                    Text(
                        text = "Done",
                        fontWeight = FontWeight.Bold,
                        color = AlphaPicsColors.BrightBlue
                    )
                }
            },
            containerColor = if (isDarkMode) AlphaPicsColors.SurfaceRaised else Color.White
        )
    }

    if (isRateUsDialogOpen) {
        Dialog(onDismissRequest = { isRateUsDialogOpen = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF111827) // sleek dark gray/black background for dark mode mockup
                ),
                border = BorderStroke(2.dp, Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF60A5FA)))),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color(0xFF3B82F6),
                        spotColor = Color(0xFF3B82F6),
                        clip = false
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Close button at top right
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        IconButton(
                            onClick = { isRateUsDialogOpen = false },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("rate_dialog_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF9CA3AF)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Vibrant animated happy emoji with glowing star eyes
                    AnimatedHappyEmojiWithStarEyes()

                    Spacer(modifier = Modifier.height(20.dp))

                    // Header title: "Enjoying Smart Compress?"
                    Text(
                        text = "Enjoying AlphaPics AI?",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sub-text in high contrast light gray
                    Text(
                        text = "We'd love to hear your feedback! Support us with 5 stars to help us keep improving.",
                        fontSize = 13.sp,
                        color = Color(0xFF9CA3AF),
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Row of 5 beautifully styled glowing neon-blue star icons
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        for (star in 1..5) {
                            val isSelected = star <= selectedRating
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable {
                                        selectedRating = star
                                    }
                                    .testTag("rate_dialog_star_$star"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isSelected) CustomFilledStarIcon else CustomOutlinedStarIcon,
                                    contentDescription = "Star $star",
                                    tint = if (isSelected) Color(0xFF60A5FA) else Color(0xFF374151),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            if (star < 5) {
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Solid primary blue action button reading "Rate Us on Play Store" with elegant spacing
                    Button(
                        onClick = {
                            val appPackageName = context.packageName
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName"))
                                context.startActivity(intent)
                            }
                            isRateUsDialogOpen = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB), // solid primary blue
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("rate_dialog_submit_button")
                    ) {
                        Text(
                            text = "Rate Us on Play Store",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }

    if (isSupportDialogOpen) {
        var supportDescription by remember { mutableStateOf("") }
        var selectedSupportTag by remember { mutableStateOf("Report Bug") }
        val supportTags = listOf("Report Bug", "Feature Request", "Other")

        Dialog(onDismissRequest = { isSupportDialogOpen = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF111827) else Color.White
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isDarkMode) Color(0xFF374151) else Color(0xFFE5E7EB)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = if (isDarkMode) Color(0xFF3B82F6) else Color(0x22000000),
                        spotColor = if (isDarkMode) Color(0xFF3B82F6) else Color(0x22000000),
                        clip = false
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Close button and Header Title
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Help Desk Support",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color(0xFF111827),
                            fontFamily = FontFamily.SansSerif
                        )
                        IconButton(
                            onClick = { isSupportDialogOpen = false },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(32.dp)
                                .testTag("support_dialog_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF4B5563)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "How can we assist you today?",
                        fontSize = 14.sp,
                        color = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF4B5563),
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Horizontal row of capsule-shaped quick-issue filter tags
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        supportTags.forEach { tag ->
                            val isSelected = tag == selectedSupportTag
                            val tagBgColor = if (isSelected) {
                                Color(0xFF2563EB) // Solid Primary Blue
                            } else {
                                if (isDarkMode) Color(0xFF1F2937) else Color(0xFFE5E7EB)
                            }
                            val tagTextColor = if (isSelected) {
                                Color.White
                            } else {
                                if (isDarkMode) Color(0xFFD1D5DB) else Color(0xFF374151)
                            }

                            Box(
                                modifier = Modifier
                                    .background(tagBgColor, shape = RoundedCornerShape(50))
                                    .clickable { selectedSupportTag = tag }
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                                    .testTag("support_tag_$tag"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = tagTextColor,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Text field background F5F6F9 (light) / 1F2937 (dark)
                    val textFieldBg = if (isDarkMode) Color(0xFF1F2937) else Color(0xFFF5F6F9)
                    // outlined user input text field with dark-gray outline (#374151 or #4B5563)
                    val outlineColor = if (isDarkMode) Color(0xFF4B5563) else Color(0xFF374151)
                    val textColor = if (isDarkMode) Color.White else Color(0xFF111827)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(textFieldBg, shape = RoundedCornerShape(12.dp))
                            .border(
                                width = 1.5.dp,
                                color = outlineColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = supportDescription,
                            onValueChange = { supportDescription = it },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = textColor,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.SansSerif
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("support_input_field"),
                            decorationBox = { innerTextField ->
                                if (supportDescription.isEmpty()) {
                                    Text(
                                        text = "Describe your issue here...",
                                        color = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280),
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Submit Ticket primary action button in solid primary blue
                    Button(
                        onClick = {
                            if (supportDescription.trim().isEmpty()) {
                                Toast.makeText(context, "Please describe your issue first.", Toast.LENGTH_SHORT).show()
                            } else {
                                try {
                                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:")
                                        putExtra(Intent.EXTRA_EMAIL, arrayOf("sourabhsinghrajput51@gmail.com"))
                                        putExtra(Intent.EXTRA_SUBJECT, "[AlphaPics AI Support] - $selectedSupportTag")
                                        putExtra(Intent.EXTRA_TEXT, supportDescription)
                                    }
                                    context.startActivity(Intent.createChooser(emailIntent, "Send ticket via..."))
                                    isSupportDialogOpen = false
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No email client found. Please email sourabhsinghrajput51@gmail.com", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB), // Solid primary blue
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("support_submit_button")
                    ) {
                        Text(
                            text = "Submit Ticket",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedHappyEmojiWithStarEyes(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "emojiAnim")
    
    // Smooth pulsating scale
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    // Infinite star eye rotation
    val starRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .size(90.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = size.center
            val radius = size.minDimension / 2f
            
            // 1. Draw soft neon-blue/yellow glowing background halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF60A5FA).copy(alpha = 0.25f), Color.Transparent),
                    center = center,
                    radius = radius + 16.dp.toPx()
                ),
                radius = radius + 16.dp.toPx()
            )
            
            // 2. Face Base - beautiful gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF59D), Color(0xFFFBC02D)),
                    center = center,
                    radius = radius
                ),
                radius = radius
            )
            
            // 3. Smiling Mouth - smooth vector arc path
            val mouthPath = Path().apply {
                val topY = center.y + radius * 0.15f
                val bottomY = center.y + radius * 0.65f
                val leftX = center.x - radius * 0.5f
                val rightX = center.x + radius * 0.5f
                val controlX1 = center.x - radius * 0.3f
                val controlX2 = center.x + radius * 0.3f
                
                moveTo(leftX, topY)
                cubicTo(controlX1, bottomY, controlX2, bottomY, rightX, topY)
                cubicTo(controlX2, topY + radius * 0.2f, controlX1, topY + radius * 0.2f, leftX, topY)
            }
            drawPath(mouthPath, color = Color(0xFF4E342E))
            
            // Rosy cheeks
            drawCircle(
                color = Color(0xFFFF8A80).copy(alpha = 0.7f),
                radius = radius * 0.18f,
                center = Offset(center.x - radius * 0.55f, center.y + radius * 0.1f)
            )
            drawCircle(
                color = Color(0xFFFF8A80).copy(alpha = 0.7f),
                radius = radius * 0.18f,
                center = Offset(center.x + radius * 0.55f, center.y + radius * 0.1f)
            )
            
            // 4. Rotating Glowing Star Eyes (drawn on canvas with transformation)
            val eyeY = center.y - radius * 0.25f
            val leftEyeX = center.x - radius * 0.35f
            val rightEyeX = center.x + radius * 0.35f
            
            // Left Eye
            withTransform({
                rotate(degrees = starRotation, pivot = Offset(leftEyeX, eyeY))
            }) {
                val starPath = Path()
                val angleStep = PI / 5
                var currentAngle = -PI / 2.0
                val outerR = radius * 0.3f
                val innerR = radius * 0.12f
                for (i in 0 until 10) {
                    val r = if (i % 2 == 0) outerR else innerR
                    val x = leftEyeX + r * cos(currentAngle).toFloat()
                    val y = eyeY + r * sin(currentAngle).toFloat()
                    if (i == 0) {
                        starPath.moveTo(x, y)
                    } else {
                        starPath.lineTo(x, y)
                    }
                    currentAngle += angleStep
                }
                starPath.close()
                
                // Draw neon blue glow star
                drawPath(path = starPath, color = Color(0xFF60A5FA))
                drawPath(path = starPath, color = Color.White, style = Stroke(width = 2.dp.toPx()))
            }
            
            // Right Eye
            withTransform({
                rotate(degrees = -starRotation, pivot = Offset(rightEyeX, eyeY))
            }) {
                val starPath = Path()
                val angleStep = PI / 5
                var currentAngle = -PI / 2.0
                val outerR = radius * 0.3f
                val innerR = radius * 0.12f
                for (i in 0 until 10) {
                    val r = if (i % 2 == 0) outerR else innerR
                    val x = rightEyeX + r * cos(currentAngle).toFloat()
                    val y = eyeY + r * sin(currentAngle).toFloat()
                    if (i == 0) {
                        starPath.moveTo(x, y)
                    } else {
                        starPath.lineTo(x, y)
                    }
                    currentAngle += angleStep
                }
                starPath.close()
                
                // Draw neon blue glow star
                drawPath(path = starPath, color = Color(0xFF60A5FA))
                drawPath(path = starPath, color = Color.White, style = Stroke(width = 2.dp.toPx()))
            }
        }
    }
}

@Composable
fun SettingsListItem(
    icon: ImageVector,
    title: String,
    description: String,
    textColor: Color,
    secondaryTextColor: Color,
    containerColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0D47A1).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFF0D47A1),
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = secondaryTextColor,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun TabSwitcher(
    mode: CompressionMode,
    onModeChange: (CompressionMode) -> Unit,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    data class TabSpec(val mode: CompressionMode, val label: String, val darkAccent: Color, val lightAccent: Color)
    val tabs = listOf(
        TabSpec(CompressionMode.TARGET_SIZE, "Target Size", AlphaPicsColors.BrightBlue, Color(0xFF2563EB)),
        TabSpec(CompressionMode.QUALITY, "Quality", AlphaPicsColors.Violet, Color(0xFF6D4AFF))
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(if (isDarkMode) AlphaPicsColors.SurfaceRaised else Color(0xFFF1F5F9))
            .border(
                BorderStroke(
                    1.dp,
                    if (isDarkMode) AlphaPicsColors.BorderSoft else Color(0xFFCBD5E1)
                ),
                RoundedCornerShape(27.dp)
            )
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val isSelected = mode == tab.mode
            val accent = if (isDarkMode) tab.darkAccent else tab.lightAccent
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(23.dp))
                    .background(
                        if (isSelected) {
                            if (isDarkMode) accent.copy(alpha = 0.15f) else accent
                        } else {
                            Color.Transparent
                        }
                    )
                    .border(
                        BorderStroke(
                            if (isSelected) 1.dp else 0.dp,
                            if (isSelected) accent else Color.Transparent
                        ),
                        RoundedCornerShape(23.dp)
                    )
                    .clickable { onModeChange(tab.mode) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.label,
                    color = if (isSelected) {
                        Color.White
                    } else {
                        if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF4B5563)
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun BeforeAfterSlider(
    originalUri: Uri,
    compressedUri: Uri,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0.5f) }
    var containerWidth by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
            .clip(RoundedCornerShape(24.dp))
            .border(
                BorderStroke(
                    1.5.dp,
                    Brush.linearGradient(
                        colors = listOf(AlphaPicsColors.Cyan, AlphaPicsColors.Violet)
                    )
                ),
                RoundedCornerShape(24.dp)
            )
            .onGloballyPositioned {
                containerWidth = it.size.width
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    if (containerWidth > 0) {
                        offsetX = (offsetX + dragAmount.x / containerWidth).coerceIn(0f, 1f)
                    }
                }
            }
    ) {
        // Base: Original Image (Before)
        AsyncImage(
            model = originalUri,
            contentDescription = "Original image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Label "BEFORE"
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "BEFORE",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // Overlay: Compressed Image (After) clipped horizontally
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(offsetX)
                .clipToBounds()
        ) {
            AsyncImage(
                model = compressedUri,
                contentDescription = "Compressed image",
                modifier = Modifier
                    .fillMaxHeight()
                    .width(with(density) { containerWidth.toDp() }),
                contentScale = ContentScale.Crop
            )
        }

        // AFTER label anchored to the top-right of the main container
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(AlphaPicsColors.ElectricBlue, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "AFTER",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // Slidable bar and handle
        val handleX = with(density) { (offsetX * containerWidth).toDp() }

        // Divider vertical line
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .offset(x = handleX - 1.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AlphaPicsColors.Cyan, AlphaPicsColors.Violet)
                    )
                )
        )

        // Interactive double arrow handle
        Box(
            modifier = Modifier
                .size(36.dp)
                .align(Alignment.CenterStart)
                .offset(x = handleX - 18.dp)
                .background(Color.White, CircleShape)
                .border(2.dp, AlphaPicsColors.BrightBlue, CircleShape)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = AlphaPicsColors.BrightBlue,
                    modifier = Modifier.size(14.dp)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = AlphaPicsColors.Violet,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
