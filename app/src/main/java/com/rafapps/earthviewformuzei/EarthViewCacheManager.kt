package com.rafapps.earthviewformuzei

import android.content.Context
import java.io.File

class EarthViewCacheManager {

    companion object {

        private const val maxAge: Long = 604800000

        fun clearCache(context: Context) {
            for (f in getCachedFiles(context, maxAge))
                f.delete()
        }

        private fun getCachedFiles(context: Context, maxAge: Long): List<File> {
            val dirs = context.cacheDir.listFiles { f -> f.isDirectory }
            val files = mutableListOf<File>()
            for (d in dirs) {
                files.addAll(d.listFiles { f -> !f.isDirectory && f.lastModified() < System.currentTimeMillis() - maxAge })
            }
            return files
        }
    }
}