package com.example.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.min

/** Bridges the deterministic pixel engine to Android bitmaps without allocating a full pixel copy. */
object EditorBitmapRenderer {

    private const val SPATIAL_BLOCK_ROWS = 32

    fun renderToneColor(source: Bitmap, state: EditorState): Bitmap {
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
            ?: throw IllegalStateException("Unable to allocate editor preview")
        applyToneColorInPlace(output, state)
        return output
    }

    fun renderAll(source: Bitmap, state: EditorState): Bitmap {
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
            ?: throw IllegalStateException("Unable to allocate editor preview")
        applyAllInPlace(output, state)
        return output
    }

    fun applyAllInPlace(
        bitmap: Bitmap,
        state: EditorState,
        onProgress: (Float) -> Unit = {},
        checkpoint: () -> Unit = {}
    ) {
        val tonePlan = EditorPixelEngine.createPlan(state)
        val spatialPlan = EditorSpatialEngine.createPlan(state)
        if (tonePlan.isNeutral && spatialPlan.isNeutral) {
            onProgress(1f)
            return
        }

        if (!tonePlan.isNeutral) {
            applyToneColorInPlace(bitmap, state) { progress ->
                onProgress(progress * if (spatialPlan.isNeutral) 1f else 0.46f)
                checkpoint()
            }
        }
        if (!spatialPlan.isNeutral) {
            applySpatialInPlace(bitmap, state, checkpoint) { progress ->
                val start = if (tonePlan.isNeutral) 0f else 0.46f
                onProgress(start + progress * (1f - start))
            }
        }
        onProgress(1f)
    }

    fun applyToneColorInPlace(
        bitmap: Bitmap,
        state: EditorState,
        onProgress: (Float) -> Unit = {}
    ) {
        require(bitmap.isMutable) { "Editor bitmap must be mutable" }
        val plan = EditorPixelEngine.createPlan(state)
        if (plan.isNeutral) {
            onProgress(1f)
            return
        }

        val row = IntArray(bitmap.width)
        for (y in 0 until bitmap.height) {
            bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
            EditorPixelEngine.processPixelsInPlace(row, plan)
            bitmap.setPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
            if (y % 32 == 0 || y == bitmap.height - 1) {
                onProgress((y + 1f) / bitmap.height.toFloat())
            }
        }
    }

    fun applySpatialInPlace(
        bitmap: Bitmap,
        state: EditorState,
        checkpoint: () -> Unit = {},
        onProgress: (Float) -> Unit = {}
    ) {
        require(bitmap.isMutable) { "Editor bitmap must be mutable" }
        val plan = EditorSpatialEngine.createPlan(state)
        if (plan.isNeutral) {
            onProgress(1f)
            return
        }

        val source = bitmap.copy(Bitmap.Config.ARGB_8888, false)
            ?: throw IllegalStateException("Unable to allocate spatial render source")
        try {
            var outputStartY = 0
            while (outputStartY < bitmap.height) {
                checkpoint()
                val outputRows = min(SPATIAL_BLOCK_ROWS, bitmap.height - outputStartY)
                val sourceStartY = max(0, outputStartY - plan.requiredRadius)
                val sourceEndY = min(
                    bitmap.height,
                    outputStartY + outputRows + plan.requiredRadius
                )
                val sourceRows = sourceEndY - sourceStartY
                val sourceBlock = IntArray(bitmap.width * sourceRows)
                val outputBlock = IntArray(bitmap.width * outputRows)
                source.getPixels(
                    sourceBlock,
                    0,
                    bitmap.width,
                    0,
                    sourceStartY,
                    bitmap.width,
                    sourceRows
                )
                EditorSpatialEngine.processBlock(
                    source = sourceBlock,
                    sourceStartY = sourceStartY,
                    output = outputBlock,
                    outputStartY = outputStartY,
                    outputRowCount = outputRows,
                    width = bitmap.width,
                    height = bitmap.height,
                    plan = plan
                )
                bitmap.setPixels(
                    outputBlock,
                    0,
                    bitmap.width,
                    0,
                    outputStartY,
                    bitmap.width,
                    outputRows
                )
                outputStartY += outputRows
                onProgress(outputStartY / bitmap.height.toFloat())
            }
        } finally {
            source.recycle()
        }
    }
}

/** Memory-bounded source loading and rendering for the interactive editor canvas. */
object EditorPreviewRenderer {

    private const val MAX_PREVIEW_DIMENSION = 1440

    suspend fun loadSource(context: Context, imageModel: Any): Result<Bitmap> =
        withContext(Dispatchers.IO) {
            runCatching {
                when (imageModel) {
                    is Bitmap -> scaleForPreview(imageModel.copy(Bitmap.Config.ARGB_8888, false))
                    is Uri -> loadUri(context, imageModel)
                    is Int -> loadResource(context, imageModel)
                    else -> throw IllegalArgumentException("Unsupported editor image source")
                }
            }
        }

    suspend fun render(source: Bitmap, state: EditorState): Bitmap =
        withContext(Dispatchers.Default) {
            coroutineContext.ensureActive()
            val output = EditorGeometryEngine.render(
                source = source,
                transform = state.transform,
                checkpoint = { coroutineContext.ensureActive() }
            )
            try {
                EditorBitmapRenderer.applyAllInPlace(
                    bitmap = output,
                    state = state,
                    checkpoint = { coroutineContext.ensureActive() }
                )
                EditorRetouchEngine.applyInPlace(
                    bitmap = output,
                    retouch = state.retouch,
                    checkpoint = { coroutineContext.ensureActive() }
                )
                EditorOverlayEngine.applyInPlace(
                    bitmap = output,
                    overlays = state.overlays,
                    checkpoint = { coroutineContext.ensureActive() }
                )
                coroutineContext.ensureActive()
                output
            } catch (error: Throwable) {
                output.recycle()
                throw error
            }
        }

    private fun loadUri(context: Context, uri: Uri): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot read selected photo")
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalStateException("Selected photo has invalid dimensions")
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = previewSampleSize(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: throw IllegalStateException("Cannot decode selected photo")

        val orientation = resolver.openInputStream(uri)?.use { input ->
            ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        return normalizeOrientation(decoded, orientation)
    }

    private fun loadResource(context: Context, resourceId: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resourceId, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalStateException("Editor preview resource has invalid dimensions")
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = previewSampleSize(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeResource(context.resources, resourceId, options)
            ?: throw IllegalStateException("Cannot decode editor preview resource")
    }

    private fun scaleForPreview(bitmap: Bitmap): Bitmap {
        val largest = max(bitmap.width, bitmap.height)
        if (largest <= MAX_PREVIEW_DIMENSION) return bitmap
        val ratio = MAX_PREVIEW_DIMENSION.toFloat() / largest.toFloat()
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true
        )
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    private fun previewSampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (max(width / sample, height / sample) > MAX_PREVIEW_DIMENSION) sample *= 2
        return sample
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
        val normalized = Bitmap.createBitmap(
            source,
            0,
            0,
            source.width,
            source.height,
            matrix,
            true
        )
        if (normalized !== source) source.recycle()
        return normalized
    }
}
