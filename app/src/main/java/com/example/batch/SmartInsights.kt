package com.example.batch

/**
 * Deterministic, rule-based insights about an image's compression characteristics.
 * Deliberately NOT called "AI" anywhere - these are plain measurable-property checks,
 * exactly as asked for. Every insight is derived only from values passed in; nothing here
 * is guessed or invented per-image.
 */
object SmartInsights {

    data class Insight(val message: String, val level: Level)
    enum class Level { INFO, TIP, WARNING }

    fun generate(
        originalWidth: Int,
        originalHeight: Int,
        originalSizeBytes: Long,
        hasTransparency: Boolean,
        inputFormat: String,
        finalSizeBytes: Long?
    ): List<Insight> {
        val insights = mutableListOf<Insight>()
        val megapixels = (originalWidth.toLong() * originalHeight.toLong()) / 1_000_000.0
        val originalKb = originalSizeBytes / 1024.0

        // Large-dimension check: a genuinely oversized image for common use cases.
        if (originalWidth > 2048 || originalHeight > 2048) {
            insights += Insight(
                "This image is ${originalWidth}x${originalHeight} - resizing toward 2048px on " +
                    "the long edge can meaningfully reduce file size with little visible difference " +
                    "for most uses (screens, web, forms).",
                Level.TIP
            )
        }

        // Large file size relative to megapixel count - suggests inefficient source encoding.
        if (megapixels > 0) {
            val kbPerMegapixel = originalKb / megapixels
            if (kbPerMegapixel > 600) {
                insights += Insight(
                    "This file is large for its pixel dimensions (about ${kbPerMegapixel.toInt()} KB " +
                        "per megapixel) - it's likely using a low compression ratio and has real room " +
                        "to shrink without a visible quality loss.",
                    Level.INFO
                )
            }
        }

        // Transparency + JPEG-unfriendly guidance.
        if (hasTransparency) {
            insights += Insight(
                "Transparency detected. JPEG isn't used for this image because it would silently " +
                    "flatten the transparent areas - PNG or WebP is used instead to keep it intact.",
                Level.WARNING
            )
        }

        // Already-efficient check, using the actual achieved result when available.
        if (finalSizeBytes != null && originalSizeBytes > 0) {
            val reduction = 1.0 - (finalSizeBytes.toDouble() / originalSizeBytes.toDouble())
            if (reduction < 0.05) {
                insights += Insight(
                    "This image was already efficiently compressed - there wasn't much size to " +
                        "recover without a real quality trade-off.",
                    Level.INFO
                )
            }
        }

        // Format-specific note for screenshots/PNG source without transparency.
        if (inputFormat.equals("png", ignoreCase = true) && !hasTransparency) {
            insights += Insight(
                "Source is PNG with no transparency (common for screenshots) - WebP or JPEG " +
                    "typically compress this kind of image much smaller than keeping it as PNG.",
                Level.TIP
            )
        }

        return insights
    }
}
