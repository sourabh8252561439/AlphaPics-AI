package com.example.batch

import android.net.Uri
import com.example.compression.CompressionSettingsSnapshot

enum class BatchItemStatus {
    PENDING,
    PROCESSING,
    COMPRESSED,
    TARGET_NOT_REACHED,
    SKIPPED,
    FAILED
}

data class BatchItemResult(
    val originalSizeBytes: Long,
    val finalSizeBytes: Long,
    val bytesSaved: Long,
    val percentSaved: Int,
    val finalWidth: Int,
    val finalHeight: Int,
    val outputFormat: String,
    val outputUri: Uri?,
    val settings: CompressionSettingsSnapshot,
    val targetReached: Boolean?,
    val message: String,
    val insights: List<SmartInsights.Insight>
)

data class BatchImageItem(
    val id: String,
    val uri: Uri,
    val displayName: String,
    val originalSizeBytes: Long,
    val originalWidth: Int,
    val originalHeight: Int,
    val inputFormat: String,
    val status: BatchItemStatus = BatchItemStatus.PENDING,
    val result: BatchItemResult? = null,
    val errorMessage: String? = null
)

data class PlannedBatchRequest(
    val itemId: String,
    val settings: CompressionSettingsSnapshot
)

/** Creates one immutable settings snapshot reference for every planned image. */
fun createBatchPlan(
    itemIds: List<String>,
    settings: CompressionSettingsSnapshot
): List<PlannedBatchRequest> = itemIds.map { PlannedBatchRequest(it, settings) }

enum class BatchAccountingOutcome {
    COMPRESSED,
    TARGET_NOT_REACHED,
    SKIPPED,
    FAILED,
    UNPROCESSED
}

data class BatchAccountingRecord(
    val originalSizeBytes: Long,
    val finalSizeBytes: Long?,
    val outcome: BatchAccountingOutcome
)

data class BatchSummary(
    val totalSelected: Int,
    val compressed: Int,
    val targetNotReached: Int,
    val skipped: Int,
    val failed: Int,
    val originalTotalBytes: Long,
    val finalTotalBytes: Long
) {
    val successfulOutputs: Int get() = compressed + targetNotReached
    val processed: Int get() = compressed + targetNotReached + skipped + failed
    val bytesSaved: Long get() = (originalTotalBytes - finalTotalBytes).coerceAtLeast(0L)
    val percentSaved: Int get() = if (originalTotalBytes > 0L) {
        ((bytesSaved.toDouble() / originalTotalBytes.toDouble()) * 100.0)
            .toInt()
            .coerceIn(0, 100)
    } else {
        0
    }

    companion object {
        fun calculate(
            totalSelected: Int,
            records: Collection<BatchAccountingRecord>
        ): BatchSummary {
            var compressed = 0
            var targetNotReached = 0
            var skipped = 0
            var failed = 0
            var originalTotal = 0L
            var finalTotal = 0L

            records.forEach { record ->
                val original = record.originalSizeBytes.coerceAtLeast(0L)
                originalTotal += original
                when (record.outcome) {
                    BatchAccountingOutcome.COMPRESSED -> {
                        compressed++
                        finalTotal += record.finalSizeBytes
                            ?.coerceIn(0L, original)
                            ?: original
                    }
                    BatchAccountingOutcome.TARGET_NOT_REACHED -> {
                        targetNotReached++
                        finalTotal += record.finalSizeBytes
                            ?.coerceIn(0L, original)
                            ?: original
                    }
                    BatchAccountingOutcome.SKIPPED -> {
                        skipped++
                        finalTotal += original
                    }
                    BatchAccountingOutcome.FAILED -> {
                        failed++
                        finalTotal += original
                    }
                    BatchAccountingOutcome.UNPROCESSED -> {
                        finalTotal += original
                    }
                }
            }

            return BatchSummary(
                totalSelected = totalSelected.coerceAtLeast(records.size),
                compressed = compressed,
                targetNotReached = targetNotReached,
                skipped = skipped,
                failed = failed,
                originalTotalBytes = originalTotal,
                finalTotalBytes = finalTotal
            )
        }
    }
}

const val MAX_BATCH_ITEMS = 20
