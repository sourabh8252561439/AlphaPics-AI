package com.example.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object AlphaPicsStorageManager {

    suspend fun getCacheSizeBytes(context: Context): Long = withContext(Dispatchers.IO) {
        var total = 0L
        try {
            val cacheDirs = listOfNotNull(
                context.cacheDir,
                context.externalCacheDir,
                File(context.cacheDir, "enhancement_results"),
                File(context.cacheDir, "editor_temp")
            )
            for (dir in cacheDirs) {
                if (dir.exists()) {
                    total += calculateFolderSize(dir)
                }
            }
        } catch (_: Exception) {}
        total
    }

    suspend fun clearCache(context: Context): Long = withContext(Dispatchers.IO) {
        val initialSize = getCacheSizeBytes(context)
        try {
            val dirsToClean = listOfNotNull(
                File(context.cacheDir, "enhancement_results"),
                File(context.cacheDir, "editor_temp"),
                File(context.cacheDir, "image_cache"),
                context.cacheDir
            )
            for (dir in dirsToClean) {
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles()?.forEach { file ->
                        try {
                            if (file.isDirectory) file.deleteRecursively() else file.delete()
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Exception) {}
        initialSize
    }

    private fun calculateFolderSize(dir: File): Long {
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (file in files) {
            size += if (file.isDirectory) calculateFolderSize(file) else file.length()
        }
        return size
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(java.util.Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(java.util.Locale.US, "%.1f MB", mb)
    }
}
