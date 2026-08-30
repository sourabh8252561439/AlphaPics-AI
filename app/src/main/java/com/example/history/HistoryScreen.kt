package com.example.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.alphapics.components.AlphaPicsGlyph
import com.example.ui.alphapics.navigation.AlphaPicsIcon
import com.example.ui.alphapics.theme.AlphaPicsColors
import com.example.ui.alphapics.theme.AlphaPicsShapes
import com.example.ui.alphapics.theme.AlphaPicsTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    return if (kb >= 1024) "%.1f MB".format(kb / 1024.0) else "${kb.toInt()} KB"
}

private val dateFormatter = SimpleDateFormat("MMM d, yyyy - h:mm a", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { CompressionHistoryDatabase.getInstance(context).historyDao() }

    var entries by remember { mutableStateOf<List<CompressionHistoryEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun reload() {
        scope.launch {
            isLoading = true
            entries = try {
                dao.getAll()
            } catch (t: Throwable) {
                emptyList()
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    val bgColor = AlphaPicsColors.Void
    val textColor = AlphaPicsColors.TextPrimary
    val cardColor = AlphaPicsColors.SurfaceRaised

    AlphaPicsTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = bgColor,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("History", color = textColor, fontWeight = FontWeight.Bold)
                            Text(
                                "ALPHAPICS AI  •  COMPRESSION RESULTS",
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
                        if (entries.isNotEmpty()) {
                            TextButton(onClick = {
                                scope.launch {
                                    try {
                                        dao.clearAll()
                                    } catch (t: Throwable) {
                                    }
                                    reload()
                                }
                            }) {
                                Text("Clear all", color = AlphaPicsColors.Cyan)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AlphaPicsColors.Surface.copy(alpha = 0.98f)
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when {
                    isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AlphaPicsColors.Cyan)
                        }
                    }
                    entries.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                                .align(Alignment.Center)
                                .background(
                                    AlphaPicsColors.SurfaceRaised.copy(alpha = 0.76f),
                                    AlphaPicsShapes.Card
                                )
                                .border(1.dp, AlphaPicsColors.BorderSoft, AlphaPicsShapes.Card)
                                .padding(horizontal = 28.dp, vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AlphaPicsGlyph(
                                icon = AlphaPicsIcon.HISTORY,
                                accent = AlphaPicsColors.Cyan,
                                size = 64.dp
                            )
                            Spacer(Modifier.height(18.dp))
                            Text(
                                "Your recent results will appear here",
                                color = textColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Compress a photo or batch to start building your history.",
                                color = AlphaPicsColors.TextSecondary
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(entries, key = { it.id }) { entry ->
                                HistoryRow(
                                    entry = entry,
                                    cardColor = cardColor,
                                    textColor = textColor,
                                    onDelete = {
                                        scope.launch {
                                            try {
                                                dao.delete(entry)
                                            } catch (t: Throwable) {
                                            }
                                            reload()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    entry: CompressionHistoryEntity,
    cardColor: Color,
    textColor: Color,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardColor, AlphaPicsShapes.Card)
            .border(1.dp, AlphaPicsColors.BorderSoft, AlphaPicsShapes.Card)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.originalFileName,
                color = textColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${formatBytes(entry.originalSizeBytes)} → ${formatBytes(entry.finalSizeBytes)}  •  ${entry.percentSaved}% saved",
                color = AlphaPicsColors.TextSecondary
            )
            Text(
                "${entry.inputFormat.uppercase()} → ${entry.outputFormat.uppercase()}  •  ${entry.finalWidth} × ${entry.finalHeight}",
                color = AlphaPicsColors.TextTertiary
            )
            if (entry.settingValue.isNotBlank()) {
                Text(
                    if (entry.targetReached == false) {
                        "Target not reached • ${entry.settingValue}"
                    } else {
                        entry.settingValue
                    },
                    color = if (entry.targetReached == false) {
                        Color(0xFFF97316)
                    } else {
                        AlphaPicsColors.TextTertiary
                    }
                )
            }
            Text(
                dateFormatter.format(Date(entry.timestampMillis)),
                color = AlphaPicsColors.TextTertiary.copy(alpha = 0.78f)
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete history item",
                tint = AlphaPicsColors.TextTertiary
            )
        }
    }
}
