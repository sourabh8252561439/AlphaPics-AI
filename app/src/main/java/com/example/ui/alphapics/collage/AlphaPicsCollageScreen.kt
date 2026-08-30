package com.example.ui.alphapics.collage

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
import com.example.collage.CollageBackgroundMode
import com.example.collage.CollageEngine
import com.example.collage.CollageExportManager
import com.example.collage.CollageLayoutCatalog
import com.example.collage.CollagePhotoTransform
import com.example.collage.CollageState
import com.example.collage.MAX_COLLAGE_PHOTOS
import com.example.editor.EditorPreviewRenderer
import com.example.editor.ExportFormat
import com.example.editor.ExportResult
import com.example.editor.OverlayStickerKind
import com.example.editor.StickerOverlay
import com.example.editor.TextOverlay
import com.example.ui.alphapics.components.AlphaPicsBackdrop
import com.example.ui.alphapics.components.AlphaPicsGlyph
import com.example.ui.alphapics.components.AlphaPicsValueSlider
import com.example.ui.alphapics.navigation.AlphaPicsIcon
import com.example.ui.alphapics.theme.AlphaPicsColors
import com.example.ui.alphapics.theme.AlphaPicsShapes
import com.example.ui.alphapics.theme.AlphaPicsSpacing
import com.example.ui.alphapics.theme.AlphaPicsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.coroutineContext

private enum class CollagePanel(val label: String) {
    LAYOUT("Layout"),
    CANVAS("Canvas"),
    DECORATE("Decorate"),
    EXPORT("Export")
}

private data class CollageUndoSnapshot(
    val photoModels: List<Any>,
    val backgroundModel: Any?,
    val state: CollageState,
    val selectedPhoto: Int
)

@Composable
fun AlphaPicsCollageScreen(
    onBack: () -> Unit,
    initialImageModels: List<Any> = emptyList(),
    initialState: CollageState = CollageState(
        layoutId = defaultLayoutId(initialImageModels.size)
    ).ensurePhotoCount(initialImageModels.size),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var photoModels by remember(initialImageModels) {
        mutableStateOf(initialImageModels.take(MAX_COLLAGE_PHOTOS))
    }
    var previewSources by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var backgroundModel by remember { mutableStateOf<Any?>(null) }
    var backgroundPreview by remember { mutableStateOf<Bitmap?>(null) }
    var state by remember(initialImageModels, initialState) {
        mutableStateOf(initialState.ensurePhotoCount(initialImageModels.size))
    }
    var selectedPhoto by remember { mutableIntStateOf(0) }
    var panel by remember { mutableStateOf(CollagePanel.LAYOUT) }
    var freestyleFrameMode by remember { mutableStateOf(false) }
    var collagePreview by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingSources by remember { mutableStateOf(false) }
    var previewError by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }
    var exportFormat by remember { mutableStateOf(ExportFormat.JPEG) }
    var exportQuality by remember { mutableFloatStateOf(94f) }
    var exportResult by remember { mutableStateOf<ExportResult?>(null) }
    var exportError by remember { mutableStateOf<String?>(null) }
    var decorateMode by remember { mutableStateOf("text") }
    var undoStack by remember { mutableStateOf<List<CollageUndoSnapshot>>(emptyList()) }
    var redoStack by remember { mutableStateOf<List<CollageUndoSnapshot>>(emptyList()) }

    fun currentSnapshot() = CollageUndoSnapshot(photoModels, backgroundModel, state, selectedPhoto)

    fun commitChange(
        nextPhotoModels: List<Any> = photoModels,
        nextBackgroundModel: Any? = backgroundModel,
        nextState: CollageState = state,
        nextSelectedPhoto: Int = selectedPhoto
    ) {
        val safePhotos = nextPhotoModels.take(MAX_COLLAGE_PHOTOS)
        val safeSelected = nextSelectedPhoto.coerceIn(0, (safePhotos.size - 1).coerceAtLeast(0))
        val safeState = nextState.ensurePhotoCount(safePhotos.size)
        if (
            safePhotos == photoModels &&
            nextBackgroundModel == backgroundModel &&
            safeState == state &&
            safeSelected == selectedPhoto
        ) return
        undoStack = (undoStack + currentSnapshot()).takeLast(40)
        redoStack = emptyList()
        photoModels = safePhotos
        backgroundModel = nextBackgroundModel
        state = safeState
        selectedPhoto = safeSelected
        exportResult = null
        exportError = null
    }

    fun undoLastChange() {
        val snapshot = undoStack.lastOrNull() ?: return
        redoStack = (redoStack + currentSnapshot()).takeLast(40)
        undoStack = undoStack.dropLast(1)
        photoModels = snapshot.photoModels
        backgroundModel = snapshot.backgroundModel
        state = snapshot.state
        selectedPhoto = snapshot.selectedPhoto
        exportResult = null
        exportError = null
    }

    fun redoLastChange() {
        val snapshot = redoStack.lastOrNull() ?: return
        undoStack = (undoStack + currentSnapshot()).takeLast(40)
        redoStack = redoStack.dropLast(1)
        photoModels = snapshot.photoModels
        backgroundModel = snapshot.backgroundModel
        state = snapshot.state
        selectedPhoto = snapshot.selectedPhoto
        exportResult = null
        exportError = null
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_COLLAGE_PHOTOS)
    ) { uris ->
        if (uris.isNotEmpty()) {
            commitChange(
                nextPhotoModels = uris,
                nextState = state.copy(layoutId = defaultLayoutId(uris.size)),
                nextSelectedPhoto = 0
            )
        }
    }
    val backgroundPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            commitChange(
                nextBackgroundModel = uri,
                nextState = state.copy(background = state.background.copy(mode = CollageBackgroundMode.IMAGE))
            )
        }
    }

    fun launchPhotoPicker() {
        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    LaunchedEffect(photoModels) {
        if (photoModels.isEmpty()) {
            previewSources = emptyList()
            collagePreview = null
            return@LaunchedEffect
        }
        isLoadingSources = true
        previewError = null
        val loaded = mutableListOf<Bitmap>()
        try {
            photoModels.forEach { model ->
                coroutineContext.ensureActive()
                loaded += EditorPreviewRenderer.loadSource(context, model).getOrThrow()
            }
            previewSources = loaded
            state = state.ensurePhotoCount(loaded.size)
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            previewError = error.localizedMessage ?: "Unable to open one of these photos."
            loaded.filterNot { it.isRecycled }.forEach { it.recycle() }
        } finally {
            isLoadingSources = false
        }
    }

    LaunchedEffect(backgroundModel) {
        backgroundPreview = backgroundModel?.let { model ->
            EditorPreviewRenderer.loadSource(context, model).getOrNull()
        }
    }

    LaunchedEffect(previewSources, state, backgroundPreview) {
        if (previewSources.isEmpty()) return@LaunchedEffect
        delay(45)
        previewError = null
        val ratio = state.aspectRatio
        val previewLongEdge = 900
        val width = if (ratio >= 1f) previewLongEdge else (previewLongEdge * ratio).toInt().coerceAtLeast(1)
        val height = if (ratio >= 1f) (previewLongEdge / ratio).toInt().coerceAtLeast(1) else previewLongEdge
        collagePreview = try {
            withContext(Dispatchers.Default) {
                val renderContext = coroutineContext
                CollageEngine.render(
                    sources = previewSources,
                    state = state,
                    outputWidth = width,
                    outputHeight = height,
                    backgroundImage = backgroundPreview,
                    allowIncomplete = true,
                    checkpoint = { renderContext.ensureActive() }
                )
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            previewError = error.localizedMessage ?: "Unable to render collage preview."
            null
        }
    }

    fun performExport() {
        val uris = photoModels.mapNotNull { it as? Uri }
        val preset = CollageLayoutCatalog.find(state.layoutId)
        val required = if (preset.isFreestyle) 2 else preset.requiredPhotos
        if (photoModels.size < required) {
            exportError = "${preset.label} needs $required photos."
            return
        }
        if (uris.size != photoModels.size) {
            exportError = "Choose device photos before exporting this preview."
            return
        }
        if (state.background.mode == CollageBackgroundMode.IMAGE && backgroundModel !is Uri) {
            exportError = "Choose a background image or use Solid or Gradient."
            return
        }
        isExporting = true
        exportProgress = 0f
        exportResult = null
        exportError = null
        scope.launch {
            val result = CollageExportManager.export(
                context = context,
                sourceUris = uris,
                state = state,
                backgroundImageUri = backgroundModel as? Uri,
                format = exportFormat,
                quality = exportQuality.toInt(),
                onProgress = { exportProgress = it }
            )
            isExporting = false
            result.onSuccess { exportResult = it }
                .onFailure { exportError = it.localizedMessage ?: "Collage export failed." }
        }
    }

    fun shareResult(result: ExportResult) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = result.mimeType
            putExtra(Intent.EXTRA_STREAM, result.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share collage"))
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
                        .testTag("alphapics_collage_workspace")
                ) {
                    CollageTopBar(
                        photoCount = photoModels.size,
                        onBack = onBack,
                        onAddPhotos = ::launchPhotoPicker,
                        onOpenExport = { panel = CollagePanel.EXPORT }
                    )

                    if (photoModels.isEmpty()) {
                        CollageEmptyState(onAddPhotos = ::launchPhotoPicker, modifier = Modifier.weight(1f))
                        return@Column
                    }

                    Spacer(Modifier.height(AlphaPicsSpacing.Xs))
                    CollageCanvas(
                        preview = collagePreview,
                        state = state,
                        photoCount = previewSources.size,
                        selectedPhoto = selectedPhoto,
                        freestyleFrameMode = freestyleFrameMode,
                        onSelectedPhotoChange = { selectedPhoto = it },
                        onStateChange = { commitChange(nextState = it) },
                        isLoading = isLoadingSources,
                        errorMessage = previewError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    Spacer(Modifier.height(AlphaPicsSpacing.Xs))
                    CollagePhotoStrip(
                        photoCount = photoModels.size,
                        selectedPhoto = selectedPhoto,
                        onSelectedPhotoChange = { selectedPhoto = it },
                        onSwapNext = {
                            if (photoModels.size > 1) {
                                val next = (selectedPhoto + 1) % photoModels.size
                                val swappedPhotos = photoModels.toMutableList().apply {
                                    val temporary = this[selectedPhoto]
                                    this[selectedPhoto] = this[next]
                                    this[next] = temporary
                                }
                                commitChange(
                                    nextPhotoModels = swappedPhotos,
                                    nextState = state.swapTransforms(selectedPhoto, next),
                                    nextSelectedPhoto = next
                                )
                            }
                        },
                        onResetView = {
                            commitChange(
                                nextState = state.updatePhotoTransform(selectedPhoto, CollagePhotoTransform())
                            )
                        }
                    )

                    Spacer(Modifier.height(AlphaPicsSpacing.Xs))
                    CollageInspector(
                        panel = panel,
                        onPanelChange = { panel = it },
                        state = state,
                        onStateChange = { commitChange(nextState = it) },
                        canUndo = undoStack.isNotEmpty(),
                        onUndo = ::undoLastChange,
                        canRedo = redoStack.isNotEmpty(),
                        onRedo = ::redoLastChange,
                        photoCount = photoModels.size,
                        freestyleFrameMode = freestyleFrameMode,
                        onFreestyleFrameModeChange = { freestyleFrameMode = it },
                        onPickBackground = {
                            backgroundPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        decorateMode = decorateMode,
                        onDecorateModeChange = { mode ->
                            decorateMode = mode
                            if (mode != "text") focusManager.clearFocus()
                        },
                        exportFormat = exportFormat,
                        onExportFormatChange = { exportFormat = it },
                        exportQuality = exportQuality,
                        onExportQualityChange = { exportQuality = it },
                        isExporting = isExporting,
                        exportProgress = exportProgress,
                        exportResult = exportResult,
                        exportError = exportError,
                        onExport = ::performExport,
                        onShare = ::shareResult,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (panel == CollagePanel.LAYOUT) 210.dp else 292.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CollageTopBar(
    photoCount: Int,
    onBack: () -> Unit,
    onAddPhotos: () -> Unit,
    onOpenExport: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(54.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AlphaPicsColors.TextPrimary)
            }
            Spacer(Modifier.width(5.dp))
            Column {
                Text("Collage Studio", color = AlphaPicsColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (photoCount == 0) "Choose 2–$MAX_COLLAGE_PHOTOS photos" else "$photoCount photos · Offline canvas",
                    color = AlphaPicsColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (photoCount > 0) {
                IconButton(
                    onClick = onAddPhotos,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(AlphaPicsColors.Surface)
                        .border(1.dp, AlphaPicsColors.BorderSoft, CircleShape)
                        .testTag("collage_add_photos")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Replace photos", tint = AlphaPicsColors.Cyan)
                }
            }
            Button(
                onClick = onOpenExport,
                enabled = photoCount >= 2,
                shape = AlphaPicsShapes.Pill,
                modifier = Modifier.height(38.dp).testTag("collage_open_export"),
                colors = ButtonDefaults.buttonColors(containerColor = AlphaPicsColors.ElectricBlue)
            ) {
                Text("Export", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun CollageEmptyState(onAddPhotos: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AlphaPicsShapes.Hero)
                .background(AlphaPicsColors.SurfaceRaised.copy(alpha = 0.82f))
                .border(1.dp, AlphaPicsColors.BorderSoft, AlphaPicsShapes.Hero)
                .padding(AlphaPicsSpacing.Xl)
                .testTag("collage_empty"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AlphaPicsGlyph(icon = AlphaPicsIcon.EDIT, accent = AlphaPicsColors.Cyan, size = 58.dp)
            Spacer(Modifier.height(AlphaPicsSpacing.Md))
            Text("Build a photo story", color = AlphaPicsColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(AlphaPicsSpacing.Xs))
            Text(
                "Choose 2–$MAX_COLLAGE_PHOTOS photos. Everything is arranged and exported on this device.",
                color = AlphaPicsColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(AlphaPicsSpacing.Lg))
            Button(
                onClick = onAddPhotos,
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("collage_choose_photos"),
                shape = AlphaPicsShapes.Medium,
                colors = ButtonDefaults.buttonColors(containerColor = AlphaPicsColors.ElectricBlue)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Choose photos")
            }
        }
    }
}

@Composable
private fun CollageCanvas(
    preview: Bitmap?,
    state: CollageState,
    photoCount: Int,
    selectedPhoto: Int,
    freestyleFrameMode: Boolean,
    onSelectedPhotoChange: (Int) -> Unit,
    onStateChange: (CollageState) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    val currentState by rememberUpdatedState(state)
    val currentSelected by rememberUpdatedState(onSelectedPhotoChange)
    val currentStateChange by rememberUpdatedState(onStateChange)
    val currentFrameMode by rememberUpdatedState(freestyleFrameMode)
    val currentPhotoCount by rememberUpdatedState(photoCount)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val canvasModifier = if (state.aspectRatio >= 1f) {
            Modifier.fillMaxWidth().aspectRatio(state.aspectRatio)
        } else {
            Modifier.fillMaxHeight().aspectRatio(state.aspectRatio)
        }
        Box(
            modifier = canvasModifier
                .clip(AlphaPicsShapes.Card)
                .clipToBounds()
                .background(AlphaPicsColors.Surface)
                .border(1.dp, AlphaPicsColors.BorderFocus, AlphaPicsShapes.Card)
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val activeState = currentState
                        val preset = CollageLayoutCatalog.find(activeState.layoutId)
                        val slots = CollageLayoutCatalog.slots(
                            activeState.layoutId,
                            currentPhotoCount.coerceAtLeast(2),
                            activeState.freestyleRects
                        )
                        val normalizedX = centroid.x / size.width
                        val normalizedY = centroid.y / size.height
                        val index = slots.indexOfLast { rect ->
                            normalizedX in rect.x..(rect.x + rect.width) &&
                                normalizedY in rect.y..(rect.y + rect.height)
                        }.takeIf { it in 0 until currentPhotoCount } ?: return@detectTransformGestures
                        currentSelected(index)
                        if (preset.isFreestyle && currentFrameMode) {
                            val rect = activeState.freestyleRects[index]
                                .movedBy(pan.x / size.width, pan.y / size.height)
                                .scaledBy(zoom)
                            currentStateChange(activeState.updateFreestyleRect(index, rect))
                        } else {
                            val slot = slots[index]
                            val old = activeState.photoTransforms[index]
                            currentStateChange(
                                activeState.updatePhotoTransform(
                                    index,
                                    old.copy(
                                        zoom = old.zoom * zoom,
                                        offsetX = old.offsetX + pan.x / (size.width * slot.width).coerceAtLeast(1f) * 2f,
                                        offsetY = old.offsetY + pan.y / (size.height * slot.height).coerceAtLeast(1f) * 2f
                                    )
                                )
                            )
                        }
                    }
                }
                .testTag("collage_canvas")
        ) {
            if (preview != null && !preview.isRecycled) {
                Image(
                    bitmap = preview.asImageBitmap(),
                    contentDescription = "Collage preview",
                    modifier = Modifier.fillMaxSize().testTag("collage_rendered_preview"),
                    contentScale = ContentScale.FillBounds
                )
            }
            Canvas(Modifier.fillMaxSize()) {
                val slots = CollageLayoutCatalog.slots(
                    state.layoutId,
                    photoCount.coerceAtLeast(2),
                    state.freestyleRects
                )
                slots.forEachIndexed { index, rect ->
                    val left = rect.x * size.width
                    val top = rect.y * size.height
                    val width = rect.width * size.width
                    val height = rect.height * size.height
                    val selected = index == selectedPhoto && index < photoCount
                    drawRoundRect(
                        color = if (selected) AlphaPicsColors.Cyan else Color.White.copy(alpha = 0.18f),
                        topLeft = Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(width, height),
                        cornerRadius = CornerRadius(7.dp.toPx()),
                        style = Stroke(width = if (selected) 2.dp.toPx() else 0.8.dp.toPx())
                    )
                }
            }
            if (isLoading) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AlphaPicsColors.Cyan, strokeWidth = 2.dp)
                }
            }
            if (errorMessage != null) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)).padding(20.dp), contentAlignment = Alignment.Center) {
                    Text(errorMessage, color = AlphaPicsColors.Danger, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun CollagePhotoStrip(
    photoCount: Int,
    selectedPhoto: Int,
    onSelectedPhotoChange: (Int) -> Unit,
    onSwapNext: () -> Unit,
    onResetView: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            repeat(photoCount) { index ->
                CollageChoiceChip(
                    label = "${index + 1}",
                    selected = selectedPhoto == index,
                    onClick = { onSelectedPhotoChange(index) },
                    modifier = Modifier.width(42.dp).testTag("collage_photo_$index")
                )
            }
        }
        Text(
            "Reset view",
            color = AlphaPicsColors.TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.clickable(role = Role.Button, onClick = onResetView).padding(7.dp)
        )
        Text(
            "Swap next",
            color = AlphaPicsColors.Cyan,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.clickable(role = Role.Button, onClick = onSwapNext).padding(7.dp).testTag("collage_swap")
        )
    }
}

@Composable
private fun CollageInspector(
    panel: CollagePanel,
    onPanelChange: (CollagePanel) -> Unit,
    state: CollageState,
    onStateChange: (CollageState) -> Unit,
    canUndo: Boolean,
    onUndo: () -> Unit,
    canRedo: Boolean,
    onRedo: () -> Unit,
    photoCount: Int,
    freestyleFrameMode: Boolean,
    onFreestyleFrameModeChange: (Boolean) -> Unit,
    onPickBackground: () -> Unit,
    decorateMode: String,
    onDecorateModeChange: (String) -> Unit,
    exportFormat: ExportFormat,
    onExportFormatChange: (ExportFormat) -> Unit,
    exportQuality: Float,
    onExportQualityChange: (Float) -> Unit,
    isExporting: Boolean,
    exportProgress: Float,
    exportResult: ExportResult?,
    exportError: String?,
    onExport: () -> Unit,
    onShare: (ExportResult) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(AlphaPicsShapes.Card)
            .background(AlphaPicsColors.SurfaceRaised.copy(alpha = 0.96f))
            .border(1.dp, AlphaPicsColors.BorderSoft, AlphaPicsShapes.Card)
            .padding(AlphaPicsSpacing.Md)
            .testTag("collage_inspector")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)
            ) {
                CollagePanel.entries.forEach { item ->
                    CollageChoiceChip(
                        label = item.label,
                        selected = item == panel,
                        onClick = { onPanelChange(item) },
                        modifier = Modifier.testTag("collage_panel_${item.name.lowercase(Locale.US)}")
                    )
                }
            }
            Spacer(Modifier.width(AlphaPicsSpacing.Xs))
            Text(
                "Undo",
                color = if (canUndo) AlphaPicsColors.Cyan else AlphaPicsColors.TextTertiary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .then(if (canUndo) Modifier.clickable(role = Role.Button, onClick = onUndo) else Modifier)
                    .padding(horizontal = 4.dp, vertical = 9.dp)
                    .testTag("collage_undo")
            )
            Text(
                "Redo",
                color = if (canRedo) AlphaPicsColors.Cyan else AlphaPicsColors.TextTertiary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .then(if (canRedo) Modifier.clickable(role = Role.Button, onClick = onRedo) else Modifier)
                    .padding(horizontal = 4.dp, vertical = 9.dp)
                    .testTag("collage_redo")
            )
        }
        Spacer(Modifier.height(AlphaPicsSpacing.Xs))
        when (panel) {
            CollagePanel.LAYOUT -> CollageLayoutPanel(
                state,
                onStateChange,
                photoCount,
                freestyleFrameMode,
                onFreestyleFrameModeChange,
                Modifier.weight(1f)
            )
            CollagePanel.CANVAS -> CollageCanvasPanel(
                state,
                onStateChange,
                onPickBackground,
                Modifier.weight(1f)
            )
            CollagePanel.DECORATE -> CollageDecoratePanel(
                state,
                onStateChange,
                decorateMode,
                onDecorateModeChange,
                Modifier.weight(1f)
            )
            CollagePanel.EXPORT -> CollageExportPanel(
                state = state,
                onStateChange = onStateChange,
                format = exportFormat,
                onFormatChange = onExportFormatChange,
                quality = exportQuality,
                onQualityChange = onExportQualityChange,
                isExporting = isExporting,
                progress = exportProgress,
                result = exportResult,
                error = exportError,
                onExport = onExport,
                onShare = onShare,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CollageLayoutPanel(
    state: CollageState,
    onStateChange: (CollageState) -> Unit,
    photoCount: Int,
    freestyleFrameMode: Boolean,
    onFreestyleFrameModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)
        ) {
            CollageLayoutCatalog.Presets.forEach { preset ->
                CollageChoiceChip(
                    label = preset.label,
                    selected = state.layoutId == preset.id,
                    onClick = { onStateChange(state.copy(layoutId = preset.id).ensurePhotoCount(photoCount)) },
                    modifier = Modifier.testTag("collage_layout_${preset.id}")
                )
            }
        }
        Spacer(Modifier.height(AlphaPicsSpacing.Sm))
        val preset = CollageLayoutCatalog.find(state.layoutId)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (preset.isFreestyle) "Drag frames or pan photos directly on canvas."
                else if (photoCount >= preset.requiredPhotos) "${preset.requiredPhotos}-photo layout ready"
                else "Add ${preset.requiredPhotos - photoCount} more photo(s) to export",
                color = if (photoCount >= preset.requiredPhotos) AlphaPicsColors.TextSecondary else AlphaPicsColors.Warning,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            if (preset.isFreestyle) {
                Spacer(Modifier.width(AlphaPicsSpacing.Sm))
                CollageChoiceChip(
                    label = if (freestyleFrameMode) "Frame drag" else "Photo pan",
                    selected = freestyleFrameMode,
                    onClick = { onFreestyleFrameModeChange(!freestyleFrameMode) },
                    modifier = Modifier.testTag("collage_freestyle_mode")
                )
            }
        }
    }
}

@Composable
private fun CollageCanvasPanel(
    state: CollageState,
    onStateChange: (CollageState) -> Unit,
    onPickBackground: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            listOf("1:1", "4:5", "3:4", "9:16", "16:9", "4:3").forEach { aspect ->
                CollageChoiceChip(
                    label = aspect,
                    selected = state.aspectId == aspect,
                    onClick = { onStateChange(state.copy(aspectId = aspect)) },
                    modifier = Modifier.testTag("collage_aspect_${aspect.replace(':', '_')}")
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CollageBackgroundMode.entries.forEach { mode ->
                CollageChoiceChip(
                    label = mode.name.lowercase().replaceFirstChar(Char::uppercase),
                    selected = state.background.mode == mode,
                    onClick = {
                        onStateChange(state.copy(background = state.background.copy(mode = mode)))
                        if (mode == CollageBackgroundMode.IMAGE) onPickBackground()
                    },
                    modifier = Modifier.weight(1f).testTag("collage_background_${mode.name.lowercase()}")
                )
            }
        }
        CollageColorPalette(
            selectedColor = state.background.firstColorArgb,
            onColorSelected = { color ->
                onStateChange(
                    state.copy(
                        background = state.background.copy(
                            firstColorArgb = color,
                            secondColorArgb = gradientPartner(color)
                        )
                    )
                )
            }
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Md)) {
            AlphaPicsValueSlider(
                label = "Spacing",
                value = state.spacing,
                onValueChange = { onStateChange(state.copy(spacing = it)) },
                valueRange = 0f..12f,
                modifier = Modifier.weight(1f)
            )
            AlphaPicsValueSlider(
                label = "Corners",
                value = state.cornerRadius,
                onValueChange = { onStateChange(state.copy(cornerRadius = it)) },
                valueRange = 0f..24f,
                modifier = Modifier.weight(1f)
            )
        }
        AlphaPicsValueSlider(
            label = "Border",
            value = state.borderWidth,
            onValueChange = { onStateChange(state.copy(borderWidth = it)) },
            valueRange = 0f..6f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CollageDecoratePanel(
    state: CollageState,
    onStateChange: (CollageState) -> Unit,
    mode: String,
    onModeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            CollageChoiceChip("Text", mode == "text", { onModeChange("text") }, Modifier.testTag("collage_decorate_text"))
            CollageChoiceChip("Stickers", mode == "sticker", { onModeChange("sticker") }, Modifier.testTag("collage_decorate_sticker"))
        }
        if (mode == "text") {
            val overlays = state.overlays
            val template = overlays.textTemplate
            Row(horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Sm), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = overlays.textDraft,
                    onValueChange = { onStateChange(state.copy(overlays = overlays.copy(textDraft = it.take(80)))) },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(AlphaPicsShapes.Medium)
                        .background(AlphaPicsColors.Surface)
                        .border(1.dp, AlphaPicsColors.BorderSoft, AlphaPicsShapes.Medium)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("collage_text_input"),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = AlphaPicsColors.TextPrimary),
                    cursorBrush = SolidColor(AlphaPicsColors.Cyan),
                    singleLine = true
                )
                Button(
                    onClick = {
                        val item = template.copy(text = overlays.textDraft)
                        onStateChange(
                            state.copy(
                                overlays = overlays.copy(textTemplate = item).addText(item)
                            )
                        )
                    },
                    modifier = Modifier.heightIn(min = 40.dp).testTag("collage_add_text"),
                    shape = AlphaPicsShapes.Medium,
                    colors = ButtonDefaults.buttonColors(containerColor = AlphaPicsColors.ElectricBlue)
                ) { Text("Add") }
            }
            CollageColorPalette(
                selectedColor = template.colorArgb,
                onColorSelected = { color ->
                    onStateChange(state.copy(overlays = updateTextTemplate(overlays, template.copy(colorArgb = color))))
                }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Md)) {
                AlphaPicsValueSlider(
                    label = "Size",
                    value = template.fontSize,
                    onValueChange = { onStateChange(state.copy(overlays = updateTextTemplate(overlays, template.copy(fontSize = it)))) },
                    valueRange = 2f..24f,
                    modifier = Modifier.weight(1f)
                )
                AlphaPicsValueSlider(
                    label = "Position X",
                    value = template.x * 100f,
                    onValueChange = { onStateChange(state.copy(overlays = updateTextTemplate(overlays, template.copy(x = it / 100f)))) },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f)
                )
            }
            AlphaPicsValueSlider(
                label = "Position Y",
                value = template.y * 100f,
                onValueChange = { onStateChange(state.copy(overlays = updateTextTemplate(overlays, template.copy(y = it / 100f)))) },
                valueRange = 0f..100f,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            val overlays = state.overlays
            val template = overlays.stickerTemplate
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                OverlayStickerKind.entries.forEach { kind ->
                    CollageChoiceChip(
                        label = kind.name.lowercase().replaceFirstChar(Char::uppercase),
                        selected = template.kind == kind,
                        onClick = {
                            val item = template.copy(kind = kind)
                            onStateChange(state.copy(overlays = overlays.copy(stickerTemplate = item).addSticker(item)))
                        },
                        modifier = Modifier.weight(1f).testTag("collage_sticker_${kind.name.lowercase()}")
                    )
                }
                CollageChoiceChip(
                    label = "Delete",
                    selected = false,
                    onClick = { onStateChange(state.copy(overlays = overlays.copy(stickers = overlays.stickers.dropLast(1)))) }
                )
            }
            CollageColorPalette(
                selectedColor = template.colorArgb,
                onColorSelected = { color ->
                    onStateChange(state.copy(overlays = updateStickerTemplate(overlays, template.copy(colorArgb = color))))
                }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Md)) {
                AlphaPicsValueSlider(
                    label = "Size",
                    value = template.scale,
                    onValueChange = { onStateChange(state.copy(overlays = updateStickerTemplate(overlays, template.copy(scale = it)))) },
                    valueRange = 3f..50f,
                    modifier = Modifier.weight(1f)
                )
                AlphaPicsValueSlider(
                    label = "Rotate",
                    value = template.rotation,
                    onValueChange = { onStateChange(state.copy(overlays = updateStickerTemplate(overlays, template.copy(rotation = it)))) },
                    valueRange = -180f..180f,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Md)) {
                AlphaPicsValueSlider(
                    label = "Position X",
                    value = template.x * 100f,
                    onValueChange = { onStateChange(state.copy(overlays = updateStickerTemplate(overlays, template.copy(x = it / 100f)))) },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f)
                )
                AlphaPicsValueSlider(
                    label = "Position Y",
                    value = template.y * 100f,
                    onValueChange = { onStateChange(state.copy(overlays = updateStickerTemplate(overlays, template.copy(y = it / 100f)))) },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CollageExportPanel(
    state: CollageState,
    onStateChange: (CollageState) -> Unit,
    format: ExportFormat,
    onFormatChange: (ExportFormat) -> Unit,
    quality: Float,
    onQualityChange: (Float) -> Unit,
    isExporting: Boolean,
    progress: Float,
    result: ExportResult?,
    error: String?,
    onExport: () -> Unit,
    onShare: (ExportResult) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            ExportFormat.entries.forEach { item ->
                CollageChoiceChip(
                    label = item.name,
                    selected = format == item,
                    onClick = { onFormatChange(item) },
                    modifier = Modifier.weight(1f).testTag("collage_format_${item.name.lowercase()}")
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf(2048 to "2K", 3072 to "3K", 4096 to "4K").forEach { (edge, label) ->
                CollageChoiceChip(
                    label = label,
                    selected = state.outputLongEdge == edge,
                    onClick = { onStateChange(state.copy(outputLongEdge = edge)) },
                    modifier = Modifier.weight(1f).testTag("collage_resolution_$edge")
                )
            }
        }
        val dimensions = state.outputDimensions()
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AlphaPicsValueSlider(
                label = "Quality",
                value = quality,
                onValueChange = onQualityChange,
                valueRange = 40f..100f,
                valueFormatter = { "${it.toInt()}%" },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(AlphaPicsSpacing.Md))
            Text(
                "${dimensions.first} × ${dimensions.second}",
                color = AlphaPicsColors.TextSecondary,
                style = MaterialTheme.typography.labelMedium
            )
        }
        if (isExporting) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(28.dp), color = AlphaPicsColors.Cyan, strokeWidth = 3.dp)
                Text("Rendering locally · ${(progress * 100).toInt()}%", color = AlphaPicsColors.TextSecondary)
            }
        } else if (result != null) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = AlphaPicsColors.Success)
                Spacer(Modifier.width(6.dp))
                Text(
                    "Saved · ${formatBytes(result.sizeBytes)}",
                    color = AlphaPicsColors.Success,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onShare(result) }, modifier = Modifier.testTag("collage_share")) {
                    Icon(Icons.Filled.Share, contentDescription = "Share collage", tint = AlphaPicsColors.Cyan)
                }
            }
        } else if (error != null) {
            Text(error, color = AlphaPicsColors.Danger, style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }
        Button(
            onClick = onExport,
            enabled = !isExporting,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("collage_export"),
            shape = AlphaPicsShapes.Medium,
            colors = ButtonDefaults.buttonColors(containerColor = AlphaPicsColors.ElectricBlue)
        ) {
            Text(if (result == null) "Save high-resolution collage" else "Save another copy")
        }
    }
}

@Composable
private fun CollageColorPalette(selectedColor: Long, onColorSelected: (Long) -> Unit) {
    val colors = listOf(0xFF070B14, 0xFFFFFFFF, 0xFF1D335F, 0xFF2F7BFF, 0xFF26D9FF, 0xFF9B6CFF, 0xFFFFC34D)
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEachIndexed { index, color ->
            val selected = selectedColor == color
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(color.toInt()))
                    .border(if (selected) 2.5.dp else 1.dp, if (selected) AlphaPicsColors.Cyan else AlphaPicsColors.BorderSoft, CircleShape)
                    .clickable(role = Role.RadioButton) { onColorSelected(color) }
                    .semantics { this.selected = selected; contentDescription = "Collage color ${index + 1}" }
                    .testTag("collage_color_$index")
            )
        }
    }
}

@Composable
private fun CollageChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 38.dp)
            .clip(AlphaPicsShapes.Medium)
            .background(if (selected) AlphaPicsColors.ElectricBlue.copy(alpha = 0.24f) else AlphaPicsColors.Surface)
            .border(
                if (selected) 1.5.dp else 1.dp,
                if (selected) AlphaPicsColors.BrightBlue else AlphaPicsColors.BorderSoft,
                AlphaPicsShapes.Medium
            )
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 11.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) AlphaPicsColors.TextPrimary else AlphaPicsColors.TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun updateTextTemplate(
    overlays: com.example.editor.OverlayAdjustments,
    template: TextOverlay
): com.example.editor.OverlayAdjustments = overlays.copy(
    textTemplate = template.sanitized(),
    texts = if (overlays.texts.isEmpty()) overlays.texts else overlays.texts.dropLast(1) + template.sanitized()
)

private fun updateStickerTemplate(
    overlays: com.example.editor.OverlayAdjustments,
    template: StickerOverlay
): com.example.editor.OverlayAdjustments = overlays.copy(
    stickerTemplate = template.sanitized(),
    stickers = if (overlays.stickers.isEmpty()) overlays.stickers else overlays.stickers.dropLast(1) + template.sanitized()
)

private fun defaultLayoutId(photoCount: Int): String = when (photoCount) {
    3 -> CollageLayoutCatalog.ThreeFeature.id
    4 -> CollageLayoutCatalog.FourGrid.id
    5 -> CollageLayoutCatalog.FiveMosaic.id
    6 -> CollageLayoutCatalog.SixGrid.id
    else -> CollageLayoutCatalog.TwoSplit.id
}

private fun gradientPartner(color: Long): Long = when (color) {
    0xFF070B14 -> 0xFF172448
    0xFFFFFFFF -> 0xFFB9C9E8
    0xFF1D335F -> 0xFF5A2F8D
    0xFF2F7BFF -> 0xFF713BCA
    0xFF26D9FF -> 0xFF2F7BFF
    0xFF9B6CFF -> 0xFF352060
    else -> 0xFFFF6D75
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(Locale.US, bytes / 1024.0)
    else -> "%.1f MB".format(Locale.US, bytes / 1024.0 / 1024.0)
}
