package com.example.compression

import kotlin.math.ceil
import kotlin.math.max

/** Central policy used before an encoded output may be saved, shared, or counted as success. */
object CompressionBenefitPolicy {
    const val MIN_ABSOLUTE_SAVING_BYTES = 1024L
    const val MIN_RELATIVE_SAVING = 0.01

    fun meaningfulSavingThreshold(originalBytes: Long): Long = max(
        MIN_ABSOLUTE_SAVING_BYTES,
        ceil(originalBytes.coerceAtLeast(0L) * MIN_RELATIVE_SAVING).toLong()
    )

    fun evaluate(originalBytes: Long, candidateBytes: Long): BenefitDecision {
        if (originalBytes <= 0L) {
            return BenefitDecision.Rejected("The original file size could not be read reliably.")
        }
        if (candidateBytes <= 0L) {
            return BenefitDecision.Rejected("Compression produced an empty output.")
        }
        if (candidateBytes >= originalBytes) {
            return BenefitDecision.Rejected(
                "The encoded result was not smaller than the original. The original was retained."
            )
        }

        val bytesSaved = originalBytes - candidateBytes
        val threshold = meaningfulSavingThreshold(originalBytes)
        if (bytesSaved < threshold) {
            return BenefitDecision.Rejected(
                "The possible saving was too small to justify another encoded copy. The original was retained."
            )
        }
        return BenefitDecision.Accepted(bytesSaved = bytesSaved, thresholdBytes = threshold)
    }
}

sealed interface BenefitDecision {
    data class Accepted(val bytesSaved: Long, val thresholdBytes: Long) : BenefitDecision
    data class Rejected(val reason: String) : BenefitDecision
}

data class EvaluatedQualityCandidate<T>(
    val quality: Int,
    val sizeBytes: Long,
    val payload: T
)

/**
 * Selects from every evaluated candidate rather than trusting the last binary-search step.
 * This remains correct for the evaluated set even when an encoder's byte sizes are not strictly
 * monotonic with quality.
 */
fun <T> selectTargetCandidate(
    candidates: Collection<EvaluatedQualityCandidate<T>>,
    targetBytes: Long
): TargetCandidateSelection<T> {
    val nonEmpty = candidates.filter { it.sizeBytes > 0L }
    val qualifying = nonEmpty.filter { it.sizeBytes <= targetBytes }
    val reached = qualifying.maxWithOrNull(
        compareBy<EvaluatedQualityCandidate<T>> { it.quality }
            .thenBy { -it.sizeBytes }
    )
    if (reached != null) return TargetCandidateSelection.Reached(reached)

    val bestAttempt = nonEmpty.minWithOrNull(
        compareBy<EvaluatedQualityCandidate<T>> { it.sizeBytes }
            .thenByDescending { it.quality }
    )
    return if (bestAttempt != null) {
        TargetCandidateSelection.NotReached(bestAttempt)
    } else {
        TargetCandidateSelection.None
    }
}

sealed interface TargetCandidateSelection<out T> {
    data class Reached<T>(val candidate: EvaluatedQualityCandidate<T>) : TargetCandidateSelection<T>
    data class NotReached<T>(val candidate: EvaluatedQualityCandidate<T>) : TargetCandidateSelection<T>
    data object None : TargetCandidateSelection<Nothing>
}

fun originalAlreadyWithinTarget(originalBytes: Long, targetBytes: Long): Boolean =
    originalBytes > 0L && targetBytes > 0L && originalBytes <= targetBytes
