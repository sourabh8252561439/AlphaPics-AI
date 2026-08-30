package com.example.compression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompressionPolicyTest {

    @Test
    fun `candidate larger than original is rejected`() {
        assertTrue(CompressionBenefitPolicy.evaluate(100_000, 100_001) is BenefitDecision.Rejected)
    }

    @Test
    fun `candidate equal to original is rejected`() {
        assertTrue(CompressionBenefitPolicy.evaluate(100_000, 100_000) is BenefitDecision.Rejected)
    }

    @Test
    fun `insignificant saving is rejected`() {
        assertTrue(CompressionBenefitPolicy.evaluate(100_000, 99_500) is BenefitDecision.Rejected)
    }

    @Test
    fun `meaningful smaller candidate is accepted`() {
        val decision = CompressionBenefitPolicy.evaluate(100_000, 98_000)
        assertTrue(decision is BenefitDecision.Accepted)
        assertEquals(2_000, (decision as BenefitDecision.Accepted).bytesSaved)
    }

    @Test
    fun `highest quality evaluated candidate satisfying target wins despite non monotonic sizes`() {
        val selection = selectTargetCandidate(
            candidates = listOf(
                EvaluatedQualityCandidate(70, 90_000, "q70"),
                EvaluatedQualityCandidate(80, 101_000, "q80"),
                EvaluatedQualityCandidate(85, 98_000, "q85"),
                EvaluatedQualityCandidate(90, 110_000, "q90")
            ),
            targetBytes = 100_000
        ) as TargetCandidateSelection.Reached

        assertEquals(85, selection.candidate.quality)
        assertEquals("q85", selection.candidate.payload)
    }

    @Test
    fun `unreachable target returns smallest evaluated attempt honestly`() {
        val selection = selectTargetCandidate(
            candidates = listOf(
                EvaluatedQualityCandidate(5, 60_000, "smallest"),
                EvaluatedQualityCandidate(20, 75_000, "larger")
            ),
            targetBytes = 50_000
        ) as TargetCandidateSelection.NotReached

        assertEquals("smallest", selection.candidate.payload)
    }

    @Test
    fun `original already below target is detected before encoding`() {
        assertTrue(originalAlreadyWithinTarget(80_000, 100_000))
    }
}
