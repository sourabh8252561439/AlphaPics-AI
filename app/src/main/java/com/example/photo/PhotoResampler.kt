package com.example.photo

import android.graphics.Bitmap
import kotlin.math.max

/**
 * Memory-bounded high-quality scaler. Large reductions are performed in filtered half-size
 * passes before the final filtered resize, which reduces aliasing compared with one extreme pass.
 */
object PhotoResampler {
    fun resize(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        onProgress: (Float) -> Unit = {},
        checkpoint: () -> Unit = {}
    ): Bitmap {
        require(!source.isRecycled && source.width > 0 && source.height > 0) { "Invalid source bitmap" }
        validateOutputDimensions(targetWidth, targetHeight)
        if (source.width == targetWidth && source.height == targetHeight) {
            checkpoint()
            onProgress(1f)
            return source.copy(Bitmap.Config.ARGB_8888, true)
        }

        var current = source
        var ownsCurrent = false
        var pass = 0
        while (current.width / 2 >= targetWidth && current.height / 2 >= targetHeight) {
            checkpoint()
            val nextWidth = max(targetWidth, current.width / 2)
            val nextHeight = max(targetHeight, current.height / 2)
            val next = Bitmap.createScaledBitmap(current, nextWidth, nextHeight, true)
            if (ownsCurrent && next !== current) current.recycle()
            current = next
            ownsCurrent = current !== source
            pass++
            onProgress((0.18f * pass).coerceAtMost(0.72f))
        }

        checkpoint()
        val result = if (current.width == targetWidth && current.height == targetHeight) {
            if (ownsCurrent) current else current.copy(Bitmap.Config.ARGB_8888, true)
        } else {
            Bitmap.createScaledBitmap(current, targetWidth, targetHeight, true).also {
                if (ownsCurrent && it !== current) current.recycle()
            }
        }
        result.setHasAlpha(source.hasAlpha())
        onProgress(1f)
        return result
    }
}
