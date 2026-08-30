package com.example.collage

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.example.editor.ExportFormat
import com.example.editor.ExportResult
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

/** Original-source high-resolution collage decoding, rendering, and scoped-storage export. */
object CollageExportManager {

    suspend fun export(
        context: Context,
        sourceUris: List<Uri>,
        state: CollageState,
        backgroundImageUri: Uri? = null,
        format: ExportFormat = ExportFormat.JPEG,
        quality: Int = 94,
        onProgress: (Float) -> Unit = {}
    ): Result<ExportResult> = withContext(Dispatchers.IO) {
        val decodedSources = mutableListOf<Bitmap>()
        var decodedBackground: Bitmap? = null
        var rendered: Bitmap? = null
        try {
            require(sourceUris.size in 2..MAX_COLLAGE_PHOTOS) {
                "Choose between 2 and $MAX_COLLAGE_PHOTOS photos"
            }
            val safeState = state.ensurePhotoCount(sourceUris.size)
            val preset = CollageLayoutCatalog.find(safeState.layoutId)
            val usedCount = if (preset.isFreestyle) sourceUris.size else preset.requiredPhotos
            require(sourceUris.size >= usedCount) { "${preset.label} needs $usedCount photos" }
            val (outputWidth, outputHeight) = safeState.outputDimensions()
            val slots = CollageLayoutCatalog.slots(
                safeState.layoutId,
                usedCount,
                safeState.freestyleRects
            )

            sourceUris.take(usedCount).forEachIndexed { index, uri ->
                coroutineContext.ensureActive()
                val slot = slots[index]
                val zoom = safeState.photoTransforms[index].zoom.coerceIn(1f, 4f)
                val targetWidth = (outputWidth * slot.width * zoom).toInt().coerceAtLeast(256)
                val targetHeight = (outputHeight * slot.height * zoom).toInt().coerceAtLeast(256)
                decodedSources += decodeOriented(context, uri, targetWidth, targetHeight)
                onProgress(0.04f + (index + 1f) / usedCount * 0.31f)
            }

            if (safeState.background.mode == CollageBackgroundMode.IMAGE && backgroundImageUri != null) {
                coroutineContext.ensureActive()
                decodedBackground = decodeOriented(context, backgroundImageUri, outputWidth, outputHeight)
            }

            rendered = CollageEngine.render(
                sources = decodedSources,
                state = safeState,
                outputWidth = outputWidth,
                outputHeight = outputHeight,
                backgroundImage = decodedBackground,
                checkpoint = { coroutineContext.ensureActive() },
                onProgress = { progress -> onProgress(0.38f + progress * 0.47f) }
            )

            decodedSources.forEach(Bitmap::recycle)
            decodedSources.clear()
            decodedBackground?.recycle()
            decodedBackground = null
            coroutineContext.ensureActive()

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "AlphaPics_Collage_$timeStamp.${format.extension}"
            val (uri, bytes) = saveToMediaStore(
                context = context,
                bitmap = rendered,
                format = format,
                quality = quality,
                fileName = fileName
            )
            rendered.recycle()
            rendered = null
            onProgress(1f)
            Result.success(
                ExportResult(
                    uri = uri,
                    width = outputWidth,
                    height = outputHeight,
                    sizeBytes = bytes,
                    mimeType = format.mimeType,
                    filename = fileName
                )
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (outOfMemory: OutOfMemoryError) {
            Result.failure(
                IllegalStateException(
                    "This collage is too large to render safely on this device. Choose a smaller export size.",
                    outOfMemory
                )
            )
        } catch (error: Exception) {
            Result.failure(error)
        } finally {
            decodedSources.filterNot { it.isRecycled }.forEach { it.recycle() }
            decodedBackground?.takeUnless { it.isRecycled }?.recycle()
            rendered?.takeUnless { it.isRecycled }?.recycle()
        }
    }

    private fun decodeOriented(
        context: Context,
        uri: Uri,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot read a collage photo")
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalStateException("A selected photo has invalid dimensions")
        }

        val orientation = try {
            resolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val swapsAxes = orientation in setOf(
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_ROTATE_270,
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_TRANSVERSE
        )
        val orientedWidth = if (swapsAxes) bounds.outHeight else bounds.outWidth
        val orientedHeight = if (swapsAxes) bounds.outWidth else bounds.outHeight
        var sample = 1
        while (
            orientedWidth / (sample * 2) >= targetWidth &&
            orientedHeight / (sample * 2) >= targetHeight
        ) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: throw IllegalStateException("Cannot decode a collage photo")
        return normalizeOrientation(decoded, orientation)
    }

    private fun normalizeOrientation(source: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
        }
        if (matrix.isIdentity) return source
        val normalized = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (normalized !== source) source.recycle()
        return normalized
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
            val itemUri = resolver.insert(collection, values)
                ?: throw IllegalStateException("Unable to create MediaStore entry")
            return try {
                val encoded = prepareForFormat(bitmap, format)
                try {
                    resolver.openOutputStream(itemUri)?.use { output ->
                        check(encoded.compress(compressFormat, quality.coerceIn(1, 100), output)) {
                            "Collage encoder failed"
                        }
                        output.flush()
                    } ?: throw IllegalStateException("Unable to open collage output")
                } finally {
                    if (encoded !== bitmap) encoded.recycle()
                }
                val bytes = resolver.openAssetFileDescriptor(itemUri, "r")?.use { it.length } ?: 0L
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
                itemUri to bytes
            } catch (error: Throwable) {
                resolver.delete(itemUri, null, null)
                throw error
            }
        }

        val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val directory = File(pictures, "AlphaPics AI").apply { if (!exists()) mkdirs() }
        val file = File(directory, fileName)
        val encoded = prepareForFormat(bitmap, format)
        try {
            FileOutputStream(file).use { output ->
                check(encoded.compress(compressFormat, quality.coerceIn(1, 100), output)) {
                    "Collage encoder failed"
                }
                output.flush()
            }
        } catch (error: Throwable) {
            file.delete()
            throw error
        } finally {
            if (encoded !== bitmap) encoded.recycle()
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
