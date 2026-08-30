package com.example.ui.alphapics.photo

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.editor.EditorPreviewRenderer
import com.example.editor.ExportFormat
import com.example.editor.ExportResult
import com.example.photo.MetadataPolicy
import com.example.photo.PhotoMetadata
import com.example.photo.PhotoResampler
import com.example.photo.PhotoUtilityEngine
import com.example.photo.PhotoUtilitySettings
import com.example.photo.PhotoUtilityTab
import com.example.photo.ResizeMode
import com.example.ui.alphapics.components.AlphaPicsBackdrop
import com.example.ui.alphapics.components.AlphaPicsPhotoEntry
import com.example.ui.alphapics.components.AlphaPicsValueSlider
import com.example.ui.alphapics.theme.AlphaPicsColors
import com.example.ui.alphapics.theme.AlphaPicsShapes
import com.example.ui.alphapics.theme.AlphaPicsSpacing
import com.example.ui.alphapics.theme.AlphaPicsTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

@Composable
fun AlphaPicsPhotoUtilityScreen(
    imageModel: Any?,
    initialTabId: String,
    onBack: () -> Unit,
    onChoosePhoto: () -> Unit,
    onOpenCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember(initialTabId) { mutableStateOf(PhotoUtilityTab.fromId(initialTabId)) }
    var sourcePreview by remember(imageModel) { mutableStateOf<Bitmap?>(null) }
    var outputPreview by remember(imageModel) { mutableStateOf<Bitmap?>(null) }
    var metadata by remember(imageModel) { mutableStateOf<PhotoMetadata?>(null) }
    var loadError by remember(imageModel) { mutableStateOf<String?>(null) }
    var isLoading by remember(imageModel) { mutableStateOf(false) }
    var settings by remember(imageModel) { mutableStateOf(PhotoUtilitySettings()) }
    var undoStack by remember(imageModel) { mutableStateOf<List<PhotoUtilitySettings>>(emptyList()) }
    var redoStack by remember(imageModel) { mutableStateOf<List<PhotoUtilitySettings>>(emptyList()) }
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }
    var exportResult by remember { mutableStateOf<ExportResult?>(null) }
    var exportError by remember { mutableStateOf<String?>(null) }

    fun commit(next: PhotoUtilitySettings) {
        if (next == settings) return
        undoStack = (undoStack + settings).takeLast(40)
        redoStack = emptyList()
        settings = next
        exportResult = null
        exportError = null
    }

    fun undo() {
        val previous = undoStack.lastOrNull() ?: return
        redoStack = (redoStack + settings).takeLast(40)
        undoStack = undoStack.dropLast(1)
        settings = previous
        exportResult = null
        exportError = null
    }

    fun redo() {
        val next = redoStack.lastOrNull() ?: return
        undoStack = (undoStack + settings).takeLast(40)
        redoStack = redoStack.dropLast(1)
        settings = next
        exportResult = null
        exportError = null
    }

    LaunchedEffect(imageModel) {
        sourcePreview = null
        outputPreview = null
        metadata = null
        loadError = null
        if (imageModel == null) return@LaunchedEffect
        isLoading = true
        try {
            val preview = EditorPreviewRenderer.loadSource(context, imageModel).getOrThrow()
            sourcePreview = preview
            val uri = imageModel as? Uri
            if (uri != null) {
                val info = com.example.photo.PhotoMetadataReader.read(context, uri).getOrThrow()
                metadata = info
                settings = settings.copy(
                    resize = settings.resize.withOriginalDimensions(info.width, info.height)
                )
            } else {
                settings = settings.copy(
                    resize = settings.resize.withOriginalDimensions(preview.width, preview.height)
                )
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            loadError = error.localizedMessage ?: "Unable to open this photo."
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(sourcePreview, settings.resize, metadata) {
        val source = sourcePreview ?: return@LaunchedEffect
        delay(45)
        try {
            val sourceWidth = metadata?.width ?: source.width
            val sourceHeight = metadata?.height ?: source.height
            val output = settings.resize.resolvedDimensions(sourceWidth, sourceHeight)
            val ratio = output.first.toFloat() / output.second
            val longEdge = 900
            val previewWidth = if (ratio >= 1f) longEdge else (longEdge * ratio).roundToInt().coerceAtLeast(1)
            val previewHeight = if (ratio >= 1f) (longEdge / ratio).roundToInt().coerceAtLeast(1) else longEdge
            outputPreview = withContext(Dispatchers.Default) {
                val renderContext = coroutineContext
                PhotoResampler.resize(
                    source,
                    previewWidth,
                    previewHeight,
                    checkpoint = { renderContext.ensureActive() }
                )
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            exportError = error.localizedMessage ?: "Unable to preview these dimensions."
        }
    }

    fun performExport() {
        val uri = imageModel as? Uri
        if (uri == null) {
            exportError = "Choose a device photo before saving."
            return
        }
        isExporting = true
        exportProgress = 0f
        exportResult = null
        exportError = null
        scope.launch {
            val result = PhotoUtilityEngine.export(
                context = context,
                sourceUri = uri,
                settings = settings,
                onProgress = { exportProgress = it }
            )
            isExporting = false
            result.onSuccess { exportResult = it }
                .onFailure { exportError = it.localizedMessage ?: "Unable to save this photo." }
        }
    }

    fun share(result: ExportResult) {
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = result.mimeType
                    putExtra(Intent.EXTRA_STREAM, result.uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Share photo"
            )
        )
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
                        .testTag("alphapics_photo_utility")
                ) {
                    PhotoUtilityTopBar(
                        selectedTab = selectedTab,
                        hasImage = imageModel != null,
                        onBack = onBack,
                        onChoosePhoto = onChoosePhoto
                    )
                    if (imageModel == null) {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            AlphaPicsPhotoEntry(
                                onChoosePhoto = onChoosePhoto,
                                onOpenCamera = onOpenCamera,
                                modifier = Modifier.testTag("photo_utility_empty")
                            )
                        }
                        return@Column
                    }
                    Spacer(Modifier.height(AlphaPicsSpacing.Xs))
                    PhotoUtilityCanvas(
                        preview = outputPreview ?: sourcePreview,
                        isLoading = isLoading,
                        error = loadError,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                    metadata?.let { info ->
                        Text(
                            "${info.width} × ${info.height} · ${info.formatLabel} · ${formatBytes(info.sizeBytes)}",
                            color = AlphaPicsColors.TextTertiary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 6.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    PhotoUtilityInspector(
                        selectedTab = selectedTab,
                        onSelectedTab = { selectedTab = it },
                        settings = settings,
                        onSettingsChange = ::commit,
                        metadata = metadata,
                        canUndo = undoStack.isNotEmpty(),
                        canRedo = redoStack.isNotEmpty(),
                        onUndo = ::undo,
                        onRedo = ::redo,
                        isExporting = isExporting,
                        exportProgress = exportProgress,
                        exportResult = exportResult,
                        exportError = exportError,
                        onExport = ::performExport,
                        onShare = ::share,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (selectedTab == PhotoUtilityTab.INFO) 360.dp else 330.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoUtilityTopBar(
    selectedTab: PhotoUtilityTab,
    hasImage: Boolean,
    onBack: () -> Unit,
    onChoosePhoto: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(54.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = AlphaPicsColors.TextPrimary)
            }
            Spacer(Modifier.width(5.dp))
            Column {
                Text("Photo Utilities", color = AlphaPicsColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${selectedTab.label} · Local device processing",
                    color = AlphaPicsColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        if (hasImage) {
            Text(
                "Replace",
                color = AlphaPicsColors.Cyan,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clickable(role = Role.Button, onClick = onChoosePhoto).padding(10.dp)
            )
        }
    }
}

@Composable
private fun PhotoUtilityCanvas(
    preview: Bitmap?,
    isLoading: Boolean,
    error: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(AlphaPicsShapes.Hero)
            .clipToBounds()
            .background(Color(0xFF02040A))
            .border(1.dp, AlphaPicsColors.BorderFocus, AlphaPicsShapes.Hero)
            .testTag("photo_utility_canvas"),
        contentAlignment = Alignment.Center
    ) {
        if (preview != null && !preview.isRecycled) {
            Image(
                bitmap = preview.asImageBitmap(),
                contentDescription = "Photo utility preview",
                modifier = Modifier.fillMaxSize().testTag("photo_utility_preview"),
                contentScale = ContentScale.Fit
            )
        }
        if (isLoading) CircularProgressIndicator(color = AlphaPicsColors.Cyan, strokeWidth = 2.dp)
        if (error != null) {
            Text(error, color = AlphaPicsColors.Danger, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
        }
    }
}

@Composable
private fun PhotoUtilityInspector(
    selectedTab: PhotoUtilityTab,
    onSelectedTab: (PhotoUtilityTab) -> Unit,
    settings: PhotoUtilitySettings,
    onSettingsChange: (PhotoUtilitySettings) -> Unit,
    metadata: PhotoMetadata?,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
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
            .testTag("photo_utility_inspector")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Xs)
            ) {
                PhotoChoiceChip("Resize", selectedTab == PhotoUtilityTab.RESIZE, { onSelectedTab(PhotoUtilityTab.RESIZE) }, Modifier.testTag("photo_tab_resize"))
                PhotoChoiceChip("Convert", selectedTab == PhotoUtilityTab.CONVERT, { onSelectedTab(PhotoUtilityTab.CONVERT) }, Modifier.testTag("photo_tab_convert"))
                PhotoChoiceChip("Info", selectedTab == PhotoUtilityTab.INFO, { onSelectedTab(PhotoUtilityTab.INFO) }, Modifier.testTag("photo_tab_info"))
            }
            if (selectedTab != PhotoUtilityTab.INFO) {
                HistoryText("Undo", canUndo, onUndo, "photo_utility_undo")
                HistoryText("Redo", canRedo, onRedo, "photo_utility_redo")
            }
        }
        Spacer(Modifier.height(AlphaPicsSpacing.Xs))
        when (selectedTab) {
            PhotoUtilityTab.RESIZE -> ResizePanel(
                settings,
                onSettingsChange,
                metadata,
                Modifier.weight(1f)
            )
            PhotoUtilityTab.CONVERT -> ConvertPanel(
                settings,
                onSettingsChange,
                Modifier.weight(1f)
            )
            PhotoUtilityTab.INFO -> InfoPanel(metadata, Modifier.weight(1f))
        }
        if (selectedTab != PhotoUtilityTab.INFO) {
            ExportFooter(
                isExporting,
                exportProgress,
                exportResult,
                exportError,
                onExport,
                onShare
            )
        }
    }
}

@Composable
private fun ResizePanel(
    settings: PhotoUtilitySettings,
    onSettingsChange: (PhotoUtilitySettings) -> Unit,
    metadata: PhotoMetadata?,
    modifier: Modifier = Modifier
) {
    val resize = settings.resize
    val sourceWidth = metadata?.width ?: resize.targetWidth.coerceAtLeast(1)
    val sourceHeight = metadata?.height ?: resize.targetHeight.coerceAtLeast(1)
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PhotoChoiceChip("Dimensions", resize.mode == ResizeMode.DIMENSIONS, {
                onSettingsChange(settings.copy(resize = resize.copy(mode = ResizeMode.DIMENSIONS)))
            }, Modifier.testTag("resize_mode_dimensions"))
            PhotoChoiceChip("Percentage", resize.mode == ResizeMode.PERCENTAGE, {
                onSettingsChange(settings.copy(resize = resize.copy(mode = ResizeMode.PERCENTAGE)))
            }, Modifier.testTag("resize_mode_percentage"))
            PhotoChoiceChip(if (resize.maintainAspectRatio) "Ratio locked" else "Free ratio", resize.maintainAspectRatio, {
                onSettingsChange(settings.copy(resize = resize.copy(maintainAspectRatio = !resize.maintainAspectRatio)))
            }, Modifier.testTag("resize_aspect_lock"))
        }
        if (resize.mode == ResizeMode.DIMENSIONS) {
            Row(horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Sm)) {
                DimensionInput(
                    label = "Width",
                    value = resize.targetWidth,
                    onValue = { width ->
                        val height = if (resize.maintainAspectRatio && width > 0) {
                            (width * sourceHeight.toFloat() / sourceWidth).roundToInt().coerceAtLeast(1)
                        } else resize.targetHeight
                        onSettingsChange(settings.copy(resize = resize.copy(targetWidth = width, targetHeight = height)))
                    },
                    modifier = Modifier.weight(1f).testTag("resize_width")
                )
                DimensionInput(
                    label = "Height",
                    value = resize.targetHeight,
                    onValue = { height ->
                        val width = if (resize.maintainAspectRatio && height > 0) {
                            (height * sourceWidth.toFloat() / sourceHeight).roundToInt().coerceAtLeast(1)
                        } else resize.targetWidth
                        onSettingsChange(settings.copy(resize = resize.copy(targetWidth = width, targetHeight = height)))
                    },
                    modifier = Modifier.weight(1f).testTag("resize_height")
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PhotoChoiceChip("Original", resize.targetWidth == sourceWidth && resize.targetHeight == sourceHeight, {
                    onSettingsChange(settings.copy(resize = resize.withOriginalDimensions(sourceWidth, sourceHeight)))
                }, Modifier.testTag("resize_preset_original"))
                listOf(1080, 2048, 4096).forEach { edge ->
                    PhotoChoiceChip("${edge}px", false, {
                        onSettingsChange(settings.copy(resize = resize.withLongEdge(sourceWidth, sourceHeight, edge)))
                    }, Modifier.testTag("resize_preset_$edge"))
                }
            }
        } else {
            AlphaPicsValueSlider(
                label = "Scale",
                value = resize.percentage,
                onValueChange = { onSettingsChange(settings.copy(resize = resize.copy(percentage = it))) },
                valueRange = 1f..400f,
                valueFormatter = { "${it.roundToInt()}%" },
                modifier = Modifier.fillMaxWidth()
            )
        }
        val dimensions = runCatching { resize.resolvedDimensions(sourceWidth, sourceHeight) }.getOrNull()
        Text(
            dimensions?.let { "Output ${it.first} × ${it.second} · multi-pass filtered resampling" }
                ?: "Enter dimensions between 1 and 8192 pixels (maximum 64 MP).",
            color = if (dimensions == null) AlphaPicsColors.Warning else AlphaPicsColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ConvertPanel(
    settings: PhotoUtilitySettings,
    onSettingsChange: (PhotoUtilitySettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ExportFormat.entries.forEach { format ->
                PhotoChoiceChip(
                    format.name,
                    settings.format == format,
                    {
                        onSettingsChange(
                            settings.copy(
                                format = format,
                                metadataPolicy = if (format == ExportFormat.JPEG) settings.metadataPolicy else MetadataPolicy.REMOVE
                            )
                        )
                    },
                    Modifier.weight(1f).testTag("convert_format_${format.name.lowercase()}")
                )
            }
        }
        if (settings.format != ExportFormat.PNG) {
            AlphaPicsValueSlider(
                label = "Quality",
                value = settings.quality.toFloat(),
                onValueChange = { onSettingsChange(settings.copy(quality = it.roundToInt())) },
                valueRange = 40f..100f,
                valueFormatter = { "${it.roundToInt()}%" },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PhotoChoiceChip("Remove metadata", settings.metadataPolicy == MetadataPolicy.REMOVE, {
                onSettingsChange(settings.copy(metadataPolicy = MetadataPolicy.REMOVE))
            }, Modifier.testTag("metadata_remove"))
            PhotoChoiceChip(
                "Preserve safe",
                settings.metadataPolicy == MetadataPolicy.PRESERVE_SAFE,
                {
                    if (settings.format == ExportFormat.JPEG) {
                        onSettingsChange(settings.copy(metadataPolicy = MetadataPolicy.PRESERVE_SAFE))
                    }
                },
                Modifier.testTag("metadata_preserve")
            )
        }
        Text(
            when {
                settings.format == ExportFormat.JPEG -> "JPEG removes transparency. Safe preserve keeps camera/date fields, normalizes orientation, and omits GPS."
                settings.format == ExportFormat.PNG -> "PNG preserves transparency. Metadata is removed for predictable compatibility."
                else -> "WebP supports compact output and transparency. Metadata is removed for predictable compatibility."
            },
            color = AlphaPicsColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun InfoPanel(metadata: PhotoMetadata?, modifier: Modifier = Modifier) {
    if (metadata == null) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("Metadata is available for device photos.", color = AlphaPicsColors.TextSecondary)
        }
        return
    }
    Column(modifier.verticalScroll(rememberScrollState()).testTag("photo_info_rows")) {
        InfoRow("File", metadata.displayName)
        InfoRow("Dimensions", "${metadata.width} × ${metadata.height}")
        InfoRow("Megapixels", "%.2f MP".format(metadata.megapixels))
        InfoRow("Aspect ratio", "%.3f : 1".format(metadata.aspectRatio))
        InfoRow("Format", "${metadata.formatLabel} · ${metadata.mimeType}")
        InfoRow("File size", formatBytes(metadata.sizeBytes))
        InfoRow("Transparency", if (metadata.hasTransparency) "Present" else "None detected")
        InfoRow("EXIF orientation", metadata.orientationLabel)
        InfoRow("Color", metadata.colorDescription)
        metadata.dateLabel?.let { InfoRow("Date", it) }
        metadata.exif.forEach { (label, value) -> InfoRow(label, value) }
    }
}

@Composable
private fun ExportFooter(
    isExporting: Boolean,
    progress: Float,
    result: ExportResult?,
    error: String?,
    onExport: () -> Unit,
    onShare: (ExportResult) -> Unit
) {
    if (isExporting) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(26.dp), color = AlphaPicsColors.Cyan, strokeWidth = 3.dp)
            Text("Processing locally · ${(progress * 100).roundToInt()}%", color = AlphaPicsColors.TextSecondary)
        }
    } else if (result != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Check, null, tint = AlphaPicsColors.Success)
            Spacer(Modifier.width(6.dp))
            Text("Saved · ${result.width} × ${result.height} · ${formatBytes(result.sizeBytes)}", color = AlphaPicsColors.Success, modifier = Modifier.weight(1f), maxLines = 1)
            IconButton(onClick = { onShare(result) }, modifier = Modifier.testTag("photo_utility_share")) {
                Icon(Icons.Filled.Share, "Share", tint = AlphaPicsColors.Cyan)
            }
        }
    } else if (error != null) {
        Text(error, color = AlphaPicsColors.Danger, style = MaterialTheme.typography.bodySmall, maxLines = 2)
    }
    Button(
        onClick = onExport,
        enabled = !isExporting,
        modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp).testTag("photo_utility_export"),
        shape = AlphaPicsShapes.Medium,
        colors = ButtonDefaults.buttonColors(containerColor = AlphaPicsColors.ElectricBlue)
    ) {
        Text(if (result == null) "Save processed photo" else "Save another copy")
    }
}

@Composable
private fun DimensionInput(
    label: String,
    value: Int,
    onValue: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(label, color = AlphaPicsColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
        BasicTextField(
            value = value.takeIf { it > 0 }?.toString().orEmpty(),
            onValueChange = { raw -> onValue(raw.filter(Char::isDigit).take(4).toIntOrNull() ?: 0) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = AlphaPicsColors.TextPrimary, fontWeight = FontWeight.Bold),
            cursorBrush = SolidColor(AlphaPicsColors.Cyan),
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(AlphaPicsShapes.Medium)
                .background(AlphaPicsColors.Surface)
                .border(1.dp, AlphaPicsColors.BorderSoft, AlphaPicsShapes.Medium)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun PhotoChoiceChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(AlphaPicsShapes.Pill)
            .background(if (isSelected) AlphaPicsColors.ElectricBlue.copy(alpha = 0.28f) else AlphaPicsColors.Surface)
            .border(1.dp, if (isSelected) AlphaPicsColors.BorderFocus else AlphaPicsColors.BorderSoft, AlphaPicsShapes.Pill)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { selected = isSelected }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (isSelected) AlphaPicsColors.TextPrimary else AlphaPicsColors.TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun HistoryText(label: String, enabled: Boolean, onClick: () -> Unit, tag: String) {
    Text(
        label,
        color = if (enabled) AlphaPicsColors.Cyan else AlphaPicsColors.TextTertiary,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .then(if (enabled) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 4.dp, vertical = 9.dp)
            .testTag(tag)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, color = AlphaPicsColors.TextTertiary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.38f))
        Text(value, color = AlphaPicsColors.TextPrimary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, modifier = Modifier.weight(0.62f))
    }
}

private fun formatBytes(bytes: Long?): String {
    if (bytes == null || bytes < 0) return "Unknown size"
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    return "%.1f MB".format(kb / 1024.0)
}
