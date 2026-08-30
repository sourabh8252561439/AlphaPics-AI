package com.example.compression

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.example.imaging.ImageCompressionEngine
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

data class CompressionArtifact(
    val originalSizeBytes: Long,
    val finalSizeBytes: Long,
    val bytesSaved: Long,
    val percentSaved: Int,
    val originalWidth: Int,
    val originalHeight: Int,
    val finalWidth: Int,
    val finalHeight: Int,
    val inputFormat: String,
    val outputFormat: String,
    val mimeType: String,
    val outputUri: Uri,
    val qualityUsed: Int,
    val hasTransparency: Boolean,
    val settings: CompressionSettingsSnapshot,
    val targetReached: Boolean?
)

sealed interface ImageCompressionOutcome {
    val originalSizeBytes: Long
    val settings: CompressionSettingsSnapshot
    val effectiveFinalSizeBytes: Long

    data class Compressed(val artifact: CompressionArtifact) : ImageCompressionOutcome {
        override val originalSizeBytes: Long = artifact.originalSizeBytes
        override val settings: CompressionSettingsSnapshot = artifact.settings
        override val effectiveFinalSizeBytes: Long = artifact.finalSizeBytes
    }

    data class TargetNotReached(
        val artifact: CompressionArtifact,
        val reason: String
    ) : ImageCompressionOutcome {
        override val originalSizeBytes: Long = artifact.originalSizeBytes
        override val settings: CompressionSettingsSnapshot = artifact.settings
        override val effectiveFinalSizeBytes: Long = artifact.finalSizeBytes
    }

    data class Skipped(
        override val originalSizeBytes: Long,
        override val settings: CompressionSettingsSnapshot,
        val reason: String
    ) : ImageCompressionOutcome {
        override val effectiveFinalSizeBytes: Long = originalSizeBytes.coerceAtLeast(0L)
    }

    data class Failed(
        override val originalSizeBytes: Long,
        override val settings: CompressionSettingsSnapshot,
        val reason: String
    ) : ImageCompressionOutcome {
        override val effectiveFinalSizeBytes: Long = originalSizeBytes.coerceAtLeast(0L)
    }
}

data class ImageSourceInfo(
    val displayName: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val inputFormat: String
)

object ImageSourceInspector {
    fun inspect(context: Context, uri: Uri): ImageSourceInfo? {
        val displayName = readDisplayName(context, uri)
        val sizeBytes = readByteSize(context, uri) ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
        } catch (_: Throwable) {
            return null
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        return ImageSourceInfo(
            displayName = displayName ?: uri.lastPathSegment ?: "image",
            sizeBytes = sizeBytes,
            width = bounds.outWidth,
            height = bounds.outHeight,
            inputFormat = resolveInputFormat(
                context = context,
                uri = uri,
                displayName = displayName,
                decodedMimeType = bounds.outMimeType
            )
        )
    }

    fun readByteSize(context: Context, uri: Uri): Long? {
        try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                if (descriptor.length > 0L) return descriptor.length
            }
        } catch (_: Throwable) {
            // Fall through to metadata and stream counting.
        }

        try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0 && cursor.moveToFirst()) {
                    val value = cursor.getLong(index)
                    if (value > 0L) return value
                }
            }
        } catch (_: Throwable) {
            // Fall through to stream counting.
        }

        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    total += read
                }
                total.takeIf { it > 0L }
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun readDisplayName(context: Context, uri: Uri): String? = try {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    } catch (_: Throwable) {
        null
    }

    private fun resolveInputFormat(
        context: Context,
        uri: Uri,
        displayName: String?,
        decodedMimeType: String?
    ): String {
        val mimeType = decodedMimeType ?: context.contentResolver.getType(uri)
        val fromMime = mimeType?.let {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(it)
        }
        val fromName = displayName
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase()
            ?.ifBlank { null }
        return fromMime ?: fromName ?: "image"
    }
}

/**
 * Shared decode -> orient -> encode -> temporary validation -> gallery-save pipeline.
 * Neither UI path may bypass its benefit and readability validation.
 */
object ImageCompressionProcessor {
    suspend fun process(
        context: Context,
        uri: Uri,
        settings: CompressionSettingsSnapshot,
        knownSource: ImageSourceInfo? = null
    ): ImageCompressionOutcome {
        // Re-read metadata at processing time; the batch selection copy is only a fallback.
        val source = ImageSourceInspector.inspect(context, uri) ?: knownSource
            ?: return ImageCompressionOutcome.Failed(
                originalSizeBytes = 0L,
                settings = settings,
                reason = "The source image metadata or byte size could not be read."
            )

        if (settings is CompressionSettingsSnapshot.TargetSize &&
            originalAlreadyWithinTarget(source.sizeBytes, settings.targetBytes)
        ) {
            return ImageCompressionOutcome.Skipped(
                originalSizeBytes = source.sizeBytes,
                settings = settings,
                reason = "The original is already at or below ${settings.kilobytes} KB. No duplicate file was created."
            )
        }

        currentCoroutineContext().ensureActive()
        var bitmap: Bitmap? = null
        var candidateFile: File? = null
        try {
            bitmap = decodeOriginalDimensions(context, uri)
                ?: return ImageCompressionOutcome.Failed(
                    originalSizeBytes = source.sizeBytes,
                    settings = settings,
                    reason = "The image could not be decoded at its original dimensions."
                )

            currentCoroutineContext().ensureActive()
            bitmap = ImageCompressionEngine.applyExifOrientation(
                bitmap,
                ImageCompressionEngine.readExifOrientation(context, uri)
            )
            val hasTransparency = ImageCompressionEngine.hasRealTransparency(bitmap)

            currentCoroutineContext().ensureActive()
            val encoded = when (settings) {
                is CompressionSettingsSnapshot.Quality ->
                    ImageCompressionEngine.compressAtQuality(
                        bitmap = bitmap,
                        qualityPercent = settings.percentage,
                        hasTransparency = hasTransparency
                    )

                is CompressionSettingsSnapshot.TargetSize ->
                    if (hasTransparency) {
                        ImageCompressionEngine.compressToTargetSizePreservingTransparency(
                            bitmap,
                            settings.targetBytes
                        )
                    } else {
                        ImageCompressionEngine.compressToTargetSize(bitmap, settings.targetBytes)
                    }
            }

            if (encoded.bytes.isEmpty()) {
                return ImageCompressionOutcome.Failed(
                    originalSizeBytes = source.sizeBytes,
                    settings = settings,
                    reason = "The encoder produced no readable output."
                )
            }

            candidateFile = File.createTempFile(
                "compression_candidate_",
                ".${encoded.fileExtension}",
                context.cacheDir
            )
            candidateFile.outputStream().use { it.write(encoded.bytes) }

            currentCoroutineContext().ensureActive()
            when (val validation = validateCandidate(candidateFile, source.sizeBytes)) {
                is CandidateValidation.Invalid -> {
                    return ImageCompressionOutcome.Failed(
                        originalSizeBytes = source.sizeBytes,
                        settings = settings,
                        reason = validation.reason
                    )
                }

                is CandidateValidation.NoBenefit -> {
                    return ImageCompressionOutcome.Skipped(
                        originalSizeBytes = source.sizeBytes,
                        settings = settings,
                        reason = validation.reason
                    )
                }

                is CandidateValidation.Valid -> Unit
            }

            currentCoroutineContext().ensureActive()
            val savedUri = ValidatedGallerySaver.save(
                context = context,
                candidate = candidateFile,
                mimeType = encoded.mimeType,
                fileExtension = encoded.fileExtension
            ) ?: return ImageCompressionOutcome.Failed(
                originalSizeBytes = source.sizeBytes,
                settings = settings,
                reason = "The validated output could not be saved to the gallery."
            )

            val finalSize = candidateFile.length()
            val bytesSaved = (source.sizeBytes - finalSize).coerceAtLeast(0L)
            val percentSaved = if (source.sizeBytes > 0L) {
                ((bytesSaved.toDouble() / source.sizeBytes.toDouble()) * 100.0)
                    .toInt()
                    .coerceIn(0, 100)
            } else {
                0
            }
            val artifact = CompressionArtifact(
                originalSizeBytes = source.sizeBytes,
                finalSizeBytes = finalSize,
                bytesSaved = bytesSaved,
                percentSaved = percentSaved,
                originalWidth = source.width,
                originalHeight = source.height,
                finalWidth = encoded.finalWidth,
                finalHeight = encoded.finalHeight,
                inputFormat = source.inputFormat,
                outputFormat = encoded.fileExtension,
                mimeType = encoded.mimeType,
                outputUri = savedUri,
                qualityUsed = encoded.qualityUsed,
                hasTransparency = hasTransparency,
                settings = settings,
                targetReached = encoded.targetReached
            )

            return if (settings is CompressionSettingsSnapshot.TargetSize &&
                encoded.targetReached != true
            ) {
                ImageCompressionOutcome.TargetNotReached(
                    artifact = artifact,
                    reason = "A smaller file was saved, but ${settings.kilobytes} KB could not be reached without resizing dimensions."
                )
            } else {
                ImageCompressionOutcome.Compressed(artifact)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (outOfMemory: OutOfMemoryError) {
            return ImageCompressionOutcome.Failed(
                originalSizeBytes = source.sizeBytes,
                settings = settings,
                reason = "This image is too large to process at full dimensions on this device."
            )
        } catch (error: Throwable) {
            return ImageCompressionOutcome.Failed(
                originalSizeBytes = source.sizeBytes,
                settings = settings,
                reason = error.message?.takeIf { it.isNotBlank() } ?: "Compression failed."
            )
        } finally {
            try {
                bitmap?.recycle()
            } catch (_: Throwable) {
                // Bitmap may already have been recycled while applying orientation.
            }
            candidateFile?.delete()
        }
    }

    private fun decodeOriginalDimensions(context: Context, uri: Uri): Bitmap? = try {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    } catch (_: Throwable) {
        null
    }

    private fun validateCandidate(candidate: File, originalBytes: Long): CandidateValidation {
        if (!candidate.exists() || candidate.length() <= 0L) {
            return CandidateValidation.Invalid("Compression produced an empty temporary file.")
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(candidate.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return CandidateValidation.Invalid("The encoded output could not be decoded safely.")
        }

        return when (
            val benefit = CompressionBenefitPolicy.evaluate(
                originalBytes = originalBytes,
                candidateBytes = candidate.length()
            )
        ) {
            is BenefitDecision.Accepted -> CandidateValidation.Valid
            is BenefitDecision.Rejected -> CandidateValidation.NoBenefit(benefit.reason)
        }
    }
}

private sealed interface CandidateValidation {
    data object Valid : CandidateValidation
    data class NoBenefit(val reason: String) : CandidateValidation
    data class Invalid(val reason: String) : CandidateValidation
}

/** Saves only a previously validated temporary candidate and removes partial MediaStore rows. */
private object ValidatedGallerySaver {
    fun save(
        context: Context,
        candidate: File,
        mimeType: String,
        fileExtension: String
    ): Uri? {
        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val values = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "compressed_img_${System.currentTimeMillis()}.$fileExtension"
            )
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        var createdUri: Uri? = null
        return try {
            createdUri = resolver.insert(collection, values) ?: return null
            val output = resolver.openOutputStream(createdUri)
                ?: throw IllegalStateException("Gallery output stream was unavailable.")
            output.use { destination ->
                candidate.inputStream().use { source -> source.copyTo(destination) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val publishValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                if (resolver.update(createdUri, publishValues, null, null) <= 0) {
                    throw IllegalStateException("Gallery output could not be published.")
                }
            }
            createdUri
        } catch (_: Throwable) {
            createdUri?.let { resolver.delete(it, null, null) }
            null
        }
    }
}
