package com.example.ui.alphapics.navigation

import androidx.compose.runtime.Immutable

enum class AlphaPicsAccent {
    BLUE,
    VIOLET,
    CYAN,
    PURPLE
}

enum class AlphaPicsIcon {
    ENHANCE,
    FACE,
    RESTORE,
    UPSCALE,
    BACKGROUND,
    EDIT,
    ERASER,
    RELIGHT,
    RETOUCH,
    RESIZE,
    CONVERT,
    COMPRESS,
    BATCH,
    HISTORY
}

@Immutable
data class AlphaPicsFeature(
    val id: String,
    val title: String,
    val description: String,
    val icon: AlphaPicsIcon,
    val accent: AlphaPicsAccent,
    val badge: String = "AI"
)

/**
 * Consumer-facing photo feature catalog. Generation and video concepts are intentionally absent
 * from routable product destinations.
 */
object AlphaPicsFeatureCatalog {
    val AiEnhance = AlphaPicsFeature(
        id = "ai-enhance",
        title = "Enhance",
        description = "Improve clarity, detail and natural color.",
        icon = AlphaPicsIcon.ENHANCE,
        accent = AlphaPicsAccent.BLUE
    )
    val FaceEnhance = AlphaPicsFeature(
        id = "face-enhance",
        title = "Face Enhance",
        description = "Recover cleaner portrait and facial detail.",
        icon = AlphaPicsIcon.FACE,
        accent = AlphaPicsAccent.VIOLET
    )
    val RestorePhoto = AlphaPicsFeature(
        id = "restore-photo",
        title = "Restore",
        description = "Revive aged, faded and damaged photos.",
        icon = AlphaPicsIcon.RESTORE,
        accent = AlphaPicsAccent.CYAN
    )
    val AiRetouch = AlphaPicsFeature(
        id = "ai-retouch",
        title = "Portrait Retouch",
        description = "Refine portraits while keeping a natural look.",
        icon = AlphaPicsIcon.RETOUCH,
        accent = AlphaPicsAccent.PURPLE
    )
    val Unblur = AlphaPicsFeature(
        id = "unblur",
        title = "Unblur",
        description = "Recover cleaner edges and sharper focus.",
        icon = AlphaPicsIcon.ENHANCE,
        accent = AlphaPicsAccent.BLUE
    )
    val Upscale = AlphaPicsFeature(
        id = "upscale",
        title = "Upscale",
        description = "Prepare photos for higher-resolution output.",
        icon = AlphaPicsIcon.UPSCALE,
        accent = AlphaPicsAccent.VIOLET
    )
    val RemoveBackground = AlphaPicsFeature(
        id = "remove-background",
        title = "Remove Background",
        description = "Isolate the subject with a clean cutout.",
        icon = AlphaPicsIcon.BACKGROUND,
        accent = AlphaPicsAccent.CYAN
    )
    val MagicEraser = AlphaPicsFeature(
        id = "magic-eraser",
        title = "Magic Eraser",
        description = "Remove unwanted distractions from a photo.",
        icon = AlphaPicsIcon.ERASER,
        accent = AlphaPicsAccent.PURPLE
    )
    val LowLightFix = AlphaPicsFeature(
        id = "low-light-fix",
        title = "Low-Light Fix",
        description = "Recover detail and balance in dark scenes.",
        icon = AlphaPicsIcon.RELIGHT,
        accent = AlphaPicsAccent.BLUE
    )
    val Colorize = AlphaPicsFeature(
        id = "colorize",
        title = "Colorize",
        description = "Bring natural color to black-and-white photos.",
        icon = AlphaPicsIcon.RESTORE,
        accent = AlphaPicsAccent.VIOLET
    )
    val Relight = AlphaPicsFeature(
        id = "relight",
        title = "Relight",
        description = "Refine light, exposure and mood.",
        icon = AlphaPicsIcon.RELIGHT,
        accent = AlphaPicsAccent.CYAN
    )
    val BackgroundReplace = AlphaPicsFeature(
        id = "background-replace",
        title = "Replace Background",
        description = "Use your own photo as a replacement background.",
        icon = AlphaPicsIcon.BACKGROUND,
        accent = AlphaPicsAccent.BLUE
    )
    val Retouch = AlphaPicsFeature(
        id = "retouch",
        title = "Retouch",
        description = "Polish color, tone and portrait detail.",
        icon = AlphaPicsIcon.RETOUCH,
        accent = AlphaPicsAccent.PURPLE
    )
    val EditPhoto = AlphaPicsFeature(
        id = "edit-photo",
        title = "Edit Photo",
        description = "A focused workspace for adjustments, retouching and transforms.",
        icon = AlphaPicsIcon.EDIT,
        accent = AlphaPicsAccent.VIOLET,
        badge = "EDITOR"
    )
    val Resize = AlphaPicsFeature(
        id = "resize",
        title = "Resize",
        description = "Resize tools are coming in a future update.",
        icon = AlphaPicsIcon.RESIZE,
        accent = AlphaPicsAccent.CYAN,
        badge = "SOON"
    )
    val Convert = AlphaPicsFeature(
        id = "convert",
        title = "Format Convert",
        description = "Format conversion is coming in a future update.",
        icon = AlphaPicsIcon.CONVERT,
        accent = AlphaPicsAccent.VIOLET,
        badge = "SOON"
    )

    val HomeFeatures = listOf(
        AiEnhance,
        RestorePhoto,
        Upscale,
        RemoveBackground
    )

    val PhotoEnhancement = listOf(
        AiEnhance,
        FaceEnhance,
        RestorePhoto,
        AiRetouch,
        Unblur,
        Upscale,
        RemoveBackground,
        MagicEraser,
        LowLightFix,
        Colorize,
        Relight,
        BackgroundReplace,
        Retouch,
        EditPhoto,
        Resize,
        Convert
    )

    private val all = PhotoEnhancement.distinctBy { it.id }

    fun find(id: String?): AlphaPicsFeature? = all.firstOrNull { it.id == id }
}
