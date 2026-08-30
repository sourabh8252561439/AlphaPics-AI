package com.example.imaging

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.example.compression.EvaluatedQualityCandidate
import com.example.compression.TargetCandidateSelection
import com.example.compression.selectTargetCandidate
import java.io.ByteArrayOutputStream

/**
 * Format-aware encoder used by both single and batch processing. It never resizes dimensions:
 * target-size mode searches encoder quality only and reports when the target is unattainable.
 */
object ImageCompressionEngine {
    private const val TARGET_MIN_QUALITY = 5
    private const val TARGET_MAX_QUALITY = 95
    private const val MAX_TARGET_EVALUATIONS = 15

    data class CompressedResult(
        val bytes: ByteArray,
        val mimeType: String,
        val fileExtension: String,
        val finalWidth: Int,
        val finalHeight: Int,
        val qualityUsed: Int,
        val targetReached: Boolean?
    )

    /** Reads all EXIF orientation variants, including mirrored orientations. */
    fun readExifOrientation(context: Context, uri: Uri): Int = try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (_: Throwable) {
        ExifInterface.ORIENTATION_NORMAL
    }

    /** Applies the source EXIF transform and recycles the untransformed bitmap when needed. */
    fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        if (orientation == ExifInterface.ORIENTATION_NORMAL ||
            orientation == ExifInterface.ORIENTATION_UNDEFINED
        ) {
            return bitmap
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }

        val transformed = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
        if (transformed !== bitmap) bitmap.recycle()
        return transformed
    }

    /**
     * Checks every pixel alpha value using one bounded row buffer. Sampling can miss small
     * transparent regions, while a full-bitmap pixel array would unnecessarily double memory.
     */
    fun hasRealTransparency(bitmap: Bitmap): Boolean {
        if (!bitmap.hasAlpha() || bitmap.width <= 0 || bitmap.height <= 0) return false
        val row = IntArray(bitmap.width)
        for (y in 0 until bitmap.height) {
            bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
            for (pixel in row) {
                if ((pixel ushr 24) != 0xFF) return true
            }
        }
        return false
    }

    /** WebP quality control with alpha support. */
    @Suppress("DEPRECATION")
    fun lossyWebpFormatForDevice(): Bitmap.CompressFormat =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }

    fun mimeTypeFor(format: Bitmap.CompressFormat): String = when (format) {
        Bitmap.CompressFormat.PNG -> "image/png"
        else -> if (format.name.startsWith("WEBP")) "image/webp" else "image/jpeg"
    }

    fun fileExtensionFor(format: Bitmap.CompressFormat): String = when (format) {
        Bitmap.CompressFormat.PNG -> "png"
        else -> if (format.name.startsWith("WEBP")) "webp" else "jpg"
    }

    private fun encode(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int): ByteArray {
        val stream = ByteArrayOutputStream()
        val encoded = bitmap.compress(format, quality.coerceIn(0, 100), stream)
        return if (encoded) stream.toByteArray() else byteArrayOf()
    }

    /** Quality mode performs exactly one encode at the snapshotted user percentage. */
    fun compressAtQuality(
        bitmap: Bitmap,
        qualityPercent: Int,
        hasTransparency: Boolean
    ): CompressedResult {
        val selectedQuality = qualityPercent.coerceIn(1, 100)
        val format = if (hasTransparency) {
            lossyWebpFormatForDevice()
        } else {
            Bitmap.CompressFormat.JPEG
        }
        return CompressedResult(
            bytes = encode(bitmap, format, selectedQuality),
            mimeType = mimeTypeFor(format),
            fileExtension = fileExtensionFor(format),
            finalWidth = bitmap.width,
            finalHeight = bitmap.height,
            qualityUsed = selectedQuality,
            targetReached = null
        )
    }

    fun compressToTargetSize(bitmap: Bitmap, targetBytes: Long): CompressedResult =
        compressToTarget(bitmap, Bitmap.CompressFormat.JPEG, targetBytes)

    /**
     * A transparent source first tries lossless PNG. If that is over target, lossy WebP is used
     * because it respects encoder quality while retaining alpha. PNG quality percentages are
     * never presented as meaningful because Android's PNG encoder ignores them.
     */
    fun compressToTargetSizePreservingTransparency(
        bitmap: Bitmap,
        targetBytes: Long
    ): CompressedResult {
        require(targetBytes > 0L) { "Target size must be positive." }
        val png = encode(bitmap, Bitmap.CompressFormat.PNG, 100)
        if (png.isNotEmpty() && png.size.toLong() <= targetBytes) {
            return CompressedResult(
                bytes = png,
                mimeType = "image/png",
                fileExtension = "png",
                finalWidth = bitmap.width,
                finalHeight = bitmap.height,
                qualityUsed = 100,
                targetReached = true
            )
        }
        return compressToTarget(bitmap, lossyWebpFormatForDevice(), targetBytes)
    }

    /**
     * Evaluates a broad quality grid, then adaptively refines above the best qualifying point.
     * All evaluated sizes are retained and final selection is made across the whole set, so a
     * local non-monotonic encoder result cannot overwrite a better evaluated candidate. The
     * search plus final encode is deterministically bounded to 16 encodes.
     */
    private fun compressToTarget(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        targetBytes: Long
    ): CompressedResult {
        require(targetBytes > 0L) { "Target size must be positive." }

        val evaluatedSizes = linkedMapOf<Int, Long>()
        fun evaluate(quality: Int) {
            if (evaluatedSizes.size >= MAX_TARGET_EVALUATIONS || quality in evaluatedSizes) return
            evaluatedSizes[quality] = encode(bitmap, format, quality).size.toLong()
        }

        listOf(5, 15, 25, 35, 45, 55, 65, 75, 85, 95).forEach(::evaluate)

        var selection = selectTargetCandidate(
            evaluatedSizes.map { (quality, size) ->
                EvaluatedQualityCandidate(quality, size, quality)
            },
            targetBytes
        )

        // If no coarse point qualifies, spend the remaining bounded evaluations between
        // anchors. This catches useful local size dips from encoders that are not monotonic.
        if (selection !is TargetCandidateSelection.Reached) {
            listOf(10, 30, 50, 70, 90).forEach(::evaluate)
            selection = selectTargetCandidate(
                evaluatedSizes.map { (quality, size) ->
                    EvaluatedQualityCandidate(quality, size, quality)
                },
                targetBytes
            )
        }

        if (selection is TargetCandidateSelection.Reached) {
            var lower = selection.candidate.quality
            var upper = evaluatedSizes.keys.filter { it > lower }.minOrNull()
                ?: (TARGET_MAX_QUALITY + 1)
            while (upper - lower > 1 && evaluatedSizes.size < MAX_TARGET_EVALUATIONS) {
                val midpoint = (lower + upper) / 2
                evaluate(midpoint)
                val midpointBytes = evaluatedSizes.getValue(midpoint)
                if (midpointBytes in 1..targetBytes) lower = midpoint else upper = midpoint
            }
            selection = selectTargetCandidate(
                evaluatedSizes.map { (quality, size) ->
                    EvaluatedQualityCandidate(quality, size, quality)
                },
                targetBytes
            )
        }

        val selectedQuality = when (selection) {
            is TargetCandidateSelection.Reached -> selection.candidate.quality
            is TargetCandidateSelection.NotReached -> selection.candidate.quality
            TargetCandidateSelection.None -> TARGET_MIN_QUALITY
        }.coerceIn(TARGET_MIN_QUALITY, TARGET_MAX_QUALITY)

        val selectedBytes = encode(bitmap, format, selectedQuality)
        return CompressedResult(
            bytes = selectedBytes,
            mimeType = mimeTypeFor(format),
            fileExtension = fileExtensionFor(format),
            finalWidth = bitmap.width,
            finalHeight = bitmap.height,
            qualityUsed = selectedQuality,
            targetReached = selectedBytes.isNotEmpty() && selectedBytes.size.toLong() <= targetBytes
        )
    }
}
