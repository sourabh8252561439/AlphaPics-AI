package com.example.batch

import android.content.Context
import android.net.Uri
import com.example.compression.CompressionSettingsSnapshot
import com.example.compression.ImageCompressionOutcome
import com.example.compression.ImageCompressionProcessor
import com.example.compression.ImageSourceInfo
import com.example.compression.ImageSourceInspector

/** Metadata inspection does not decode a full-resolution bitmap. */
object BatchImageInspector {
    fun inspect(context: Context, uri: Uri): BatchImageItem? {
        val source = ImageSourceInspector.inspect(context, uri) ?: return null
        return BatchImageItem(
            id = uri.toString(),
            uri = uri,
            displayName = source.displayName,
            originalSizeBytes = source.sizeBytes,
            originalWidth = source.width,
            originalHeight = source.height,
            inputFormat = source.inputFormat
        )
    }
}

/** Batch delegates to the exact same validated pipeline as single-image processing. */
object BatchImageProcessor {
    suspend fun process(
        context: Context,
        item: BatchImageItem,
        settings: CompressionSettingsSnapshot
    ): ImageCompressionOutcome {
        val knownSource = if (
            item.originalSizeBytes > 0L &&
            item.originalWidth > 0 &&
            item.originalHeight > 0
        ) {
            ImageSourceInfo(
                displayName = item.displayName,
                sizeBytes = item.originalSizeBytes,
                width = item.originalWidth,
                height = item.originalHeight,
                inputFormat = item.inputFormat
            )
        } else {
            null
        }
        return ImageCompressionProcessor.process(
            context = context,
            uri = item.uri,
            settings = settings,
            knownSource = knownSource
        )
    }
}
