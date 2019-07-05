package com.rafapps.earthviewformuzei

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.*
import com.android.volley.Request.Method
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.ProviderContract


class EarthViewWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {


    companion object {
        private const val TAG = "UnsplashExample"

        internal fun enqueueLoad() {
            val workManager = WorkManager.getInstance()
            workManager.enqueue(
                OneTimeWorkRequestBuilder<EarthViewWorker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()
            )
        }
    }

    override fun doWork(): Result {
        Log.v("EarthView", "Do Work")

        val queue = Volley.newRequestQueue(applicationContext)
        val url = "https://www.gstatic.com/prettyearth/assets/data/v2/1003.json"


        val jsonObjectRequest = JsonObjectRequest(

            Method.GET, url, null,
            Response.Listener { response ->
                // Display the first 500 characters of the response string.
                Log.v("EarthView Response", response.getString("attribution"))

                val providerClient = ProviderContract.getProviderClient(
                    applicationContext, "com.rafapps.earthviewformuzei"
                )

                val art = Artwork.Builder()
                    .webUri(Uri.parse("https://www.gstatic.com/prettyearth/assets/full/1003.jpg"))
                    .title("Example image")
                    .byline("Unknown person, c. 1980")
                    .build()

                providerClient.addArtwork(art)


            },
            Response.ErrorListener { val err = "That didn't work!" })

        queue.add(jsonObjectRequest)

        return Result.success()

    }
}