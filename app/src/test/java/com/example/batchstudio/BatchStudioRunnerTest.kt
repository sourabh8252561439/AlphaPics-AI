package com.example.batchstudio

import android.net.Uri
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchStudioRunnerTest {

    @Test
    fun `one failed item does not prevent later items from succeeding`() = runTest {
        val items = listOf(item("bad"), item("good"))
        val output = BatchStudioOutput(
            uri = Uri.parse("content://alphapics/output"),
            width = 80,
            height = 60,
            sizeBytes = 512,
            mimeType = "image/png",
            filename = "output.png"
        )
        var finalProgress = 0f

        val result = BatchStudioRunner.run(
            items = items,
            process = { current, onProgress ->
                onProgress(0.5f)
                if (current.id == "bad") Result.failure(IllegalStateException("Unreadable item"))
                else Result.success(output)
            },
            onUpdate = { _, progress -> finalProgress = progress }
        )

        assertEquals(BatchStudioItemStatus.FAILED, result[0].status)
        assertEquals("Unreadable item", result[0].errorMessage)
        assertEquals(BatchStudioItemStatus.SUCCEEDED, result[1].status)
        assertEquals(output, result[1].output)
        assertEquals(1f, finalProgress, 0.001f)
    }

    @Test
    fun `runner exposes per item processing states`() = runTest {
        val observedStatuses = mutableListOf<List<BatchStudioItemStatus>>()
        val output = BatchStudioOutput(
            Uri.parse("content://alphapics/one"), 10, 10, 10, "image/jpeg", "one.jpg"
        )

        BatchStudioRunner.run(
            items = listOf(item("one")),
            process = { _, onProgress -> onProgress(0.4f); Result.success(output) },
            onUpdate = { updated, _ -> observedStatuses += updated.map(BatchStudioItem::status) }
        )

        assertTrue(observedStatuses.any { it.single() == BatchStudioItemStatus.PROCESSING })
        assertEquals(BatchStudioItemStatus.SUCCEEDED, observedStatuses.last().single())
    }

    private fun item(id: String) = BatchStudioItem(
        id = id,
        uri = Uri.parse("content://alphapics/$id"),
        displayName = "$id.jpg",
        width = 100,
        height = 80,
        mimeType = "image/jpeg",
        sizeBytes = 1000
    )
}
