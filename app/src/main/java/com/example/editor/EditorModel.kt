package com.example.editor

import androidx.compose.runtime.Immutable

/**
 * Tonal and light adjustments.
 * Values typically range from -100f to +100f where 0f is neutral.
 */
@Immutable
data class LightAdjustments(
    val exposure: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val whites: Float = 0f,
    val blacks: Float = 0f,
    val gamma: Float = 0f
) {
    val isNeutral: Boolean
        get() = exposure == 0f && brightness == 0f && contrast == 0f &&
            highlights == 0f && shadows == 0f && whites == 0f && blacks == 0f &&
            gamma == 0f
}

/**
 * Color and white-balance adjustments.
 * Values range from -100f to +100f where 0f is neutral.
 */
@Immutable
data class ColorAdjustments(
    val saturation: Float = 0f,
    val vibrance: Float = 0f,
    val warmth: Float = 0f,
    val tint: Float = 0f
) {
    val isNeutral: Boolean
        get() = saturation == 0f && vibrance == 0f && warmth == 0f && tint == 0f
}

/** Independent RGB gain mixer. Values range from -100..100; zero preserves the channel. */
@Immutable
data class ColorMixAdjustments(
    val red: Float = 0f,
    val green: Float = 0f,
    val blue: Float = 0f
) {
    val isNeutral: Boolean
        get() = red == 0f && green == 0f && blue == 0f
}

/** Two-way shadow/highlight toning with a movable tonal balance. */
@Immutable
data class SplitToneAdjustments(
    val shadowHue: Float = 220f,
    val shadowSaturation: Float = 0f,
    val highlightHue: Float = 42f,
    val highlightSaturation: Float = 0f,
    val balance: Float = 0f
) {
    val isNeutral: Boolean
        get() = shadowSaturation == 0f && highlightSaturation == 0f
}

@Immutable
data class ColorGradeRange(
    val hue: Float = 0f,
    val saturation: Float = 0f,
    val luminance: Float = 0f
) {
    val isNeutral: Boolean
        get() = saturation == 0f && luminance == 0f
}

enum class ColorGradeRegion(val label: String) {
    SHADOWS("Shadows"),
    MIDTONES("Midtones"),
    HIGHLIGHTS("Highlights")
}

@Immutable
data class ColorGradingAdjustments(
    val shadows: ColorGradeRange = ColorGradeRange(hue = 220f),
    val midtones: ColorGradeRange = ColorGradeRange(hue = 30f),
    val highlights: ColorGradeRange = ColorGradeRange(hue = 45f)
) {
    val isNeutral: Boolean
        get() = shadows.isNeutral && midtones.isNeutral && highlights.isNeutral

    operator fun get(region: ColorGradeRegion): ColorGradeRange = when (region) {
        ColorGradeRegion.SHADOWS -> shadows
        ColorGradeRegion.MIDTONES -> midtones
        ColorGradeRegion.HIGHLIGHTS -> highlights
    }

    fun update(region: ColorGradeRegion, value: ColorGradeRange): ColorGradingAdjustments =
        when (region) {
            ColorGradeRegion.SHADOWS -> copy(shadows = value)
            ColorGradeRegion.MIDTONES -> copy(midtones = value)
            ColorGradeRegion.HIGHLIGHTS -> copy(highlights = value)
        }
}

enum class HslColorChannel(val label: String, val centerHue: Float) {
    RED("Red", 0f),
    ORANGE("Orange", 30f),
    YELLOW("Yellow", 60f),
    GREEN("Green", 120f),
    CYAN("Aqua", 180f),
    BLUE("Blue", 240f),
    PURPLE("Purple", 275f),
    MAGENTA("Magenta", 315f)
}

@Immutable
data class HslChannelAdjustment(
    val hue: Float = 0f,
    val saturation: Float = 0f,
    val luminance: Float = 0f
) {
    val isNeutral: Boolean
        get() = hue == 0f && saturation == 0f && luminance == 0f
}

@Immutable
data class HslAdjustments(
    val red: HslChannelAdjustment = HslChannelAdjustment(),
    val orange: HslChannelAdjustment = HslChannelAdjustment(),
    val yellow: HslChannelAdjustment = HslChannelAdjustment(),
    val green: HslChannelAdjustment = HslChannelAdjustment(),
    val cyan: HslChannelAdjustment = HslChannelAdjustment(),
    val blue: HslChannelAdjustment = HslChannelAdjustment(),
    val purple: HslChannelAdjustment = HslChannelAdjustment(),
    val magenta: HslChannelAdjustment = HslChannelAdjustment()
) {
    val isNeutral: Boolean
        get() = HslColorChannel.entries.all { get(it).isNeutral }

    operator fun get(channel: HslColorChannel): HslChannelAdjustment = when (channel) {
        HslColorChannel.RED -> red
        HslColorChannel.ORANGE -> orange
        HslColorChannel.YELLOW -> yellow
        HslColorChannel.GREEN -> green
        HslColorChannel.CYAN -> cyan
        HslColorChannel.BLUE -> blue
        HslColorChannel.PURPLE -> purple
        HslColorChannel.MAGENTA -> magenta
    }

    fun update(channel: HslColorChannel, value: HslChannelAdjustment): HslAdjustments =
        when (channel) {
            HslColorChannel.RED -> copy(red = value)
            HslColorChannel.ORANGE -> copy(orange = value)
            HslColorChannel.YELLOW -> copy(yellow = value)
            HslColorChannel.GREEN -> copy(green = value)
            HslColorChannel.CYAN -> copy(cyan = value)
            HslColorChannel.BLUE -> copy(blue = value)
            HslColorChannel.PURPLE -> copy(purple = value)
            HslColorChannel.MAGENTA -> copy(magenta = value)
        }
}

enum class CurveChannel(val label: String) {
    MASTER("RGB"),
    RED("Red"),
    GREEN("Green"),
    BLUE("Blue")
}

@Immutable
data class CurvePoint(
    val x: Float,
    val y: Float
)

@Immutable
data class ToneCurve(
    val points: List<CurvePoint> = IdentityPoints
) {
    val isIdentity: Boolean
        get() = CurveEngine.isIdentity(this)

    companion object {
        val IdentityPoints = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))
    }
}

@Immutable
data class CurvesAdjustments(
    val master: ToneCurve = ToneCurve(),
    val red: ToneCurve = ToneCurve(),
    val green: ToneCurve = ToneCurve(),
    val blue: ToneCurve = ToneCurve()
) {
    val isNeutral: Boolean
        get() = master.isIdentity && red.isIdentity && green.isIdentity && blue.isIdentity

    operator fun get(channel: CurveChannel): ToneCurve = when (channel) {
        CurveChannel.MASTER -> master
        CurveChannel.RED -> red
        CurveChannel.GREEN -> green
        CurveChannel.BLUE -> blue
    }

    fun update(channel: CurveChannel, curve: ToneCurve): CurvesAdjustments = when (channel) {
        CurveChannel.MASTER -> copy(master = curve)
        CurveChannel.RED -> copy(red = curve)
        CurveChannel.GREEN -> copy(green = curve)
        CurveChannel.BLUE -> copy(blue = curve)
    }
}

/**
 * Detail and effect adjustments.
 */
@Immutable
data class DetailAdjustments(
    val sharpen: Float = 0f,
    val structure: Float = 0f,
    val clarity: Float = 0f,
    val texture: Float = 0f,
    val noiseReduction: Float = 0f,
    val dehaze: Float = 0f
) {
    val isNeutral: Boolean
        get() = sharpen == 0f && structure == 0f && clarity == 0f && texture == 0f &&
            noiseReduction == 0f && dehaze == 0f
}

/** Spatial and finish effects. Blur and grain values are 0..100; vignette is -100..100. */
@Immutable
data class EffectAdjustments(
    val vignette: Float = 0f,
    val grain: Float = 0f,
    val fade: Float = 0f,
    val gaussianBlur: Float = 0f,
    val focusBlur: Float = 0f,
    val radialBlur: Float = 0f
) {
    val isNeutral: Boolean
        get() = vignette == 0f && grain == 0f && fade == 0f && gaussianBlur == 0f &&
            focusBlur == 0f && radialBlur == 0f
}

/** Normalized source crop. Values are fractions of the current oriented image bounds. */
@Immutable
data class NormalizedCropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    val isFull: Boolean
        get() = left == 0f && top == 0f && right == 1f && bottom == 1f

    fun sanitized(minimumSpan: Float = 0.05f): NormalizedCropRect {
        val safeLeft = left.coerceIn(0f, 1f - minimumSpan)
        val safeTop = top.coerceIn(0f, 1f - minimumSpan)
        val safeRight = right.coerceIn(safeLeft + minimumSpan, 1f)
        val safeBottom = bottom.coerceIn(safeTop + minimumSpan, 1f)
        return NormalizedCropRect(safeLeft, safeTop, safeRight, safeBottom)
    }
}

enum class CropGrid { OFF, THIRDS, SQUARE }

/**
 * Non-destructive crop, rotation, perspective and manual lens geometry.
 * Signed geometry values use a -100..100 editor range; straighten uses degrees.
 */
@Immutable
data class TransformAdjustments(
    val aspectId: String = "original",
    val rotationDegrees: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val cropRect: NormalizedCropRect = NormalizedCropRect(),
    val customAspectWidth: Float = 4f,
    val customAspectHeight: Float = 5f,
    val straightenDegrees: Float = 0f,
    val perspectiveHorizontal: Float = 0f,
    val perspectiveVertical: Float = 0f,
    val lensDistortion: Float = 0f,
    val geometryHorizontal: Float = 0f,
    val geometryVertical: Float = 0f,
    val grid: CropGrid = CropGrid.OFF
) {
    val targetAspectRatio: Float?
        get() = when (aspectId) {
            "1:1" -> 1f
            "4:5" -> 4f / 5f
            "3:4" -> 3f / 4f
            "2:3" -> 2f / 3f
            "3:2" -> 3f / 2f
            "4:3" -> 4f / 3f
            "16:9" -> 16f / 9f
            "9:16" -> 9f / 16f
            "1.91:1" -> 1.91f
            "custom" -> customAspectWidth.coerceAtLeast(1f) /
                customAspectHeight.coerceAtLeast(1f)
            else -> null
        }

    val isNeutral: Boolean
        get() = aspectId == "original" && rotationDegrees == 0 && !flipHorizontal && !flipVertical &&
            cropRect.isFull && straightenDegrees == 0f && perspectiveHorizontal == 0f &&
            perspectiveVertical == 0f && lensDistortion == 0f && geometryHorizontal == 0f &&
            geometryVertical == 0f
}

enum class LocalRetouchMode {
    HEAL,
    CLONE,
    BLEMISH,
    RED_EYE,
    BLUR,
    SHARPEN,
    EXPOSURE,
    BRIGHTNESS,
    SATURATION,
    TEMPERATURE,
    ERASE_MASK
}

@Immutable
data class RetouchPoint(
    val x: Float,
    val y: Float
) {
    fun sanitized(): RetouchPoint = RetouchPoint(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
}

/** One replayable local edit stroke in normalized post-transform image coordinates. */
@Immutable
data class RetouchStroke(
    val mode: LocalRetouchMode,
    val points: List<RetouchPoint>,
    val size: Float,
    val feather: Float,
    val strength: Float,
    val cloneSourceOffsetX: Float = -8f,
    val cloneSourceOffsetY: Float = -8f
) {
    fun sanitized(): RetouchStroke = copy(
        points = points.take(RetouchAdjustments.MAX_RETOUCH_POINTS_PER_STROKE)
            .map(RetouchPoint::sanitized),
        size = size.coerceIn(1f, 30f),
        feather = feather.coerceIn(0f, 100f),
        strength = strength.coerceIn(1f, 100f),
        cloneSourceOffsetX = cloneSourceOffsetX.coerceIn(-50f, 50f),
        cloneSourceOffsetY = cloneSourceOffsetY.coerceIn(-50f, 50f)
    )
}

@Immutable
data class RetouchAdjustments(
    val strokes: List<RetouchStroke> = emptyList(),
    val activeMode: LocalRetouchMode = LocalRetouchMode.HEAL,
    val brushSize: Float = 8f,
    val brushFeather: Float = 55f,
    val brushStrength: Float = 65f,
    val cloneSourceOffsetX: Float = -8f,
    val cloneSourceOffsetY: Float = -8f,
    val showMask: Boolean = false
) {
    val isNeutral: Boolean
        get() = strokes.isEmpty()

    fun append(stroke: RetouchStroke): RetouchAdjustments {
        val safeStroke = stroke.sanitized()
        if (safeStroke.points.isEmpty()) return this
        return copy(strokes = (strokes + safeStroke).takeLast(MAX_RETOUCH_STROKES))
    }

    fun removeLastStroke(): RetouchAdjustments =
        if (strokes.isEmpty()) this else copy(strokes = strokes.dropLast(1))

    companion object {
        const val MAX_RETOUCH_STROKES = 128
        const val MAX_RETOUCH_POINTS_PER_STROKE = 512
    }
}

enum class OverlayTextAlignment { LEFT, CENTER, RIGHT }

@Immutable
data class TextOverlay(
    val text: String,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val fontSize: Float = 8f,
    val weight: Int = 700,
    val alignment: OverlayTextAlignment = OverlayTextAlignment.CENTER,
    val colorArgb: Long = 0xFFFFFFFF,
    val opacity: Float = 100f,
    val rotation: Float = 0f,
    val scale: Float = 100f,
    val letterSpacing: Float = 0f,
    val lineSpacing: Float = 110f,
    val backgroundArgb: Long? = null,
    val outlineArgb: Long? = 0xFF000000,
    val shadow: Float = 35f
) {
    fun sanitized(): TextOverlay = copy(
        text = text.take(240),
        x = x.coerceIn(0f, 1f),
        y = y.coerceIn(0f, 1f),
        fontSize = fontSize.coerceIn(2f, 24f),
        weight = if (weight >= 600) 700 else 400,
        opacity = opacity.coerceIn(0f, 100f),
        rotation = rotation.coerceIn(-180f, 180f),
        scale = scale.coerceIn(25f, 300f),
        letterSpacing = letterSpacing.coerceIn(-0.08f, 0.30f),
        lineSpacing = lineSpacing.coerceIn(75f, 200f),
        shadow = shadow.coerceIn(0f, 100f)
    )
}

@Immutable
data class DrawOverlayStroke(
    val points: List<RetouchPoint>,
    val colorArgb: Long = 0xFFFFFFFF,
    val opacity: Float = 100f,
    val size: Float = 2f,
    val eraser: Boolean = false
) {
    fun sanitized(): DrawOverlayStroke = copy(
        points = points.take(512).map(RetouchPoint::sanitized),
        opacity = opacity.coerceIn(0f, 100f),
        size = size.coerceIn(0.2f, 12f)
    )
}

enum class OverlayShapeKind { RECTANGLE, ROUNDED_RECTANGLE, CIRCLE, LINE, ARROW }

@Immutable
data class ShapeOverlay(
    val kind: OverlayShapeKind,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val width: Float = 0.38f,
    val height: Float = 0.24f,
    val rotation: Float = 0f,
    val fillArgb: Long? = null,
    val strokeArgb: Long = 0xFFFFFFFF,
    val strokeWidth: Float = 1.2f,
    val opacity: Float = 100f
) {
    fun sanitized(): ShapeOverlay = copy(
        x = x.coerceIn(0f, 1f),
        y = y.coerceIn(0f, 1f),
        width = width.coerceIn(0.05f, 1f),
        height = height.coerceIn(0.05f, 1f),
        rotation = rotation.coerceIn(-180f, 180f),
        strokeWidth = strokeWidth.coerceIn(0.2f, 8f),
        opacity = opacity.coerceIn(0f, 100f)
    )
}

enum class OverlayStickerKind { STAR, HEART, SPARKLE }

@Immutable
data class StickerOverlay(
    val kind: OverlayStickerKind,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val scale: Float = 18f,
    val rotation: Float = 0f,
    val flipHorizontal: Boolean = false,
    val colorArgb: Long = 0xFFFFFFFF,
    val opacity: Float = 100f
) {
    fun sanitized(): StickerOverlay = copy(
        x = x.coerceIn(0f, 1f),
        y = y.coerceIn(0f, 1f),
        scale = scale.coerceIn(3f, 80f),
        rotation = rotation.coerceIn(-180f, 180f),
        opacity = opacity.coerceIn(0f, 100f)
    )
}

enum class WatermarkAnchor { TOP_LEFT, TOP_RIGHT, CENTER, BOTTOM_LEFT, BOTTOM_RIGHT }

enum class OverlayToolMode { TEXT, DRAW, SHAPE, STICKER, FRAME, WATERMARK }

@Immutable
data class WatermarkAdjustment(
    val enabled: Boolean = false,
    val text: String = "AlphaPics AI",
    val anchor: WatermarkAnchor = WatermarkAnchor.BOTTOM_RIGHT,
    val scale: Float = 4f,
    val rotation: Float = 0f,
    val opacity: Float = 55f,
    val padding: Float = 3f,
    val colorArgb: Long = 0xFFFFFFFF
) {
    val isNeutral: Boolean
        get() = !enabled || text.isBlank()
}

@Immutable
data class FrameAdjustments(
    val borderEnabled: Boolean = false,
    val borderColorArgb: Long = 0xFFFFFFFF,
    val borderThickness: Float = 2f,
    val cornerRadius: Float = 0f,
    val presetId: String = "none"
) {
    val isNeutral: Boolean
        get() = !borderEnabled && cornerRadius == 0f && presetId == "none"
}

@Immutable
data class OverlayAdjustments(
    val texts: List<TextOverlay> = emptyList(),
    val drawing: List<DrawOverlayStroke> = emptyList(),
    val drawingRedo: List<DrawOverlayStroke> = emptyList(),
    val shapes: List<ShapeOverlay> = emptyList(),
    val stickers: List<StickerOverlay> = emptyList(),
    val frame: FrameAdjustments = FrameAdjustments(),
    val watermark: WatermarkAdjustment = WatermarkAdjustment(),
    val activeTool: OverlayToolMode = OverlayToolMode.TEXT,
    val textDraft: String = "Your text",
    val textTemplate: TextOverlay = TextOverlay(text = "Your text"),
    val drawColorArgb: Long = 0xFFFFFFFF,
    val drawOpacity: Float = 100f,
    val drawSize: Float = 2f,
    val drawEraser: Boolean = false,
    val shapeTemplate: ShapeOverlay = ShapeOverlay(OverlayShapeKind.RECTANGLE),
    val stickerTemplate: StickerOverlay = StickerOverlay(OverlayStickerKind.STAR)
) {
    val isNeutral: Boolean
        get() = texts.isEmpty() && drawing.isEmpty() && shapes.isEmpty() && stickers.isEmpty() &&
            frame.isNeutral && watermark.isNeutral

    fun addText(item: TextOverlay): OverlayAdjustments =
        copy(texts = (texts + item.sanitized()).takeLast(MAX_OVERLAY_ITEMS))

    fun addDrawing(item: DrawOverlayStroke): OverlayAdjustments =
        copy(
            drawing = (drawing + item.sanitized()).takeLast(MAX_DRAW_STROKES),
            drawingRedo = emptyList()
        )

    fun undoDrawing(): OverlayAdjustments {
        val last = drawing.lastOrNull() ?: return this
        return copy(
            drawing = drawing.dropLast(1),
            drawingRedo = (drawingRedo + last).takeLast(MAX_DRAW_STROKES)
        )
    }

    fun redoDrawing(): OverlayAdjustments {
        val restored = drawingRedo.lastOrNull() ?: return this
        return copy(
            drawing = (drawing + restored).takeLast(MAX_DRAW_STROKES),
            drawingRedo = drawingRedo.dropLast(1)
        )
    }

    fun addShape(item: ShapeOverlay): OverlayAdjustments =
        copy(shapes = (shapes + item.sanitized()).takeLast(MAX_OVERLAY_ITEMS))

    fun addSticker(item: StickerOverlay): OverlayAdjustments =
        copy(stickers = (stickers + item.sanitized()).takeLast(MAX_OVERLAY_ITEMS))

    companion object {
        const val MAX_OVERLAY_ITEMS = 64
        const val MAX_DRAW_STROKES = 128
    }
}

/**
 * Filter preset and intensity.
 */
@Immutable
data class FilterAdjustment(
    val presetId: String = "original",
    val intensity: Float = 100f
) {
    val isNeutral: Boolean
        get() = presetId == "original" || intensity == 0f
}

/**
 * Complete non-destructive photo editor state.
 */
@Immutable
data class EditorState(
    val light: LightAdjustments = LightAdjustments(),
    val color: ColorAdjustments = ColorAdjustments(),
    val colorMix: ColorMixAdjustments = ColorMixAdjustments(),
    val splitTone: SplitToneAdjustments = SplitToneAdjustments(),
    val colorGrading: ColorGradingAdjustments = ColorGradingAdjustments(),
    val hsl: HslAdjustments = HslAdjustments(),
    val curves: CurvesAdjustments = CurvesAdjustments(),
    val detail: DetailAdjustments = DetailAdjustments(),
    val effects: EffectAdjustments = EffectAdjustments(),
    val transform: TransformAdjustments = TransformAdjustments(),
    val retouch: RetouchAdjustments = RetouchAdjustments(),
    val overlays: OverlayAdjustments = OverlayAdjustments(),
    val filter: FilterAdjustment = FilterAdjustment()
) {
    val isDefault: Boolean
        get() = light.isNeutral && color.isNeutral && colorMix.isNeutral && splitTone.isNeutral &&
            colorGrading.isNeutral && hsl.isNeutral && curves.isNeutral && detail.isNeutral &&
            effects.isNeutral && transform.isNeutral && retouch.isNeutral && overlays.isNeutral &&
            filter.isNeutral
}

/**
 * Definition of a deterministic local color filter preset.
 */
@Immutable
data class FilterPreset(
    val id: String,
    val label: String,
    val exposure: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val warmth: Float = 0f,
    val tint: Float = 0f
)

/**
 * Curated preset catalog for on-device grading.
 */
object FilterPresetCatalog {
    val Original = FilterPreset("original", "Original")
    val Clean = FilterPreset("clean", "Clean", exposure = 6f, contrast = 8f, saturation = 6f)
    val Natural = FilterPreset("natural", "Natural", exposure = 3f, contrast = 5f, saturation = 4f, warmth = 2f)
    val Portrait = FilterPreset("portrait", "Portrait", exposure = 6f, contrast = -4f, saturation = 5f, warmth = 8f, tint = 2f)
    val Cinema = FilterPreset("cinema", "Cinema", contrast = 18f, saturation = -14f, warmth = 10f, tint = -6f)
    val Cinematic = FilterPreset("cinematic", "Cinematic", exposure = -3f, contrast = 20f, saturation = -12f, warmth = 8f, tint = -8f)
    val Film = FilterPreset("film", "Film", exposure = 4f, contrast = -5f, saturation = -8f, warmth = 12f, tint = 3f)
    val Mono = FilterPreset("mono", "Mono", contrast = 14f, saturation = -100f)
    val Cool = FilterPreset("cool", "Cool", contrast = 8f, saturation = -5f, warmth = -32f)
    val Warm = FilterPreset("warm", "Warm", exposure = 4f, contrast = 6f, saturation = 8f, warmth = 28f)
    val Vintage = FilterPreset("vintage", "Vintage", exposure = 8f, contrast = -10f, saturation = -20f, warmth = 22f)
    val Vivid = FilterPreset("vivid", "Vivid", exposure = 2f, contrast = 12f, saturation = 30f, warmth = 4f)
    val Moody = FilterPreset("moody", "Moody", exposure = -8f, contrast = 22f, saturation = -18f, warmth = -12f)
    val Fade = FilterPreset("fade", "Fade", contrast = -16f, saturation = -12f, warmth = 8f)
    val Street = FilterPreset("street", "Street", exposure = -2f, contrast = 24f, saturation = -6f, warmth = -4f)
    val Food = FilterPreset("food", "Food", exposure = 5f, contrast = 10f, saturation = 20f, warmth = 12f, tint = 2f)
    val Travel = FilterPreset("travel", "Travel", exposure = 5f, contrast = 12f, saturation = 16f, warmth = 5f)
    val Landscape = FilterPreset("landscape", "Landscape", exposure = 2f, contrast = 16f, saturation = 18f, warmth = -2f)

    val Presets = listOf(
        Original,
        Natural,
        Portrait,
        Clean,
        Cinematic,
        Film,
        Cinema,
        Mono,
        Cool,
        Warm,
        Vintage,
        Vivid,
        Moody,
        Fade,
        Street,
        Food,
        Travel,
        Landscape
    )

    fun find(id: String): FilterPreset = Presets.firstOrNull { it.id == id } ?: Original
}
