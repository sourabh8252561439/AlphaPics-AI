package com.example.batchstudio

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/** Sequential, memory-safe orchestration with per-item failure isolation. */
object BatchStudioRunner {
    suspend fun run(
        items: List<BatchStudioItem>,
        process: suspend (BatchStudioItem, (Float) -> Unit) -> Result<BatchStudioOutput>,
        onUpdate: (items: List<BatchStudioItem>, overallProgress: Float) -> Unit = { _, _ -> }
    ): List<BatchStudioItem> {
        if (items.isEmpty()) return emptyList()
        var current = items.map {
            it.copy(
                status = BatchStudioItemStatus.QUEUED,
                progress = 0f,
                output = null,
                errorMessage = null
            )
        }
        onUpdate(current, 0f)

        items.forEachIndexed { index, sourceItem ->
            coroutineContext.ensureActive()
            current = current.update(sourceItem.id) {
                it.copy(status = BatchStudioItemStatus.PROCESSING, progress = 0f)
            }
            onUpdate(current, index / items.size.toFloat())

            val result = try {
                process(sourceItem) { itemProgress ->
                    val safe = itemProgress.coerceIn(0f, 1f)
                    current = current.update(sourceItem.id) { it.copy(progress = safe) }
                    onUpdate(current, (index + safe) / items.size.toFloat())
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                Result.failure(error)
            }

            current = result.fold(
                onSuccess = { output ->
                    current.update(sourceItem.id) {
                        it.copy(
                            status = BatchStudioItemStatus.SUCCEEDED,
                            progress = 1f,
                            output = output,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    current.update(sourceItem.id) {
                        it.copy(
                            status = BatchStudioItemStatus.FAILED,
                            progress = 1f,
                            output = null,
                            errorMessage = error.localizedMessage ?: "This item could not be processed."
                        )
                    }
                }
            )
            onUpdate(current, (index + 1f) / items.size.toFloat())
        }
        return current
    }

    private fun List<BatchStudioItem>.update(
        id: String,
        transform: (BatchStudioItem) -> BatchStudioItem
    ): List<BatchStudioItem> = map { item -> if (item.id == id) transform(item) else item }
}
