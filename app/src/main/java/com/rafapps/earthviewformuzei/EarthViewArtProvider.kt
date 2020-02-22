package com.rafapps.earthviewformuzei

import com.google.android.apps.muzei.api.provider.MuzeiArtProvider

class EarthViewArtProvider : MuzeiArtProvider() {
    override fun onLoadRequested(initial: Boolean) {
        val context = context ?: return
        EarthViewWorker.enqueueLoad(context)
    }
}