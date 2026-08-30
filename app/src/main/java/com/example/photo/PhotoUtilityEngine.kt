package com.example.photo

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.example.editor.ExportFormat
import com.example.editor.ExportResult
import com.example.imaging.ImageCompressionEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.coroutineContext

/** Original-source Resize and Format Convert pipeline with explicit metadata policy. */
object PhotoUtilityEngine {
    suspend fun export(
        context: Context,
        sourceUri: Uri,
        settings: PhotoUtilitySettings,
        onProgress: (Float) -> Unit = {}
    ): Result<ExportResult> = withContext(Dispatchers.IO) {
        var decoded: Bitmap? = null
        var oriented: Bitmap? = null
        var resized: Bitmap? = null
        try {
            require(settings.metadataPolicy != MetadataPolicy.PRESERVE_SAFE || settings.format == ExportFormat.JPEG) {
                "Safe metadata preservation is available for JPEG output only"
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val boundsStream = context.contentResolver.openInputStream(sourceUri)
                ?: error("Unable to read this photo")
            boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "This file is not a readable image" }

            val orientationValue = ImageCompressionEngine.readExifOrientation(context, sourceUri)
            val swapsAxes = orientationValue in setOf(
                ExifInterface.ORIENTATION_ROTATE_90,
                ExifInterface.ORIENTATION_ROTATE_270,
                ExifInterface.ORIENTATION_TRANSPOSE,
                ExifInterface.ORIENTATION_TRANSVERSE
            )
            val sourceWidth = if (swapsAxes) bounds.outHeight else bounds.outWidth
            val sourceHeight = if (swapsAxes) bounds.outWidth else bounds.outHeight
            val (targetWidth, targetHeight) = settings.resize.resolvedDimensions(sourceWidth, sourceHeight)
            val sample = decodeSample(sourceWidth, sourceHeight, targetWidth, targetHeight)
            coroutineContext.ensureActive()
            decoded = context.contentResolver.openInputStream(sourceUri)?.use {
                BitmapFactory.decodeStream(
                    it,
                    null,
                    BitmapFactory.Options().apply {
                        inSampleSize = sample
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                )
            } ?: error("Unable to decode this photo")
            onProgress(0.18f)

            oriented = ImageCompressionEngine.applyExifOrientation(decoded, orientationValue)
            if (oriented !== decoded) decoded = null
            coroutineContext.ensureActive()
            resized = PhotoResampler.resize(
                source = oriented,
                targetWidth = targetWidth,
                targetHeight = targetHeight,
                checkpoint = { coroutineContext.ensureActive() },
                onProgress = { onProgress(0.18f + it * 0.56f) }
            )
            oriented.recycle()
            oriented = null

            val safeExif = if (settings.metadataPolicy == MetadataPolicy.PRESERVE_SAFE) {
                readSafeExif(context, sourceUri)
            } else {
                emptyMap()
            }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "AlphaPics_${if (settings.resize.resolvedDimensions(sourceWidth, sourceHeight) == (sourceWidth to sourceHeight)) "Converted" else "Resized"}_$timestamp.${settings.format.extension}"
            val saved = saveToMediaStore(
                context = context,
                bitmap = resized,
                format = settings.format,
                quality = settings.quality,
                fileName = fileName,
                exif = safeExif,
                width = targetWidth,
                height = targetHeight
            )
            resized.recycle()
            resized = null
            onProgress(1f)
            Result.success(
                ExportResult(
                    uri = saved.first,
                    width = targetWidth,
                    height = targetHeight,
                    sizeBytes = saved.second,
                    mimeType = settings.format.mimeType,
                    filename = fileName
                )
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (outOfMemory: OutOfMemoryError) {
            Result.failure(
                IllegalStateException(
                    "This resize is too large for available device memory. Choose smaller dimensions.",
                    outOfMemory
                )
            )
        } catch (error: Exception) {
            Result.failure(error)
        } finally {
            decoded?.takeUnless { it.isRecycled }?.recycle()
            oriented?.takeUnless { it.isRecycled }?.recycle()
            resized?.takeUnless { it.isRecycled }?.recycle()
        }
    }

    private fun decodeSample(
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Int {
        var sample = 1
        while (
            sourceWidth / (sample * 2) >= targetWidth &&
            sourceHeight / (sample * 2) >= targetHeight
        ) sample *= 2
        return sample
    }

    private val safeExifTags = listOf(
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_LENS_MODEL,
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_EXPOSURE_TIME,
        ExifInterface.TAG_F_NUMBER,
        ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
        ExifInterface.TAG_FOCAL_LENGTH,
        ExifInterface.TAG_FLASH,
        ExifInterface.TAG_WHITE_BALANCE,
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_COPYRIGHT,
        ExifInterface.TAG_IMAGE_DESCRIPTION
    )

    private fun readSafeExif(context: Context, uri: Uri): Map<String, String> = try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            ExifInterface(input).let { exif ->
                buildMap {
                    safeExifTags.forEach { tag -> exif.getAttribute(tag)?.let { put(tag, it) } }
                }
            }
        } ?: emptyMap()
    } catch (_: Exception) {
        emptyMap()
    }

    private fun saveToMediaStore(
        context: Context,
        bitmap: Bitmap,
        format: ExportFormat,
        quality: Int,
        fileName: String,
        exif: Map<String, String>,
        width: Int,
        height: Int
    ): Pair<Uri, Long> {
        val resolver = context.contentResolver
        val compressFormat = when (format) {
            ExportFormat.JPEG -> Bitmap.CompressFormat.JPEG
            ExportFormat.PNG -> Bitmap.CompressFormat.PNG
            ExportFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/AlphaPics AI")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = resolver.insert(collection, values)
                ?: error("Unable to create MediaStore entry")
            return try {
                if (exif.isNotEmpty() && format == ExportFormat.JPEG) {
                    val staging = File.createTempFile("alphapics_metadata_", ".jpg", context.cacheDir)
                    try {
                        FileOutputStream(staging).use { output ->
                            encode(output, bitmap, format, compressFormat, quality)
                        }
                        writeSafeExif(staging, exif, width, height)
                        resolver.openOutputStream(uri)?.use { output ->
                            staging.inputStream().use { input -> input.copyTo(output) }
                            output.flush()
                        } ?: error("Unable to open photo output")
                    } finally {
                        staging.delete()
                    }
                } else {
                    encode(resolver.openOutputStream(uri), bitmap, format, compressFormat, quality)
                }
                val bytes = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri to bytes
            } catch (error: Throwable) {
                resolver.delete(uri, null, null)
                throw error
            }
        }

        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "AlphaPics AI"
        ).apply { if (!exists()) mkdirs() }
        val file = File(directory, fileName)
        try {
            FileOutputStream(file).use { output ->
                encode(output, bitmap, format, compressFormat, quality)
            }
            if (exif.isNotEmpty() && format == ExportFormat.JPEG) {
                writeSafeExif(file, exif, width, height)
            }
        } catch (error: Throwable) {
            file.delete()
            throw error
        }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
            put(MediaStore.Images.Media.DATA, file.absolutePath)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: Uri.fromFile(file)
        return uri to file.length()
    }

    private fun encode(
        output: java.io.OutputStream?,
        bitmap: Bitmap,
        format: ExportFormat,
        compressFormat: Bitmap.CompressFormat,
        quality: Int
    ) {
        output ?: error("Unable to open photo output")
        output.use { stream ->
            val encoded = prepareForFormat(bitmap, format)
            try {
                check(encoded.compress(compressFormat, quality.coerceIn(1, 100), stream)) {
                    "Photo encoder failed"
                }
                stream.flush()
            } finally {
                if (encoded !== bitmap) encoded.recycle()
            }
        }
    }

    private fun prepareForFormat(bitmap: Bitmap, format: ExportFormat): Bitmap {
        if (format != ExportFormat.JPEG || !bitmap.hasAlpha()) return bitmap
        return Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888).also { opaque ->
            Canvas(opaque).apply {
                drawColor(Color.WHITE)
                drawBitmap(bitmap, 0f, 0f, null)
            }
            opaque.setHasAlpha(false)
        }
    }

    private fun writeSafeExif(
        file: File,
        tags: Map<String, String>,
        width: Int,
        height: Int
    ) {
        ExifInterface(file.absolutePath).apply {
            tags.forEach(::setAttribute)
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            setAttribute(ExifInterface.TAG_IMAGE_WIDTH, width.toString())
            setAttribute(ExifInterface.TAG_IMAGE_LENGTH, height.toString())
            saveAttributes()
        }
    }
}
