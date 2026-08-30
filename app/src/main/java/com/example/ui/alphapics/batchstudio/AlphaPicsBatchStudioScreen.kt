package com.example.ui.alphapics.batchstudio

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.batch.MAX_BATCH_ITEMS
import com.example.batchstudio.BatchOutputFormat
import com.example.batchstudio.BatchPlacement
import com.example.batchstudio.BatchResizeMode
import com.example.batchstudio.BatchStudioEngine
import com.example.batchstudio.BatchStudioItem
import com.example.batchstudio.BatchStudioItemStatus
import com.example.batchstudio.BatchStudioSettings
import com.example.batchstudio.BatchStudioTool
import com.example.batchstudio.BatchStudioViewModel
import com.example.editor.FilterPresetCatalog
import com.example.ui.alphapics.components.AlphaPicsBackdrop
import com.example.ui.alphapics.components.AlphaPicsGlyph
import com.example.ui.alphapics.components.AlphaPicsValueSlider
import com.example.ui.alphapics.components.AlphaPicsWorkspaceTopBar
import com.example.ui.alphapics.navigation.AlphaPicsIcon
import com.example.ui.alphapics.theme.AlphaPicsColors
import com.example.ui.alphapics.theme.AlphaPicsShapes
import com.example.ui.alphapics.theme.AlphaPicsSpacing
import com.example.ui.alphapics.theme.AlphaPicsTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun AlphaPicsBatchStudioScreen(
    onBack: () -> Unit,
    onOpenBatchCompress: () -> Unit,
    modifier: Modifier = Modifier,
    initialUris: List<Uri> = emptyList(),
    viewModel: BatchStudioViewModel = viewModel()
) {
    val context = LocalContext.current
    val items by viewModel.items.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isInspecting by viewModel.isInspecting.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val overallProgress by viewModel.overallProgress.collectAsState()
    val summary by viewModel.summary.collectAsState()
    var selectedTool by remember { mutableStateOf(BatchStudioTool.RESIZE) }
    var undoStack by remember { mutableStateOf<List<BatchStudioSettings>>(emptyList()) }
    var redoStack by remember { mutableStateOf<List<BatchStudioSettings>>(emptyList()) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var previewError by remember { mutableStateOf<String?>(null) }
    var isPreviewing by remember { mutableStateOf(false) }

    fun commit(next: BatchStudioSettings) {
        if (isProcessing || next == settings) return
        undoStack = (undoStack + settings).takeLast(40)
        redoStack = emptyList()
        viewModel.updateSettings(next)
    }

    fun undo() {
        val previous = undoStack.lastOrNull() ?: return
        redoStack = (redoStack + settings).takeLast(40)
        undoStack = undoStack.dropLast(1)
        viewModel.updateSettings(previous)
    }

    fun redo() {
        val next = redoStack.lastOrNull() ?: return
        undoStack = (undoStack + settings).takeLast(40)
        redoStack = redoStack.dropLast(1)
        viewModel.updateSettings(next)
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_BATCH_ITEMS)
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.setSelection(context, uris)
    }
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) commit(settings.copy(logoEnabled = true, logoUri = uri))
    }

    LaunchedEffect(initialUris) {
        if (initialUris.isNotEmpty() && items.isEmpty()) viewModel.setSelection(context, initialUris)
    }

    LaunchedEffect(items.firstOrNull()?.uri, settings) {
        val source = items.firstOrNull()?.uri
        if (source == null) {
            preview = null
            previewError = null
            return@LaunchedEffect
        }
        delay(70)
        isPreviewing = true
        previewError = null
        try {
            val result = BatchStudioEngine.renderPreview(context, source, settings)
            result.onSuccess { rendered -> preview = rendered }
                .onFailure { previewError = it.localizedMessage ?: "Preview unavailable" }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } finally {
            isPreviewing = false
        }
    }

    DisposableEffect(preview) {
        val current = preview
        onDispose { current?.takeUnless(Bitmap::isRecycled)?.recycle() }
    }

    fun shareOutputs() {
        val outputs = ArrayList(items.mapNotNull { it.output?.uri })
        if (outputs.isEmpty()) return
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "image/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, outputs)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Share batch"
            )
        )
    }

    AlphaPicsTheme {
        AlphaPicsBackdrop(modifier) {
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
                        .testTag("alphapics_batch_studio")
                ) {
                    AlphaPicsWorkspaceTopBar(
                        title = "Batch Studio",
                        subtitle = "${items.size} of $MAX_BATCH_ITEMS · local device",
                        onBack = onBack,
                        trailing = {
                            Text(
                                "Compress",
                                color = AlphaPicsColors.Cyan,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier
                                    .clickable(role = Role.Button, onClick = onOpenBatchCompress)
                                    .padding(10.dp)
                                    .testTag("batch_studio_open_compress")
                            )
                        }
                    )
                    Spacer(Modifier.height(AlphaPicsSpacing.Xs))
                    BatchSelectionActions(
                        itemCount = items.size,
                        enabled = !isProcessing,
                        onAdd = {
                            picker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onClear = viewModel::clearAll
                    )
                    if (items.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            BatchStudioEmptyState(
                                isInspecting = isInspecting,
                                onChoose = {
                                    picker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            )
                        }
                        return@Column
                    }
                    Spacer(Modifier.height(AlphaPicsSpacing.Xs))
                    BatchPreview(
                        preview = preview,
                        isLoading = isPreviewing,
                        error = previewError,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                    Spacer(Modifier.height(6.dp))
                    BatchItemRail(
                        items = items,
                        enabled = !isProcessing,
                        onRemove = viewModel::removeItem
                    )
                    Spacer(Modifier.height(8.dp))
                    BatchInspector(
                        selectedTool = selectedTool,
                        onSelectedTool = { selectedTool = it },
                        settings = settings,
                        onSettingsChange = ::commit,
                        onChooseLogo = {
                            logoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        canUndo = undoStack.isNotEmpty(),
                        canRedo = redoStack.isNotEmpty(),
                        onUndo = ::undo,
                        onRedo = ::redo,
                        itemCount = items.size,
                        isProcessing = isProcessing,
                        progress = overallProgress,
                        summary = summary,
                        onProcess = { viewModel.processAll(context) },
                        onCancel = viewModel::cancelProcessing,
                        onShare = ::shareOutputs,
                        modifier = Modifier.fillMaxWidth().height(355.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BatchSelectionActions(
    itemCount: Int,
    enabled: Boolean,
    onAdd: () -> Unit,
    onClear: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onAdd,
            enabled = enabled,
            modifier = Modifier.weight(1f).height(44.dp).testTag("batch_studio_add")
        ) {
            Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (itemCount == 0) "Choose photos" else "Replace photos")
        }
        if (itemCount > 0) {
            OutlinedButton(
                onClick = onClear,
                enabled = enabled,
                modifier = Modifier.height(44.dp).testTag("batch_studio_clear")
            ) {
                Text("Clear")
            }
        }
    }
}

@Composable
private fun BatchStudioEmptyState(isInspecting: Boolean, onChoose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AlphaPicsShapes.Card)
            .background(AlphaPicsColors.SurfaceRaised.copy(alpha = 0.92f))
            .border(1.dp, AlphaPicsColors.BorderFocus, AlphaPicsShapes.Card)
            .padding(AlphaPicsSpacing.Xl)
            .testTag("batch_studio_empty"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AlphaPicsGlyph(AlphaPicsIcon.BATCH, AlphaPicsColors.Violet, size = 68.dp)
        Spacer(Modifier.height(AlphaPicsSpacing.Md))
        Text("One setup. Every photo.", color = AlphaPicsColors.TextPrimary, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(
            "Resize, convert, watermark, pad, align, or apply a safe local preset to up to 20 photos.",
            color = AlphaPicsColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(AlphaPicsSpacing.Lg))
        Button(
            onClick = onChoose,
            enabled = !isInspecting,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AlphaPicsColors.ElectricBlue)
        ) {
            if (isInspecting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("Choose photos")
        }
    }
}

@Composable
private fun BatchPreview(preview: Bitmap?, isLoading: Boolean, error: String?, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(AlphaPicsShapes.Hero)
            .clipToBounds()
            .background(Color(0xFF02040A))
            .border(1.dp, AlphaPicsColors.BorderFocus, AlphaPicsShapes.Hero)
            .testTag("batch_studio_preview_canvas"),
        contentAlignment = Alignment.Center
    ) {
        if (preview != null && !preview.isRecycled) {
            Image(
                bitmap = preview.asImageBitmap(),
                contentDescription = "First batch photo preview",
                modifier = Modifier.fillMaxSize().testTag("batch_studio_preview"),
                contentScale = ContentScale.Fit
            )
        }
        if (isLoading) CircularProgressIndicator(color = AlphaPicsColors.Cyan, strokeWidth = 2.dp)
        if (error != null) Text(error, color = AlphaPicsColors.Danger, textAlign = TextAlign.Center, modifier = Modifier.padding(20.dp))
        Text(
            "PREVIEW · FIRST PHOTO",
            color = AlphaPicsColors.TextPrimary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .background(Color.Black.copy(alpha = 0.62f), AlphaPicsShapes.Pill)
                .padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun BatchItemRail(items: List<BatchStudioItem>, enabled: Boolean, onRemove: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().height(86.dp).testTag("batch_studio_items"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = BatchStudioItem::id) { item ->
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(AlphaPicsShapes.Medium)
                    .background(AlphaPicsColors.Surface)
                    .border(1.dp, statusColor(item.status), AlphaPicsShapes.Medium)
                    .testTag("batch_studio_item_${item.id.hashCode()}")
            ) {
                AsyncImage(item.uri, item.displayName, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Text(
                    statusLabel(item.status),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(4.dp)
                )
                if (item.status == BatchStudioItemStatus.PROCESSING) {
                    CircularProgressIndicator(
                        progress = { item.progress },
                        modifier = Modifier.align(Alignment.Center).size(28.dp),
                        color = AlphaPicsColors.Cyan,
                        strokeWidth = 3.dp
                    )
                } else if (item.status == BatchStudioItemStatus.SUCCEEDED) {
                    Icon(Icons.Filled.Check, "Saved", tint = AlphaPicsColors.Success, modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(20.dp))
                } else if (item.status == BatchStudioItemStatus.FAILED) {
                    Icon(Icons.Filled.Close, item.errorMessage ?: "Failed", tint = AlphaPicsColors.Danger, modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(20.dp))
                } else if (enabled) {
                    IconButton(onClick = { onRemove(item.id) }, modifier = Modifier.align(Alignment.TopEnd).size(32.dp)) {
                        Icon(Icons.Filled.Close, "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchInspector(
    selectedTool: BatchStudioTool,
    onSelectedTool: (BatchStudioTool) -> Unit,
    settings: BatchStudioSettings,
    onSettingsChange: (BatchStudioSettings) -> Unit,
    onChooseLogo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    itemCount: Int,
    isProcessing: Boolean,
    progress: Float,
    summary: com.example.batchstudio.BatchStudioSummary?,
    onProcess: () -> Unit,
    onCancel: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .clip(AlphaPicsShapes.Card)
            .background(AlphaPicsColors.SurfaceRaised.copy(alpha = 0.97f))
            .border(1.dp, AlphaPicsColors.BorderSoft, AlphaPicsShapes.Card)
            .padding(AlphaPicsSpacing.Md)
            .testTag("batch_studio_inspector")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BatchStudioTool.entries.forEach { tool ->
                    BatchChip(
                        tool.label,
                        selectedTool == tool,
                        { onSelectedTool(tool) },
                        Modifier.testTag("batch_tool_${tool.name.lowercase()}")
                    )
                }
            }
            HistoryAction("Undo", canUndo && !isProcessing, onUndo, "batch_studio_undo")
            HistoryAction("Redo", canRedo && !isProcessing, onRedo, "batch_studio_redo")
        }
        Spacer(Modifier.height(6.dp))
        when (selectedTool) {
            BatchStudioTool.RESIZE -> ResizeControls(settings, onSettingsChange, Modifier.weight(1f))
            BatchStudioTool.CONVERT -> ConvertControls(settings, onSettingsChange, Modifier.weight(1f))
            BatchStudioTool.WATERMARK -> WatermarkControls(settings, onSettingsChange, Modifier.weight(1f))
            BatchStudioTool.LOGO -> LogoControls(settings, onSettingsChange, onChooseLogo, Modifier.weight(1f))
            BatchStudioTool.PADDING -> PaddingControls(settings, onSettingsChange, Modifier.weight(1f))
            BatchStudioTool.PRESET -> PresetControls(settings, onSettingsChange, Modifier.weight(1f))
        }
        BatchProcessFooter(
            itemCount,
            settings,
            isProcessing,
            progress,
            summary,
            onProcess,
            onCancel,
            onShare
        )
    }
}

@Composable
private fun ResizeControls(settings: BatchStudioSettings, onChange: (BatchStudioSettings) -> Unit, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BatchChip("Original", settings.resizeMode == BatchResizeMode.NONE, { onChange(settings.copy(resizeMode = BatchResizeMode.NONE)) }, Modifier.testTag("batch_resize_none"))
            BatchChip("Same box", settings.resizeMode == BatchResizeMode.DIMENSIONS, { onChange(settings.copy(resizeMode = BatchResizeMode.DIMENSIONS)) }, Modifier.testTag("batch_resize_dimensions"))
            BatchChip("Percentage", settings.resizeMode == BatchResizeMode.PERCENTAGE, { onChange(settings.copy(resizeMode = BatchResizeMode.PERCENTAGE)) }, Modifier.testTag("batch_resize_percentage"))
        }
        when (settings.resizeMode) {
            BatchResizeMode.NONE -> Text("Original dimensions are retained for every item.", color = AlphaPicsColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            BatchResizeMode.DIMENSIONS -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BatchNumberInput("Width", settings.targetWidth, { onChange(settings.copy(targetWidth = it)) }, Modifier.weight(1f).testTag("batch_resize_width"))
                    BatchNumberInput("Height", settings.targetHeight, { onChange(settings.copy(targetHeight = it)) }, Modifier.weight(1f).testTag("batch_resize_height"))
                }
                BatchChip(
                    if (settings.maintainAspectRatio) "Fit inside · ratio locked" else "Exact dimensions · free ratio",
                    settings.maintainAspectRatio,
                    { onChange(settings.copy(maintainAspectRatio = !settings.maintainAspectRatio)) },
                    Modifier.testTag("batch_resize_aspect")
                )
            }
            BatchResizeMode.PERCENTAGE -> AlphaPicsValueSlider(
                label = "Scale every photo",
                value = settings.percentage,
                onValueChange = { onChange(settings.copy(percentage = it)) },
                valueRange = 1f..400f,
                valueFormatter = { "${it.roundToInt()}%" }
            )
        }
    }
}

@Composable
private fun ConvertControls(settings: BatchStudioSettings, onChange: (BatchStudioSettings) -> Unit, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BatchOutputFormat.entries.forEach { format ->
                BatchChip(
                    format.label,
                    settings.outputFormat == format,
                    { onChange(settings.copy(outputFormat = format)) },
                    Modifier.weight(1f).testTag("batch_format_${format.name.lowercase()}")
                )
            }
        }
        if (settings.outputFormat != BatchOutputFormat.PNG) {
            AlphaPicsValueSlider(
                label = "Quality",
                value = settings.quality.toFloat(),
                onValueChange = { onChange(settings.copy(quality = it.roundToInt())) },
                valueRange = 40f..100f,
                valueFormatter = { "${it.roundToInt()}%" }
            )
        }
        Text(
            "Keep type retains JPEG, PNG, or WebP when possible; other readable photo formats export as JPEG. Output metadata is removed.",
            color = AlphaPicsColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun WatermarkControls(settings: BatchStudioSettings, onChange: (BatchStudioSettings) -> Unit, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        BatchChip(
            if (settings.watermarkEnabled) "Text watermark on" else "Text watermark off",
            settings.watermarkEnabled,
            { onChange(settings.copy(watermarkEnabled = !settings.watermarkEnabled)) },
            Modifier.testTag("batch_watermark_enabled")
        )
        BatchTextInput("Watermark", settings.watermarkText, { onChange(settings.copy(watermarkText = it)) }, Modifier.testTag("batch_watermark_text"))
        PlacementRow(settings.watermarkPlacement) { onChange(settings.copy(watermarkPlacement = it)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AlphaPicsValueSlider(
                "Size",
                settings.watermarkScale,
                { onChange(settings.copy(watermarkScale = it)) },
                Modifier.weight(1f),
                1.5f..12f,
                valueFormatter = { "%.1f%%".format(it) }
            )
            AlphaPicsValueSlider(
                "Opacity",
                settings.watermarkOpacity,
                { onChange(settings.copy(watermarkOpacity = it)) },
                Modifier.weight(1f),
                0f..100f,
                valueFormatter = { "${it.roundToInt()}%" }
            )
        }
    }
}

@Composable
private fun LogoControls(
    settings: BatchStudioSettings,
    onChange: (BatchStudioSettings) -> Unit,
    onChooseLogo: () -> Unit,
    modifier: Modifier
) {
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BatchChip(
                if (settings.logoEnabled) "Logo on" else "Logo off",
                settings.logoEnabled,
                { onChange(settings.copy(logoEnabled = !settings.logoEnabled)) },
                Modifier.testTag("batch_logo_enabled")
            )
            OutlinedButton(onClick = onChooseLogo, modifier = Modifier.height(40.dp).testTag("batch_logo_choose")) {
                Text(if (settings.logoUri == null) "Choose logo" else "Replace logo")
            }
        }
        Text(
            if (settings.logoUri == null) "Choose a local transparent PNG or photo. Nothing is uploaded."
            else "Logo selected · local device layer",
            color = AlphaPicsColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
        PlacementRow(settings.logoPlacement) { onChange(settings.copy(logoPlacement = it)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AlphaPicsValueSlider(
                "Size",
                settings.logoScale,
                { onChange(settings.copy(logoScale = it)) },
                Modifier.weight(1f),
                5f..50f,
                valueFormatter = { "${it.roundToInt()}%" }
            )
            AlphaPicsValueSlider(
                "Opacity",
                settings.logoOpacity,
                { onChange(settings.copy(logoOpacity = it)) },
                Modifier.weight(1f),
                0f..100f,
                valueFormatter = { "${it.roundToInt()}%" }
            )
        }
    }
}

@Composable
private fun PaddingControls(settings: BatchStudioSettings, onChange: (BatchStudioSettings) -> Unit, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        BatchChip(
            if (settings.paddingEnabled) "Canvas padding on" else "Canvas padding off",
            settings.paddingEnabled,
            { onChange(settings.copy(paddingEnabled = !settings.paddingEnabled)) },
            Modifier.testTag("batch_padding_enabled")
        )
        AlphaPicsValueSlider(
            "Padding",
            settings.paddingPercent,
            { onChange(settings.copy(paddingPercent = it)) },
            valueRange = 0f..50f,
            valueFormatter = { "${it.roundToInt()}%" }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                "Navy" to 0xFF050814L,
                "Black" to 0xFF000000L,
                "White" to 0xFFFFFFFFL,
                "Clear" to 0x00000000L
            ).forEach { (label, color) ->
                BatchChip(label, settings.paddingColorArgb == color, { onChange(settings.copy(paddingColorArgb = color)) })
            }
        }
        Text("Photo alignment inside the expanded canvas", color = AlphaPicsColors.TextTertiary, style = MaterialTheme.typography.labelSmall)
        PlacementRow(settings.alignment) { onChange(settings.copy(alignment = it)) }
    }
}

@Composable
private fun PresetControls(settings: BatchStudioSettings, onChange: (BatchStudioSettings) -> Unit, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        BatchChip(
            if (settings.presetEnabled) "Preset on" else "Preset off",
            settings.presetEnabled,
            { onChange(settings.copy(presetEnabled = !settings.presetEnabled)) },
            Modifier.testTag("batch_preset_enabled")
        )
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterPresetCatalog.Presets.take(12).forEach { preset ->
                BatchChip(
                    preset.label,
                    settings.presetId == preset.id,
                    { onChange(settings.copy(presetEnabled = preset.id != "original", presetId = preset.id)) },
                    Modifier.testTag("batch_preset_${preset.id}")
                )
            }
        }
        AlphaPicsValueSlider(
            "Preset intensity",
            settings.presetIntensity,
            { onChange(settings.copy(presetIntensity = it)) },
            valueRange = 0f..100f,
            valueFormatter = { "${it.roundToInt()}%" }
        )
        Text("Only deterministic local AlphaPics looks are available in Batch Studio.", color = AlphaPicsColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PlacementRow(selected: BatchPlacement, onSelected: (BatchPlacement) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        BatchPlacement.entries.forEach { placement ->
            BatchChip(placement.label, selected == placement, { onSelected(placement) })
        }
    }
}

@Composable
private fun BatchProcessFooter(
    itemCount: Int,
    settings: BatchStudioSettings,
    isProcessing: Boolean,
    progress: Float,
    summary: com.example.batchstudio.BatchStudioSummary?,
    onProcess: () -> Unit,
    onCancel: () -> Unit,
    onShare: () -> Unit
) {
    if (isProcessing) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(5.dp).testTag("batch_studio_progress"),
            color = AlphaPicsColors.Cyan,
            trackColor = AlphaPicsColors.Surface
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Processing locally · ${(progress * 100).roundToInt()}%", color = AlphaPicsColors.TextSecondary, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onCancel, modifier = Modifier.height(40.dp).testTag("batch_studio_cancel")) { Text("Cancel") }
        }
        return
    }
    if (summary != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${summary.succeeded} saved · ${summary.failed} failed${if (summary.cancelled > 0) " · ${summary.cancelled} cancelled" else ""}",
                color = if (summary.failed == 0) AlphaPicsColors.Success else AlphaPicsColors.Warning,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            if (summary.succeeded > 0) {
                IconButton(onClick = onShare, modifier = Modifier.testTag("batch_studio_share")) {
                    Icon(Icons.Filled.Share, "Share saved photos", tint = AlphaPicsColors.Cyan)
                }
            }
        }
    }
    Button(
        onClick = onProcess,
        enabled = itemCount > 0 && settings.hasVisibleOperation,
        modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp).testTag("batch_studio_process"),
        colors = ButtonDefaults.buttonColors(containerColor = AlphaPicsColors.ElectricBlue),
        shape = AlphaPicsShapes.Medium
    ) {
        Text(
            if (settings.hasVisibleOperation) "Process $itemCount photo${if (itemCount == 1) "" else "s"}"
            else "Choose at least one batch tool"
        )
    }
}

@Composable
private fun BatchNumberInput(label: String, value: Int, onValue: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = AlphaPicsColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
        BasicTextField(
            value = value.toString(),
            onValueChange = { onValue(it.filter(Char::isDigit).take(4).toIntOrNull() ?: 1) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = AlphaPicsColors.TextPrimary, fontWeight = FontWeight.Bold),
            cursorBrush = SolidColor(AlphaPicsColors.Cyan),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(AlphaPicsShapes.Medium)
                .background(AlphaPicsColors.Surface)
                .border(1.dp, AlphaPicsColors.BorderSoft, AlphaPicsShapes.Medium)
                .padding(horizontal = 11.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun BatchTextInput(label: String, value: String, onValue: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = AlphaPicsColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
        BasicTextField(
            value = value,
            onValueChange = { onValue(it.take(80)) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = AlphaPicsColors.TextPrimary),
            cursorBrush = SolidColor(AlphaPicsColors.Cyan),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(AlphaPicsShapes.Medium)
                .background(AlphaPicsColors.Surface)
                .border(1.dp, AlphaPicsColors.BorderSoft, AlphaPicsShapes.Medium)
                .padding(horizontal = 11.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun BatchChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(AlphaPicsShapes.Pill)
            .background(if (selected) AlphaPicsColors.ElectricBlue.copy(alpha = 0.28f) else AlphaPicsColors.Surface)
            .border(1.dp, if (selected) AlphaPicsColors.BorderFocus else AlphaPicsColors.BorderSoft, AlphaPicsShapes.Pill)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) AlphaPicsColors.TextPrimary else AlphaPicsColors.TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun HistoryAction(label: String, enabled: Boolean, onClick: () -> Unit, tag: String) {
    Text(
        label,
        color = if (enabled) AlphaPicsColors.Cyan else AlphaPicsColors.TextTertiary,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .then(if (enabled) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 4.dp, vertical = 10.dp)
            .testTag(tag)
    )
}

private fun statusLabel(status: BatchStudioItemStatus): String = when (status) {
    BatchStudioItemStatus.QUEUED -> "Queued"
    BatchStudioItemStatus.PROCESSING -> "Processing"
    BatchStudioItemStatus.SUCCEEDED -> "Saved"
    BatchStudioItemStatus.FAILED -> "Failed"
    BatchStudioItemStatus.CANCELLED -> "Cancelled"
}

private fun statusColor(status: BatchStudioItemStatus): Color = when (status) {
    BatchStudioItemStatus.QUEUED -> AlphaPicsColors.BorderSoft
    BatchStudioItemStatus.PROCESSING -> AlphaPicsColors.Cyan
    BatchStudioItemStatus.SUCCEEDED -> AlphaPicsColors.Success
    BatchStudioItemStatus.FAILED -> AlphaPicsColors.Danger
    BatchStudioItemStatus.CANCELLED -> AlphaPicsColors.Warning
}
