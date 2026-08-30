package com.example.editor

import kotlin.math.abs
import kotlin.math.roundToInt

/** Deterministic curve editing and monotone cubic interpolation. */
object CurveEngine {

    private const val MIN_POINT_DISTANCE = 0.01f
    private const val MAX_POINTS = 16

    fun sanitize(curve: ToneCurve): ToneCurve {
        val sorted = curve.points
            .map { CurvePoint(it.x.coerceIn(0f, 1f), it.y.coerceIn(0f, 1f)) }
            .sortedBy { it.x }

        val unique = mutableListOf<CurvePoint>()
        for (point in sorted) {
            if (unique.isEmpty() || point.x - unique.last().x >= MIN_POINT_DISTANCE) {
                unique += point
            } else {
                unique[unique.lastIndex] = point
            }
        }

        if (unique.isEmpty() || unique.first().x > MIN_POINT_DISTANCE) {
            unique.add(0, CurvePoint(0f, 0f))
        } else {
            unique[0] = unique.first().copy(x = 0f)
        }
        if (unique.last().x < 1f - MIN_POINT_DISTANCE) {
            unique += CurvePoint(1f, 1f)
        } else {
            unique[unique.lastIndex] = unique.last().copy(x = 1f)
        }

        if (unique.size <= MAX_POINTS) return ToneCurve(unique)

        val retained = buildList {
            add(unique.first())
            val interiorSlots = MAX_POINTS - 2
            repeat(interiorSlots) { slot ->
                val sourceIndex = 1 + ((slot + 1f) * (unique.size - 1) / (interiorSlots + 1f))
                    .roundToInt()
                    .coerceIn(1, unique.lastIndex - 1)
                add(unique[sourceIndex])
            }
            add(unique.last())
        }.distinctBy { it.x }
        return ToneCurve(retained)
    }

    fun addPoint(curve: ToneCurve, x: Float, y: Float): ToneCurve {
        val sanitized = sanitize(curve)
        if (sanitized.points.size >= MAX_POINTS) return sanitized
        val safeX = x.coerceIn(MIN_POINT_DISTANCE, 1f - MIN_POINT_DISTANCE)
        val safeY = y.coerceIn(0f, 1f)
        val nearestIndex = sanitized.points.indices.minByOrNull {
            abs(sanitized.points[it].x - safeX)
        }
        if (nearestIndex != null && abs(sanitized.points[nearestIndex].x - safeX) < 0.025f) {
            return movePoint(sanitized, nearestIndex, safeX, safeY)
        }
        return sanitize(ToneCurve(sanitized.points + CurvePoint(safeX, safeY)))
    }

    fun movePoint(curve: ToneCurve, index: Int, x: Float, y: Float): ToneCurve {
        val points = sanitize(curve).points.toMutableList()
        if (index !in points.indices) return ToneCurve(points)
        val safeX = when (index) {
            0 -> 0f
            points.lastIndex -> 1f
            else -> x.coerceIn(
                points[index - 1].x + MIN_POINT_DISTANCE,
                points[index + 1].x - MIN_POINT_DISTANCE
            )
        }
        points[index] = CurvePoint(safeX, y.coerceIn(0f, 1f))
        return ToneCurve(points)
    }

    fun deletePoint(curve: ToneCurve, index: Int): ToneCurve {
        val points = sanitize(curve).points.toMutableList()
        if (index <= 0 || index >= points.lastIndex) return ToneCurve(points)
        points.removeAt(index)
        return ToneCurve(points)
    }

    fun reset(): ToneCurve = ToneCurve()

    fun isIdentity(curve: ToneCurve): Boolean =
        sanitize(curve).points.all { abs(it.x - it.y) <= 0.0001f }

    fun buildLut(curve: ToneCurve): IntArray {
        val points = sanitize(curve).points
        val count = points.size
        val slopes = FloatArray(count - 1)
        for (index in slopes.indices) {
            val width = points[index + 1].x - points[index].x
            slopes[index] = (points[index + 1].y - points[index].y) / width
        }

        val tangents = FloatArray(count)
        tangents[0] = slopes.first()
        tangents[count - 1] = slopes.last()
        for (index in 1 until count - 1) {
            val before = slopes[index - 1]
            val after = slopes[index]
            tangents[index] = if (before == 0f || after == 0f || before * after <= 0f) {
                0f
            } else {
                2f * before * after / (before + after)
            }
        }

        val lut = IntArray(256)
        var segment = 0
        for (input in 0..255) {
            val x = input / 255f
            while (segment < count - 2 && x > points[segment + 1].x) segment++

            val left = points[segment]
            val right = points[segment + 1]
            val width = right.x - left.x
            val t = ((x - left.x) / width).coerceIn(0f, 1f)
            val t2 = t * t
            val t3 = t2 * t
            val h00 = 2f * t3 - 3f * t2 + 1f
            val h10 = t3 - 2f * t2 + t
            val h01 = -2f * t3 + 3f * t2
            val h11 = t3 - t2
            val y = h00 * left.y + h10 * width * tangents[segment] +
                h01 * right.y + h11 * width * tangents[segment + 1]
            lut[input] = (y.coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255)
        }
        return lut
    }
}
