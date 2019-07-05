package com.rafapps.earthviewformuzei

import android.util.Log
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.MuzeiArtProvider
import java.io.InputStream
import java.net.URL

class EarthViewArtProvider : MuzeiArtProvider() {
    override fun onLoadRequested(initial: Boolean) {
        Log.v("EarthView", "Load Requested")
        EarthViewWorker.enqueueLoad()
    }

    override fun openFile(artwork: Artwork): InputStream {
        return URL(artwork.webUri.toString()).openStream()

    }
}