package com.rafapps.earthviewformuzei

import android.content.Context
import android.util.Log

class EarthViewCacheManager {

    companion object {

        private const val maxAge: Long = 604800000 // 7 days

        fun clearCache(context: Context) {
            try {
                context.cacheDir.listFiles { a -> a.isDirectory }
                    ?.forEach { b ->
                        b?.listFiles { c -> c.isFile && c.lastModified() < (System.currentTimeMillis() - maxAge) }
                            ?.forEach { d -> d.delete() }
                    }
            } catch (e: Exception) {
                Log.d("EarthViewCacheManager", e.message ?: "Exception when clearing cache")
            }
        }
    }
}