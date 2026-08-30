package com.example.batchstudio

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.editor.EditorBitmapRenderer
import com.example.editor.EditorPreviewRenderer
import com.example.editor.EditorState
import com.example.editor.ExportFormat
import com.example.editor.FilterAdjustment
import com.example.editor.OverlayAdjustments
import com.example.editor.WatermarkAdjustment
import com.example.imaging.ImageCompressionEngine
import com.example.photo.PhotoResampler
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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object BatchStudioEngine {

    private const val PREVIEW_LONG_EDGE = 720

    suspend fun processItem(
        context: Context,
        sourceUri: Uri,
        sourceMimeType: String?,
        sourceName: String,
        settings: BatchStudioSettings,
        onProgress: (Float) -> Unit = {}
    ): Result<BatchStudioOutput> = withContext(Dispatchers.IO) {
        var decoded: Bitmap? = null
        var oriented: Bitmap? = null
        var working: Bitmap? = null
        try {
            coroutineContext.ensureActive()
            val orientation = ImageCompressionEngine.readExifOrientation(context, sourceUri)
            val rawDimensions = readBounds(context, sourceUri)
            val sourceDimensions = orientedDimensions(rawDimensions, orientation)
            val safeSettings = settings.sanitized()
            val contentDimensions = safeSettings.resolveContentDimensions(
                sourceDimensions.first,
                sourceDimensions.second
            )
            safeSettings.resolveOutputDimensions(contentDimensions.first, contentDimensions.second)

            val rawTarget = if (rotatesAxes(orientation)) {
                contentDimensions.second to contentDimensions.first
            } else {
                contentDimensions
            }
            val sample = decodeSample(
                rawDimensions.first,
                rawDimensions.second,
                rawTarget.first,
                rawTarget.second
            )
            decoded = decode(context, sourceUri, sample)
            onProgress(0.12f)
            coroutineContext.ensureActive()
            oriented = ImageCompressionEngine.applyExifOrientation(decoded, orientation)
            if (oriented !== decoded) decoded = null

            working = PhotoResampler.resize(
                source = oriented,
                targetWidth = contentDimensions.first,
                targetHeight = contentDimensions.second,
                checkpoint = { coroutineContext.ensureActive() },
                onProgress = { onProgress(0.12f + it * 0.35f) }
            )
            oriented.recycle()
            oriented = null
            working = ensureMutable(working)
            applyPreset(working, safeSettings) { progress -> onProgress(0.47f + progress * 0.15f) }

            if (safeSettings.paddingEnabled && safeSettings.paddingPercent > 0f) {
                val padded = addPadding(working, safeSettings)
                if (padded !== working) working.recycle()
                working = padded
            }
            onProgress(0.70f)
            applyWatermark(working, safeSettings) { coroutineContext.ensureActive() }
            onProgress(0.79f)
            applyLogo(context, working, safeSettings) { coroutineContext.ensureActive() }
            onProgress(0.87f)
            coroutineContext.ensureActive()

            val format = safeSettings.outputFormat.resolve(sourceMimeType)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
            val baseName = sourceName.substringBeforeLast('.').replace(Regex("[^A-Za-z0-9_-]"), "_")
                .take(38).ifBlank { "Photo" }
            val fileName = "AlphaPics_Batch_${baseName}_$timestamp.${format.extension}"
            val saved = saveToMediaStore(
                context = context,
                bitmap = working,
                format = format,
                quality = safeSettings.quality,
                fileName = fileName
            )
            val result = BatchStudioOutput(
                uri = saved.first,
                width = working.width,
                height = working.height,
                sizeBytes = saved.second,
                mimeType = format.mimeType,
                filename = fileName
            )
            working.recycle()
            working = null
            onProgress(1f)
            Result.success(result)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (outOfMemory: OutOfMemoryError) {
            Result.failure(
                IllegalStateException(
                    "This item is too large for available device memory. Choose smaller batch dimensions.",
                    outOfMemory
                )
            )
        } catch (error: Exception) {
            Result.failure(error)
        } finally {
            decoded?.takeUnless(Bitmap::isRecycled)?.recycle()
            oriented?.takeUnless(Bitmap::isRecycled)?.recycle()
            working?.takeUnless(Bitmap::isRecycled)?.recycle()
        }
    }

    suspend fun renderPreview(
        context: Context,
        sourceUri: Uri,
        settings: BatchStudioSettings
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        var source: Bitmap? = null
        var working: Bitmap? = null
        try {
            coroutineContext.ensureActive()
            source = EditorPreviewRenderer.loadSource(context, sourceUri).getOrThrow()
            val safe = settings.sanitized()
            val target = safe.resolveContentDimensions(source.width, source.height)
            val scale = min(1f, PREVIEW_LONG_EDGE.toFloat() / max(target.first, target.second))
            val previewWidth = (target.first * scale).roundToInt().coerceAtLeast(1)
            val previewHeight = (target.second * scale).roundToInt().coerceAtLeast(1)
            working = PhotoResampler.resize(
                source,
                previewWidth,
                previewHeight,
                checkpoint = { coroutineContext.ensureActive() }
            )
            source.recycle()
            source = null
            working = ensureMutable(working)
            applyPreset(working, safe)
            if (safe.paddingEnabled && safe.paddingPercent > 0f) {
                val padded = addPadding(working, safe, validateDimensions = false)
                if (padded !== working) working.recycle()
                working = padded
            }
            applyWatermark(working, safe) { coroutineContext.ensureActive() }
            applyLogo(context, working, safe) { coroutineContext.ensureActive() }
            Result.success(working).also { working = null }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        } finally {
            source?.takeUnless(Bitmap::isRecycled)?.recycle()
            working?.takeUnless(Bitmap::isRecycled)?.recycle()
        }
    }

    private fun readBounds(context: Context, uri: Uri): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val input = context.contentResolver.openInputStream(uri) ?: error("Unable to read this photo")
        input.use { BitmapFactory.decodeStream(it, null, options) }
        require(options.outWidth > 0 && options.outHeight > 0) { "This item is not a readable image" }
        return options.outWidth to options.outHeight
    }

    private fun decode(context: Context, uri: Uri, sample: Int): Bitmap =
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(
                input,
                null,
                BitmapFactory.Options().apply {
                    inSampleSize = sample.coerceAtLeast(1)
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inMutable = true
                }
            )
        } ?: error("Unable to decode this photo")

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

    private fun orientedDimensions(raw: Pair<Int, Int>, orientation: Int): Pair<Int, Int> =
        if (rotatesAxes(orientation)) raw.second to raw.first else raw

    private fun rotatesAxes(orientation: Int): Boolean = orientation in setOf(
        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90,
        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270,
        androidx.exifinterface.media.ExifInterface.ORIENTATION_TRANSPOSE,
        androidx.exifinterface.media.ExifInterface.ORIENTATION_TRANSVERSE
    )

    private fun ensureMutable(bitmap: Bitmap): Bitmap {
        if (bitmap.isMutable && bitmap.config != Bitmap.Config.HARDWARE) return bitmap
        return bitmap.copy(Bitmap.Config.ARGB_8888, true).also { bitmap.recycle() }
    }

    private fun applyPreset(
        bitmap: Bitmap,
        settings: BatchStudioSettings,
        onProgress: (Float) -> Unit = {}
    ) {
        if (!settings.presetEnabled) {
            onProgress(1f)
            return
        }
        EditorBitmapRenderer.applyAllInPlace(
            bitmap = bitmap,
            state = EditorState(
                filter = FilterAdjustment(
                    presetId = settings.presetId,
                    intensity = settings.presetIntensity
                )
            ),
            onProgress = onProgress
        )
    }

    private fun addPadding(
        bitmap: Bitmap,
        settings: BatchStudioSettings,
        validateDimensions: Boolean = true
    ): Bitmap {
        val fraction = settings.paddingPercent.coerceIn(0f, 50f) / 100f
        val extraWidth = (bitmap.width * fraction * 2f).roundToInt()
        val extraHeight = (bitmap.height * fraction * 2f).roundToInt()
        if (extraWidth == 0 && extraHeight == 0) return bitmap
        val outputWidth = bitmap.width + extraWidth
        val outputHeight = bitmap.height + extraHeight
        if (validateDimensions) validateBatchStudioDimensions(outputWidth, outputHeight)
        val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        output.setHasAlpha(Color.alpha(settings.paddingColorArgb.toInt()) < 255 || bitmap.hasAlpha())
        val canvas = Canvas(output)
        canvas.drawColor(settings.paddingColorArgb.toInt())
        val (left, top) = alignedOffset(
            settings.alignment,
            outputWidth - bitmap.width,
            outputHeight - bitmap.height
        )
        canvas.drawBitmap(bitmap, left.toFloat(), top.toFloat(), Paint(Paint.ANTI_ALIAS_FLAG))
        return output
    }

    private fun applyWatermark(
        bitmap: Bitmap,
        settings: BatchStudioSettings,
        checkpoint: () -> Unit
    ) {
        if (!settings.watermarkEnabled || settings.watermarkText.isBlank()) return
        com.example.editor.EditorOverlayEngine.applyInPlace(
            bitmap = bitmap,
            overlays = OverlayAdjustments(
                watermark = WatermarkAdjustment(
                    enabled = true,
                    text = settings.watermarkText,
                    anchor = settings.watermarkPlacement.asWatermarkAnchor(),
                    scale = settings.watermarkScale,
                    opacity = settings.watermarkOpacity,
                    padding = 3f
                )
            ),
            checkpoint = checkpoint
        )
    }

    private fun applyLogo(
        context: Context,
        bitmap: Bitmap,
        settings: BatchStudioSettings,
        checkpoint: () -> Unit
    ) {
        val logoUri = settings.logoUri
        if (!settings.logoEnabled || logoUri == null) return
        checkpoint()
        var logo = EditorPreviewRenderer.loadSourceBlocking(context, logoUri)
        try {
            val desiredLongEdge = (min(bitmap.width, bitmap.height) * settings.logoScale / 100f)
                .roundToInt().coerceAtLeast(1)
            val ratio = logo.width.toFloat() / logo.height
            val targetWidth: Int
            val targetHeight: Int
            if (logo.width >= logo.height) {
                targetWidth = desiredLongEdge
                targetHeight = (desiredLongEdge / ratio).roundToInt().coerceAtLeast(1)
            } else {
                targetHeight = desiredLongEdge
                targetWidth = (desiredLongEdge * ratio).roundToInt().coerceAtLeast(1)
            }
            val scaled = Bitmap.createScaledBitmap(logo, targetWidth, targetHeight, true)
            if (scaled !== logo) {
                logo.recycle()
                logo = scaled
            }
            val padding = (min(bitmap.width, bitmap.height) * 0.03f).roundToInt()
            val availableX = (bitmap.width - logo.width - padding * 2).coerceAtLeast(0)
            val availableY = (bitmap.height - logo.height - padding * 2).coerceAtLeast(0)
            val (offsetX, offsetY) = alignedOffset(settings.logoPlacement, availableX, availableY)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                alpha = (settings.logoOpacity.coerceIn(0f, 100f) * 2.55f).roundToInt()
            }
            Canvas(bitmap).drawBitmap(
                logo,
                (padding + offsetX).toFloat(),
                (padding + offsetY).toFloat(),
                paint
            )
        } finally {
            logo.takeUnless(Bitmap::isRecycled)?.recycle()
        }
    }

    private fun alignedOffset(
        placement: BatchPlacement,
        availableWidth: Int,
        availableHeight: Int
    ): Pair<Int, Int> = when (placement) {
        BatchPlacement.TOP_LEFT -> 0 to 0
        BatchPlacement.TOP_RIGHT -> availableWidth to 0
        BatchPlacement.CENTER -> availableWidth / 2 to availableHeight / 2
        BatchPlacement.BOTTOM_LEFT -> 0 to availableHeight
        BatchPlacement.BOTTOM_RIGHT -> availableWidth to availableHeight
    }

    private fun saveToMediaStore(
        context: Context,
        bitmap: Bitmap,
        format: ExportFormat,
        quality: Int,
        fileName: String
    ): Pair<Uri, Long> {
        val resolver = context.contentResolver
        val compressFormat = when (format) {
            ExportFormat.JPEG -> Bitmap.CompressFormat.JPEG
            ExportFormat.PNG -> Bitmap.CompressFormat.PNG
            ExportFormat.WEBP -> ImageCompressionEngine.lossyWebpFormatForDevice()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/AlphaPics AI")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = resolver.insert(collection, values) ?: error("Unable to create batch output")
            return try {
                resolver.openOutputStream(uri)?.use { output ->
                    val prepared = prepareForFormat(bitmap, format)
                    try {
                        check(prepared.compress(compressFormat, quality.coerceIn(40, 100), output)) {
                            "Batch image encoder failed"
                        }
                        output.flush()
                    } finally {
                        if (prepared !== bitmap) prepared.recycle()
                    }
                } ?: error("Unable to open batch output")
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
                val prepared = prepareForFormat(bitmap, format)
                try {
                    check(prepared.compress(compressFormat, quality.coerceIn(40, 100), output)) {
                        "Batch image encoder failed"
                    }
                    output.flush()
                } finally {
                    if (prepared !== bitmap) prepared.recycle()
                }
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
}

/** Blocking helper is private to background batch code and keeps EditorPreviewRenderer's URI rules shared. */
private fun EditorPreviewRenderer.loadSourceBlocking(context: Context, uri: Uri): Bitmap {
    val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
    return context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    } ?: error("Unable to decode logo")
}
