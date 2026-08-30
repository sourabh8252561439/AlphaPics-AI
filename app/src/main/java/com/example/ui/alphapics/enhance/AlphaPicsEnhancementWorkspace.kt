package com.example.ui.alphapics.enhance

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.enhance.EnhancementCatalog
import com.example.enhance.EnhancementEngine
import com.example.enhance.EnhancementMode
import com.example.enhance.EnhancementResult
import com.example.ui.alphapics.components.AlphaPicsAvailabilityCard
import com.example.ui.alphapics.components.AlphaPicsBackdrop
import com.example.ui.alphapics.components.AlphaPicsErrorState
import com.example.ui.alphapics.components.AlphaPicsLoadingState
import com.example.ui.alphapics.components.AlphaPicsPhotoEntry
import com.example.ui.alphapics.components.AlphaPicsWorkspaceTopBar
import com.example.ui.alphapics.theme.AlphaPicsColors
import com.example.ui.alphapics.theme.AlphaPicsShapes
import com.example.ui.alphapics.theme.AlphaPicsSpacing
import com.example.ui.alphapics.theme.AlphaPicsTheme
import kotlinx.coroutines.launch
import java.util.Locale

private val SparkleIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Sparkle",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 2f)
            lineToRelative(2.4f, 6.6f)
            lineTo(21f, 11f)
            lineToRelative(-6.6f, 2.4f)
            lineTo(12f, 20f)
            lineToRelative(-2.4f, -6.6f)
            lineTo(3f, 11f)
            lineToRelative(6.6f, -2.4f)
            close()
        }
    }.build()

private val SaveIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Save",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(17f, 3f)
            horizontalLineTo(5f)
            curveToRelative(-1.11f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(14f)
            curveToRelative(0f, 1.1f, 0.89f, 2f, 2f, 2f)
            horizontalLineToRelative(14f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineTo(7f)
            lineToRelative(-4f, -4f)
            close()
            moveTo(12f, 19f)
            curveToRelative(-1.66f, 0f, -3f, -1.34f, -3f, -3f)
            reflectiveCurveToRelative(1.34f, -3f, 3f, -3f)
            reflectiveCurveToRelative(3f, 1.34f, 3f, 3f)
            reflectiveCurveToRelative(-1.34f, 3f, -3f, 3f)
            close()
            moveTo(15f, 9f)
            horizontalLineTo(5f)
            verticalLineTo(5f)
            horizontalLineToRelative(10f)
            verticalLineToRelative(4f)
            close()
        }
    }.build()

private val ShareIconVector: ImageVector
    get() = ImageVector.Builder(
        name = "Share",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(18f, 16.08f)
            curveToRelative(-0.76f, 0f, -1.44f, 0.3f, -1.96f, 0.77f)
            lineTo(8.91f, 12.7f)
            curveToRelative(0.05f, -0.23f, 0.09f, -0.46f, 0.09f, -0.7f)
            reflectiveCurveToRelative(-0.04f, -0.47f, -0.09f, -0.7f)
            lineToRelative(7.05f, -4.11f)
            curveToRelative(0.54f, 0.5f, 1.25f, 0.81f, 2.04f, 0.81f)
            curveToRelative(1.66f, 0f, 3f, -1.34f, 3f, -3f)
            reflectiveCurveToRelative(-1.34f, -3f, -3f, -3f)
            reflectiveCurveToRelative(-3f, 1.34f, -3f, 3f)
            curveToRelative(0f, 0.24f, 0.04f, 0.47f, 0.09f, 0.7f)
            lineTo(8.04f, 9.81f)
            curveTo(7.5f, 9.31f, 6.79f, 9f, 6f, 9f)
            curveToRelative(-1.66f, 0f, -3f, 1.34f, -3f, 3f)
            reflectiveCurveToRelative(1.34f, 3f, 3f, 3f)
            curveToRelative(0.79f, 0f, 1.5f, -0.31f, 2.04f, -0.81f)
            lineToRelative(7.12f, 4.16f)
            curveToRelative(-0.05f, 0.21f, -0.08f, 0.43f, -0.08f, 0.65f)
            curveToRelative(0f, 1.61f, 1.31f, 2.92f, 2.92f, 2.92f)
            reflectiveCurveToRelative(2.92f, -1.31f, 2.92f, -2.92f)
            curveToRelative(0f, -1.61f, -1.31f, -2.92f, -2.92f, -2.92f)
            close()
        }
    }.build()

private enum class EnhancementPhotoLoadState {
    LOADING,
    SUCCESS,
    ERROR
}

@Composable
fun AlphaPicsEnhancementWorkspace(
    imageModel: Any?,
    initialModeId: String,
    onBack: () -> Unit,
    onChoosePhoto: () -> Unit,
    onOpenCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedModeId by remember(initialModeId) {
        mutableStateOf(EnhancementCatalog.find(initialModeId).id)
    }
    val selectedMode = EnhancementCatalog.find(selectedModeId)

    var isProcessing by remember { mutableStateOf(false) }
    var processingProgress by remember { mutableFloatStateOf(0f) }
    var processingStatusText by remember { mutableStateOf("Enhancing photo...") }

    var enhancedResult by remember(imageModel) { mutableStateOf<EnhancementResult.Success?>(null) }
    var isComparingBefore by remember { mutableStateOf(false) }
    var splitPosition by remember { mutableFloatStateOf(0.5f) }
    var isSaving by remember { mutableStateOf(false) }

    val modeScrollState = rememberScrollState()
    LaunchedEffect(selectedModeId) {
        val selectedIndex = EnhancementCatalog.Modes.indexOfFirst { it.id == selectedModeId }
        val target = (selectedIndex.coerceAtLeast(0) * 80).coerceAtMost(modeScrollState.maxValue)
        modeScrollState.scrollTo(target)
    }

    fun startEnhancement() {
        val uri = imageModel as? Uri
        if (uri == null) {
            Toast.makeText(context, "Choose a photo first", Toast.LENGTH_SHORT).show()
            return
        }

        if (!selectedMode.isLocalAvailable) {
            Toast.makeText(context, "${selectedMode.label} requires Cloud AI provider setup", Toast.LENGTH_SHORT).show()
            return
        }

        isProcessing = true
        processingProgress = 0.1f
        processingStatusText = "Preparing enhancement..."

        coroutineScope.launch {
            val result = EnhancementEngine.processPhoto(
                context = context,
                sourceUri = uri,
                modeId = selectedMode.id,
                onProgress = { prog, text ->
                    processingProgress = prog
                    processingStatusText = text
                }
            )

            isProcessing = false
            when (result) {
                is EnhancementResult.Success -> {
                    enhancedResult = result
                    Toast.makeText(context, "${selectedMode.label} enhancement ready!", Toast.LENGTH_SHORT).show()
                }
                is EnhancementResult.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
                is EnhancementResult.ProviderNotConfigured -> {
                    Toast.makeText(context, "Provider not configured", Toast.LENGTH_SHORT).show()
                }
                is EnhancementResult.TokenRequired -> {
                    Toast.makeText(context, "Tokens required", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun saveToGallery() {
        val result = enhancedResult ?: return
        isSaving = true
        coroutineScope.launch {
            val saveResult = EnhancementEngine.saveResultToGallery(context, result.outputUri, result.modeId)
            isSaving = false
            saveResult.onSuccess {
                Toast.makeText(context, "Saved enhanced photo to Pictures/AlphaPics AI", Toast.LENGTH_LONG).show()
            }.onFailure {
                Toast.makeText(context, "Failed to save: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareResult() {
        val result = enhancedResult ?: return
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, result.outputUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Enhanced Photo"))
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
                        .testTag("alphapics_enhancement_workspace")
                ) {
                    Spacer(Modifier.height(AlphaPicsSpacing.Sm))
                    AlphaPicsWorkspaceTopBar(
                        title = "Enhance",
                        subtitle = if (imageModel == null) "Choose a photo" else if (enhancedResult != null) "Enhanced Result" else "Photo workspace",
                        onBack = onBack,
                        trailing = {
                            Box(
                                modifier = Modifier
                                    .background(
                                        AlphaPicsColors.Cyan.copy(alpha = 0.11f),
                                        AlphaPicsShapes.Pill
                                    )
                                    .border(
                                        1.dp,
                                        AlphaPicsColors.Cyan.copy(alpha = 0.28f),
                                        AlphaPicsShapes.Pill
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (selectedMode.isLocalAvailable) "STUDIO READY" else "AI SOON",
                                    color = AlphaPicsColors.Cyan,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    )
                    Spacer(Modifier.height(AlphaPicsSpacing.Md))

                    // Center Interactive Canvas
                    EnhancementCanvas(
                        imageModel = imageModel,
                        enhancedResult = enhancedResult,
                        isComparingBefore = isComparingBefore,
                        splitPosition = splitPosition,
                        onSplitChange = { splitPosition = it },
                        isProcessing = isProcessing,
                        progress = processingProgress,
                        progressStatus = processingStatusText,
                        onChoosePhoto = onChoosePhoto,
                        onOpenCamera = onOpenCamera,
                        modifier = if (imageModel == null) {
                            Modifier
                                .fillMaxWidth()
                                .height(430.dp)
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .heightIn(min = 260.dp, max = 450.dp)
                        }
                    )

                    Spacer(Modifier.height(AlphaPicsSpacing.Sm))

                    // Mode Selection Rail
                    Text(
                        text = "ENHANCEMENT MODE",
                        color = AlphaPicsColors.TextTertiary,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(Modifier.height(AlphaPicsSpacing.Xs))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(modeScrollState)
                            .testTag("enhancement_mode_rail"),
                        horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Sm)
                    ) {
                        EnhancementCatalog.Modes.forEach { mode ->
                            EnhancementModeChip(
                                mode = mode,
                                selected = mode.id == selectedModeId,
                                onClick = {
                                    selectedModeId = mode.id
                                    enhancedResult = null
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(AlphaPicsSpacing.Sm))

                    // Context Card / Action Bar
                    if (enhancedResult != null) {
                        // Result Action Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    AlphaPicsColors.SurfaceRaised,
                                    AlphaPicsShapes.Card
                                )
                                .border(1.dp, AlphaPicsColors.BorderSoft, AlphaPicsShapes.Card)
                                .padding(AlphaPicsSpacing.Md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AlphaPicsSpacing.Md)
                        ) {
                            Button(
                                onClick = { saveToGallery() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AlphaPicsColors.ElectricBlue,
                                    contentColor = Color.White
                                ),
                                shape = AlphaPicsShapes.Pill,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(imageVector = SaveIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (isSaving) "Saving..." else "Save Result", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { shareResult() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AlphaPicsColors.Surface,
                                    contentColor = AlphaPicsColors.BrightBlue
                                ),
                                shape = AlphaPicsShapes.Pill,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .border(1.dp, AlphaPicsColors.BorderFocus, AlphaPicsShapes.Pill)
                            ) {
                                Icon(imageVector = ShareIconVector, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Share", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        AlphaPicsAvailabilityCard(
                            title = selectedMode.label,
                            description = if (imageModel == null) {
                                "Choose a photo to prepare this workspace. ${selectedMode.description}"
                            } else if (selectedMode.isLocalAvailable) {
                                "${selectedMode.description} Ready to enhance."
                            } else {
                                "${selectedMode.description} Processing is not available in this build, so your original photo remains unchanged."
                            },
                            modifier = Modifier.testTag("enhancement_unavailable_state")
                        )

                        if (selectedMode.isLocalAvailable && imageModel != null) {
                            Spacer(Modifier.height(AlphaPicsSpacing.Sm))
                            Button(
                                onClick = { startEnhancement() },
                                enabled = !isProcessing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AlphaPicsColors.ElectricBlue,
                                    contentColor = Color.White
                                ),
                                shape = AlphaPicsShapes.Pill,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Icon(imageVector = SparkleIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isProcessing) "Enhancing..." else "Enhance Photo (${selectedMode.label})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(AlphaPicsSpacing.Sm))
                }
            }
        }
    }
}

@Composable
private fun EnhancementCanvas(
    imageModel: Any?,
    enhancedResult: EnhancementResult.Success?,
    isComparingBefore: Boolean,
    splitPosition: Float,
    onSplitChange: (Float) -> Unit,
    isProcessing: Boolean,
    progress: Float,
    progressStatus: String,
    onChoosePhoto: () -> Unit,
    onOpenCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    var canvasWidthPx by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .clip(AlphaPicsShapes.Hero)
            .clipToBounds()
            .background(Color(0xFF02040A))
            .border(1.dp, AlphaPicsColors.BorderFocus.copy(alpha = 0.72f), AlphaPicsShapes.Hero)
            .onSizeChanged { canvasWidthPx = it.width.toFloat().coerceAtLeast(1f) }
            .testTag("enhancement_canvas"),
        contentAlignment = Alignment.Center
    ) {
        if (imageModel == null) {
            AlphaPicsPhotoEntry(
                onChoosePhoto = onChoosePhoto,
                onOpenCamera = onOpenCamera,
                modifier = Modifier.padding(AlphaPicsSpacing.Lg)
            )
        } else {
            var loadState by remember(imageModel) {
                mutableStateOf(EnhancementPhotoLoadState.LOADING)
            }

            // Original photo layer
            AsyncImage(
                model = imageModel,
                contentDescription = "Photo selected for enhancement",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                onLoading = { loadState = EnhancementPhotoLoadState.LOADING },
                onSuccess = { loadState = EnhancementPhotoLoadState.SUCCESS },
                onError = { loadState = EnhancementPhotoLoadState.ERROR }
            )

            // Enhanced result layer (when available)
            if (enhancedResult != null && !isComparingBefore) {
                AsyncImage(
                    model = enhancedResult.outputUri,
                    contentDescription = "Enhanced photo result",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            // Interactive Drag Divider Handle (when result is present)
            if (enhancedResult != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val newPos = (splitPosition + (dragAmount.x / canvasWidthPx)).coerceIn(0.05f, 0.95f)
                                onSplitChange(newPos)
                            }
                        }
                )
            }

            when (loadState) {
                EnhancementPhotoLoadState.LOADING -> AlphaPicsLoadingState(
                    title = "Opening photo",
                    description = "Preparing the original photo for this workspace.",
                    modifier = Modifier.padding(AlphaPicsSpacing.Lg)
                )

                EnhancementPhotoLoadState.ERROR -> AlphaPicsErrorState(
                    title = "This photo couldn’t be opened",
                    description = "Choose another photo or try the camera again. Nothing was changed.",
                    actionLabel = "Choose another photo",
                    onAction = onChoosePhoto,
                    modifier = Modifier.padding(AlphaPicsSpacing.Lg)
                )

                EnhancementPhotoLoadState.SUCCESS -> {
                    if (isProcessing) {
                        // Processing Scrim
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { progress },
                                    color = AlphaPicsColors.Cyan,
                                    trackColor = AlphaPicsColors.BorderSoft,
                                    modifier = Modifier.size(52.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = progressStatus,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Top Badges
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(AlphaPicsSpacing.Md)
                            .background(Color.Black.copy(alpha = 0.62f), AlphaPicsShapes.Pill)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (enhancedResult != null) "ENHANCED" else "ORIGINAL",
                            color = if (enhancedResult != null) AlphaPicsColors.Cyan else Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (enhancedResult == null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(AlphaPicsSpacing.Md)
                                .background(AlphaPicsColors.Surface.copy(alpha = 0.86f), AlphaPicsShapes.Pill)
                                .border(1.dp, AlphaPicsColors.BorderSoft, AlphaPicsShapes.Pill)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "READY",
                                color = AlphaPicsColors.TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(AlphaPicsSpacing.Md)
                                .background(AlphaPicsColors.ElectricBlue.copy(alpha = 0.25f), AlphaPicsShapes.Pill)
                                .border(1.dp, AlphaPicsColors.Cyan.copy(alpha = 0.6f), AlphaPicsShapes.Pill)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${enhancedResult.width}×${enhancedResult.height} • ${enhancedResult.processingTimeMs}ms",
                                color = AlphaPicsColors.Cyan,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Change photo pill button
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(AlphaPicsSpacing.Md)
                            .background(Color.Black.copy(alpha = 0.66f), AlphaPicsShapes.Pill)
                            .clickable(role = Role.Button, onClick = onChoosePhoto)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("enhancement_change_photo")
                    ) {
                        Text(
                            text = "Change photo",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancementModeChip(
    mode: EnhancementMode,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .size(width = 72.dp, height = 66.dp)
            .background(
                if (selected) AlphaPicsColors.ElectricBlue.copy(alpha = 0.20f)
                else AlphaPicsColors.SurfaceRaised.copy(alpha = 0.86f),
                AlphaPicsShapes.Medium
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) AlphaPicsColors.BrightBlue else AlphaPicsColors.BorderSoft,
                shape = AlphaPicsShapes.Medium
            )
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .testTag("enhancement_mode_${mode.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    if (selected) AlphaPicsColors.Cyan else AlphaPicsColors.TextTertiary,
                    CircleShape
                )
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = mode.label,
            color = if (selected) AlphaPicsColors.TextPrimary else AlphaPicsColors.TextSecondary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
