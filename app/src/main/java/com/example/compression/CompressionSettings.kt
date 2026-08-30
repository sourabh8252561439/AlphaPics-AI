package com.example.compression

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/** The only compression modes supported by the product. */
enum class CompressionMode {
    TARGET_SIZE,
    QUALITY;

    companion object {
        /** Unknown or retired persisted values safely migrate to Target Size. */
        fun fromStored(value: String?): CompressionMode =
            entries.firstOrNull { it.name == value } ?: TARGET_SIZE
    }
}

/** Immutable settings passed to the image pipeline when processing begins. */
sealed interface CompressionSettingsSnapshot {
    val mode: CompressionMode
    val displayLabel: String
    val historyValue: String

    data class Quality(val percentage: Int) : CompressionSettingsSnapshot {
        init {
            require(percentage in QUALITY_MIN_PERCENT..QUALITY_MAX_PERCENT)
        }

        override val mode: CompressionMode = CompressionMode.QUALITY
        override val displayLabel: String = "Quality: $percentage%"
        override val historyValue: String = "quality"
    }

    data class TargetSize(val kilobytes: Int) : CompressionSettingsSnapshot {
        init {
            require(kilobytes in TargetSizeRules.MIN_KB..TargetSizeRules.MAX_KB)
        }

        val targetBytes: Long = kilobytes.toLong() * 1024L
        override val mode: CompressionMode = CompressionMode.TARGET_SIZE
        override val displayLabel: String = "Target Size: $kilobytes KB"
        override val historyValue: String = "target_size"
    }
}

const val QUALITY_MIN_PERCENT = 10
const val QUALITY_MAX_PERCENT = 100

/**
 * Pure target-input rules shared by single and batch UI. The editable text is a draft and is
 * never replaced with a default zero. Clamping happens only at a deliberate commit point.
 */
object TargetSizeRules {
    const val MIN_KB = 10
    const val MAX_KB = 15 * 1024
    const val SLIDER_DEFAULT_KB = 100

    fun filterDigits(raw: String): String = raw.filter(Char::isDigit)

    fun kbToSliderPosition(kilobytes: Float): Float {
        val clamped = kilobytes.coerceIn(MIN_KB.toFloat(), MAX_KB.toFloat())
        return (
            ln(clamped / MIN_KB.toFloat()) /
                ln(MAX_KB.toFloat() / MIN_KB.toFloat())
            ).coerceIn(0f, 1f)
    }

    fun sliderPositionToKb(position: Float): Float =
        MIN_KB.toFloat() *
            (MAX_KB.toFloat() / MIN_KB.toFloat()).pow(position.coerceIn(0f, 1f))
}

data class TargetSizeInputState(
    val text: String = "",
    /** Continuous slider position is kept independently from rounded display text. */
    val sliderPosition: Float = TargetSizeRules.kbToSliderPosition(
        TargetSizeRules.SLIDER_DEFAULT_KB.toFloat()
    ),
    val committedKilobytes: Int? = null,
    val validationMessage: String? = null
) {
    val previewKilobytes: Int?
        get() = text.toLongOrNull()
            ?.takeIf { it in TargetSizeRules.MIN_KB.toLong()..TargetSizeRules.MAX_KB.toLong() }
            ?.toInt()

    fun withText(raw: String): TargetSizeInputState = copy(
        text = TargetSizeRules.filterDigits(raw),
        committedKilobytes = null,
        validationMessage = null
    )

    fun withSliderPosition(position: Float): TargetSizeInputState {
        val stablePosition = position.coerceIn(0f, 1f)
        val kilobytes = TargetSizeRules.sliderPositionToKb(stablePosition)
            .roundToInt()
            .coerceIn(TargetSizeRules.MIN_KB, TargetSizeRules.MAX_KB)
        return copy(
            text = kilobytes.toString(),
            sliderPosition = stablePosition,
            committedKilobytes = kilobytes,
            validationMessage = null
        )
    }

    fun withPreset(kilobytes: Int): TargetSizeInputState {
        val clamped = kilobytes.coerceIn(TargetSizeRules.MIN_KB, TargetSizeRules.MAX_KB)
        return copy(
            text = clamped.toString(),
            sliderPosition = TargetSizeRules.kbToSliderPosition(clamped.toFloat()),
            committedKilobytes = clamped,
            validationMessage = null
        )
    }

    fun commit(): TargetSizeCommit {
        val parsed = text.toLongOrNull()
            ?: return TargetSizeCommit(
                state = copy(
                    committedKilobytes = null,
                    validationMessage = "Enter a target from ${TargetSizeRules.MIN_KB} to ${TargetSizeRules.MAX_KB} KB."
                ),
                isValid = false,
                wasClamped = false
            )

        val clamped = parsed.coerceIn(
            TargetSizeRules.MIN_KB.toLong(),
            TargetSizeRules.MAX_KB.toLong()
        ).toInt()
        val wasClamped = parsed != clamped.toLong()
        return TargetSizeCommit(
            state = copy(
                text = clamped.toString(),
                sliderPosition = TargetSizeRules.kbToSliderPosition(clamped.toFloat()),
                committedKilobytes = clamped,
                validationMessage = if (wasClamped) {
                    "Adjusted to the supported range: $clamped KB."
                } else {
                    null
                }
            ),
            isValid = true,
            wasClamped = wasClamped
        )
    }
}

data class TargetSizeCommit(
    val state: TargetSizeInputState,
    val isValid: Boolean,
    val wasClamped: Boolean
)

data class CompressionSettingsState(
    val mode: CompressionMode = CompressionMode.TARGET_SIZE,
    val qualitySliderValue: Float = 80f,
    val targetSize: TargetSizeInputState = TargetSizeInputState()
) {
    val confirmationLabel: String
        get() = when (mode) {
            CompressionMode.QUALITY -> "Quality: ${qualitySliderValue.roundToInt()}%"
            CompressionMode.TARGET_SIZE -> targetSize.previewKilobytes?.let {
                "Target Size: $it KB"
            } ?: "Target Size: enter ${TargetSizeRules.MIN_KB}-${TargetSizeRules.MAX_KB} KB"
        }

    fun withQuality(value: Float): CompressionSettingsState = copy(
        qualitySliderValue = value.coerceIn(
            QUALITY_MIN_PERCENT.toFloat(),
            QUALITY_MAX_PERCENT.toFloat()
        )
    )

    fun validateForProcessing(): SettingsValidation = when (mode) {
        CompressionMode.QUALITY -> SettingsValidation.Valid(
            state = this,
            snapshot = CompressionSettingsSnapshot.Quality(
                qualitySliderValue.roundToInt().coerceIn(
                    QUALITY_MIN_PERCENT,
                    QUALITY_MAX_PERCENT
                )
            ),
            wasClamped = false
        )

        CompressionMode.TARGET_SIZE -> {
            val commit = targetSize.commit()
            val committedState = copy(targetSize = commit.state)
            if (commit.isValid) {
                SettingsValidation.Valid(
                    state = committedState,
                    snapshot = CompressionSettingsSnapshot.TargetSize(
                        requireNotNull(commit.state.committedKilobytes)
                    ),
                    wasClamped = commit.wasClamped
                )
            } else {
                SettingsValidation.Invalid(
                    state = committedState,
                    message = requireNotNull(commit.state.validationMessage)
                )
            }
        }
    }
}

sealed interface SettingsValidation {
    val state: CompressionSettingsState

    data class Valid(
        override val state: CompressionSettingsState,
        val snapshot: CompressionSettingsSnapshot,
        val wasClamped: Boolean
    ) : SettingsValidation

    data class Invalid(
        override val state: CompressionSettingsState,
        val message: String
    ) : SettingsValidation
}
