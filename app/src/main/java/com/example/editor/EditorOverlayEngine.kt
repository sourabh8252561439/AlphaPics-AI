package com.example.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Replays editable text, drawing, shape, sticker, frame and watermark overlays. */
object EditorOverlayEngine {

    fun applyInPlace(
        bitmap: Bitmap,
        overlays: OverlayAdjustments,
        onProgress: (Float) -> Unit = {},
        checkpoint: () -> Unit = {}
    ) {
        require(bitmap.isMutable) { "Overlay bitmap must be mutable" }
        if (overlays.isNeutral) {
            onProgress(1f)
            return
        }
        checkpoint()
        val layer = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(layer)
            drawDrawing(canvas, overlays.drawing, bitmap.width, bitmap.height, checkpoint)
            onProgress(0.20f)
            overlays.shapes.forEach { drawShape(canvas, it.sanitized(), bitmap.width, bitmap.height) }
            onProgress(0.38f)
            overlays.stickers.forEach { drawSticker(canvas, it.sanitized(), bitmap.width, bitmap.height) }
            onProgress(0.52f)
            overlays.texts.forEach { drawText(canvas, it.sanitized(), bitmap.width, bitmap.height) }
            if (!overlays.watermark.isNeutral) {
                drawWatermark(canvas, overlays.watermark, bitmap.width, bitmap.height)
            }
            checkpoint()
            Canvas(bitmap).drawBitmap(layer, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
            onProgress(0.76f)
            applyFrame(bitmap, overlays.frame)
            onProgress(1f)
        } finally {
            layer.recycle()
        }
    }

    private fun drawDrawing(
        canvas: Canvas,
        strokes: List<DrawOverlayStroke>,
        width: Int,
        height: Int,
        checkpoint: () -> Unit
    ) {
        val minimumDimension = min(width, height).toFloat()
        strokes.forEachIndexed { index, raw ->
            if (index % 8 == 0) checkpoint()
            val stroke = raw.sanitized()
            if (stroke.points.isEmpty()) return@forEachIndexed
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                strokeWidth = (stroke.size / 100f * minimumDimension).coerceAtLeast(1f)
                color = withOpacity(stroke.colorArgb.toInt(), stroke.opacity)
                if (stroke.eraser) xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
            if (stroke.points.size == 1) {
                val point = stroke.points.first()
                paint.style = Paint.Style.FILL
                canvas.drawCircle(point.x * width, point.y * height, paint.strokeWidth / 2f, paint)
            } else {
                val path = Path().apply {
                    moveTo(stroke.points.first().x * width, stroke.points.first().y * height)
                    stroke.points.drop(1).forEach { lineTo(it.x * width, it.y * height) }
                }
                canvas.drawPath(path, paint)
            }
            paint.xfermode = null
        }
    }

    private fun drawText(canvas: Canvas, item: TextOverlay, width: Int, height: Int) {
        if (item.text.isBlank()) return
        val minimumDimension = min(width, height).toFloat()
        val textSize = item.fontSize / 100f * minimumDimension * item.scale / 100f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = withOpacity(item.colorArgb.toInt(), item.opacity)
            this.textSize = textSize
            typeface = if (item.weight >= 600) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            textAlign = when (item.alignment) {
                OverlayTextAlignment.LEFT -> Paint.Align.LEFT
                OverlayTextAlignment.CENTER -> Paint.Align.CENTER
                OverlayTextAlignment.RIGHT -> Paint.Align.RIGHT
            }
            letterSpacing = item.letterSpacing
            if (item.shadow > 0f) {
                setShadowLayer(textSize * 0.10f, textSize * 0.06f, textSize * 0.08f, Color.BLACK)
            }
        }
        val lines = item.text.lines().take(8)
        val lineHeight = textSize * item.lineSpacing / 100f
        val widest = lines.maxOfOrNull(paint::measureText) ?: 0f
        val blockHeight = lineHeight * lines.size
        val anchorX = item.x * width
        val anchorY = item.y * height
        canvas.save()
        canvas.rotate(item.rotation, anchorX, anchorY)
        item.backgroundArgb?.let { background ->
            val left = when (item.alignment) {
                OverlayTextAlignment.LEFT -> anchorX
                OverlayTextAlignment.CENTER -> anchorX - widest / 2f
                OverlayTextAlignment.RIGHT -> anchorX - widest
            }
            val padding = textSize * 0.22f
            canvas.drawRoundRect(
                RectF(
                    left - padding,
                    anchorY - textSize - padding,
                    left + widest + padding,
                    anchorY - textSize + blockHeight + padding
                ),
                padding,
                padding,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = withOpacity(background.toInt(), item.opacity * 0.82f)
                }
            )
        }
        lines.forEachIndexed { index, line ->
            val baseline = anchorY + index * lineHeight
            item.outlineArgb?.let { outline ->
                val outlinePaint = Paint(paint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = (textSize * 0.055f).coerceAtLeast(1f)
                    color = withOpacity(outline.toInt(), item.opacity)
                    clearShadowLayer()
                }
                canvas.drawText(line, anchorX, baseline, outlinePaint)
            }
            canvas.drawText(line, anchorX, baseline, paint)
        }
        canvas.restore()
    }

    private fun drawShape(canvas: Canvas, item: ShapeOverlay, width: Int, height: Int) {
        val minimumDimension = min(width, height).toFloat()
        val centerX = item.x * width
        val centerY = item.y * height
        val halfWidth = item.width * width / 2f
        val halfHeight = item.height * height / 2f
        val rect = RectF(centerX - halfWidth, centerY - halfHeight, centerX + halfWidth, centerY + halfHeight)
        canvas.save()
        canvas.rotate(item.rotation, centerX, centerY)
        item.fillArgb?.let { fill ->
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = withOpacity(fill.toInt(), item.opacity)
            }
            when (item.kind) {
                OverlayShapeKind.RECTANGLE -> canvas.drawRect(rect, fillPaint)
                OverlayShapeKind.ROUNDED_RECTANGLE -> canvas.drawRoundRect(rect, halfHeight * 0.22f, halfHeight * 0.22f, fillPaint)
                OverlayShapeKind.CIRCLE -> canvas.drawOval(rect, fillPaint)
                OverlayShapeKind.LINE, OverlayShapeKind.ARROW -> Unit
            }
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = (item.strokeWidth / 100f * minimumDimension).coerceAtLeast(1f)
            color = withOpacity(item.strokeArgb.toInt(), item.opacity)
        }
        when (item.kind) {
            OverlayShapeKind.RECTANGLE -> canvas.drawRect(rect, strokePaint)
            OverlayShapeKind.ROUNDED_RECTANGLE -> canvas.drawRoundRect(rect, halfHeight * 0.22f, halfHeight * 0.22f, strokePaint)
            OverlayShapeKind.CIRCLE -> canvas.drawOval(rect, strokePaint)
            OverlayShapeKind.LINE -> canvas.drawLine(rect.left, centerY, rect.right, centerY, strokePaint)
            OverlayShapeKind.ARROW -> {
                canvas.drawLine(rect.left, centerY, rect.right, centerY, strokePaint)
                val head = min(halfWidth * 0.34f, minimumDimension * 0.08f)
                canvas.drawLine(rect.right, centerY, rect.right - head, centerY - head * 0.62f, strokePaint)
                canvas.drawLine(rect.right, centerY, rect.right - head, centerY + head * 0.62f, strokePaint)
            }
        }
        canvas.restore()
    }

    private fun drawSticker(canvas: Canvas, item: StickerOverlay, width: Int, height: Int) {
        val radius = item.scale / 200f * min(width, height)
        val centerX = item.x * width
        val centerY = item.y * height
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = withOpacity(item.colorArgb.toInt(), item.opacity)
        }
        val path = when (item.kind) {
            OverlayStickerKind.STAR -> starPath(radius)
            OverlayStickerKind.HEART -> heartPath(radius)
            OverlayStickerKind.SPARKLE -> sparklePath(radius)
        }
        path.transform(
            Matrix().apply {
                if (item.flipHorizontal) postScale(-1f, 1f)
                postRotate(item.rotation)
                postTranslate(centerX, centerY)
            }
        )
        canvas.drawPath(path, paint)
    }

    private fun drawWatermark(canvas: Canvas, watermark: WatermarkAdjustment, width: Int, height: Int) {
        val padding = watermark.padding.coerceIn(0f, 20f) / 100f * min(width, height)
        val textSize = watermark.scale.coerceIn(1.5f, 12f) / 100f * min(width, height)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = withOpacity(watermark.colorArgb.toInt(), watermark.opacity)
            this.textSize = textSize
            typeface = Typeface.DEFAULT_BOLD
        }
        val measured = paint.measureText(watermark.text)
        val metrics = paint.fontMetrics
        val textHeight = metrics.descent - metrics.ascent
        val position = when (watermark.anchor) {
            WatermarkAnchor.TOP_LEFT -> padding to padding - metrics.ascent
            WatermarkAnchor.TOP_RIGHT -> width - padding - measured to padding - metrics.ascent
            WatermarkAnchor.CENTER -> (width - measured) / 2f to height / 2f - (metrics.ascent + metrics.descent) / 2f
            WatermarkAnchor.BOTTOM_LEFT -> padding to height - padding - metrics.descent
            WatermarkAnchor.BOTTOM_RIGHT -> width - padding - measured to height - padding - metrics.descent
        }
        val centerX = position.first + measured / 2f
        val centerY = position.second - textHeight / 2f
        canvas.save()
        canvas.rotate(watermark.rotation.coerceIn(-180f, 180f), centerX, centerY)
        canvas.drawText(watermark.text.take(80), position.first, position.second, paint)
        canvas.restore()
    }

    private fun applyFrame(bitmap: Bitmap, frame: FrameAdjustments) {
        if (frame.isNeutral) return
        val minimumDimension = min(bitmap.width, bitmap.height).toFloat()
        val preset = frame.presetId
        val cornerPercent = when (preset) {
            "rounded" -> maxOf(frame.cornerRadius, 6f)
            else -> frame.cornerRadius
        }
        if (cornerPercent > 0f) {
            val mask = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            try {
                val radius = cornerPercent.coerceIn(0f, 20f) / 100f * minimumDimension
                Canvas(mask).drawRoundRect(
                    RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()),
                    radius,
                    radius,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
                )
                Canvas(bitmap).drawBitmap(
                    mask,
                    0f,
                    0f,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                    }
                )
            } finally {
                mask.recycle()
            }
        }
        val enabled = frame.borderEnabled || preset in setOf("white", "black", "film", "rounded")
        if (!enabled) return
        val color = when (preset) {
            "black", "film" -> Color.BLACK
            "white" -> Color.WHITE
            else -> frame.borderColorArgb.toInt()
        }
        val thicknessPercent = when (preset) {
            "film" -> maxOf(frame.borderThickness, 5f)
            else -> frame.borderThickness
        }
        val strokeWidth = thicknessPercent.coerceIn(0.2f, 12f) / 100f * minimumDimension
        val inset = strokeWidth / 2f
        Canvas(bitmap).drawRoundRect(
            RectF(inset, inset, bitmap.width - inset, bitmap.height - inset),
            cornerPercent / 100f * minimumDimension,
            cornerPercent / 100f * minimumDimension,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
                this.color = color
            }
        )
    }

    private fun starPath(radius: Float): Path = Path().apply {
        for (index in 0 until 10) {
            val angle = -Math.PI / 2.0 + index * Math.PI / 5.0
            val pointRadius = if (index % 2 == 0) radius else radius * 0.44f
            val x = cos(angle).toFloat() * pointRadius
            val y = sin(angle).toFloat() * pointRadius
            if (index == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    private fun heartPath(radius: Float): Path = Path().apply {
        moveTo(0f, radius * 0.82f)
        cubicTo(-radius * 1.25f, 0f, -radius * 0.72f, -radius, 0f, -radius * 0.36f)
        cubicTo(radius * 0.72f, -radius, radius * 1.25f, 0f, 0f, radius * 0.82f)
        close()
    }

    private fun sparklePath(radius: Float): Path = Path().apply {
        moveTo(0f, -radius)
        lineTo(radius * 0.24f, -radius * 0.24f)
        lineTo(radius, 0f)
        lineTo(radius * 0.24f, radius * 0.24f)
        lineTo(0f, radius)
        lineTo(-radius * 0.24f, radius * 0.24f)
        lineTo(-radius, 0f)
        lineTo(-radius * 0.24f, -radius * 0.24f)
        close()
    }

    private fun withOpacity(color: Int, opacity: Float): Int {
        val alpha = (Color.alpha(color) * opacity.coerceIn(0f, 100f) / 100f).toInt().coerceIn(0, 255)
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
