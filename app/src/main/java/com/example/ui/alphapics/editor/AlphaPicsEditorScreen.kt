package com.example.ui.alphapics.editor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.editor.ColorAdjustments
import com.example.editor.ColorGradeRegion
import com.example.editor.ColorGradingAdjustments
import com.example.editor.ColorMixAdjustments
import com.example.editor.CropGrid
import com.example.editor.CurveChannel
import com.example.editor.CurveEngine
import com.example.editor.CurvePoint
import com.example.editor.CurvesAdjustments
import com.example.editor.DetailAdjustments
import com.example.editor.DrawOverlayStroke
import com.example.editor.EffectAdjustments
import com.example.editor.EditorColorEngine
import com.example.editor.EditorCustomPreset
import com.example.editor.EditorExportManager
import com.example.editor.EditorHistogram
import com.example.editor.EditorHistogramEngine
import com.example.editor.EditorHistoryEntry
import com.example.editor.EditorPreviewRenderer
import com.example.editor.EditorPresetStore
import com.example.editor.EditorSession
import com.example.editor.EditorState
import com.example.editor.ExportFormat
import com.example.editor.ExportResult
import com.example.editor.FilterPreset
import com.example.editor.FilterPresetCatalog
import com.example.editor.HslAdjustments
import com.example.editor.HslChannelAdjustment
import com.example.editor.HslColorChannel
import com.example.editor.LightAdjustments
import com.example.editor.LocalRetouchMode
import com.example.editor.NormalizedCropRect
import com.example.editor.OverlayAdjustments
import com.example.editor.OverlayShapeKind
import com.example.editor.OverlayStickerKind
import com.example.editor.OverlayTextAlignment
import com.example.editor.OverlayToolMode
import com.example.editor.RetouchAdjustments
import com.example.editor.RetouchPoint
import com.example.editor.RetouchStroke
import com.example.editor.SplitToneAdjustments
import com.example.editor.ShapeOverlay
import com.example.editor.StickerOverlay
import com.example.editor.TextOverlay
import com.example.editor.ToneCurve
import com.example.editor.TransformAdjustments
import com.example.editor.WatermarkAnchor
import com.example.editor.rememberEditorSession
import com.example.ui.alphapics.components.AlphaPicsAvailabilityCard
import com.example.ui.alphapics.components.AlphaPicsBackdrop
import com.example.ui.alphapics.components.AlphaPicsBadge
import com.example.ui.alphapics.components.AlphaPicsContextActions
import com.example.ui.alphapics.components.AlphaPicsErrorState
import com.example.ui.alphapics.components.AlphaPicsGlyph
import com.example.ui.alphapics.components.AlphaPicsLoadingState
import com.example.ui.alphapics.components.AlphaPicsPhotoEntry
import com.example.ui.alphapics.components.AlphaPicsValueSlider
import com.example.ui.alphapics.navigation.AlphaPicsIcon
import com.example.ui.alphapics.theme.AlphaPicsColors
import com.example.ui.alphapics.theme.AlphaPicsShapes
import com.example.ui.alphapics.theme.AlphaPicsSpacing
import com.example.ui.alphapics.theme.AlphaPicsTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.hypot

private val UndoIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Undo",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12.5f, 8f)
            curveToRelative(-2.65f, 0f, -5.05f, 0.99f, -6.9f, 2.6f)
            lineTo(2f, 7f)
            verticalLineToRelative(9f)
            horizontalLineToRelative(9f)
            lineToRelative(-3.62f, -3.62f)
            curveToRelative(1.39f, -1.16f, 3.16f, -1.88f, 5.12f, -1.88f)
            curveToRelative(3.54f, 0f, 6.55f, 2.31f, 7.6f, 5.5f)
            lineToRelative(2.37f, -0.78f)
            curveTo(21.08f, 11.03f, 17.15f, 8f, 12.5f, 8f)
            close()
        }
    }.build()

private val RedoIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Redo",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(18.4f, 10.6f)
            curveTo(16.55f, 8.99f, 14.15f, 8f, 11.5f, 8f)
            curveToRelative(-4.65f, 0f, -8.58f, 3.03f, -9.96f, 7.22f)
            lineTo(3.9f, 16f)
            curveToRelative(1.05f, -3.19f, 4.05f, -5.5f, 7.6f, -5.5f)
            curveToRelative(1.95f, 0f, 3.73f, 0.72f, 5.12f, 1.88f)
            lineTo(13f, 16f)
            horizontalLineToRelative(9f)
            verticalLineTo(7f)
            lineToRelative(-3.6f, 3.6f)
            close()
        }
    }.build()

private val VisibilityIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Visibility",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 4.5f)
            curveTo(7f, 4.5f, 2.73f, 7.61f, 1f, 12f)
            curveToRelative(1.73f, 4.39f, 6f, 7.5f, 11f, 7.5f)
            reflectiveCurveToRelative(9.27f, -3.11f, 11f, -7.5f)
            curveToRelative(-1.73f, -4.39f, -6f, -7.5f, -11f, -7.5f)
            close()
            moveTo(12f, 17f)
            curveToRelative(-2.76f, 0f, -5f, -2.24f, -5f, -5f)
            reflectiveCurveToRelative(2.24f, -5f, 5f, -5f)
            reflectiveCurveToRelative(5f, 2.24f, 5f, 5f)
            reflectiveCurveToRelative(-2.24f, 5f, -5f, 5f)
            close()
            moveTo(12f, 9f)
            curveToRelative(-1.66f, 0f, -3f, 1.34f, -3f, 3f)
            reflectiveCurveToRelative(1.34f, 3f, 3f, 3f)
            reflectiveCurveToRelative(3f, -1.34f, 3f, -3f)
            reflectiveCurveToRelative(-1.34f, -3f, -3f, -3f)
            close()
        }
    }.build()

private val SaveDownloadIcon: ImageVector
    get() = ImageVector.Builder(
        name = "SaveDownload",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(19.35f, 10.04f)
            curveTo(18.67f, 6.59f, 15.64f, 4f, 12f, 4f)
            curveTo(9.11f, 4f, 6.6f, 5.64f, 5.35f, 8.04f)
            curveTo(2.34f, 8.36f, 0f, 10.91f, 0f, 14f)
            curveToRelative(0f, 3.31f, 2.69f, 6f, 6f, 6f)
            horizontalLineToRelative(13f)
            curveToRelative(2.76f, 0f, 5f, -2.24f, 5f, -5f)
            curveToRelative(0f, -2.64f, -2.05f, -4.78f, -4.65f, -4.96f)
            close()
            moveTo(17f, 13f)
            lineToRelative(-5f, 5f)
            lineToRelative(-5f, -5f)
            horizontalLineToRelative(3f)
            verticalLineTo(9f)
            horizontalLineToRelative(4f)
            verticalLineToRelative(4f)
            horizontalLineToRelative(3f)
            close()
        }
    }.build()

enum class AlphaPicsEditorTool(
    val id: String,
    val label: String,
    val icon: AlphaPicsIcon
) {
    ADJUST("adjust", "Adjust", AlphaPicsIcon.EDIT),
    FILTERS("filters", "Filters", AlphaPicsIcon.CONVERT),
    CROP("crop", "Crop", AlphaPicsIcon.RESIZE),
    RETOUCH("retouch", "Retouch", AlphaPicsIcon.RETOUCH),
    REMOVE("remove", "Remove", AlphaPicsIcon.ERASER),
    BACKGROUND("background", "Background", AlphaPicsIcon.BACKGROUND),
    DETAIL("detail", "Detail", AlphaPicsIcon.ENHANCE),
    TEXT("text", "Text", AlphaPicsIcon.EDIT),
    HISTORY("history", "History", AlphaPicsIcon.HISTORY);

    companion object {
        fun fromId(id: String): AlphaPicsEditorTool = entries.firstOrNull { it.id == id } ?: ADJUST
    }
}

private val AlphaPicsEditorTool.historyLabel: String
    get() = when (this) {
        AlphaPicsEditorTool.ADJUST -> "Adjust look"
        AlphaPicsEditorTool.FILTERS -> "Apply preset"
        AlphaPicsEditorTool.CROP -> "Crop & transform"
        AlphaPicsEditorTool.RETOUCH -> "Retouch"
        AlphaPicsEditorTool.DETAIL -> "Detail & effects"
        AlphaPicsEditorTool.TEXT -> "Creative overlay"
        AlphaPicsEditorTool.REMOVE -> "Remove"
        AlphaPicsEditorTool.BACKGROUND -> "Background"
        AlphaPicsEditorTool.HISTORY -> "History"
    }

private enum class EditorPhotoLoadState {
    LOADING,
    SUCCESS,
    ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlphaPicsEditorScreen(
    imageModel: Any?,
    initialToolId: String,
    onBack: () -> Unit,
    onChoosePhoto: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenCollage: () -> Unit = {},
    onOpenPhotoUtilities: () -> Unit = {},
    initialEditorState: EditorState = EditorState(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val session = rememberEditorSession(initialEditorState)

    var selectedTool by remember(initialToolId) {
        mutableStateOf(AlphaPicsEditorTool.fromId(initialToolId))
    }

    var showExportSheet by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }
    var exportResult by remember { mutableStateOf<ExportResult?>(null) }
    var exportError by remember { mutableStateOf<String?>(null) }

    val compareSource = remember { MutableInteractionSource() }
    val isComparingHeld by compareSource.collectIsPressedAsState()

    // Determine active display state (original vs edited)
    val displayState = if (isComparingHeld || session.isBeforeAfterActive) {
        EditorState()
    } else {
        session.workingState
    }

    var sourcePreview by remember(imageModel) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var renderedPreview by remember(imageModel) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var histogram by remember(imageModel) { mutableStateOf<EditorHistogram?>(null) }

    LaunchedEffect(imageModel) {
        sourcePreview = null
        renderedPreview = null
        val source = imageModel ?: return@LaunchedEffect
        EditorPreviewRenderer.loadSource(context, source).onSuccess { loaded ->
            sourcePreview = loaded
        }
    }

    LaunchedEffect(sourcePreview, displayState) {
        val source = sourcePreview ?: return@LaunchedEffect
        renderedPreview = if (
            displayState.light.isNeutral &&
            displayState.color.isNeutral &&
            displayState.colorMix.isNeutral &&
            displayState.splitTone.isNeutral &&
            displayState.colorGrading.isNeutral &&
            displayState.hsl.isNeutral &&
            displayState.curves.isNeutral &&
            displayState.detail.isNeutral &&
            displayState.effects.isNeutral &&
            displayState.transform.isNeutral &&
            displayState.retouch.isNeutral &&
            displayState.overlays.isNeutral &&
            displayState.filter.isNeutral
        ) {
            source
        } else {
            delay(40)
            EditorPreviewRenderer.render(source, displayState)
        }
    }

    LaunchedEffect(sourcePreview, renderedPreview) {
        val bitmap = renderedPreview ?: sourcePreview
        histogram = if (bitmap == null) null else withContext(Dispatchers.Default) {
            val analysisContext = kotlinx.coroutines.currentCoroutineContext()
            EditorHistogramEngine.analyze(bitmap) { analysisContext.ensureActive() }
        }
    }

    val isShowingOriginal = isComparingHeld || session.isBeforeAfterActive
    val canvasImageModel = if (isShowingOriginal) {
        sourcePreview ?: imageModel
    } else {
        renderedPreview ?: sourcePreview ?: imageModel
    }
    val fallbackColorFilter = remember(displayState, renderedPreview) {
        if (renderedPreview == null && !isShowingOriginal) {
            ColorFilter.colorMatrix(EditorColorEngine.buildColorMatrix(displayState))
        } else {
            null
        }
    }

    fun selectTool(tool: AlphaPicsEditorTool) {
        session.cancelWorkingState()
        selectedTool = tool
    }

    fun performExport(format: ExportFormat, quality: Int) {
        val uri = imageModel as? Uri
        if (uri == null) {
            Toast.makeText(context, "No photo to export", Toast.LENGTH_SHORT).show()
            return
        }

        isExporting = true
        exportProgress = 0.05f
        exportResult = null
        exportError = null

        coroutineScope.launch {
            val result = EditorExportManager.exportImage(
                context = context,
                sourceUri = uri,
                state = session.state,
                format = format,
                quality = quality,
                onProgress = { exportProgress = it }
            )
            isExporting = false
            result.onSuccess {
                exportResult = it
            }.onFailure {
                exportError = it.localizedMessage ?: "Export failed. Please try again."
            }
        }
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
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = AlphaPicsSpacing.ScreenHorizontal)
                        .testTag("alphapics_editor_workspace")
                ) {
                    Spacer(Modifier.height(AlphaPicsSpacing.Xs))

                    // Enhanced Workspace Top Bar with Undo, Redo, Compare & Export
                    EditorTopBar(
                        canUndo = session.canUndo,
                        canRedo = session.canRedo,
                        isComparing = isComparingHeld || session.isBeforeAfterActive,
                        compareSource = compareSource,
                        onUndo = { session.undo() },
                        onRedo = { session.redo() },
                        onToggleCompare = { session.isBeforeAfterActive = !session.isBeforeAfterActive },
                        onBack = onBack,
                        onExport = {
                            if (imageModel != null) {
                                session.commitWorkingState(selectedTool.historyLabel)
                                showExportSheet = true
                            } else {
                                Toast.makeText(context, "Choose a photo first", Toast.LENGTH_SHORT).show()
                            }
                        },
                        hasImage = imageModel != null
                    )

                    Spacer(Modifier.height(AlphaPicsSpacing.Sm))

                    // Interactive Center Canvas
                    EditorCanvas(
                        imageModel = canvasImageModel,
                        colorFilter = fallbackColorFilter,
                        transform = displayState.transform,
                        retouch = displayState.retouch,
                        overlays = displayState.overlays,
                        retouchEnabled = selectedTool == AlphaPicsEditorTool.RETOUCH && !isShowingOriginal,
                        onRetouchStroke = { stroke ->
                            session.updateWorkingState { state ->
                                state.copy(retouch = state.retouch.append(stroke))
                            }
                        },
                        drawingEnabled = selectedTool == AlphaPicsEditorTool.TEXT &&
                            displayState.overlays.activeTool == OverlayToolMode.DRAW &&
                            !isShowingOriginal,
                        onDrawingStroke = { stroke ->
                            session.updateWorkingState { state ->
                                state.copy(overlays = state.overlays.addDrawing(stroke))
                            }
                        },
                        isProcessedPreview = renderedPreview != null && !isShowingOriginal,
                        showTransformGuides = selectedTool == AlphaPicsEditorTool.CROP,
                        isOriginalShown = isShowingOriginal,
                        onChoosePhoto = onChoosePhoto,
                        onOpenCamera = onOpenCamera,
                        onOpenCollage = onOpenCollage,
                        onOpenPhotoUtilities = onOpenPhotoUtilities,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .heightIn(min = 220.dp, max = 420.dp)
                    )

                    Spacer(Modifier.height(AlphaPicsSpacing.Xs))

                    // Dynamic Tool Context Inspector
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 152.dp, max = 248.dp)
                            .background(
                                AlphaPicsColors.SurfaceRaised.copy(alpha = 0.94f),
                                AlphaPicsShapes.Card
                            )
                            .border(1.dp, AlphaPicsColors.BorderSoft, AlphaPicsShapes.Card)
                            .padding(AlphaPicsSpacing.Md)
                            .testTag("editor_context_${selectedTool.id}")
                    ) {
                        EditorContextPanel(
                            selectedTool = selectedTool,
                            imageModel = imageModel,
                            workingState = session.workingState,
                            histogram = histogram,
                            historyEntries = session.historyEntries,
                            activeHistoryIndex = session.activeHistoryIndex,
                            canUndo = session.canUndo,
                            canRedo = session.canRedo,
                            onStateChange = { newState -> session.updateWorkingState { newState } },
                            onCancel = { session.cancelWorkingState() },
                            onApply = { session.commitWorkingState(selectedTool.historyLabel) },
                            onUndo = { session.undo() },
                            onRedo = { session.redo() },
                            onJumpToHistory = { session.jumpToHistory(it) },
                            onResetAll = { session.resetAll() },
                            onResetCategory = {
                                when (selectedTool) {
                                    AlphaPicsEditorTool.ADJUST -> {
                                        session.resetLight()
                                        session.resetColor()
                                        session.resetColorMix()
                                        session.resetSplitTone()
                                        session.resetColorGrading()
                                        session.resetHsl()
                                        session.resetCurves()
                                    }
                                    AlphaPicsEditorTool.DETAIL -> {
                                        session.resetDetail()
                                        session.resetEffects()
                                    }
                                    AlphaPicsEditorTool.CROP -> session.resetTransform()
                                    AlphaPicsEditorTool.RETOUCH -> session.resetRetouch()
                                    AlphaPicsEditorTool.TEXT -> session.resetOverlays()
                                    AlphaPicsEditorTool.FILTERS -> session.resetFilter()
                                    AlphaPicsEditorTool.HISTORY -> session.resetAll()
                                    else -> session.cancelWorkingState()
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(Modifier.height(AlphaPicsSpacing.Sm))

                    // Bottom Tool Rail
                    EditorToolRail(
                        selectedTool = selectedTool,
                        onToolSelected = ::selectTool,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(AlphaPicsSpacing.Xs))
                }
            }

            // Full Resolution Export Sheet
            if (showExportSheet) {
                EditorExportBottomSheet(
                    isExporting = isExporting,
                    progress = exportProgress,
                    exportResult = exportResult,
                    errorMessage = exportError,
                    onDismiss = {
                        showExportSheet = false
                        exportResult = null
                        exportError = null
                    },
                    onExport = { format, quality -> performExport(format, quality) },
                    onShare = { result ->
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = result.mimeType
                            putExtra(Intent.EXTRA_STREAM, result.uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Edited Photo"))
                    }
                )
            }
        }
    }
}

@Composable
private fun EditorTopBar(
    canUndo: Boolean,
    canRedo: Boolean,
    isComparing: Boolean,
    compareSource: MutableInteractionSource,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleCompare: () -> Unit,
    onBack: () -> Unit,
    onExport: () -> Unit,
    hasImage: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AlphaPicsColors.TextPrimary
                )
            }
            Spacer(Modifier.width(6.dp))
            Column {
                Text(
                    text = "Photo Editor",
                    color = AlphaPicsColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (hasImage) "Studio Session" else "Choose a photo",
                    color = AlphaPicsColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = UndoIcon,
                    contentDescription = "Undo",
                    tint = if (canUndo) AlphaPicsColors.TextPrimary else AlphaPicsColors.TextTertiary.copy(alpha = 0.4f)
                )
            }

            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = RedoIcon,
                    contentDescription = "Redo",
                    tint = if (canRedo) AlphaPicsColors.TextPrimary else AlphaPicsColors.TextTertiary.copy(alpha = 0.4f)
                )
            }

            // Before/After comparison hold button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isComparing) AlphaPicsColors.ElectricBlue.copy(alpha = 0.25f)
                        else AlphaPicsColors.Surface
                    )
                    .border(
                        1.dp,
                        if (isComparing) AlphaPicsColors.Cyan else AlphaPicsColors.BorderSoft,
                        CircleShape
                    )
                    .clickable(
                        interactionSource = compareSource,
                        indication = null,
                        onClick = onToggleCompare
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = VisibilityIcon,
                    contentDescription = "Compare Before/After",
                    tint = if (isComparing) AlphaPicsColors.Cyan else AlphaPicsColors.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(4.dp))

            // Export button
            Button(
                onClick = onExport,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlphaPicsColors.ElectricBlue,
                    contentColor = Color.White
                ),
                shape = AlphaPicsShapes.Pill,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(
                    imageVector = SaveDownloadIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Save",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EditorCanvas(
    imageModel: Any?,
    colorFilter: ColorFilter?,
    transform: TransformAdjustments,
    retouch: RetouchAdjustments,
    overlays: OverlayAdjustments,
    retouchEnabled: Boolean,
    onRetouchStroke: (RetouchStroke) -> Unit,
    drawingEnabled: Boolean,
    onDrawingStroke: (DrawOverlayStroke) -> Unit,
    isProcessedPreview: Boolean,
    showTransformGuides: Boolean,
    isOriginalShown: Boolean,
    onChoosePhoto: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenCollage: () -> Unit,
    onOpenPhotoUtilities: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imageAspect = (imageModel as? android.graphics.Bitmap)?.let { bitmap ->
        bitmap.width.toFloat() / bitmap.height.toFloat()
    }
    val currentOnRetouchStroke by rememberUpdatedState(onRetouchStroke)
    val currentOnDrawingStroke by rememberUpdatedState(onDrawingStroke)
    val gestureModifier = if (retouchEnabled && imageAspect != null) {
        Modifier
            .pointerInput(imageModel, retouch.activeMode, retouch.brushSize, retouch.brushFeather, retouch.brushStrength) {
                detectTapGestures { offset ->
                    retouchPointFromCanvasOffset(
                        offset,
                        Size(size.width.toFloat(), size.height.toFloat()),
                        imageAspect
                    )?.let { point ->
                        currentOnRetouchStroke(retouchStrokeFromSettings(retouch, listOf(point)))
                    }
                }
            }
            .pointerInput(imageModel, retouch.activeMode, retouch.brushSize, retouch.brushFeather, retouch.brushStrength) {
                var points = mutableListOf<RetouchPoint>()
                detectDragGestures(
                    onDragStart = { offset ->
                        points = mutableListOf()
                        retouchPointFromCanvasOffset(
                            offset,
                            Size(size.width.toFloat(), size.height.toFloat()),
                            imageAspect
                        )?.let(points::add)
                    },
                    onDragEnd = {
                        if (points.isNotEmpty()) {
                            currentOnRetouchStroke(retouchStrokeFromSettings(retouch, points))
                        }
                        points = mutableListOf()
                    },
                    onDragCancel = { points = mutableListOf() }
                ) { change, _ ->
                    retouchPointFromCanvasOffset(
                        change.position,
                        Size(size.width.toFloat(), size.height.toFloat()),
                        imageAspect
                    )?.let { point ->
                        if (points.isEmpty() || hypot(
                                points.last().x - point.x,
                                points.last().y - point.y
                            ) >= 0.004f
                        ) {
                            points.add(point)
                        }
                    }
                    change.consume()
                }
            }
    } else {
        Modifier
    }
    val drawingGestureModifier = if (drawingEnabled && imageAspect != null) {
        Modifier
            .pointerInput(
                imageModel,
                overlays.drawColorArgb,
                overlays.drawOpacity,
                overlays.drawSize,
                overlays.drawEraser
            ) {
                detectTapGestures { offset ->
                    retouchPointFromCanvasOffset(
                        offset,
                        Size(size.width.toFloat(), size.height.toFloat()),
                        imageAspect
                    )?.let { point ->
                        currentOnDrawingStroke(
                            DrawOverlayStroke(
                                points = listOf(point),
                                colorArgb = overlays.drawColorArgb,
                                opacity = overlays.drawOpacity,
                                size = overlays.drawSize,
                                eraser = overlays.drawEraser
                            )
                        )
                    }
                }
            }
            .pointerInput(
                imageModel,
                overlays.drawColorArgb,
                overlays.drawOpacity,
                overlays.drawSize,
                overlays.drawEraser
            ) {
                var points = mutableListOf<RetouchPoint>()
                detectDragGestures(
                    onDragStart = { offset ->
                        points = mutableListOf()
                        retouchPointFromCanvasOffset(
                            offset,
                            Size(size.width.toFloat(), size.height.toFloat()),
                            imageAspect
                        )?.let(points::add)
                    },
                    onDragEnd = {
                        if (points.isNotEmpty()) {
                            currentOnDrawingStroke(
                                DrawOverlayStroke(
                                    points = points,
                                    colorArgb = overlays.drawColorArgb,
                                    opacity = overlays.drawOpacity,
                                    size = overlays.drawSize,
                                    eraser = overlays.drawEraser
                                )
                            )
                        }
                        points = mutableListOf()
                    },
                    onDragCancel = { points = mutableListOf() }
                ) { change, _ ->
                    retouchPointFromCanvasOffset(
                        change.position,
                        Size(size.width.toFloat(), size.height.toFloat()),
                        imageAspect
                    )?.let { point ->
                        if (points.isEmpty() || hypot(
                                points.last().x - point.x,
                                points.last().y - point.y
                            ) >= 0.004f
                        ) {
                            points.add(point)
                        }
                    }
                    change.consume()
                }
            }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(AlphaPicsShapes.Hero)
            .clipToBounds()
            .background(Color(0xFF02040A))
            .border(1.dp, AlphaPicsColors.BorderFocus.copy(alpha = 0.68f), AlphaPicsShapes.Hero)
            .testTag("editor_canvas")
            .then(gestureModifier)
            .then(drawingGestureModifier),
        contentAlignment = Alignment.Center
    ) {
        if (imageModel == null) {
            Column(
                modifier = Modifier.padding(AlphaPicsSpacing.Lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Sm)
            ) {
                AlphaPicsPhotoEntry(
                    onChoosePhoto = onChoosePhoto,
                    onOpenCamera = onOpenCamera,
                    compact = true
                )
                Button(
                    onClick = onOpenCollage,
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .testTag("editor_open_collage"),
                    shape = AlphaPicsShapes.Medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlphaPicsColors.SurfaceRaised,
                        contentColor = AlphaPicsColors.Cyan
                    )
                ) {
                    Text("Create collage")
                }
                Button(
                    onClick = onOpenPhotoUtilities,
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .testTag("editor_open_photo_utilities"),
                    shape = AlphaPicsShapes.Medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlphaPicsColors.SurfaceRaised,
                        contentColor = AlphaPicsColors.Cyan
                    )
                ) {
                    Text("Resize, convert & info")
                }
            }
        } else {
            var loadState by remember(imageModel) {
                mutableStateOf(
                    if (imageModel is android.graphics.Bitmap) EditorPhotoLoadState.SUCCESS
                    else EditorPhotoLoadState.LOADING
                )
            }
            val previewModifier = Modifier
                .fillMaxSize()
                .testTag(if (isProcessedPreview) "editor_processed_preview" else "editor_source_preview")
            if (imageModel is android.graphics.Bitmap) {
                Image(
                    bitmap = imageModel.asImageBitmap(),
                    contentDescription = "Photo editor preview",
                    modifier = previewModifier,
                    contentScale = ContentScale.Fit,
                    colorFilter = colorFilter
                )
            } else {
                AsyncImage(
                    model = imageModel,
                    contentDescription = "Photo editor preview",
                    modifier = previewModifier,
                    contentScale = ContentScale.Fit,
                    colorFilter = colorFilter,
                    onLoading = { loadState = EditorPhotoLoadState.LOADING },
                    onSuccess = { loadState = EditorPhotoLoadState.SUCCESS },
                    onError = { loadState = EditorPhotoLoadState.ERROR }
                )
            }

            when (loadState) {
                EditorPhotoLoadState.LOADING -> AlphaPicsLoadingState(
                    title = "Opening photo",
                    description = "Preparing session preview.",
                    modifier = Modifier.padding(AlphaPicsSpacing.Lg)
                )

                EditorPhotoLoadState.ERROR -> AlphaPicsErrorState(
                    title = "This photo couldn’t be opened",
                    description = "Choose another photo. Your source file remains unchanged.",
                    actionLabel = "Choose another photo",
                    onAction = onChoosePhoto,
                    modifier = Modifier.padding(AlphaPicsSpacing.Lg)
                )

                EditorPhotoLoadState.SUCCESS -> {
                    val imageRatio = imageAspect
                    val cropRatio = transform.targetAspectRatio ?: imageRatio
                    val showCropGuide = showTransformGuides && (
                        transform.aspectId != "original" || !transform.cropRect.isFull ||
                            transform.grid != CropGrid.OFF
                        )
                    if (showCropGuide && cropRatio != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (cropRatio < 0.82f) 0.56f else 0.86f)
                                .aspectRatio(cropRatio)
                                .border(1.5.dp, Color.White.copy(alpha = 0.82f), AlphaPicsShapes.Small)
                                .testTag("crop_preview_frame")
                        ) {
                            if (transform.grid != CropGrid.OFF) {
                                Canvas(Modifier.fillMaxSize().testTag("crop_grid_overlay")) {
                                    val lineColor = Color.White.copy(alpha = 0.52f)
                                    when (transform.grid) {
                                        CropGrid.THIRDS -> {
                                            for (step in 1..2) {
                                                val fraction = step / 3f
                                                drawLine(lineColor, Offset(size.width * fraction, 0f), Offset(size.width * fraction, size.height), 1.dp.toPx())
                                                drawLine(lineColor, Offset(0f, size.height * fraction), Offset(size.width, size.height * fraction), 1.dp.toPx())
                                            }
                                        }
                                        CropGrid.SQUARE -> {
                                            for (step in 1..3) {
                                                val fraction = step / 4f
                                                drawLine(lineColor, Offset(size.width * fraction, 0f), Offset(size.width * fraction, size.height), 0.75.dp.toPx())
                                                drawLine(lineColor, Offset(0f, size.height * fraction), Offset(size.width, size.height * fraction), 0.75.dp.toPx())
                                            }
                                        }
                                        CropGrid.OFF -> Unit
                                    }
                                }
                            }
                        }
                    }

                    if (retouchEnabled && retouch.showMask && imageAspect != null) {
                        RetouchMaskOverlay(
                            retouch = retouch,
                            imageAspect = imageAspect,
                            modifier = Modifier.fillMaxSize().testTag("retouch_mask_overlay")
                        )
                    }

                    // Before/After indicator badge
                    AnimatedVisibility(
                        visible = isOriginalShown,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(AlphaPicsSpacing.Md)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.72f), AlphaPicsShapes.Pill)
                                .border(1.dp, AlphaPicsColors.Cyan.copy(alpha = 0.6f), AlphaPicsShapes.Pill)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "ORIGINAL",
                                color = AlphaPicsColors.Cyan,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Change photo pill button
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(AlphaPicsSpacing.Md)
                            .background(Color.Black.copy(alpha = 0.66f), AlphaPicsShapes.Pill)
                            .clickable(role = Role.Button, onClick = onChoosePhoto)
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "Change photo",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

private fun retouchPointFromCanvasOffset(
    offset: Offset,
    canvasSize: Size,
    imageAspect: Float
): RetouchPoint? {
    if (canvasSize.width <= 0f || canvasSize.height <= 0f || imageAspect <= 0f) return null
    val canvasAspect = canvasSize.width / canvasSize.height
    val imageWidth: Float
    val imageHeight: Float
    if (imageAspect >= canvasAspect) {
        imageWidth = canvasSize.width
        imageHeight = imageWidth / imageAspect
    } else {
        imageHeight = canvasSize.height
        imageWidth = imageHeight * imageAspect
    }
    val left = (canvasSize.width - imageWidth) / 2f
    val top = (canvasSize.height - imageHeight) / 2f
    if (offset.x < left || offset.x > left + imageWidth || offset.y < top || offset.y > top + imageHeight) {
        return null
    }
    return RetouchPoint(
        x = ((offset.x - left) / imageWidth).coerceIn(0f, 1f),
        y = ((offset.y - top) / imageHeight).coerceIn(0f, 1f)
    )
}

private fun retouchStrokeFromSettings(
    retouch: RetouchAdjustments,
    points: List<RetouchPoint>
): RetouchStroke = RetouchStroke(
    mode = retouch.activeMode,
    points = points,
    size = retouch.brushSize,
    feather = retouch.brushFeather,
    strength = retouch.brushStrength,
    cloneSourceOffsetX = retouch.cloneSourceOffsetX,
    cloneSourceOffsetY = retouch.cloneSourceOffsetY
)

@Composable
private fun RetouchMaskOverlay(
    retouch: RetouchAdjustments,
    imageAspect: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val canvasAspect = size.width / size.height
        val imageWidth: Float
        val imageHeight: Float
        if (imageAspect >= canvasAspect) {
            imageWidth = size.width
            imageHeight = imageWidth / imageAspect
        } else {
            imageHeight = size.height
            imageWidth = imageHeight * imageAspect
        }
        val left = (size.width - imageWidth) / 2f
        val top = (size.height - imageHeight) / 2f
        val minimumDimension = minOf(imageWidth, imageHeight)
        retouch.strokes.forEach { stroke ->
            if (stroke.points.isEmpty()) return@forEach
            val color = if (stroke.mode == LocalRetouchMode.ERASE_MASK) {
                Color.Black.copy(alpha = 0.62f)
            } else {
                AlphaPicsColors.Cyan.copy(alpha = 0.34f + stroke.strength / 100f * 0.22f)
            }
            val width = (stroke.size / 100f * minimumDimension).coerceAtLeast(2.dp.toPx())
            if (stroke.points.size == 1) {
                val point = stroke.points.first()
                drawCircle(
                    color = color,
                    radius = width / 2f,
                    center = Offset(left + point.x * imageWidth, top + point.y * imageHeight)
                )
            } else {
                val path = Path().apply {
                    val first = stroke.points.first()
                    moveTo(left + first.x * imageWidth, top + first.y * imageHeight)
                    stroke.points.drop(1).forEach { point ->
                        lineTo(left + point.x * imageWidth, top + point.y * imageHeight)
                    }
                }
                drawPath(
                    path,
                    color = color.copy(alpha = color.alpha * 0.42f),
                    style = Stroke(width = width * 1.36f, cap = StrokeCap.Round)
                )
                drawPath(path, color = color, style = Stroke(width = width, cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
private fun EditorContextPanel(
    selectedTool: AlphaPicsEditorTool,
    imageModel: Any?,
    workingState: EditorState,
    histogram: EditorHistogram?,
    historyEntries: List<EditorHistoryEntry>,
    activeHistoryIndex: Int,
    canUndo: Boolean,
    canRedo: Boolean,
    onStateChange: (EditorState) -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onJumpToHistory: (Long) -> Unit,
    onResetAll: () -> Unit,
    onResetCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (selectedTool) {
        AlphaPicsEditorTool.ADJUST -> AdjustPanel(
            light = workingState.light,
            color = workingState.color,
            colorMix = workingState.colorMix,
            splitTone = workingState.splitTone,
            colorGrading = workingState.colorGrading,
            hsl = workingState.hsl,
            curves = workingState.curves,
            histogram = histogram,
            onLightChange = { onStateChange(workingState.copy(light = it)) },
            onColorChange = { onStateChange(workingState.copy(color = it)) },
            onColorMixChange = { onStateChange(workingState.copy(colorMix = it)) },
            onSplitToneChange = { onStateChange(workingState.copy(splitTone = it)) },
            onColorGradingChange = { onStateChange(workingState.copy(colorGrading = it)) },
            onHslChange = { onStateChange(workingState.copy(hsl = it)) },
            onCurvesChange = { onStateChange(workingState.copy(curves = it)) },
            onReset = onResetCategory,
            onCancel = onCancel,
            onApply = onApply,
            enabled = imageModel != null,
            modifier = modifier
        )

        AlphaPicsEditorTool.FILTERS -> FilterPresetsPanel(
            imageModel = imageModel,
            state = workingState,
            onStateChange = onStateChange,
            onCancel = onCancel,
            onApply = onApply,
            modifier = modifier
        )

        AlphaPicsEditorTool.CROP -> TransformPanel(
            transform = workingState.transform,
            onChange = { onStateChange(workingState.copy(transform = it)) },
            onReset = onResetCategory,
            onCancel = onCancel,
            onApply = onApply,
            enabled = imageModel != null,
            modifier = modifier
        )

        AlphaPicsEditorTool.RETOUCH -> RetouchPanel(
            retouch = workingState.retouch,
            onChange = { onStateChange(workingState.copy(retouch = it)) },
            onReset = onResetCategory,
            onCancel = onCancel,
            onApply = onApply,
            enabled = imageModel != null,
            modifier = modifier
        )

        AlphaPicsEditorTool.REMOVE -> UnavailableEditorPanel(
            title = "Remove · Magic Eraser",
            description = "Object removal is not available yet. No brush mask, removal result or processing progress is simulated.",
            modifier = modifier
        )

        AlphaPicsEditorTool.BACKGROUND -> UnavailableEditorPanel(
            title = "Background",
            description = "Background removal and replacement are coming soon. Your original background remains untouched.",
            modifier = modifier
        )

        AlphaPicsEditorTool.DETAIL -> DetailAdjustPanel(
            detail = workingState.detail,
            effects = workingState.effects,
            onDetailChange = { onStateChange(workingState.copy(detail = it)) },
            onEffectsChange = { onStateChange(workingState.copy(effects = it)) },
            onReset = onResetCategory,
            onCancel = onCancel,
            onApply = onApply,
            enabled = imageModel != null,
            modifier = modifier
        )

        AlphaPicsEditorTool.TEXT -> OverlayPanel(
            overlays = workingState.overlays,
            onChange = { onStateChange(workingState.copy(overlays = it)) },
            onReset = onResetCategory,
            onCancel = onCancel,
            onApply = onApply,
            enabled = imageModel != null,
            modifier = modifier
        )

        AlphaPicsEditorTool.HISTORY -> OperationHistoryPanel(
            entries = historyEntries,
            activeIndex = activeHistoryIndex,
            canUndo = canUndo,
            canRedo = canRedo,
            onUndo = onUndo,
            onRedo = onRedo,
            onJumpToHistory = onJumpToHistory,
            onResetAll = onResetAll,
            modifier = modifier
        )
    }
}

@Composable
private fun AdjustPanel(
    light: LightAdjustments,
    color: ColorAdjustments,
    colorMix: ColorMixAdjustments,
    splitTone: SplitToneAdjustments,
    colorGrading: ColorGradingAdjustments,
    hsl: HslAdjustments,
    curves: CurvesAdjustments,
    histogram: EditorHistogram?,
    onLightChange: (LightAdjustments) -> Unit,
    onColorChange: (ColorAdjustments) -> Unit,
    onColorMixChange: (ColorMixAdjustments) -> Unit,
    onSplitToneChange: (SplitToneAdjustments) -> Unit,
    onColorGradingChange: (ColorGradingAdjustments) -> Unit,
    onHslChange: (HslAdjustments) -> Unit,
    onCurvesChange: (CurvesAdjustments) -> Unit,
    onReset: () -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (!enabled) {
        AlphaPicsAvailabilityCard(
            title = "Adjust",
            description = "Choose a photo to adjust tone, color, HSL channels and curves.",
            badge = "Choose photo",
            accent = AlphaPicsColors.BrightBlue,
            modifier = modifier
        )
        return
    }

    var adjustSubTab by remember { mutableStateOf("light") }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EditorChoiceChip(
                    label = "Light",
                    selected = adjustSubTab == "light",
                    onClick = { adjustSubTab = "light" },
                    modifier = Modifier.testTag("editor_adjust_tab_light")
                )
                EditorChoiceChip(
                    label = "Color",
                    selected = adjustSubTab == "color",
                    onClick = { adjustSubTab = "color" },
                    modifier = Modifier.testTag("editor_adjust_tab_color")
                )
                EditorChoiceChip(
                    label = "HSL",
                    selected = adjustSubTab == "hsl",
                    onClick = { adjustSubTab = "hsl" },
                    modifier = Modifier.testTag("editor_adjust_tab_hsl")
                )
                EditorChoiceChip(
                    label = "Curves",
                    selected = adjustSubTab == "curves",
                    onClick = { adjustSubTab = "curves" },
                    modifier = Modifier.testTag("editor_adjust_tab_curves")
                )
                EditorChoiceChip(
                    label = "Mix",
                    selected = adjustSubTab == "mix",
                    onClick = { adjustSubTab = "mix" },
                    modifier = Modifier.testTag("editor_adjust_tab_mix")
                )
                EditorChoiceChip(
                    label = "Split",
                    selected = adjustSubTab == "split",
                    onClick = { adjustSubTab = "split" },
                    modifier = Modifier.testTag("editor_adjust_tab_split")
                )
                EditorChoiceChip(
                    label = "Grade",
                    selected = adjustSubTab == "grade",
                    onClick = { adjustSubTab = "grade" },
                    modifier = Modifier.testTag("editor_adjust_tab_grade")
                )
                EditorChoiceChip(
                    label = "Histogram",
                    selected = adjustSubTab == "histogram",
                    onClick = { adjustSubTab = "histogram" },
                    modifier = Modifier.testTag("editor_adjust_tab_histogram")
                )
            }
            Text(
                text = "Reset",
                color = AlphaPicsColors.Cyan,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .clickable(role = Role.Button, onClick = onReset)
                    .padding(6.dp)
            )
        }

        Spacer(Modifier.height(AlphaPicsSpacing.Xs))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)
        ) {
        when (adjustSubTab) {
            "light" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Md)
            ) {
                AlphaPicsValueSlider(
                    label = "Exposure",
                    value = light.exposure,
                    onValueChange = { onLightChange(light.copy(exposure = it)) },
                    modifier = Modifier.weight(1f)
                )
                AlphaPicsValueSlider(
                    label = "Contrast",
                    value = light.contrast,
                    onValueChange = { onLightChange(light.copy(contrast = it)) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Md)
            ) {
                AlphaPicsValueSlider(
                    label = "Brightness",
                    value = light.brightness,
                    onValueChange = { onLightChange(light.copy(brightness = it)) },
                    modifier = Modifier.weight(1f)
                )
                AlphaPicsValueSlider(
                    label = "Highlights",
                    value = light.highlights,
                    onValueChange = { onLightChange(light.copy(highlights = it)) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Md)
            ) {
                AlphaPicsValueSlider(
                    label = "Shadows",
                    value = light.shadows,
                    onValueChange = { onLightChange(light.copy(shadows = it)) },
                    modifier = Modifier.weight(1f)
                )
                AlphaPicsValueSlider(
                    label = "Whites",
                    value = light.whites,
                    onValueChange = { onLightChange(light.copy(whites = it)) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Md)
            ) {
                AlphaPicsValueSlider(
                    label = "Blacks",
                    value = light.blacks,
                    onValueChange = { onLightChange(light.copy(blacks = it)) },
                    modifier = Modifier.weight(1f)
                )
                AlphaPicsValueSlider(
                    label = "Gamma",
                    value = light.gamma,
                    onValueChange = { onLightChange(light.copy(gamma = it)) },
                    modifier = Modifier.weight(1f)
                )
            }
            }
            "color" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Md)
            ) {
                AlphaPicsValueSlider(
                    label = "Saturation",
                    value = color.saturation,
                    onValueChange = { onColorChange(color.copy(saturation = it)) },
                    modifier = Modifier.weight(1f)
                )
                AlphaPicsValueSlider(
                    label = "Temperature",
                    value = color.warmth,
                    onValueChange = { onColorChange(color.copy(warmth = it)) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Md)
            ) {
                AlphaPicsValueSlider(
                    label = "Vibrance",
                    value = color.vibrance,
                    onValueChange = { onColorChange(color.copy(vibrance = it)) },
                    modifier = Modifier.weight(1f)
                )
                AlphaPicsValueSlider(
                    label = "Tint",
                    value = color.tint,
                    onValueChange = { onColorChange(color.copy(tint = it)) },
                    modifier = Modifier.weight(1f)
                )
            }
            }
            "hsl" -> HslAdjustPanel(hsl = hsl, onChange = onHslChange)
            "curves" -> CurvesAdjustPanel(curves = curves, onChange = onCurvesChange)
            "mix" -> ColorMixPanel(colorMix = colorMix, onChange = onColorMixChange)
            "split" -> SplitTonePanel(splitTone = splitTone, onChange = onSplitToneChange)
            "grade" -> ColorGradingPanel(
                colorGrading = colorGrading,
                onChange = onColorGradingChange
            )
            else -> HistogramPanel(histogram = histogram)
        }
        }

        Spacer(Modifier.height(AlphaPicsSpacing.Xs))
        AlphaPicsContextActions(onCancel = onCancel, onApply = onApply)
    }
}

@Composable
private fun HistogramPanel(
    histogram: EditorHistogram?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("editor_histogram"),
        verticalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Luminance + RGB",
                color = AlphaPicsColors.TextPrimary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = histogram?.let { "%,d samples".format(Locale.US, it.sampledPixels) }
                    ?: "Analyzing preview",
                color = AlphaPicsColors.TextTertiary,
                style = MaterialTheme.typography.labelSmall
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .clip(AlphaPicsShapes.Medium)
                .background(AlphaPicsColors.Surface.copy(alpha = 0.72f))
                .border(1.dp, AlphaPicsColors.BorderSoft, AlphaPicsShapes.Medium)
                .padding(horizontal = 8.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            if (histogram == null) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = AlphaPicsColors.Cyan,
                    strokeWidth = 2.dp
                )
            } else {
                Canvas(Modifier.fillMaxSize().testTag("editor_histogram_chart")) {
                    val chartWidth = size.width
                    val chartHeight = size.height

                    for (index in 1..3) {
                        val x = chartWidth * index / 4f
                        drawLine(
                            color = AlphaPicsColors.BorderSoft.copy(alpha = 0.55f),
                            start = Offset(x, 0f),
                            end = Offset(x, chartHeight),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    fun pathFor(values: List<Int>, closeToBaseline: Boolean): Path {
                        val normalized = histogram.normalized(values)
                        return Path().apply {
                            if (closeToBaseline) moveTo(0f, chartHeight)
                            normalized.forEachIndexed { index, value ->
                                val x = index.toFloat() / (EditorHistogram.BIN_COUNT - 1) * chartWidth
                                val y = chartHeight - value * chartHeight
                                if (index == 0 && !closeToBaseline) moveTo(x, y) else lineTo(x, y)
                            }
                            if (closeToBaseline) {
                                lineTo(chartWidth, chartHeight)
                                close()
                            }
                        }
                    }

                    drawPath(
                        path = pathFor(histogram.luminance, closeToBaseline = true),
                        color = Color.White.copy(alpha = 0.10f)
                    )
                    drawPath(
                        path = pathFor(histogram.luminance, closeToBaseline = false),
                        color = Color.White.copy(alpha = 0.72f),
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                    listOf(
                        histogram.red to Color(0xFFFF5263),
                        histogram.green to Color(0xFF45E08C),
                        histogram.blue to AlphaPicsColors.Cyan
                    ).forEach { (values, color) ->
                        drawPath(
                            path = pathFor(values, closeToBaseline = false),
                            color = color.copy(alpha = 0.82f),
                            style = Stroke(width = 1.35.dp.toPx())
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(
                "Luma" to Color.White,
                "Red" to Color(0xFFFF5263),
                "Green" to Color(0xFF45E08C),
                "Blue" to AlphaPicsColors.Cyan
            ).forEach { (label, color) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(Modifier.size(6.dp).background(color, CircleShape))
                    Text(label, color = AlphaPicsColors.TextTertiary, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun ColorMixPanel(
    colorMix: ColorMixAdjustments,
    onChange: (ColorMixAdjustments) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Md)
    ) {
        AlphaPicsValueSlider(
            label = "Red",
            value = colorMix.red,
            onValueChange = { onChange(colorMix.copy(red = it)) },
            modifier = Modifier.weight(1f)
        )
        AlphaPicsValueSlider(
            label = "Green",
            value = colorMix.green,
            onValueChange = { onChange(colorMix.copy(green = it)) },
            modifier = Modifier.weight(1f)
        )
        AlphaPicsValueSlider(
            label = "Blue",
            value = colorMix.blue,
            onValueChange = { onChange(colorMix.copy(blue = it)) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SplitTonePanel(
    splitTone: SplitToneAdjustments,
    onChange: (SplitToneAdjustments) -> Unit
) {
    var editingShadows by remember { mutableStateOf(true) }
    val hue = if (editingShadows) splitTone.shadowHue else splitTone.highlightHue
    val saturation = if (editingShadows) {
        splitTone.shadowSaturation
    } else {
        splitTone.highlightSaturation
    }

    Column(verticalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)) {
        Row(horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)) {
            EditorChoiceChip(
                label = "Shadows",
                selected = editingShadows,
                onClick = { editingShadows = true }
            )
            EditorChoiceChip(
                label = "Highlights",
                selected = !editingShadows,
                onClick = { editingShadows = false }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Md)
        ) {
            AlphaPicsValueSlider(
                label = "Hue",
                value = hue,
                valueRange = 0f..360f,
                onValueChange = { value ->
                    onChange(
                        if (editingShadows) splitTone.copy(shadowHue = value)
                        else splitTone.copy(highlightHue = value)
                    )
                },
                modifier = Modifier.weight(1f)
            )
            AlphaPicsValueSlider(
                label = "Saturation",
                value = saturation,
                valueRange = 0f..100f,
                onValueChange = { value ->
                    onChange(
                        if (editingShadows) splitTone.copy(shadowSaturation = value)
                        else splitTone.copy(highlightSaturation = value)
                    )
                },
                modifier = Modifier.weight(1f)
            )
            AlphaPicsValueSlider(
                label = "Balance",
                value = splitTone.balance,
                onValueChange = { onChange(splitTone.copy(balance = it)) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ColorGradingPanel(
    colorGrading: ColorGradingAdjustments,
    onChange: (ColorGradingAdjustments) -> Unit
) {
    var selectedRegion by remember { mutableStateOf(ColorGradeRegion.MIDTONES) }
    val range = colorGrading[selectedRegion]

    Column(verticalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)
        ) {
            ColorGradeRegion.entries.forEach { region ->
                EditorChoiceChip(
                    label = region.label,
                    selected = selectedRegion == region,
                    onClick = { selectedRegion = region }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Md)
        ) {
            AlphaPicsValueSlider(
                label = "Hue",
                value = range.hue,
                valueRange = 0f..360f,
                onValueChange = { value ->
                    onChange(colorGrading.update(selectedRegion, range.copy(hue = value)))
                },
                modifier = Modifier.weight(1f)
            )
            AlphaPicsValueSlider(
                label = "Saturation",
                value = range.saturation,
                valueRange = 0f..100f,
                onValueChange = { value ->
                    onChange(colorGrading.update(selectedRegion, range.copy(saturation = value)))
                },
                modifier = Modifier.weight(1f)
            )
            AlphaPicsValueSlider(
                label = "Luminance",
                value = range.luminance,
                onValueChange = { value ->
                    onChange(colorGrading.update(selectedRegion, range.copy(luminance = value)))
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HslAdjustPanel(
    hsl: HslAdjustments,
    onChange: (HslAdjustments) -> Unit
) {
    var selectedChannel by remember { mutableStateOf(HslColorChannel.RED) }
    val adjustment = hsl[selectedChannel]

    Column(verticalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)
        ) {
            HslColorChannel.entries.forEach { channel ->
                EditorChoiceChip(
                    label = channel.label,
                    selected = selectedChannel == channel,
                    onClick = { selectedChannel = channel }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Md)
        ) {
            AlphaPicsValueSlider(
                label = "Hue",
                value = adjustment.hue,
                onValueChange = { value ->
                    onChange(hsl.update(selectedChannel, adjustment.copy(hue = value)))
                },
                modifier = Modifier.weight(1f)
            )
            AlphaPicsValueSlider(
                label = "Saturation",
                value = adjustment.saturation,
                onValueChange = { value ->
                    onChange(hsl.update(selectedChannel, adjustment.copy(saturation = value)))
                },
                modifier = Modifier.weight(1f)
            )
            AlphaPicsValueSlider(
                label = "Luminance",
                value = adjustment.luminance,
                onValueChange = { value ->
                    onChange(hsl.update(selectedChannel, adjustment.copy(luminance = value)))
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CurvesAdjustPanel(
    curves: CurvesAdjustments,
    onChange: (CurvesAdjustments) -> Unit
) {
    var selectedChannel by remember { mutableStateOf(CurveChannel.MASTER) }
    val curve = curves[selectedChannel]

    Column(verticalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)
            ) {
                CurveChannel.entries.forEach { channel ->
                    EditorChoiceChip(
                        label = channel.label,
                        selected = selectedChannel == channel,
                        onClick = { selectedChannel = channel }
                    )
                }
            }
            Text(
                text = "Reset curve",
                color = AlphaPicsColors.Cyan,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .clickable(role = Role.Button) {
                        onChange(curves.update(selectedChannel, CurveEngine.reset()))
                    }
                    .padding(6.dp)
            )
        }
        CurveEditor(
            curve = curve,
            channel = selectedChannel,
            onCurveChange = { onChange(curves.update(selectedChannel, it)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .testTag("editor_curve_${selectedChannel.name.lowercase(Locale.ROOT)}")
        )
    }
}

@Composable
private fun CurveEditor(
    curve: ToneCurve,
    channel: CurveChannel,
    onCurveChange: (ToneCurve) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentCurve by rememberUpdatedState(curve)
    val currentOnCurveChange by rememberUpdatedState(onCurveChange)
    val sanitized = remember(curve) { CurveEngine.sanitize(curve) }
    val lut = remember(sanitized) { CurveEngine.buildLut(sanitized) }
    val accent = when (channel) {
        CurveChannel.MASTER -> AlphaPicsColors.Cyan
        CurveChannel.RED -> Color(0xFFFF657B)
        CurveChannel.GREEN -> Color(0xFF4DDA91)
        CurveChannel.BLUE -> AlphaPicsColors.BrightBlue
    }

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.36f))
            .border(1.dp, AlphaPicsColors.BorderSoft, RoundedCornerShape(12.dp))
            .semantics {
                contentDescription = "Tone curve. Tap to add, drag a point, double-tap to delete."
            }
            .pointerInput(Unit) {
                val plotInset = 8.dp.toPx()
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val index = nearestCurvePointIndex(
                            currentCurve,
                            offset,
                            Size(size.width.toFloat(), size.height.toFloat()),
                            plotInset = plotInset
                        )
                        if (index != null && index > 0 && index < currentCurve.points.lastIndex) {
                            currentOnCurveChange(CurveEngine.deletePoint(currentCurve, index))
                        }
                    },
                    onTap = { offset ->
                        val point = curvePointFromOffset(
                            offset,
                            Size(size.width.toFloat(), size.height.toFloat()),
                            plotInset
                        )
                        currentOnCurveChange(
                            CurveEngine.addPoint(
                                currentCurve,
                                point.x,
                                point.y
                            )
                        )
                    }
                )
            }
            .pointerInput(Unit) {
                val plotInset = 8.dp.toPx()
                var activeIndex = -1
                detectDragGestures(
                    onDragStart = { offset ->
                        val existingIndex = nearestCurvePointIndex(
                            currentCurve,
                            offset,
                            Size(size.width.toFloat(), size.height.toFloat()),
                            maximumDistance = 0.12f,
                            plotInset = plotInset
                        )
                        val nextCurve = if (existingIndex == null) {
                            val point = curvePointFromOffset(
                                offset,
                                Size(size.width.toFloat(), size.height.toFloat()),
                                plotInset
                            )
                            CurveEngine.addPoint(
                                currentCurve,
                                point.x,
                                point.y
                            )
                        } else {
                            currentCurve
                        }
                        activeIndex = existingIndex ?: nearestCurvePointIndex(
                            nextCurve,
                            offset,
                            Size(size.width.toFloat(), size.height.toFloat()),
                            plotInset = plotInset
                        ) ?: -1
                        if (nextCurve != currentCurve) currentOnCurveChange(nextCurve)
                    },
                    onDragEnd = { activeIndex = -1 },
                    onDragCancel = { activeIndex = -1 }
                ) { change, _ ->
                    if (activeIndex >= 0) {
                        change.consume()
                        val point = curvePointFromOffset(
                            change.position,
                            Size(size.width.toFloat(), size.height.toFloat()),
                            plotInset
                        )
                        currentOnCurveChange(
                            CurveEngine.movePoint(
                                currentCurve,
                                activeIndex,
                                point.x,
                                point.y
                            )
                        )
                    }
                }
            }
    ) {
        val plotInset = 8.dp.toPx()
        val plotWidth = (size.width - 2f * plotInset).coerceAtLeast(1f)
        val plotHeight = (size.height - 2f * plotInset).coerceAtLeast(1f)
        val gridColor = AlphaPicsColors.BorderSoft.copy(alpha = 0.55f)
        for (step in 1..3) {
            val fraction = step / 4f
            val gridX = plotInset + plotWidth * fraction
            val gridY = plotInset + plotHeight * fraction
            drawLine(gridColor, Offset(gridX, plotInset), Offset(gridX, plotInset + plotHeight))
            drawLine(gridColor, Offset(plotInset, gridY), Offset(plotInset + plotWidth, gridY))
        }

        val curvePath = Path().apply {
            moveTo(plotInset, plotInset + plotHeight - lut[0] / 255f * plotHeight)
            for (input in 1..255) {
                lineTo(
                    plotInset + input / 255f * plotWidth,
                    plotInset + plotHeight - lut[input] / 255f * plotHeight
                )
            }
        }
        drawPath(curvePath, color = accent, style = Stroke(width = 2.5.dp.toPx()))
        sanitized.points.forEach { point ->
            val center = Offset(
                plotInset + point.x * plotWidth,
                plotInset + (1f - point.y) * plotHeight
            )
            drawCircle(Color.Black.copy(alpha = 0.88f), radius = 6.dp.toPx(), center = center)
            drawCircle(accent, radius = 4.dp.toPx(), center = center)
        }
    }
}

private fun nearestCurvePointIndex(
    curve: ToneCurve,
    offset: Offset,
    size: Size,
    maximumDistance: Float = Float.POSITIVE_INFINITY,
    plotInset: Float = 0f
): Int? {
    if (size.width <= 0f || size.height <= 0f) return null
    val normalized = curvePointFromOffset(offset, size, plotInset)
    val index = curve.points.indices.minByOrNull { pointIndex ->
        val point = curve.points[pointIndex]
        hypot(point.x - normalized.x, point.y - normalized.y)
    } ?: return null
    val point = curve.points[index]
    val distance = hypot(point.x - normalized.x, point.y - normalized.y)
    return index.takeIf { distance <= maximumDistance }
}

private fun curvePointFromOffset(offset: Offset, size: Size, plotInset: Float): CurvePoint {
    val width = (size.width - 2f * plotInset).coerceAtLeast(1f)
    val height = (size.height - 2f * plotInset).coerceAtLeast(1f)
    return CurvePoint(
        x = ((offset.x - plotInset) / width).coerceIn(0f, 1f),
        y = (1f - (offset.y - plotInset) / height).coerceIn(0f, 1f)
    )
}

@Composable
private fun DetailAdjustPanel(
    detail: DetailAdjustments,
    effects: EffectAdjustments,
    onDetailChange: (DetailAdjustments) -> Unit,
    onEffectsChange: (EffectAdjustments) -> Unit,
    onReset: () -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (!enabled) {
        AlphaPicsAvailabilityCard(
            title = "Detail & Effects",
            description = "Choose a photo to refine detail, reduce noise, add finish effects and apply local blur.",
            badge = "Choose photo",
            accent = AlphaPicsColors.Cyan,
            modifier = modifier
        )
        return
    }

    var detailSubTab by remember { mutableStateOf("detail") }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)) {
                EditorChoiceChip(
                    label = "Detail",
                    selected = detailSubTab == "detail",
                    onClick = { detailSubTab = "detail" },
                    modifier = Modifier.testTag("editor_detail_tab_detail")
                )
                EditorChoiceChip(
                    label = "Effects",
                    selected = detailSubTab == "effects",
                    onClick = { detailSubTab = "effects" },
                    modifier = Modifier.testTag("editor_detail_tab_effects")
                )
            }
            Text(
                text = "Reset",
                color = AlphaPicsColors.Cyan,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .clickable(role = Role.Button, onClick = onReset)
                    .padding(6.dp)
            )
        }

        Spacer(Modifier.height(AlphaPicsSpacing.Xs))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)
        ) {
            if (detailSubTab == "detail") {
                DetailTripleSliderRow(
                    firstLabel = "Sharpen",
                    firstValue = detail.sharpen,
                    firstRange = 0f..100f,
                    onFirstChange = { onDetailChange(detail.copy(sharpen = it)) },
                    secondLabel = "Structure",
                    secondValue = detail.structure,
                    onSecondChange = { onDetailChange(detail.copy(structure = it)) },
                    thirdLabel = "Clarity",
                    thirdValue = detail.clarity,
                    onThirdChange = { onDetailChange(detail.copy(clarity = it)) }
                )
                DetailTripleSliderRow(
                    firstLabel = "Texture",
                    firstValue = detail.texture,
                    onFirstChange = { onDetailChange(detail.copy(texture = it)) },
                    secondLabel = "Denoise",
                    secondValue = detail.noiseReduction,
                    secondRange = 0f..100f,
                    onSecondChange = { onDetailChange(detail.copy(noiseReduction = it)) },
                    thirdLabel = "Dehaze",
                    thirdValue = detail.dehaze,
                    onThirdChange = { onDetailChange(detail.copy(dehaze = it)) }
                )
            } else {
                DetailTripleSliderRow(
                    firstLabel = "Vignette",
                    firstValue = effects.vignette,
                    onFirstChange = { onEffectsChange(effects.copy(vignette = it)) },
                    secondLabel = "Grain",
                    secondValue = effects.grain,
                    secondRange = 0f..100f,
                    onSecondChange = { onEffectsChange(effects.copy(grain = it)) },
                    thirdLabel = "Fade",
                    thirdValue = effects.fade,
                    thirdRange = 0f..100f,
                    onThirdChange = { onEffectsChange(effects.copy(fade = it)) }
                )
                DetailTripleSliderRow(
                    firstLabel = "Gaussian",
                    firstValue = effects.gaussianBlur,
                    firstRange = 0f..100f,
                    onFirstChange = { onEffectsChange(effects.copy(gaussianBlur = it)) },
                    secondLabel = "Focus",
                    secondValue = effects.focusBlur,
                    secondRange = 0f..100f,
                    onSecondChange = { onEffectsChange(effects.copy(focusBlur = it)) },
                    thirdLabel = "Radial",
                    thirdValue = effects.radialBlur,
                    thirdRange = 0f..100f,
                    onThirdChange = { onEffectsChange(effects.copy(radialBlur = it)) }
                )
            }
        }

        Spacer(Modifier.height(AlphaPicsSpacing.Xs))
        AlphaPicsContextActions(onCancel = onCancel, onApply = onApply)
    }
}

@Composable
private fun DetailTripleSliderRow(
    firstLabel: String,
    firstValue: Float,
    onFirstChange: (Float) -> Unit,
    secondLabel: String,
    secondValue: Float,
    onSecondChange: (Float) -> Unit,
    thirdLabel: String,
    thirdValue: Float,
    onThirdChange: (Float) -> Unit,
    firstRange: ClosedFloatingPointRange<Float> = -100f..100f,
    secondRange: ClosedFloatingPointRange<Float> = -100f..100f,
    thirdRange: ClosedFloatingPointRange<Float> = -100f..100f
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Md)
    ) {
        AlphaPicsValueSlider(
            label = firstLabel,
            value = firstValue,
            onValueChange = onFirstChange,
            valueRange = firstRange,
            modifier = Modifier.weight(1f)
        )
        AlphaPicsValueSlider(
            label = secondLabel,
            value = secondValue,
            onValueChange = onSecondChange,
            valueRange = secondRange,
            modifier = Modifier.weight(1f)
        )
        AlphaPicsValueSlider(
            label = thirdLabel,
            value = thirdValue,
            onValueChange = onThirdChange,
            valueRange = thirdRange,
            modifier = Modifier.weight(1f)
        )
    }
}

private data class EditorPresetDisplay(
    val preset: FilterPreset,
    val custom: EditorCustomPreset? = null
)

@Composable
private fun FilterPresetsPanel(
    imageModel: Any?,
    state: EditorState,
    onStateChange: (EditorState) -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (imageModel == null) {
        AlphaPicsAvailabilityCard(
            title = "Filters",
            description = "Choose a photo to preview deterministic local color filters.",
            badge = "Choose photo",
            accent = AlphaPicsColors.Violet,
            modifier = modifier
        )
        return
    }

    val context = LocalContext.current
    var library by remember { mutableStateOf(EditorPresetStore.load(context)) }
    var selectedLibraryId by remember { mutableStateOf(state.filter.presetId) }
    var customIntensity by remember { mutableFloatStateOf(100f) }
    var showSaveForm by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("My look") }

    val displays = remember(library) {
        val builtIns = FilterPresetCatalog.Presets.map(::EditorPresetDisplay)
        val custom = library.customPresets.map { EditorPresetDisplay(it.previewPreset(), it) }
        (builtIns + custom).sortedByDescending { it.preset.id in library.favoriteIds }
    }
    val selectedDisplay = displays.firstOrNull { it.preset.id == selectedLibraryId }
        ?: displays.first()
    val selectedCustom = selectedDisplay.custom
    val intensity = if (selectedCustom != null) customIntensity else state.filter.intensity
    val isFavorite = selectedDisplay.preset.id in library.favoriteIds

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Presets",
                color = AlphaPicsColors.TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = if (isFavorite) "★" else "☆",
                    color = if (isFavorite) AlphaPicsColors.Cyan else AlphaPicsColors.TextSecondary,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .clickable(role = Role.Button) {
                            library = EditorPresetStore.toggleFavorite(context, selectedDisplay.preset.id)
                        }
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                        .testTag("editor_preset_favorite")
                        .semantics { contentDescription = if (isFavorite) "Remove favorite" else "Add favorite" }
                )
                if (selectedCustom != null) {
                    Text(
                        text = "Delete",
                        color = AlphaPicsColors.TextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .clickable(role = Role.Button) {
                                library = EditorPresetStore.deleteCustom(context, selectedCustom.id)
                                selectedLibraryId = state.filter.presetId
                                customIntensity = 100f
                            }
                            .padding(7.dp)
                            .testTag("editor_preset_delete")
                    )
                }
                Text(
                    text = if (showSaveForm) "Close" else "Save look",
                    color = AlphaPicsColors.Cyan,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .clickable(role = Role.Button) { showSaveForm = !showSaveForm }
                        .padding(7.dp)
                        .testTag("editor_preset_save_toggle")
                )
            }
        }

        Spacer(Modifier.height(3.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .testTag("editor_preset_library"),
            horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)
        ) {
            displays.forEach { display ->
                val preset = display.preset
                val selected = preset.id == selectedLibraryId
                val presetMatrix = remember(preset) {
                    ColorFilter.colorMatrix(
                        EditorColorEngine.computeMatrix(
                            exposure = preset.exposure,
                            contrast = preset.contrast,
                            saturation = preset.saturation,
                            warmth = preset.warmth,
                            tint = preset.tint
                        )
                    )
                }

                Column(
                    modifier = Modifier
                        .size(width = 58.dp, height = 68.dp)
                        .clickable(role = Role.Tab) {
                            selectedLibraryId = preset.id
                            if (display.custom == null) {
                                onStateChange(state.copy(filter = state.filter.copy(presetId = preset.id)))
                            } else {
                                customIntensity = 100f
                                onStateChange(display.custom.applyTo(state))
                            }
                        }
                        .semantics { this.selected = selected }
                        .testTag("editor_preset_${preset.id}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = "${preset.label} preset preview",
                        modifier = Modifier
                            .size(46.dp)
                            .clip(AlphaPicsShapes.Medium)
                            .border(
                                if (selected) 2.dp else 1.dp,
                                if (selected) AlphaPicsColors.Cyan else AlphaPicsColors.BorderSoft,
                                AlphaPicsShapes.Medium
                            ),
                        contentScale = ContentScale.Crop,
                        colorFilter = presetMatrix
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = preset.label,
                        color = if (selected) AlphaPicsColors.TextPrimary else AlphaPicsColors.TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(3.dp))

        if (showSaveForm) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp),
                horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = presetName,
                    onValueChange = { presetName = it.take(32) },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(AlphaPicsShapes.Medium)
                        .background(AlphaPicsColors.Surface)
                        .border(1.dp, AlphaPicsColors.BorderSoft, AlphaPicsShapes.Medium)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("editor_preset_name"),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = AlphaPicsColors.TextPrimary),
                    cursorBrush = SolidColor(AlphaPicsColors.Cyan),
                    singleLine = true,
                    decorationBox = { field ->
                        Box {
                            if (presetName.isBlank()) {
                                Text("Preset name", color = AlphaPicsColors.TextTertiary, style = MaterialTheme.typography.bodyMedium)
                            }
                            field()
                        }
                    }
                )
                Button(
                    onClick = {
                        library = EditorPresetStore.saveCustom(context, presetName, state)
                        library.customPresets.lastOrNull()?.let { selectedLibraryId = it.id }
                        customIntensity = 100f
                        showSaveForm = false
                    },
                    modifier = Modifier.heightIn(min = 40.dp).testTag("editor_preset_save"),
                    shape = AlphaPicsShapes.Medium,
                    colors = ButtonDefaults.buttonColors(containerColor = AlphaPicsColors.ElectricBlue)
                ) {
                    Text("Save", style = MaterialTheme.typography.labelMedium)
                }
            }
        } else {
            AlphaPicsValueSlider(
                label = if (selectedCustom == null) "Intensity" else "Custom intensity",
                value = intensity,
                onValueChange = { value ->
                    if (selectedCustom == null) {
                        onStateChange(state.copy(filter = state.filter.copy(intensity = value)))
                    } else {
                        customIntensity = value
                        onStateChange(selectedCustom.applyTo(state, value))
                    }
                },
                valueRange = 0f..100f,
                valueFormatter = { "${it.toInt()}%" },
                modifier = Modifier.testTag("editor_preset_intensity")
            )
        }

        Spacer(Modifier.height(3.dp))
        AlphaPicsContextActions(onCancel = onCancel, onApply = onApply)
    }
}

@Composable
private fun OperationHistoryPanel(
    entries: List<EditorHistoryEntry>,
    activeIndex: Int,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onJumpToHistory: (Long) -> Unit,
    onResetAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.testTag("editor_history_panel")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Edit history",
                color = AlphaPicsColors.TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = (entries.size - 1).coerceAtLeast(0).let { count ->
                    "$count ${if (count == 1) "operation" else "operations"} · this session"
                },
                color = AlphaPicsColors.TextTertiary,
                style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(Modifier.height(AlphaPicsSpacing.Xs))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)
        ) {
            Button(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.weight(1f).heightIn(min = 40.dp).testTag("editor_history_undo"),
                shape = AlphaPicsShapes.Medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlphaPicsColors.Surface,
                    contentColor = AlphaPicsColors.TextPrimary,
                    disabledContainerColor = AlphaPicsColors.SurfaceSoft,
                    disabledContentColor = AlphaPicsColors.TextTertiary
                )
            ) {
                Text("Undo", style = MaterialTheme.typography.labelMedium)
            }
            Button(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.weight(1f).heightIn(min = 40.dp).testTag("editor_history_redo"),
                shape = AlphaPicsShapes.Medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlphaPicsColors.Surface,
                    contentColor = AlphaPicsColors.TextPrimary,
                    disabledContainerColor = AlphaPicsColors.SurfaceSoft,
                    disabledContentColor = AlphaPicsColors.TextTertiary
                )
            ) {
                Text("Redo", style = MaterialTheme.typography.labelMedium)
            }
            Button(
                onClick = onResetAll,
                enabled = activeIndex > 0,
                modifier = Modifier.weight(1f).heightIn(min = 40.dp).testTag("editor_history_reset_all"),
                shape = AlphaPicsShapes.Medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlphaPicsColors.ElectricBlue,
                    contentColor = Color.White,
                    disabledContainerColor = AlphaPicsColors.SurfaceSoft,
                    disabledContentColor = AlphaPicsColors.TextTertiary
                )
            ) {
                Text("Reset all", style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }

        Spacer(Modifier.height(AlphaPicsSpacing.Sm))

        Text(
            text = "Tap a checkpoint to jump back without flattening the photo.",
            color = AlphaPicsColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(AlphaPicsSpacing.Xs))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .testTag("editor_history_timeline"),
            horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)
        ) {
            entries.forEachIndexed { index, entry ->
                EditorChoiceChip(
                    label = if (index == 0) "0 · Original" else "$index · ${entry.label}",
                    selected = index == activeIndex,
                    onClick = { onJumpToHistory(entry.id) },
                    modifier = Modifier.testTag("editor_history_entry_${entry.id}")
                )
            }
        }
    }
}

@Composable
private fun TransformPanel(
    transform: TransformAdjustments,
    onChange: (TransformAdjustments) -> Unit,
    onReset: () -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (!enabled) {
        AlphaPicsAvailabilityCard(
            title = "Crop & Transform",
            description = "Choose a photo to preview aspect ratio, rotation and flip.",
            badge = "Choose photo",
            accent = AlphaPicsColors.Cyan,
            modifier = modifier
        )
        return
    }

    var transformTab by remember { mutableStateOf("crop") }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)) {
                listOf("crop" to "Crop", "geometry" to "Geometry", "lens" to "Lens").forEach { (id, label) ->
                    EditorChoiceChip(
                        label = label,
                        selected = transformTab == id,
                        onClick = { transformTab = id },
                        modifier = Modifier.testTag("editor_transform_tab_$id")
                    )
                }
            }
            Text(
                text = "Reset",
                color = AlphaPicsColors.Cyan,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .clickable(role = Role.Button, onClick = onReset)
                    .padding(6.dp)
            )
        }

        Spacer(Modifier.height(AlphaPicsSpacing.Xs))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)
        ) {
            when (transformTab) {
                "crop" -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)
                    ) {
                        listOf(
                            "original" to "Original",
                            "free" to "Free",
                            "1:1" to "1:1",
                            "4:5" to "4:5",
                            "3:4" to "3:4",
                            "2:3" to "2:3",
                            "3:2" to "3:2",
                            "4:3" to "4:3",
                            "16:9" to "16:9",
                            "9:16" to "9:16",
                            "1.91:1" to "Social",
                            "custom" to "Custom"
                        ).forEach { (aspect, label) ->
                            EditorChoiceChip(
                                label = label,
                                selected = transform.aspectId == aspect,
                                onClick = {
                                    onChange(
                                        transform.copy(
                                            aspectId = aspect,
                                            cropRect = if (aspect == "free") transform.cropRect
                                            else NormalizedCropRect()
                                        )
                                    )
                                },
                                modifier = Modifier.testTag("editor_aspect_$aspect")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)
                    ) {
                        EditorChoiceChip(
                            label = "Rotate 90°",
                            selected = transform.rotationDegrees != 0,
                            onClick = { onChange(transform.copy(rotationDegrees = (transform.rotationDegrees + 90) % 360)) },
                            modifier = Modifier.weight(1f)
                        )
                        EditorChoiceChip(
                            label = "Flip H",
                            selected = transform.flipHorizontal,
                            onClick = { onChange(transform.copy(flipHorizontal = !transform.flipHorizontal)) },
                            modifier = Modifier.weight(1f)
                        )
                        EditorChoiceChip(
                            label = "Flip V",
                            selected = transform.flipVertical,
                            onClick = { onChange(transform.copy(flipVertical = !transform.flipVertical)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)
                    ) {
                        CropGrid.entries.forEach { grid ->
                            EditorChoiceChip(
                                label = when (grid) {
                                    CropGrid.OFF -> "Grid off"
                                    CropGrid.THIRDS -> "Thirds"
                                    CropGrid.SQUARE -> "Fine grid"
                                },
                                selected = transform.grid == grid,
                                onClick = { onChange(transform.copy(grid = grid)) },
                                modifier = Modifier.weight(1f).testTag("editor_grid_${grid.name.lowercase()}")
                            )
                        }
                    }

                    if (transform.aspectId == "free") {
                        val crop = transform.cropRect.sanitized()
                        TransformDoubleSliderRow(
                            firstLabel = "Left trim",
                            firstValue = crop.left * 100f,
                            onFirstChange = { value -> onChange(transform.copy(cropRect = crop.copy(left = value / 100f).sanitized())) },
                            secondLabel = "Top trim",
                            secondValue = crop.top * 100f,
                            onSecondChange = { value -> onChange(transform.copy(cropRect = crop.copy(top = value / 100f).sanitized())) }
                        )
                        TransformDoubleSliderRow(
                            firstLabel = "Right trim",
                            firstValue = (1f - crop.right) * 100f,
                            onFirstChange = { value -> onChange(transform.copy(cropRect = crop.copy(right = 1f - value / 100f).sanitized())) },
                            secondLabel = "Bottom trim",
                            secondValue = (1f - crop.bottom) * 100f,
                            onSecondChange = { value -> onChange(transform.copy(cropRect = crop.copy(bottom = 1f - value / 100f).sanitized())) }
                        )
                    } else if (transform.aspectId == "custom") {
                        TransformDoubleSliderRow(
                            firstLabel = "Width",
                            firstValue = transform.customAspectWidth,
                            onFirstChange = { onChange(transform.copy(customAspectWidth = it)) },
                            secondLabel = "Height",
                            secondValue = transform.customAspectHeight,
                            onSecondChange = { onChange(transform.copy(customAspectHeight = it)) },
                            valueRange = 1f..20f
                        )
                    }
                }

                "geometry" -> {
                    DetailTripleSliderRow(
                        firstLabel = "Straighten",
                        firstValue = transform.straightenDegrees,
                        firstRange = -15f..15f,
                        onFirstChange = { onChange(transform.copy(straightenDegrees = it)) },
                        secondLabel = "Perspective H",
                        secondValue = transform.perspectiveHorizontal,
                        onSecondChange = { onChange(transform.copy(perspectiveHorizontal = it)) },
                        thirdLabel = "Perspective V",
                        thirdValue = transform.perspectiveVertical,
                        onThirdChange = { onChange(transform.copy(perspectiveVertical = it)) }
                    )
                    Text(
                        text = "Straighten auto-fills edges. Perspective corrections remain local and export at source resolution.",
                        color = AlphaPicsColors.TextTertiary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                else -> {
                    DetailTripleSliderRow(
                        firstLabel = "Distortion",
                        firstValue = transform.lensDistortion,
                        onFirstChange = { onChange(transform.copy(lensDistortion = it)) },
                        secondLabel = "Horizontal",
                        secondValue = transform.geometryHorizontal,
                        onSecondChange = { onChange(transform.copy(geometryHorizontal = it)) },
                        thirdLabel = "Vertical",
                        thirdValue = transform.geometryVertical,
                        onThirdChange = { onChange(transform.copy(geometryVertical = it)) }
                    )
                    Text(
                        text = "Distortion: negative corrects barrel; positive corrects pincushion. Geometry controls axial stretch.",
                        color = AlphaPicsColors.TextTertiary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(Modifier.height(AlphaPicsSpacing.Xs))
        AlphaPicsContextActions(onCancel = onCancel, onApply = onApply)
    }
}

@Composable
private fun TransformDoubleSliderRow(
    firstLabel: String,
    firstValue: Float,
    onFirstChange: (Float) -> Unit,
    secondLabel: String,
    secondValue: Float,
    onSecondChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..45f
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Md)
    ) {
        AlphaPicsValueSlider(
            label = firstLabel,
            value = firstValue,
            onValueChange = onFirstChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f)
        )
        AlphaPicsValueSlider(
            label = secondLabel,
            value = secondValue,
            onValueChange = onSecondChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RetouchPanel(
    retouch: RetouchAdjustments,
    onChange: (RetouchAdjustments) -> Unit,
    onReset: () -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (!enabled) {
        AlphaPicsAvailabilityCard(
            title = "Local Retouch",
            description = "Choose a photo to paint real healing, repair and local adjustment strokes.",
            badge = "Choose photo",
            accent = AlphaPicsColors.Violet,
            modifier = modifier
        )
        return
    }

    val modes = listOf(
        LocalRetouchMode.HEAL,
        LocalRetouchMode.CLONE,
        LocalRetouchMode.BLEMISH,
        LocalRetouchMode.RED_EYE,
        LocalRetouchMode.BLUR,
        LocalRetouchMode.SHARPEN,
        LocalRetouchMode.EXPOSURE,
        LocalRetouchMode.BRIGHTNESS,
        LocalRetouchMode.SATURATION,
        LocalRetouchMode.TEMPERATURE,
        LocalRetouchMode.ERASE_MASK
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Local Retouch",
                    color = AlphaPicsColors.TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${retouch.strokes.size} stroke${if (retouch.strokes.size == 1) "" else "s"} · paint on photo",
                    color = AlphaPicsColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = "Reset",
                color = AlphaPicsColors.Cyan,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clickable(role = Role.Button, onClick = onReset).padding(6.dp)
            )
        }

        Spacer(Modifier.height(AlphaPicsSpacing.Xs))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)
        ) {
            modes.forEach { mode ->
                EditorChoiceChip(
                    label = retouchModeLabel(mode),
                    selected = retouch.activeMode == mode,
                    onClick = { onChange(retouch.copy(activeMode = mode)) },
                    modifier = Modifier.testTag("editor_retouch_mode_${mode.name.lowercase()}")
                )
            }
        }

        Spacer(Modifier.height(AlphaPicsSpacing.Xs))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)
        ) {
            DetailTripleSliderRow(
                firstLabel = "Size",
                firstValue = retouch.brushSize,
                firstRange = 1f..30f,
                onFirstChange = { onChange(retouch.copy(brushSize = it)) },
                secondLabel = "Feather",
                secondValue = retouch.brushFeather,
                secondRange = 0f..100f,
                onSecondChange = { onChange(retouch.copy(brushFeather = it)) },
                thirdLabel = "Strength",
                thirdValue = retouch.brushStrength,
                thirdRange = 1f..100f,
                onThirdChange = { onChange(retouch.copy(brushStrength = it)) }
            )

            if (retouch.activeMode == LocalRetouchMode.CLONE) {
                TransformDoubleSliderRow(
                    firstLabel = "Source X",
                    firstValue = retouch.cloneSourceOffsetX,
                    onFirstChange = { onChange(retouch.copy(cloneSourceOffsetX = it)) },
                    secondLabel = "Source Y",
                    secondValue = retouch.cloneSourceOffsetY,
                    onSecondChange = { onChange(retouch.copy(cloneSourceOffsetY = it)) },
                    valueRange = -50f..50f
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)
            ) {
                EditorChoiceChip(
                    label = "Undo stroke",
                    selected = false,
                    onClick = { onChange(retouch.removeLastStroke()) },
                    modifier = Modifier.weight(1f).testTag("editor_retouch_undo")
                )
                EditorChoiceChip(
                    label = if (retouch.showMask) "Hide mask" else "Show mask",
                    selected = retouch.showMask,
                    onClick = { onChange(retouch.copy(showMask = !retouch.showMask)) },
                    modifier = Modifier.weight(1f).testTag("editor_retouch_show_mask")
                )
                EditorChoiceChip(
                    label = "Clear",
                    selected = false,
                    onClick = { onChange(retouch.copy(strokes = emptyList())) },
                    modifier = Modifier.weight(1f).testTag("editor_retouch_clear")
                )
            }
        }

        Spacer(Modifier.height(AlphaPicsSpacing.Xs))
        AlphaPicsContextActions(onCancel = onCancel, onApply = onApply)
    }
}

private fun retouchModeLabel(mode: LocalRetouchMode): String = when (mode) {
    LocalRetouchMode.HEAL -> "Heal"
    LocalRetouchMode.CLONE -> "Clone"
    LocalRetouchMode.BLEMISH -> "Blemish"
    LocalRetouchMode.RED_EYE -> "Red-eye"
    LocalRetouchMode.BLUR -> "Blur"
    LocalRetouchMode.SHARPEN -> "Sharpen"
    LocalRetouchMode.EXPOSURE -> "Exposure"
    LocalRetouchMode.BRIGHTNESS -> "Brightness"
    LocalRetouchMode.SATURATION -> "Saturation"
    LocalRetouchMode.TEMPERATURE -> "Warmth"
    LocalRetouchMode.ERASE_MASK -> "Erase mask"
}

@Composable
private fun OverlayPanel(
    overlays: OverlayAdjustments,
    onChange: (OverlayAdjustments) -> Unit,
    onReset: () -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (!enabled) {
        AlphaPicsAvailabilityCard(
            title = "Creative overlays",
            description = "Choose a photo to add editable text, drawing, shapes, stickers, frames and a text watermark.",
            badge = "Choose photo",
            accent = AlphaPicsColors.Violet,
            modifier = modifier
        )
        return
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = overlayToolLabel(overlays.activeTool),
                    color = AlphaPicsColors.TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = overlayItemSummary(overlays),
                    color = AlphaPicsColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = "Reset",
                color = AlphaPicsColors.Cyan,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clickable(role = Role.Button, onClick = onReset).padding(6.dp)
            )
        }

        Spacer(Modifier.height(AlphaPicsSpacing.Xs))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)
        ) {
            OverlayToolMode.entries.forEach { tool ->
                EditorChoiceChip(
                    label = overlayToolLabel(tool),
                    selected = overlays.activeTool == tool,
                    onClick = { onChange(overlays.copy(activeTool = tool)) },
                    modifier = Modifier.testTag("editor_overlay_tab_${tool.name.lowercase()}")
                )
            }
        }

        Spacer(Modifier.height(AlphaPicsSpacing.Xs))
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)
        ) {
            when (overlays.activeTool) {
                OverlayToolMode.TEXT -> OverlayTextControls(overlays, onChange)
                OverlayToolMode.DRAW -> OverlayDrawControls(overlays, onChange)
                OverlayToolMode.SHAPE -> OverlayShapeControls(overlays, onChange)
                OverlayToolMode.STICKER -> OverlayStickerControls(overlays, onChange)
                OverlayToolMode.FRAME -> OverlayFrameControls(overlays, onChange)
                OverlayToolMode.WATERMARK -> OverlayWatermarkControls(overlays, onChange)
            }
        }

        Spacer(Modifier.height(AlphaPicsSpacing.Xs))
        AlphaPicsContextActions(onCancel = onCancel, onApply = onApply)
    }
}

private fun overlayToolLabel(tool: OverlayToolMode): String = when (tool) {
    OverlayToolMode.TEXT -> "Text"
    OverlayToolMode.DRAW -> "Draw"
    OverlayToolMode.SHAPE -> "Shape"
    OverlayToolMode.STICKER -> "Sticker"
    OverlayToolMode.FRAME -> "Frame"
    OverlayToolMode.WATERMARK -> "Mark"
}

private fun overlayItemSummary(overlays: OverlayAdjustments): String {
    val items = overlays.texts.size + overlays.drawing.size + overlays.shapes.size + overlays.stickers.size
    return "$items editable item${if (items == 1) "" else "s"} · rendered on export"
}

@Composable
private fun OverlayTextControls(
    overlays: OverlayAdjustments,
    onChange: (OverlayAdjustments) -> Unit
) {
    fun updateTemplate(next: TextOverlay) {
        val sanitized = next.copy(text = overlays.textDraft).sanitized()
        onChange(
            overlays.copy(
                textTemplate = next,
                texts = if (overlays.texts.isEmpty()) overlays.texts
                else overlays.texts.dropLast(1) + sanitized
            )
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = overlays.textDraft,
            onValueChange = { value ->
                val bounded = value.take(120)
                onChange(
                    overlays.copy(
                        textDraft = bounded,
                        texts = if (overlays.texts.isEmpty()) overlays.texts
                        else overlays.texts.dropLast(1) + overlays.textTemplate.copy(text = bounded).sanitized()
                    )
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = AlphaPicsColors.TextPrimary),
            cursorBrush = SolidColor(AlphaPicsColors.Cyan),
            singleLine = false,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 40.dp)
                .background(AlphaPicsColors.Surface, AlphaPicsShapes.Medium)
                .border(1.dp, AlphaPicsColors.BorderSoft, AlphaPicsShapes.Medium)
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .testTag("editor_overlay_text_input")
        )
        EditorChoiceChip(
            label = if (overlays.texts.isEmpty()) "Add" else "Add new",
            selected = false,
            onClick = {
                if (overlays.textDraft.isNotBlank()) {
                    onChange(overlays.addText(overlays.textTemplate.copy(text = overlays.textDraft)))
                }
            },
            modifier = Modifier.testTag("editor_overlay_text_add")
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)
    ) {
        OverlayTextAlignment.entries.forEach { alignment ->
            EditorChoiceChip(
                label = alignment.name.lowercase().replaceFirstChar(Char::uppercase),
                selected = overlays.textTemplate.alignment == alignment,
                onClick = { updateTemplate(overlays.textTemplate.copy(alignment = alignment)) }
            )
        }
        EditorChoiceChip(
            label = "Bold",
            selected = overlays.textTemplate.weight >= 700,
            onClick = {
                updateTemplate(overlays.textTemplate.copy(weight = if (overlays.textTemplate.weight >= 700) 400 else 700))
            }
        )
        EditorChoiceChip(
            label = "Shadow",
            selected = overlays.textTemplate.shadow > 0f,
            onClick = {
                updateTemplate(overlays.textTemplate.copy(shadow = if (overlays.textTemplate.shadow > 0f) 0f else 35f))
            }
        )
        EditorChoiceChip(
            label = "Backdrop",
            selected = overlays.textTemplate.backgroundArgb != null,
            onClick = {
                updateTemplate(overlays.textTemplate.copy(backgroundArgb = if (overlays.textTemplate.backgroundArgb == null) 0xB0000000 else null))
            }
        )
        EditorChoiceChip(
            label = "Outline",
            selected = overlays.textTemplate.outlineArgb != null,
            onClick = {
                updateTemplate(overlays.textTemplate.copy(outlineArgb = if (overlays.textTemplate.outlineArgb == null) 0xFF000000 else null))
            }
        )
    }

    OverlayColorChoices(
        selectedColor = overlays.textTemplate.colorArgb,
        onColorSelected = { updateTemplate(overlays.textTemplate.copy(colorArgb = it)) },
        tag = "text"
    )
    DetailTripleSliderRow(
        firstLabel = "Size",
        firstValue = overlays.textTemplate.fontSize,
        onFirstChange = { updateTemplate(overlays.textTemplate.copy(fontSize = it)) },
        secondLabel = "Opacity",
        secondValue = overlays.textTemplate.opacity,
        onSecondChange = { updateTemplate(overlays.textTemplate.copy(opacity = it)) },
        thirdLabel = "Rotation",
        firstRange = 2f..24f,
        secondRange = 0f..100f,
        thirdValue = overlays.textTemplate.rotation,
        onThirdChange = { updateTemplate(overlays.textTemplate.copy(rotation = it)) },
        thirdRange = -180f..180f
    )
    DetailTripleSliderRow(
        firstLabel = "Position X",
        firstValue = overlays.textTemplate.x * 100f,
        onFirstChange = { updateTemplate(overlays.textTemplate.copy(x = it / 100f)) },
        secondLabel = "Position Y",
        secondValue = overlays.textTemplate.y * 100f,
        onSecondChange = { updateTemplate(overlays.textTemplate.copy(y = it / 100f)) },
        thirdLabel = "Scale",
        thirdValue = overlays.textTemplate.scale,
        onThirdChange = { updateTemplate(overlays.textTemplate.copy(scale = it)) },
        firstRange = 0f..100f,
        secondRange = 0f..100f,
        thirdRange = 25f..300f
    )
    DetailTripleSliderRow(
        firstLabel = "Letter space",
        firstValue = overlays.textTemplate.letterSpacing,
        onFirstChange = { updateTemplate(overlays.textTemplate.copy(letterSpacing = it)) },
        secondLabel = "Line space",
        secondValue = overlays.textTemplate.lineSpacing,
        onSecondChange = { updateTemplate(overlays.textTemplate.copy(lineSpacing = it)) },
        thirdLabel = "Shadow",
        thirdValue = overlays.textTemplate.shadow,
        onThirdChange = { updateTemplate(overlays.textTemplate.copy(shadow = it)) },
        firstRange = -0.08f..0.30f,
        secondRange = 75f..200f,
        thirdRange = 0f..100f
    )
    OverlayItemActions(
        canEdit = overlays.texts.isNotEmpty(),
        onDuplicate = {
            overlays.texts.lastOrNull()?.let { onChange(overlays.addText(it.copy(x = it.x + 0.04f, y = it.y + 0.04f))) }
        },
        onDelete = { onChange(overlays.copy(texts = overlays.texts.dropLast(1))) },
        onClear = { onChange(overlays.copy(texts = emptyList())) },
        tag = "text"
    )
}

@Composable
private fun OverlayDrawControls(
    overlays: OverlayAdjustments,
    onChange: (OverlayAdjustments) -> Unit
) {
    Text(
        text = "Draw directly on the photo. Strokes stay editable until you apply.",
        color = AlphaPicsColors.TextTertiary,
        style = MaterialTheme.typography.bodySmall
    )
    OverlayColorChoices(
        selectedColor = overlays.drawColorArgb,
        onColorSelected = { onChange(overlays.copy(drawColorArgb = it, drawEraser = false)) },
        tag = "draw"
    )
    OverlayDoubleSliderRow(
        firstLabel = "Brush size",
        firstValue = overlays.drawSize,
        onFirstChange = { onChange(overlays.copy(drawSize = it)) },
        firstRange = 0.2f..12f,
        secondLabel = "Opacity",
        secondValue = overlays.drawOpacity,
        onSecondChange = { onChange(overlays.copy(drawOpacity = it)) },
        secondRange = 0f..100f
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)
    ) {
        EditorChoiceChip(
            label = "Undo",
            selected = false,
            onClick = { onChange(overlays.undoDrawing()) },
            modifier = Modifier.weight(1f).testTag("editor_overlay_draw_undo")
        )
        EditorChoiceChip(
            label = "Redo",
            selected = false,
            onClick = { onChange(overlays.redoDrawing()) },
            modifier = Modifier.weight(1f).testTag("editor_overlay_draw_redo")
        )
        EditorChoiceChip(
            label = "Eraser",
            selected = overlays.drawEraser,
            onClick = { onChange(overlays.copy(drawEraser = !overlays.drawEraser)) },
            modifier = Modifier.weight(1f).testTag("editor_overlay_draw_eraser")
        )
        EditorChoiceChip(
            label = "Clear",
            selected = false,
            onClick = { onChange(overlays.copy(drawing = emptyList(), drawingRedo = emptyList())) },
            modifier = Modifier.weight(1f).testTag("editor_overlay_draw_clear")
        )
    }
}

@Composable
private fun OverlayShapeControls(
    overlays: OverlayAdjustments,
    onChange: (OverlayAdjustments) -> Unit
) {
    fun updateTemplate(next: ShapeOverlay) {
        val sanitized = next.sanitized()
        onChange(
            overlays.copy(
                shapeTemplate = next,
                shapes = if (overlays.shapes.isEmpty()) overlays.shapes
                else overlays.shapes.dropLast(1) + sanitized
            )
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)
    ) {
        OverlayShapeKind.entries.forEach { kind ->
            EditorChoiceChip(
                label = when (kind) {
                    OverlayShapeKind.RECTANGLE -> "Rectangle"
                    OverlayShapeKind.ROUNDED_RECTANGLE -> "Rounded"
                    OverlayShapeKind.CIRCLE -> "Circle"
                    OverlayShapeKind.LINE -> "Line"
                    OverlayShapeKind.ARROW -> "Arrow"
                },
                selected = overlays.shapeTemplate.kind == kind,
                onClick = { updateTemplate(overlays.shapeTemplate.copy(kind = kind)) },
                modifier = Modifier.testTag("editor_overlay_shape_${kind.name.lowercase()}")
            )
        }
        EditorChoiceChip(
            label = if (overlays.shapes.isEmpty()) "Add" else "Add new",
            selected = false,
            onClick = { onChange(overlays.addShape(overlays.shapeTemplate)) },
            modifier = Modifier.testTag("editor_overlay_shape_add")
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EditorChoiceChip(
            label = "Fill",
            selected = overlays.shapeTemplate.fillArgb != null,
            onClick = {
                updateTemplate(
                    overlays.shapeTemplate.copy(
                        fillArgb = if (overlays.shapeTemplate.fillArgb == null) overlays.shapeTemplate.strokeArgb else null
                    )
                )
            }
        )
        OverlayColorChoices(
            selectedColor = overlays.shapeTemplate.strokeArgb,
            onColorSelected = { color ->
                updateTemplate(
                    overlays.shapeTemplate.copy(
                        strokeArgb = color,
                        fillArgb = if (overlays.shapeTemplate.fillArgb == null) null else color
                    )
                )
            },
            tag = "shape",
            modifier = Modifier.weight(1f)
        )
    }
    DetailTripleSliderRow(
        firstLabel = "Width",
        firstValue = overlays.shapeTemplate.width * 100f,
        onFirstChange = { updateTemplate(overlays.shapeTemplate.copy(width = it / 100f)) },
        secondLabel = "Height",
        secondValue = overlays.shapeTemplate.height * 100f,
        onSecondChange = { updateTemplate(overlays.shapeTemplate.copy(height = it / 100f)) },
        thirdLabel = "Stroke",
        thirdValue = overlays.shapeTemplate.strokeWidth,
        onThirdChange = { updateTemplate(overlays.shapeTemplate.copy(strokeWidth = it)) },
        firstRange = 5f..100f,
        secondRange = 5f..100f,
        thirdRange = 0.2f..8f
    )
    DetailTripleSliderRow(
        firstLabel = "Position X",
        firstValue = overlays.shapeTemplate.x * 100f,
        onFirstChange = { updateTemplate(overlays.shapeTemplate.copy(x = it / 100f)) },
        secondLabel = "Position Y",
        secondValue = overlays.shapeTemplate.y * 100f,
        onSecondChange = { updateTemplate(overlays.shapeTemplate.copy(y = it / 100f)) },
        thirdLabel = "Rotation",
        thirdValue = overlays.shapeTemplate.rotation,
        onThirdChange = { updateTemplate(overlays.shapeTemplate.copy(rotation = it)) },
        firstRange = 0f..100f,
        secondRange = 0f..100f,
        thirdRange = -180f..180f
    )
    OverlayItemActions(
        canEdit = overlays.shapes.isNotEmpty(),
        onDuplicate = {
            overlays.shapes.lastOrNull()?.let { onChange(overlays.addShape(it.copy(x = it.x + 0.04f, y = it.y + 0.04f))) }
        },
        onDelete = { onChange(overlays.copy(shapes = overlays.shapes.dropLast(1))) },
        onClear = { onChange(overlays.copy(shapes = emptyList())) },
        tag = "shape"
    )
}

@Composable
private fun OverlayStickerControls(
    overlays: OverlayAdjustments,
    onChange: (OverlayAdjustments) -> Unit
) {
    fun updateTemplate(next: StickerOverlay) {
        val sanitized = next.sanitized()
        onChange(
            overlays.copy(
                stickerTemplate = next,
                stickers = if (overlays.stickers.isEmpty()) overlays.stickers
                else overlays.stickers.dropLast(1) + sanitized
            )
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)
    ) {
        OverlayStickerKind.entries.forEach { kind ->
            EditorChoiceChip(
                label = kind.name.lowercase().replaceFirstChar(Char::uppercase),
                selected = overlays.stickerTemplate.kind == kind,
                onClick = { updateTemplate(overlays.stickerTemplate.copy(kind = kind)) },
                modifier = Modifier.testTag("editor_overlay_sticker_${kind.name.lowercase()}")
            )
        }
        EditorChoiceChip(
            label = if (overlays.stickers.isEmpty()) "Add" else "Add new",
            selected = false,
            onClick = { onChange(overlays.addSticker(overlays.stickerTemplate)) },
            modifier = Modifier.testTag("editor_overlay_sticker_add")
        )
        EditorChoiceChip(
            label = "Flip",
            selected = overlays.stickerTemplate.flipHorizontal,
            onClick = { updateTemplate(overlays.stickerTemplate.copy(flipHorizontal = !overlays.stickerTemplate.flipHorizontal)) }
        )
    }
    OverlayColorChoices(
        selectedColor = overlays.stickerTemplate.colorArgb,
        onColorSelected = { updateTemplate(overlays.stickerTemplate.copy(colorArgb = it)) },
        tag = "sticker"
    )
    DetailTripleSliderRow(
        firstLabel = "Scale",
        firstValue = overlays.stickerTemplate.scale,
        onFirstChange = { updateTemplate(overlays.stickerTemplate.copy(scale = it)) },
        secondLabel = "Rotation",
        secondValue = overlays.stickerTemplate.rotation,
        onSecondChange = { updateTemplate(overlays.stickerTemplate.copy(rotation = it)) },
        thirdLabel = "Opacity",
        thirdValue = overlays.stickerTemplate.opacity,
        onThirdChange = { updateTemplate(overlays.stickerTemplate.copy(opacity = it)) },
        firstRange = 3f..80f,
        secondRange = -180f..180f,
        thirdRange = 0f..100f
    )
    TransformDoubleSliderRow(
        firstLabel = "Position X",
        firstValue = overlays.stickerTemplate.x * 100f,
        onFirstChange = { updateTemplate(overlays.stickerTemplate.copy(x = it / 100f)) },
        secondLabel = "Position Y",
        secondValue = overlays.stickerTemplate.y * 100f,
        onSecondChange = { updateTemplate(overlays.stickerTemplate.copy(y = it / 100f)) },
        valueRange = 0f..100f
    )
    OverlayItemActions(
        canEdit = overlays.stickers.isNotEmpty(),
        onDuplicate = {
            overlays.stickers.lastOrNull()?.let { onChange(overlays.addSticker(it.copy(x = it.x + 0.04f, y = it.y + 0.04f))) }
        },
        onDelete = { onChange(overlays.copy(stickers = overlays.stickers.dropLast(1))) },
        onClear = { onChange(overlays.copy(stickers = emptyList())) },
        tag = "sticker"
    )
}

@Composable
private fun OverlayFrameControls(
    overlays: OverlayAdjustments,
    onChange: (OverlayAdjustments) -> Unit
) {
    val frame = overlays.frame
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)
    ) {
        listOf(
            "none" to "None",
            "white" to "White",
            "black" to "Black",
            "rounded" to "Rounded",
            "film" to "Film"
        ).forEach { (id, label) ->
            EditorChoiceChip(
                label = label,
                selected = frame.presetId == id,
                onClick = {
                    onChange(
                        overlays.copy(
                            frame = frame.copy(
                                presetId = id,
                                borderEnabled = id != "none",
                                cornerRadius = if (id == "rounded") maxOf(frame.cornerRadius, 8f) else 0f
                            )
                        )
                    )
                },
                modifier = Modifier.testTag("editor_overlay_frame_$id")
            )
        }
    }
    OverlayColorChoices(
        selectedColor = frame.borderColorArgb,
        onColorSelected = { onChange(overlays.copy(frame = frame.copy(borderColorArgb = it, borderEnabled = true, presetId = "custom"))) },
        tag = "frame"
    )
    OverlayDoubleSliderRow(
        firstLabel = "Thickness",
        firstValue = frame.borderThickness,
        onFirstChange = { onChange(overlays.copy(frame = frame.copy(borderThickness = it, borderEnabled = true, presetId = "custom"))) },
        firstRange = 0.2f..12f,
        secondLabel = "Corner radius",
        secondValue = frame.cornerRadius,
        onSecondChange = { onChange(overlays.copy(frame = frame.copy(cornerRadius = it, presetId = "custom"))) },
        secondRange = 0f..20f
    )
    Text(
        text = "Frames render at full photo resolution. Rounded corners export with transparency in PNG.",
        color = AlphaPicsColors.TextTertiary,
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun OverlayWatermarkControls(
    overlays: OverlayAdjustments,
    onChange: (OverlayAdjustments) -> Unit
) {
    val watermark = overlays.watermark
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = watermark.text,
            onValueChange = { onChange(overlays.copy(watermark = watermark.copy(text = it.take(80)))) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = AlphaPicsColors.TextPrimary),
            cursorBrush = SolidColor(AlphaPicsColors.Cyan),
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 40.dp)
                .background(AlphaPicsColors.Surface, AlphaPicsShapes.Medium)
                .border(1.dp, AlphaPicsColors.BorderSoft, AlphaPicsShapes.Medium)
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .testTag("editor_overlay_watermark_input")
        )
        EditorChoiceChip(
            label = if (watermark.enabled) "Visible" else "Hidden",
            selected = watermark.enabled,
            onClick = { onChange(overlays.copy(watermark = watermark.copy(enabled = !watermark.enabled))) },
            modifier = Modifier.testTag("editor_overlay_watermark_toggle")
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)
    ) {
        WatermarkAnchor.entries.forEach { anchor ->
            EditorChoiceChip(
                label = when (anchor) {
                    WatermarkAnchor.TOP_LEFT -> "Top left"
                    WatermarkAnchor.TOP_RIGHT -> "Top right"
                    WatermarkAnchor.CENTER -> "Center"
                    WatermarkAnchor.BOTTOM_LEFT -> "Bottom left"
                    WatermarkAnchor.BOTTOM_RIGHT -> "Bottom right"
                },
                selected = watermark.anchor == anchor,
                onClick = { onChange(overlays.copy(watermark = watermark.copy(anchor = anchor, enabled = true))) }
            )
        }
    }
    OverlayColorChoices(
        selectedColor = watermark.colorArgb,
        onColorSelected = { onChange(overlays.copy(watermark = watermark.copy(colorArgb = it, enabled = true))) },
        tag = "watermark"
    )
    DetailTripleSliderRow(
        firstLabel = "Scale",
        firstValue = watermark.scale,
        onFirstChange = { onChange(overlays.copy(watermark = watermark.copy(scale = it, enabled = true))) },
        secondLabel = "Rotation",
        secondValue = watermark.rotation,
        onSecondChange = { onChange(overlays.copy(watermark = watermark.copy(rotation = it, enabled = true))) },
        thirdLabel = "Opacity",
        thirdValue = watermark.opacity,
        onThirdChange = { onChange(overlays.copy(watermark = watermark.copy(opacity = it, enabled = true))) },
        firstRange = 1.5f..12f,
        secondRange = -180f..180f,
        thirdRange = 0f..100f
    )
    AlphaPicsValueSlider(
        label = "Edge padding",
        value = watermark.padding,
        onValueChange = { onChange(overlays.copy(watermark = watermark.copy(padding = it, enabled = true))) },
        valueRange = 0f..15f,
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        text = "Text watermark only. Image or logo watermarks are not available yet.",
        color = AlphaPicsColors.TextTertiary,
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun OverlayColorChoices(
    selectedColor: Long,
    onColorSelected: (Long) -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    val colors = listOf(0xFFFFFFFF, 0xFF000000, 0xFF2F7BFF, 0xFF26D9FF, 0xFF9B6CFF, 0xFFFF5A75, 0xFFFFC34D)
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEachIndexed { index, color ->
            val selected = selectedColor == color
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(color.toInt()))
                    .border(
                        if (selected) 2.5.dp else 1.dp,
                        if (selected) AlphaPicsColors.Cyan else AlphaPicsColors.BorderSoft,
                        CircleShape
                    )
                    .clickable(role = Role.RadioButton) { onColorSelected(color) }
                    .semantics { this.selected = selected }
                    .testTag("editor_overlay_color_${tag}_$index")
            )
        }
    }
}

@Composable
private fun OverlayItemActions(
    canEdit: Boolean,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)
    ) {
        EditorChoiceChip(
            label = "Duplicate",
            selected = false,
            onClick = { if (canEdit) onDuplicate() },
            modifier = Modifier.weight(1f).testTag("editor_overlay_${tag}_duplicate")
        )
        EditorChoiceChip(
            label = "Delete",
            selected = false,
            onClick = { if (canEdit) onDelete() },
            modifier = Modifier.weight(1f).testTag("editor_overlay_${tag}_delete")
        )
        EditorChoiceChip(
            label = "Clear",
            selected = false,
            onClick = { if (canEdit) onClear() },
            modifier = Modifier.weight(1f).testTag("editor_overlay_${tag}_clear")
        )
    }
}

@Composable
private fun OverlayDoubleSliderRow(
    firstLabel: String,
    firstValue: Float,
    onFirstChange: (Float) -> Unit,
    firstRange: ClosedFloatingPointRange<Float>,
    secondLabel: String,
    secondValue: Float,
    onSecondChange: (Float) -> Unit,
    secondRange: ClosedFloatingPointRange<Float>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Md)
    ) {
        AlphaPicsValueSlider(
            label = firstLabel,
            value = firstValue,
            onValueChange = onFirstChange,
            valueRange = firstRange,
            modifier = Modifier.weight(1f)
        )
        AlphaPicsValueSlider(
            label = secondLabel,
            value = secondValue,
            onValueChange = onSecondChange,
            valueRange = secondRange,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun UnavailableEditorPanel(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AlphaPicsAvailabilityCard(
            title = title,
            description = description,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EditorChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .background(
                if (selected) AlphaPicsColors.ElectricBlue.copy(alpha = 0.22f)
                else AlphaPicsColors.Surface,
                AlphaPicsShapes.Medium
            )
            .border(
                if (selected) 1.5.dp else 1.dp,
                if (selected) AlphaPicsColors.BrightBlue else AlphaPicsColors.BorderSoft,
                AlphaPicsShapes.Medium
            )
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) AlphaPicsColors.TextPrimary else AlphaPicsColors.TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun EditorToolRail(
    selectedTool: AlphaPicsEditorTool,
    onToolSelected: (AlphaPicsEditorTool) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val itemStridePx = with(LocalDensity.current) { 76.dp.roundToPx() }
    LaunchedEffect(selectedTool, scrollState.maxValue, itemStridePx) {
        val target = selectedTool.ordinal * itemStridePx
        scrollState.scrollTo(target.coerceAtMost(scrollState.maxValue))
    }

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .testTag("editor_tool_rail"),
        horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Sm)
    ) {
        AlphaPicsEditorTool.entries.forEach { tool ->
            val selected = tool == selectedTool
            Column(
                modifier = Modifier
                    .size(width = 68.dp, height = 66.dp)
                    .background(
                        if (selected) AlphaPicsColors.ElectricBlue.copy(alpha = 0.22f)
                        else AlphaPicsColors.SurfaceRaised.copy(alpha = 0.88f),
                        AlphaPicsShapes.Medium
                    )
                    .border(
                        if (selected) 1.5.dp else 1.dp,
                        if (selected) AlphaPicsColors.BrightBlue else AlphaPicsColors.BorderSoft,
                        AlphaPicsShapes.Medium
                    )
                    .clickable(role = Role.Tab) { onToolSelected(tool) }
                    .semantics { this.selected = selected }
                    .padding(horizontal = 3.dp, vertical = 6.dp)
                    .testTag("editor_tool_${tool.id}"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AlphaPicsGlyph(
                    icon = tool.icon,
                    accent = if (selected) AlphaPicsColors.Cyan else AlphaPicsColors.TextTertiary,
                    size = 30.dp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = tool.label,
                    color = if (selected) AlphaPicsColors.TextPrimary else AlphaPicsColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorExportBottomSheet(
    isExporting: Boolean,
    progress: Float,
    exportResult: ExportResult?,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onExport: (ExportFormat, Int) -> Unit,
    onShare: (ExportResult) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedFormat by remember { mutableStateOf(ExportFormat.JPEG) }
    var quality by remember { mutableIntStateOf(92) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AlphaPicsColors.SurfaceRaised,
        tonalElevation = 16.dp,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (exportResult != null) "Export Succeeded!" else "Export Photo",
                    color = AlphaPicsColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = AlphaPicsColors.TextTertiary)
                }
            }

            Spacer(Modifier.height(16.dp))

            if (exportResult != null) {
                // Success Presentation
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(AlphaPicsColors.ElectricBlue.copy(alpha = 0.2f))
                        .border(2.dp, AlphaPicsColors.Cyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = AlphaPicsColors.Cyan,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Saved to Gallery",
                    color = AlphaPicsColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${exportResult.filename} • ${formatFileSize(exportResult.sizeBytes)}",
                    color = AlphaPicsColors.TextTertiary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${exportResult.width} × ${exportResult.height} px",
                    color = AlphaPicsColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onShare(exportResult) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AlphaPicsColors.Surface,
                            contentColor = AlphaPicsColors.BrightBlue
                        ),
                        shape = AlphaPicsShapes.Medium,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .border(1.dp, AlphaPicsColors.BorderFocus, AlphaPicsShapes.Medium)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AlphaPicsColors.ElectricBlue,
                            contentColor = Color.White
                        ),
                        shape = AlphaPicsShapes.Medium,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            } else if (isExporting) {
                // Progress indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 24.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        color = AlphaPicsColors.Cyan,
                        trackColor = AlphaPicsColors.BorderSoft,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Rendering full resolution...",
                        color = AlphaPicsColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(progress * 100).toInt()}% completed",
                        color = AlphaPicsColors.TextTertiary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                // Export Configuration Form
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = AlphaPicsColors.Danger,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Format selector
                Text(
                    text = "FORMAT",
                    color = AlphaPicsColors.TextTertiary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportFormat.entries.forEach { format ->
                        EditorChoiceChip(
                            label = format.extension.uppercase(),
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Quality slider (for lossy formats)
                if (selectedFormat != ExportFormat.PNG) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("QUALITY", color = AlphaPicsColors.TextTertiary, style = MaterialTheme.typography.labelSmall)
                        Text("$quality%", color = AlphaPicsColors.TextPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = quality.toFloat(),
                        onValueChange = { quality = it.toInt() },
                        valueRange = 50f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = AlphaPicsColors.Cyan,
                            activeTrackColor = AlphaPicsColors.ElectricBlue,
                            inactiveTrackColor = AlphaPicsColors.BorderSoft
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // Action button
                Button(
                    onClick = { onExport(selectedFormat, quality) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlphaPicsColors.ElectricBlue,
                        contentColor = Color.White
                    ),
                    shape = AlphaPicsShapes.Pill,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(imageVector = SaveDownloadIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save Full Resolution Photo", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

private fun formatFileSize(sizeInBytes: Long): String {
    if (sizeInBytes < 1024) return "$sizeInBytes B"
    val kb = sizeInBytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.1f MB", mb)
}
