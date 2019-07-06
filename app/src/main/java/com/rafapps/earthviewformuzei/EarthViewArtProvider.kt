package com.rafapps.earthviewformuzei

import android.util.Log
import com.google.android.apps.muzei.api.provider.MuzeiArtProvider

class EarthViewArtProvider : MuzeiArtProvider() {
    override fun onLoadRequested(initial: Boolean) {
        Log.v("EarthView", "Load Requested")
        EarthViewWorker.enqueueLoad()
    }
}