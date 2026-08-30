package com.example.batch

import com.example.compression.CompressionSettingsSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchAccountingTest {

    @Test
    fun `quality snapshot reaches every planned request`() {
        val settings = CompressionSettingsSnapshot.Quality(85)
        val plan = createBatchPlan(listOf("a", "b", "c"), settings)
        assertEquals(3, plan.size)
        plan.forEach {
            assertSame(settings, it.settings)
            assertEquals(85, (it.settings as CompressionSettingsSnapshot.Quality).percentage)
        }
    }

    @Test
    fun `target snapshot reaches every planned request`() {
        val settings = CompressionSettingsSnapshot.TargetSize(100)
        val plan = createBatchPlan(listOf("a", "b"), settings)
        plan.forEach {
            assertSame(settings, it.settings)
            assertEquals(100, (it.settings as CompressionSettingsSnapshot.TargetSize).kilobytes)
        }
    }

    @Test
    fun `mixed outcomes keep skipped and failed images at original size`() {
        val summary = BatchSummary.calculate(
            totalSelected = 4,
            records = listOf(
                BatchAccountingRecord(100_000, 70_000, BatchAccountingOutcome.COMPRESSED),
                BatchAccountingRecord(100_000, 80_000, BatchAccountingOutcome.TARGET_NOT_REACHED),
                BatchAccountingRecord(100_000, null, BatchAccountingOutcome.SKIPPED),
                BatchAccountingRecord(100_000, null, BatchAccountingOutcome.FAILED)
            )
        )

        assertEquals(1, summary.compressed)
        assertEquals(1, summary.targetNotReached)
        assertEquals(1, summary.skipped)
        assertEquals(1, summary.failed)
        assertEquals(400_000, summary.originalTotalBytes)
        assertEquals(350_000, summary.finalTotalBytes)
        assertEquals(50_000, summary.bytesSaved)
        assertEquals(12, summary.percentSaved)
    }

    @Test
    fun `rejected larger final value cannot create negative successful savings`() {
        val summary = BatchSummary.calculate(
            totalSelected = 1,
            records = listOf(
                BatchAccountingRecord(100_000, 120_000, BatchAccountingOutcome.COMPRESSED)
            )
        )
        assertEquals(100_000, summary.finalTotalBytes)
        assertEquals(0, summary.bytesSaved)
        assertTrue(summary.percentSaved >= 0)
    }

    @Test
    fun `zero original total cannot divide by zero`() {
        val summary = BatchSummary.calculate(
            totalSelected = 1,
            records = listOf(BatchAccountingRecord(0, null, BatchAccountingOutcome.FAILED))
        )
        assertEquals(0, summary.percentSaved)
    }
}
