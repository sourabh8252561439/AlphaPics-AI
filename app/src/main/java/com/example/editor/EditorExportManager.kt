package com.example.editor

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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExportFormat(val extension: String, val mimeType: String) {
    JPEG("jpg", "image/jpeg"),
    PNG("png", "image/png"),
    WEBP("webp", "image/webp")
}

data class ExportResult(
    val uri: Uri,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val mimeType: String,
    val filename: String
)

object EditorExportManager {

    suspend fun exportImage(
        context: Context,
        sourceUri: Uri,
        state: EditorState,
        format: ExportFormat = ExportFormat.JPEG,
        quality: Int = 92,
        onProgress: (Float) -> Unit = {}
    ): Result<ExportResult> = withContext(Dispatchers.IO) {
        try {
            onProgress(0.1f)
            val resolver = context.contentResolver

            // 1. Decode bounds and orientation
            val exifOrientation = getExifOrientation(context, sourceUri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            val boundsStream = resolver.openInputStream(sourceUri)
                ?: return@withContext Result.failure(IllegalStateException("Cannot read source photo"))
            boundsStream.use { BitmapFactory.decodeStream(it, null, options) }

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) {
                return@withContext Result.failure(IllegalStateException("Invalid image dimensions"))
            }

            onProgress(0.3f)

            // 2. Decode the original dimensions. Export never silently downsamples source media.
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = 1
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inMutable = true
            }
            val decodedBitmap = resolver.openInputStream(sourceUri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            } ?: return@withContext Result.failure(IllegalStateException("Failed to decode photo"))

            onProgress(0.5f)

            // 3. Normalize EXIF orientation. User geometry is rendered by the shared engine below.
            val matrix = Matrix()
            applyExifOrientation(matrix, exifOrientation)

            val orientedBitmap = if (!matrix.isIdentity) {
                val transformed = Bitmap.createBitmap(
                    decodedBitmap,
                    0,
                    0,
                    decodedBitmap.width,
                    decodedBitmap.height,
                    matrix,
                    true
                )
                if (transformed != decodedBitmap) {
                    decodedBitmap.recycle()
                }
                transformed
            } else {
                decodedBitmap
            }

            onProgress(0.65f)

            // 4. Apply the same crop/straighten/perspective/lens renderer used by preview.
            val geometryBitmap = EditorGeometryEngine.render(
                source = orientedBitmap,
                transform = state.transform,
                onProgress = { geometryProgress ->
                    onProgress(0.55f + geometryProgress * 0.18f)
                }
            )
            orientedBitmap.recycle()

            onProgress(0.75f)

            // 5. Apply the same nonlinear pixel renderer used by the interactive preview.
            val isHardwareBitmap = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                geometryBitmap.config == Bitmap.Config.HARDWARE
            val finalBitmap = if (geometryBitmap.isMutable && !isHardwareBitmap) {
                geometryBitmap
            } else {
                geometryBitmap.copy(Bitmap.Config.ARGB_8888, true).also { geometryBitmap.recycle() }
            }
            EditorBitmapRenderer.applyAllInPlace(
                bitmap = finalBitmap,
                state = state,
                onProgress = { renderProgress ->
                    onProgress(0.75f + renderProgress * 0.10f)
                }
            )
            EditorRetouchEngine.applyInPlace(
                bitmap = finalBitmap,
                retouch = state.retouch,
                onProgress = { retouchProgress ->
                    onProgress(0.85f + retouchProgress * 0.08f)
                }
            )
            EditorOverlayEngine.applyInPlace(
                bitmap = finalBitmap,
                overlays = state.overlays,
                onProgress = { overlayProgress ->
                    onProgress(0.93f + overlayProgress * 0.06f)
                }
            )

            val finalWidth = finalBitmap.width
            val finalHeight = finalBitmap.height

            // 6. Save to MediaStore (Pictures/AlphaPics AI)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "AlphaPics_$timeStamp.${format.extension}"

            val savedOutput = try {
                saveToMediaStore(
                    context = context,
                    bitmap = finalBitmap,
                    format = format,
                    quality = quality,
                    fileName = fileName
                )
            } finally {
                finalBitmap.recycle()
            }
            val (savedUri, sizeBytes) = savedOutput

            onProgress(1.0f)

            Result.success(
                ExportResult(
                    uri = savedUri,
                    width = finalWidth,
                    height = finalHeight,
                    sizeBytes = sizeBytes,
                    mimeType = format.mimeType,
                    filename = fileName
                )
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (outOfMemory: OutOfMemoryError) {
            Result.failure(
                IllegalStateException(
                    "This photo is too large to render safely on this device at original resolution.",
                    outOfMemory
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getExifOrientation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun applyExifOrientation(matrix: Matrix, orientation: Int) {
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
            ExportFormat.WEBP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/AlphaPics AI")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = resolver.insert(collection, contentValues)
                ?: throw IllegalStateException("Unable to create MediaStore entry")

            return try {
                val encodedBitmap = prepareForFormat(bitmap, format)
                try {
                    resolver.openOutputStream(itemUri)?.use { out ->
                        check(encodedBitmap.compress(compressFormat, quality.coerceIn(1, 100), out)) {
                            "Image encoder failed"
                        }
                        out.flush()
                    } ?: throw IllegalStateException("Unable to open MediaStore output")
                } finally {
                    if (encodedBitmap !== bitmap) encodedBitmap.recycle()
                }

                val writtenBytes = resolver.openAssetFileDescriptor(itemUri, "r")?.use { afd ->
                    afd.length
                } ?: 0L

                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)
                Pair(itemUri, writtenBytes)
            } catch (error: Throwable) {
                resolver.delete(itemUri, null, null)
                throw error
            }
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "AlphaPics AI").apply { if (!exists()) mkdirs() }
            val file = File(appDir, fileName)

            val encodedBitmap = prepareForFormat(bitmap, format)
            try {
                FileOutputStream(file).use { out ->
                    check(encodedBitmap.compress(compressFormat, quality.coerceIn(1, 100), out)) {
                        "Image encoder failed"
                    }
                    out.flush()
                }
            } catch (error: Throwable) {
                file.delete()
                throw error
            } finally {
                if (encodedBitmap !== bitmap) encodedBitmap.recycle()
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
                put(MediaStore.Images.Media.DATA, file.absolutePath)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: Uri.fromFile(file)

            return Pair(uri, file.length())
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
}
