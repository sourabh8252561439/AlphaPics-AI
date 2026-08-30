package com.example.batch

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ads.DailyTokenManager
import com.example.ads.RewardedAdManager
import com.example.compression.CompressionMode
import com.example.compression.CompressionModeSelector
import com.example.compression.QualityControl
import com.example.compression.TargetSizeControl
import com.example.ui.alphapics.components.AlphaPicsGlyph
import com.example.ui.alphapics.navigation.AlphaPicsIcon
import com.example.ui.alphapics.theme.AlphaPicsColors
import com.example.ui.alphapics.theme.AlphaPicsShapes
import com.example.ui.alphapics.theme.AlphaPicsTheme

private fun formatBytes(bytes: Long): String {
    val kilobytes = bytes.coerceAtLeast(0L) / 1024.0
    return if (kilobytes >= 1024.0) {
        "%.1f MB".format(kilobytes / 1024.0)
    } else {
        "%.1f KB".format(kilobytes)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchScreen(
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onOpenStudio: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val viewModel: BatchCompressionViewModel = viewModel()

    val items by viewModel.items.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val processingSettings by viewModel.processingSettings.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val dailyLimitReached by viewModel.dailyLimitReached.collectAsState()
    var rewardedAdInProgress by remember { mutableStateOf(false) }

    if (dailyLimitReached) {
        AlertDialog(
            onDismissRequest = {
                if (!rewardedAdInProgress) viewModel.resolveDailyTokenGate(false)
            },
            title = { Text("Daily Free Limit Reached!") },
            text = {
                Text(
                    "To continue compressing without a premium account, watch 1 short video " +
                        "advertisement to claim 1 Free Compression Token."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = activity != null && !rewardedAdInProgress,
                    onClick = {
                        val hostActivity = activity ?: return@TextButton
                        rewardedAdInProgress = true
                        try {
                            RewardedAdManager.show(
                                activity = hostActivity,
                                onRewardEarned = {
                                    try {
                                        val balance = DailyTokenManager.grantRewardToken(context)
                                        rewardedAdInProgress = false
                                        if (balance > 0) {
                                            viewModel.resolveDailyTokenGate(true)
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Unable to grant the compression token. Please try again.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            viewModel.resolveDailyTokenGate(false)
                                        }
                                    } catch (_: Throwable) {
                                        rewardedAdInProgress = false
                                        Toast.makeText(
                                            context,
                                            "Unable to grant the compression token. Please try again.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        viewModel.resolveDailyTokenGate(false)
                                    }
                                },
                                onCancelled = {
                                    rewardedAdInProgress = false
                                    Toast.makeText(
                                        context,
                                        "Transaction Cancelled: You must watch the complete video advertisement to receive your 1 Free Token.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    viewModel.resolveDailyTokenGate(false)
                                },
                                onUnavailable = {
                                    rewardedAdInProgress = false
                                    Toast.makeText(
                                        context,
                                        "Rewarded advertisement is not ready yet. Please try again.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    // Keep the gate/dialog open so the user can explicitly retry or cancel.
                                }
                            )
                        } catch (_: Throwable) {
                            rewardedAdInProgress = false
                            Toast.makeText(
                                context,
                                "Rewarded advertisement is not ready yet. Please try again.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                ) {
                    Text("Watch Ad")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !rewardedAdInProgress,
                    onClick = { viewModel.resolveDailyTokenGate(false) }
                ) {
                    Text("Not Now")
                }
            }
        )
    }

    val background = AlphaPicsColors.Void
    val textColor = AlphaPicsColors.TextPrimary
    val cardColor = AlphaPicsColors.SurfaceRaised

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_BATCH_ITEMS)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) viewModel.setSelection(context, uris)
    }

    AlphaPicsTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = background,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Batch Compress",
                                color = textColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "ALPHAPICS AI  •  UP TO $MAX_BATCH_ITEMS PHOTOS",
                                color = AlphaPicsColors.TextTertiary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.8.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Home",
                                tint = textColor
                            )
                        }
                    },
                    actions = {
                        if (onOpenStudio != null) {
                            TextButton(onClick = onOpenStudio) {
                                Text("Studio", color = AlphaPicsColors.Cyan)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AlphaPicsColors.Surface.copy(alpha = 0.98f)
                    )
                )
            }
        ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        pickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !isProcessing,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = AlphaPicsShapes.Medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlphaPicsColors.ElectricBlue,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Photos")
                }
                if (items.isNotEmpty()) {
                    OutlinedButton(
                        onClick = viewModel::clearAll,
                        enabled = !isProcessing,
                        modifier = Modifier.height(54.dp),
                        shape = AlphaPicsShapes.Medium
                    ) {
                        Text("Clear")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "${items.size} of $MAX_BATCH_ITEMS photos selected",
                color = AlphaPicsColors.TextTertiary
            )

            if (items.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(330.dp)
                        .padding(top = 32.dp)
                        .background(
                            AlphaPicsColors.SurfaceRaised.copy(alpha = 0.76f),
                            AlphaPicsShapes.Card
                        )
                        .border(1.dp, AlphaPicsColors.BorderSoft, AlphaPicsShapes.Card)
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AlphaPicsGlyph(
                        icon = AlphaPicsIcon.BATCH,
                        accent = AlphaPicsColors.Violet,
                        size = 64.dp
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Build a photo batch",
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Choose up to $MAX_BATCH_ITEMS photos and compress them together with one setting.",
                        color = AlphaPicsColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
                return@Column
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Compression settings",
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Spacer(Modifier.height(8.dp))
            CompressionModeSelector(
                mode = settings.mode,
                onModeChange = viewModel::setMode,
                isDarkMode = true
            )
            Spacer(Modifier.height(8.dp))
            when (settings.mode) {
                CompressionMode.QUALITY -> QualityControl(
                    value = settings.qualitySliderValue,
                    onValueChange = viewModel::setQuality,
                    isDarkMode = true
                )
                CompressionMode.TARGET_SIZE -> TargetSizeControl(
                    state = settings.targetSize,
                    onStateChange = viewModel::setTargetState,
                    isDarkMode = true
                )
            }

            Spacer(Modifier.height(10.dp))
            val activeConfirmation = processingSettings?.displayLabel ?: settings.confirmationLabel
            Text(
                "${items.size} images • $activeConfirmation",
                color = AlphaPicsColors.Cyan,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .height((((items.size + 2) / 3) * 116).dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false
            ) {
                items(items, key = BatchImageItem::id) { item ->
                    BatchGridItem(
                        item = item,
                        cardColor = cardColor,
                        enabled = !isProcessing,
                        onRemove = { viewModel.removeItem(item.id) }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            if (isProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Processed ${items.count { it.status.isTerminal }} of ${items.size}",
                        color = textColor
                    )
                    OutlinedButton(onClick = viewModel::cancelProcessing) {
                        Text("Cancel")
                    }
                }
            } else {
                Button(
                    onClick = {
                        activity?.let { viewModel.processAll(context, it) }
                    },
                    enabled = activity != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = AlphaPicsShapes.Medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlphaPicsColors.ElectricBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text("Compress ${items.size} Photo${if (items.size == 1) "" else "s"}")
                }
            }

            summary?.let {
                Spacer(Modifier.height(12.dp))
                BatchSummaryCard(it, cardColor, textColor)
            }
        }
        }
    }
}

private val BatchItemStatus.isTerminal: Boolean
    get() = this == BatchItemStatus.COMPRESSED ||
        this == BatchItemStatus.TARGET_NOT_REACHED ||
        this == BatchItemStatus.SKIPPED ||
        this == BatchItemStatus.FAILED

@Composable
private fun BatchGridItem(
    item: BatchImageItem,
    cardColor: Color,
    enabled: Boolean,
    onRemove: () -> Unit
) {
    val (label, statusColor) = when (item.status) {
        BatchItemStatus.PENDING -> "Pending" to Color(0xFF475569)
        BatchItemStatus.PROCESSING -> "Processing" to Color(0xFF2563EB)
        BatchItemStatus.COMPRESSED -> "Compressed" to Color(0xFF16A34A)
        BatchItemStatus.TARGET_NOT_REACHED -> "Target not reached" to Color(0xFFF97316)
        BatchItemStatus.SKIPPED -> "Skipped — no benefit" to Color(0xFFD97706)
        BatchItemStatus.FAILED -> "Failed" to Color(0xFFDC2626)
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(cardColor)
            .border(1.dp, AlphaPicsColors.BorderSoft, RoundedCornerShape(12.dp))
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.displayName,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.68f))
                .padding(horizontal = 5.dp, vertical = 4.dp)
        ) {
            Text(label, color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(formatBytes(item.originalSizeBytes), color = Color.White, fontSize = 9.sp)
            item.errorMessage?.let { reason ->
                Text(
                    reason,
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 8.sp,
                    maxLines = 2
                )
            }
        }

        when (item.status) {
            BatchItemStatus.PROCESSING -> {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), color = Color.White)
                }
            }
            BatchItemStatus.COMPRESSED -> StatusIcon(
                icon = Icons.Filled.Check,
                description = "Compressed",
                color = Color(0xFF22C55E)
            )
            BatchItemStatus.TARGET_NOT_REACHED -> StatusIcon(
                icon = Icons.Filled.Check,
                description = "Compressed; target not reached",
                color = Color(0xFFF97316)
            )
            BatchItemStatus.SKIPPED,
            BatchItemStatus.FAILED -> StatusIcon(
                icon = Icons.Filled.Close,
                description = label,
                color = statusColor
            )
            BatchItemStatus.PENDING -> if (enabled) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(30.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Remove",
                        tint = Color.White,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
                            .padding(4.dp)
                            .size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.StatusIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(4.dp)
            .clip(RoundedCornerShape(50))
            .background(color)
            .padding(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun BatchSummaryCard(
    summary: BatchSummary,
    cardColor: Color,
    textColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardColor, AlphaPicsShapes.Card)
            .border(1.dp, AlphaPicsColors.BorderSoft, AlphaPicsShapes.Card)
            .padding(18.dp)
    ) {
        Text("Batch Results", color = textColor, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Selected: ${summary.totalSelected}", color = textColor)
        Text("Processed: ${summary.processed} of ${summary.totalSelected}", color = textColor)
        Text("Compressed: ${summary.compressed}", color = Color(0xFF16A34A))
        Text("Target not reached: ${summary.targetNotReached}", color = Color(0xFFF97316))
        Text("Skipped — no meaningful benefit: ${summary.skipped}", color = Color(0xFFD97706))
        Text("Failed: ${summary.failed}", color = Color(0xFFDC2626))
        Spacer(Modifier.height(4.dp))
        Text(
            "Total: ${formatBytes(summary.originalTotalBytes)} → ${formatBytes(summary.finalTotalBytes)}",
            color = textColor
        )
        Text(
            "Saved ${formatBytes(summary.bytesSaved)} (${summary.percentSaved}%)",
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}
