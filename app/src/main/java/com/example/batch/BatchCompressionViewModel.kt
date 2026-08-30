package com.example.batch

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.ads.DailyTokenManager
import com.example.ads.InterstitialAdManager
import com.example.compression.CompressionArtifact
import com.example.compression.CompressionMode
import com.example.compression.CompressionSettingsSnapshot
import com.example.compression.CompressionSettingsState
import com.example.compression.ImageCompressionOutcome
import com.example.compression.ImageSourceInspector
import com.example.compression.SettingsValidation
import com.example.compression.TargetSizeInputState
import com.example.history.CompressionHistoryDatabase
import com.example.history.CompressionHistoryEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BatchCompressionViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val _items = MutableStateFlow<List<BatchImageItem>>(emptyList())
    val items: StateFlow<List<BatchImageItem>> = _items.asStateFlow()

    private val _settings = MutableStateFlow(
        CompressionSettingsState(
            mode = savedStateHandle.get<String>(KEY_MODE)?.let(CompressionMode::fromStored)
                ?: CompressionMode.QUALITY,
            qualitySliderValue = savedStateHandle.get<Float>(KEY_QUALITY) ?: 85f,
            targetSize = TargetSizeInputState(
                text = savedStateHandle.get<String>(KEY_TARGET_TEXT).orEmpty(),
                sliderPosition = savedStateHandle.get<Float>(KEY_TARGET_SLIDER)
                    ?: TargetSizeInputState().sliderPosition,
                committedKilobytes = savedStateHandle.get<Int>(KEY_TARGET_COMMITTED)
            )
        )
    )
    val settings: StateFlow<CompressionSettingsState> = _settings.asStateFlow()

    private val _processingSettings = MutableStateFlow<CompressionSettingsSnapshot?>(null)
    val processingSettings: StateFlow<CompressionSettingsSnapshot?> =
        _processingSettings.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _summary = MutableStateFlow<BatchSummary?>(null)
    val summary: StateFlow<BatchSummary?> = _summary.asStateFlow()

    private val _dailyLimitReached = MutableStateFlow(false)
    val dailyLimitReached: StateFlow<Boolean> = _dailyLimitReached.asStateFlow()

    @Volatile
    private var cancelRequested = false

    @Volatile
    private var pendingTokenGate: CompletableDeferred<Boolean>? = null

    private val historyDao by lazy {
        CompressionHistoryDatabase.getInstance(getApplication()).historyDao()
    }

    fun setMode(mode: CompressionMode) {
        if (!_isProcessing.value) {
            _settings.value = _settings.value.copy(mode = mode)
            persistSettings()
        }
    }

    fun setQuality(value: Float) {
        if (!_isProcessing.value) {
            _settings.value = _settings.value.withQuality(value)
            persistSettings()
        }
    }

    fun setTargetState(target: TargetSizeInputState) {
        if (!_isProcessing.value) {
            _settings.value = _settings.value.copy(targetSize = target)
            persistSettings()
        }
    }

    /** Replaces the current selection and enforces the 20-image cap before inspection. */
    fun setSelection(context: Context, uris: List<Uri>) {
        if (_isProcessing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            val inspected = uris
                .take(MAX_BATCH_ITEMS)
                .map { uri ->
                    try {
                        BatchImageInspector.inspect(context, uri)
                    } catch (_: Throwable) {
                        null
                    } ?: BatchImageItem(
                        id = uri.toString(),
                        uri = uri,
                        displayName = uri.lastPathSegment ?: "Unreadable image",
                        originalSizeBytes = ImageSourceInspector.readByteSize(context, uri) ?: 0L,
                        originalWidth = 0,
                        originalHeight = 0,
                        inputFormat = "image"
                    )
                }
            _items.value = inspected
            _summary.value = null
        }
    }

    fun removeItem(id: String) {
        if (_isProcessing.value) return
        _items.value = _items.value.filterNot { it.id == id }
        _summary.value = null
    }

    fun clearAll() {
        if (_isProcessing.value) return
        _items.value = emptyList()
        _summary.value = null
    }

    fun cancelProcessing() {
        cancelRequested = true
        resolveDailyTokenGate(false)
    }

    fun resolveDailyTokenGate(granted: Boolean) {
        val gate = pendingTokenGate ?: return
        if (!gate.isCompleted) gate.complete(granted)
        pendingTokenGate = null
        _dailyLimitReached.value = false
    }

    private suspend fun awaitDailyTokenGate(): Boolean {
        val gate = CompletableDeferred<Boolean>()
        pendingTokenGate = gate
        _dailyLimitReached.value = true
        return try {
            gate.await()
        } finally {
            if (pendingTokenGate === gate) pendingTokenGate = null
            _dailyLimitReached.value = false
        }
    }

    private fun persistSettings() {
        val current = _settings.value
        savedStateHandle[KEY_MODE] = current.mode.name
        savedStateHandle[KEY_QUALITY] = current.qualitySliderValue
        savedStateHandle[KEY_TARGET_TEXT] = current.targetSize.text
        savedStateHandle[KEY_TARGET_SLIDER] = current.targetSize.sliderPosition
        savedStateHandle[KEY_TARGET_COMMITTED] = current.targetSize.committedKilobytes
    }

    private fun updateItem(id: String, transform: (BatchImageItem) -> BatchImageItem) {
        _items.value = _items.value.map { item ->
            if (item.id == id) transform(item) else item
        }
    }

    /**
     * Validates once, snapshots settings once, and processes sequentially. Settings UI is locked
     * for the run, and each item receives the exact same immutable snapshot.
     */
    fun processAll(context: Context, activity: Activity) {
        if (_isProcessing.value || _items.value.isEmpty()) return
        val validation = _settings.value.validateForProcessing()
        _settings.value = validation.state
        persistSettings()
        if (validation !is SettingsValidation.Valid) return

        val settingsSnapshot = validation.snapshot
        val selectionSnapshot = _items.value
        val plannedRequests = createBatchPlan(
            selectionSnapshot.map(BatchImageItem::id),
            settingsSnapshot
        )
        cancelRequested = false

        viewModelScope.launch(Dispatchers.Default) {
            _isProcessing.value = true
            _processingSettings.value = settingsSnapshot
            _summary.value = null
            _items.value = selectionSnapshot.map {
                it.copy(
                    status = BatchItemStatus.PENDING,
                    result = null,
                    errorMessage = null
                )
            }
            val records = selectionSnapshot.associate { item ->
                item.id to BatchAccountingRecord(
                    originalSizeBytes = item.originalSizeBytes,
                    finalSizeBytes = null,
                    outcome = BatchAccountingOutcome.UNPROCESSED
                )
            }.toMutableMap()
            var anySavedOutput = false

            try {
                for (request in plannedRequests) {
                    if (cancelRequested) break

                    // One saved image costs one token. Pause before the processor can compress or
                    // write the next image when the deterministic daily balance is exhausted.
                    if (!DailyTokenManager.hasAvailableToken(context)) {
                        val granted = awaitDailyTokenGate()
                        if (!granted || cancelRequested) {
                            cancelRequested = true
                            break
                        }
                    }

                    val item = selectionSnapshot.firstOrNull { it.id == request.itemId } ?: continue
                    updateItem(item.id) {
                        it.copy(
                            status = BatchItemStatus.PROCESSING,
                            result = null,
                            errorMessage = null
                        )
                    }

                    val outcome = try {
                        BatchImageProcessor.process(context, item, request.settings)
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (error: Throwable) {
                        ImageCompressionOutcome.Failed(
                            originalSizeBytes = item.originalSizeBytes,
                            settings = request.settings,
                            reason = error.message ?: "Compression failed."
                        )
                    }

                    when (outcome) {
                        is ImageCompressionOutcome.Compressed -> {
                            anySavedOutput = true
                            val result = outcome.artifact.toBatchResult(
                                item = item,
                                message = "Compressed"
                            )
                            records[item.id] = BatchAccountingRecord(
                                originalSizeBytes = outcome.artifact.originalSizeBytes,
                                finalSizeBytes = outcome.artifact.finalSizeBytes,
                                outcome = BatchAccountingOutcome.COMPRESSED
                            )
                            updateItem(item.id) {
                                it.copy(
                                    originalSizeBytes = outcome.artifact.originalSizeBytes,
                                    status = BatchItemStatus.COMPRESSED,
                                    result = result,
                                    errorMessage = null
                                )
                            }
                            insertHistory(item, outcome.artifact)
                            DailyTokenManager.consumeAfterSuccessfulCompression(context)
                        }

                        is ImageCompressionOutcome.TargetNotReached -> {
                            anySavedOutput = true
                            val result = outcome.artifact.toBatchResult(
                                item = item,
                                message = outcome.reason
                            )
                            records[item.id] = BatchAccountingRecord(
                                originalSizeBytes = outcome.artifact.originalSizeBytes,
                                finalSizeBytes = outcome.artifact.finalSizeBytes,
                                outcome = BatchAccountingOutcome.TARGET_NOT_REACHED
                            )
                            updateItem(item.id) {
                                it.copy(
                                    originalSizeBytes = outcome.artifact.originalSizeBytes,
                                    status = BatchItemStatus.TARGET_NOT_REACHED,
                                    result = result,
                                    errorMessage = outcome.reason
                                )
                            }
                            insertHistory(item, outcome.artifact)
                            DailyTokenManager.consumeAfterSuccessfulCompression(context)
                        }

                        is ImageCompressionOutcome.Skipped -> {
                            records[item.id] = BatchAccountingRecord(
                                originalSizeBytes = outcome.originalSizeBytes,
                                finalSizeBytes = null,
                                outcome = BatchAccountingOutcome.SKIPPED
                            )
                            updateItem(item.id) {
                                it.copy(
                                    originalSizeBytes = outcome.originalSizeBytes,
                                    status = BatchItemStatus.SKIPPED,
                                    result = null,
                                    errorMessage = outcome.reason
                                )
                            }
                        }

                        is ImageCompressionOutcome.Failed -> {
                            records[item.id] = BatchAccountingRecord(
                                originalSizeBytes = outcome.originalSizeBytes
                                    .takeIf { it > 0L }
                                    ?: item.originalSizeBytes,
                                finalSizeBytes = null,
                                outcome = BatchAccountingOutcome.FAILED
                            )
                            updateItem(item.id) {
                                it.copy(
                                    status = BatchItemStatus.FAILED,
                                    result = null,
                                    errorMessage = outcome.reason
                                )
                            }
                        }
                    }

                    _summary.value = BatchSummary.calculate(
                        totalSelected = selectionSnapshot.size,
                        records = records.values
                    )
                }
            } finally {
                resolveDailyTokenGate(false)
                _summary.value = BatchSummary.calculate(
                    totalSelected = selectionSnapshot.size,
                    records = records.values
                )
                _processingSettings.value = null
                _isProcessing.value = false
            }

            if (anySavedOutput) {
                withContext(Dispatchers.Main) {
                    InterstitialAdManager.onSuccessfulCompression(activity)
                }
            }
        }
    }

    private suspend fun insertHistory(item: BatchImageItem, artifact: CompressionArtifact) {
        try {
            historyDao.insert(
                CompressionHistoryEntity(
                    timestampMillis = System.currentTimeMillis(),
                    originalFileName = item.displayName,
                    originalSizeBytes = artifact.originalSizeBytes,
                    finalSizeBytes = artifact.finalSizeBytes,
                    originalWidth = artifact.originalWidth,
                    originalHeight = artifact.originalHeight,
                    finalWidth = artifact.finalWidth,
                    finalHeight = artifact.finalHeight,
                    inputFormat = artifact.inputFormat,
                    outputFormat = artifact.outputFormat,
                    compressionMode = artifact.settings.historyValue,
                    settingValue = artifact.settings.displayLabel,
                    targetReached = artifact.targetReached,
                    outputUriString = artifact.outputUri.toString()
                )
            )
        } catch (_: Throwable) {
            // History failure must not invalidate an already saved, validated output.
        }
    }

    private fun CompressionArtifact.toBatchResult(
        item: BatchImageItem,
        message: String
    ): BatchItemResult = BatchItemResult(
        originalSizeBytes = originalSizeBytes,
        finalSizeBytes = finalSizeBytes,
        bytesSaved = bytesSaved,
        percentSaved = percentSaved,
        finalWidth = finalWidth,
        finalHeight = finalHeight,
        outputFormat = outputFormat,
        outputUri = outputUri,
        settings = settings,
        targetReached = targetReached,
        message = message,
        insights = SmartInsights.generate(
            originalWidth = item.originalWidth,
            originalHeight = item.originalHeight,
            originalSizeBytes = originalSizeBytes,
            hasTransparency = hasTransparency,
            inputFormat = item.inputFormat,
            finalSizeBytes = finalSizeBytes
        )
    )

    private companion object {
        const val KEY_MODE = "batch_compression_mode"
        const val KEY_QUALITY = "batch_compression_quality"
        const val KEY_TARGET_TEXT = "batch_target_text"
        const val KEY_TARGET_SLIDER = "batch_target_slider"
        const val KEY_TARGET_COMMITTED = "batch_target_committed"
    }
}
