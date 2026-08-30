package com.example.batchstudio

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.batch.MAX_BATCH_ITEMS
import com.example.photo.PhotoMetadataReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

class BatchStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val _items = MutableStateFlow<List<BatchStudioItem>>(emptyList())
    val items: StateFlow<List<BatchStudioItem>> = _items.asStateFlow()

    private val _settings = MutableStateFlow(BatchStudioSettings())
    val settings: StateFlow<BatchStudioSettings> = _settings.asStateFlow()

    private val _isInspecting = MutableStateFlow(false)
    val isInspecting: StateFlow<Boolean> = _isInspecting.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _overallProgress = MutableStateFlow(0f)
    val overallProgress: StateFlow<Float> = _overallProgress.asStateFlow()

    private val _summary = MutableStateFlow<BatchStudioSummary?>(null)
    val summary: StateFlow<BatchStudioSummary?> = _summary.asStateFlow()

    private var selectionJob: Job? = null
    private var processingJob: Job? = null

    fun setSelection(context: Context, uris: List<Uri>) {
        if (_isProcessing.value) return
        selectionJob?.cancel()
        val selected = uris.distinctBy(Uri::toString).take(MAX_BATCH_ITEMS)
        selectionJob = viewModelScope.launch(Dispatchers.IO) {
            _isInspecting.value = true
            try {
                val inspected = selected.map { uri ->
                    coroutineContext.ensureActive()
                    val metadata = PhotoMetadataReader.read(context, uri).getOrNull()
                    BatchStudioItem(
                        id = uri.toString(),
                        uri = uri,
                        displayName = metadata?.displayName ?: uri.lastPathSegment ?: "Unreadable photo",
                        width = metadata?.width ?: 0,
                        height = metadata?.height ?: 0,
                        mimeType = metadata?.mimeType.orEmpty(),
                        sizeBytes = metadata?.sizeBytes
                    )
                }
                _items.value = inspected
                _summary.value = null
                _overallProgress.value = 0f
            } finally {
                _isInspecting.value = false
            }
        }
    }

    fun updateSettings(settings: BatchStudioSettings) {
        if (_isProcessing.value) return
        _settings.value = settings.sanitized()
        _summary.value = null
    }

    fun removeItem(id: String) {
        if (_isProcessing.value) return
        _items.value = _items.value.filterNot { it.id == id }
        _summary.value = null
        _overallProgress.value = 0f
    }

    fun clearAll() {
        if (_isProcessing.value) return
        selectionJob?.cancel()
        _items.value = emptyList()
        _summary.value = null
        _overallProgress.value = 0f
    }

    fun processAll(context: Context) {
        if (_isProcessing.value || _items.value.isEmpty()) return
        val selection = _items.value.take(MAX_BATCH_ITEMS)
        val settingsSnapshot = _settings.value.sanitized()
        processingJob = viewModelScope.launch(Dispatchers.Default) {
            _isProcessing.value = true
            _overallProgress.value = 0f
            _summary.value = null
            _items.value = selection.map {
                it.copy(
                    status = BatchStudioItemStatus.QUEUED,
                    progress = 0f,
                    output = null,
                    errorMessage = null
                )
            }
            try {
                _items.value = BatchStudioRunner.run(
                    items = selection,
                    process = { item, onItemProgress ->
                        BatchStudioEngine.processItem(
                            context = context,
                            sourceUri = item.uri,
                            sourceMimeType = item.mimeType,
                            sourceName = item.displayName,
                            settings = settingsSnapshot,
                            onProgress = onItemProgress
                        )
                    },
                    onUpdate = { updatedItems, progress ->
                        _items.value = updatedItems
                        _overallProgress.value = progress
                    }
                )
            } catch (cancelled: CancellationException) {
                _items.value = _items.value.map { item ->
                    if (item.status == BatchStudioItemStatus.QUEUED ||
                        item.status == BatchStudioItemStatus.PROCESSING
                    ) {
                        item.copy(
                            status = BatchStudioItemStatus.CANCELLED,
                            errorMessage = "Cancelled",
                            progress = 0f
                        )
                    } else item
                }
            } finally {
                _isProcessing.value = false
                _summary.value = BatchStudioSummary.from(_items.value)
            }
        }
    }

    fun cancelProcessing() {
        processingJob?.cancel()
    }
}
