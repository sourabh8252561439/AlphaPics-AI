package com.example.enhance

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EnhancementEngine {

    private const val MAX_PROCESS_DIMENSION = 4096

    suspend fun processPhoto(
        context: Context,
        sourceUri: Uri,
        modeId: String,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): EnhancementResult = withContext(Dispatchers.IO) {
        val startTime = SystemClock.elapsedRealtime()
        val mode = EnhancementCatalog.find(modeId)

        if (!mode.isLocalAvailable) {
            return@withContext EnhancementResult.ProviderNotConfigured(
                providerName = "AlphaPics Cloud AI Engine",
                modeId = modeId
            )
        }

        try {
            onProgress(0.15f, "Reading photo...")

            val resolver = context.contentResolver
            val exifOrientation = getExifOrientation(context, sourceUri)

            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            val boundsStream = resolver.openInputStream(sourceUri)
                ?: return@withContext EnhancementResult.Error("Could not read original photo")
            boundsStream.use { BitmapFactory.decodeStream(it, null, boundsOptions) }

            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) {
                return@withContext EnhancementResult.Error("Invalid photo dimensions")
            }

            var sampleSize = 1
            while ((origWidth / sampleSize) > MAX_PROCESS_DIMENSION || (origHeight / sampleSize) > MAX_PROCESS_DIMENSION) {
                sampleSize *= 2
            }

            onProgress(0.35f, "Decoding and normalizing...")

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inMutable = true
            }
            val decodedBitmap = resolver.openInputStream(sourceUri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            } ?: return@withContext EnhancementResult.Error("Failed to decode photo")

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

            onProgress(0.60f, "Applying ${mode.label} enhancement...")

            val enhancedBitmap = LocalAutoEnhancer.enhance(orientedBitmap, modeId)
            if (enhancedBitmap != orientedBitmap) {
                orientedBitmap.recycle()
            }

            onProgress(0.85f, "Finalizing result...")

            // Save to internal cache for instant Before/After viewing
            val cacheDir = File(context.cacheDir, "enhancement_results").apply { if (!exists()) mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val tempFile = File(cacheDir, "enhanced_${modeId}_$timeStamp.jpg")

            FileOutputStream(tempFile).use { out ->
                enhancedBitmap.compress(Bitmap.CompressFormat.JPEG, 94, out)
                out.flush()
            }

            val resultUri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )
            } catch (_: Exception) {
                Uri.fromFile(tempFile)
            }

            val elapsed = SystemClock.elapsedRealtime() - startTime
            onProgress(1.0f, "Completed")

            val finalWidth = enhancedBitmap.width
            val finalHeight = enhancedBitmap.height
            val finalSize = tempFile.length()
            enhancedBitmap.recycle()

            EnhancementResult.Success(
                outputUri = resultUri,
                width = finalWidth,
                height = finalHeight,
                sizeBytes = finalSize,
                processingTimeMs = elapsed,
                modeId = modeId
            )
        } catch (e: Exception) {
            EnhancementResult.Error("Enhancement failed: ${e.localizedMessage ?: "Unexpected error"}")
        }
    }

    suspend fun saveResultToGallery(
        context: Context,
        resultUri: Uri,
        modeId: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "AlphaPics_Enhanced_${modeId.uppercase()}_$timeStamp.jpg"

            val bitmap = resolver.openInputStream(resultUri)?.use {
                BitmapFactory.decodeStream(it)
            } ?: return@withContext Result.failure(IllegalStateException("Cannot open enhanced photo"))

            val savedUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/AlphaPics AI")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = resolver.insert(collection, values)
                    ?: return@withContext Result.failure(IllegalStateException("Failed to insert MediaStore record"))

                resolver.openOutputStream(itemUri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    out.flush()
                }

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
                itemUri
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(picturesDir, "AlphaPics AI").apply { if (!exists()) mkdirs() }
                val file = File(appDir, fileName)

                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    out.flush()
                }

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.DATA, file.absolutePath)
                }
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: Uri.fromFile(file)
            }

            bitmap.recycle()
            Result.success(savedUri)
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
}
