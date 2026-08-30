package com.example.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date
import kotlin.coroutines.coroutineContext
import kotlin.math.max

/** Bounded source metadata/EXIF inspection; it never decodes a full-resolution bitmap. */
object PhotoMetadataReader {
    suspend fun read(context: Context, uri: Uri): Result<PhotoMetadata> = withContext(Dispatchers.IO) {
        runCatching {
            coroutineContext.ensureActive()
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val boundsStream = resolver.openInputStream(uri) ?: error("Unable to read this photo")
            boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "This file is not a readable image" }

            val exif = try {
                resolver.openInputStream(uri)?.use(::ExifInterface)
            } catch (_: Exception) {
                null
            }
            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL
            val swapsAxes = orientation in setOf(
                ExifInterface.ORIENTATION_ROTATE_90,
                ExifInterface.ORIENTATION_ROTATE_270,
                ExifInterface.ORIENTATION_TRANSPOSE,
                ExifInterface.ORIENTATION_TRANSVERSE
            )
            val orientedWidth = if (swapsAxes) bounds.outHeight else bounds.outWidth
            val orientedHeight = if (swapsAxes) bounds.outWidth else bounds.outHeight
            val openable = queryOpenable(context, uri)
            val date = exif?.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif?.getAttribute(ExifInterface.TAG_DATETIME)
                ?: queryDate(context, uri)

            coroutineContext.ensureActive()
            val sample = sampleForPreview(bounds.outWidth, bounds.outHeight, 512)
            val preview = resolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(
                    input,
                    null,
                    BitmapFactory.Options().apply {
                        inSampleSize = sample
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                )
            }
            val transparency = preview?.let(::containsTransparency) ?: false
            val colorDescription = preview?.let(::describeColor) ?: "Unknown color profile"
            preview?.recycle()

            PhotoMetadata(
                displayName = openable.first ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Photo",
                mimeType = resolver.getType(uri) ?: bounds.outMimeType ?: "image/unknown",
                formatLabel = formatLabel(resolver.getType(uri) ?: bounds.outMimeType),
                sizeBytes = openable.second,
                rawWidth = bounds.outWidth,
                rawHeight = bounds.outHeight,
                width = orientedWidth,
                height = orientedHeight,
                orientationLabel = orientationLabel(orientation),
                orientationValue = orientation,
                dateLabel = date,
                hasTransparency = transparency,
                colorDescription = colorDescription,
                exif = readExifRows(exif)
            )
        }.also { result ->
            (result.exceptionOrNull() as? CancellationException)?.let { throw it }
        }
    }

    private fun queryOpenable(context: Context, uri: Uri): Pair<String?, Long?> {
        if (uri.scheme == "file") {
            val file = uri.path?.let(::File)
            return file?.name to file?.takeIf(File::exists)?.length()
        }
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null to null
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                val name = nameIndex.takeIf { it >= 0 }?.let(cursor::getString)
                val size = sizeIndex.takeIf { it >= 0 && !cursor.isNull(it) }?.let(cursor::getLong)
                name to size
            } ?: (null to null)
        } catch (_: Exception) {
            null to null
        }
    }

    private fun queryDate(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") {
            val timestamp = uri.path?.let(::File)?.takeIf(File::exists)?.lastModified() ?: 0L
            return timestamp.takeIf { it > 0 }?.let { DateFormat.getDateTimeInstance().format(Date(it)) }
        }
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.Media.DATE_TAKEN, MediaStore.MediaColumns.DATE_MODIFIED),
                null,
                null,
                null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val takenIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                val modifiedIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                val taken = takenIndex.takeIf { it >= 0 && !cursor.isNull(it) }?.let(cursor::getLong)
                val modified = modifiedIndex.takeIf { it >= 0 && !cursor.isNull(it) }
                    ?.let(cursor::getLong)?.times(1000)
                (taken ?: modified)?.takeIf { it > 0 }?.let {
                    DateFormat.getDateTimeInstance().format(Date(it))
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun readExifRows(exif: ExifInterface?): Map<String, String> {
        if (exif == null) return emptyMap()
        val tags = listOf(
            "Camera make" to ExifInterface.TAG_MAKE,
            "Camera model" to ExifInterface.TAG_MODEL,
            "Lens" to ExifInterface.TAG_LENS_MODEL,
            "Captured" to ExifInterface.TAG_DATETIME_ORIGINAL,
            "Exposure" to ExifInterface.TAG_EXPOSURE_TIME,
            "Aperture" to ExifInterface.TAG_F_NUMBER,
            "ISO" to ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
            "Focal length" to ExifInterface.TAG_FOCAL_LENGTH,
            "Flash" to ExifInterface.TAG_FLASH,
            "White balance" to ExifInterface.TAG_WHITE_BALANCE,
            "Artist" to ExifInterface.TAG_ARTIST,
            "Copyright" to ExifInterface.TAG_COPYRIGHT,
            "Description" to ExifInterface.TAG_IMAGE_DESCRIPTION
        )
        val rows = linkedMapOf<String, String>()
        tags.forEach { (label, tag) ->
            exif.getAttribute(tag)?.takeIf(String::isNotBlank)?.let { rows[label] = it }
        }
        val hasLocation = runCatching { exif.latLong != null }.getOrDefault(false)
        if (hasLocation) rows["GPS location"] = "Present (coordinates hidden)"
        return rows
    }

    private fun containsTransparency(bitmap: Bitmap): Boolean {
        if (!bitmap.hasAlpha()) return false
        val step = max(1, max(bitmap.width, bitmap.height) / 256)
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                if ((bitmap.getPixel(x, y) ushr 24) < 255) return true
                x += step
            }
            y += step
        }
        return false
    }

    private fun describeColor(bitmap: Bitmap): String {
        val profile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bitmap.colorSpace?.name ?: "Unknown"
        } else {
            "sRGB-compatible"
        }
        val gamut = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.colorSpace?.isWideGamut == true) {
            "wide gamut"
        } else {
            "standard gamut"
        }
        return "$profile · $gamut · ${bitmap.config ?: Bitmap.Config.ARGB_8888}"
    }

    private fun sampleForPreview(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        while (max(width, height) / (sample * 2) >= maxEdge) sample *= 2
        return sample
    }

    private fun formatLabel(mimeType: String?): String = when (mimeType?.lowercase()) {
        "image/jpeg", "image/jpg" -> "JPEG"
        "image/png" -> "PNG"
        "image/webp" -> "WebP"
        "image/avif" -> "AVIF"
        "image/heic", "image/heif" -> "HEIF"
        else -> mimeType?.substringAfter('/')?.uppercase() ?: "Unknown"
    }

    private fun orientationLabel(value: Int): String = when (value) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> "Mirrored horizontally"
        ExifInterface.ORIENTATION_ROTATE_180 -> "Rotated 180°"
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> "Mirrored vertically"
        ExifInterface.ORIENTATION_TRANSPOSE -> "Transposed 90°"
        ExifInterface.ORIENTATION_ROTATE_90 -> "Rotated 90°"
        ExifInterface.ORIENTATION_TRANSVERSE -> "Transverse 270°"
        ExifInterface.ORIENTATION_ROTATE_270 -> "Rotated 270°"
        else -> "Normal"
    }
}
