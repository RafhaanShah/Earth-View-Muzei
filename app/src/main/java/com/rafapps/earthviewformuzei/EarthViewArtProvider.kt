package com.rafapps.earthviewformuzei

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.MuzeiArtProvider
import java.io.File


class EarthViewArtProvider : MuzeiArtProvider() {

    private val TAG = this::class.simpleName

    override fun onLoadRequested(initial: Boolean) {
        Log.d(TAG, "onLoadRequested: $initial")
        val context = context ?: return
        EarthViewWorker.enqueueLoad(context)
    }

    override fun getArtworkInfo(artwork: Artwork): PendingIntent? {
        Log.d(TAG, "getArtworkInfo: $artwork")
        val context = context ?: return null
        val path = artwork.persistentUri?.path ?: return null
        val file = File(path)
        val fileUri = FileProvider.getUriForFile(context,
            "${context.packageName}.cacheFileProvider",
            file)

        val intent = Intent()
        intent.setAction(Intent.ACTION_VIEW)
        intent.setData(fileUri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }
}
