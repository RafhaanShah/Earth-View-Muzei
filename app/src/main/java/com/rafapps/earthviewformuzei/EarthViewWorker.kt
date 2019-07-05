package com.rafapps.earthviewformuzei

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.RequestFuture
import com.android.volley.toolbox.Volley
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.ProviderContract
import org.json.JSONObject
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit


class EarthViewWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {


    companion object {
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

        val jsonURL = "https://www.gstatic.com/prettyearth/assets/data/v2/2323.json"
        val imgURL = "https://www.gstatic.com/prettyearth/assets/full/2323.jpg"

        val future = RequestFuture.newFuture<JSONObject>()
        val request = JsonObjectRequest(jsonURL, null, future, future)

        Volley.newRequestQueue(applicationContext).add(request)

        return try {
            val response = future.get(30, TimeUnit.SECONDS)
            val providerClient = ProviderContract.getProviderClient(
                applicationContext, "com.rafapps.earthviewformuzei"
            )

            val art = Artwork.Builder()
                .webUri(Uri.parse(imgURL))
                .title(response.getJSONObject("geocode").getString("country"))
                .byline(response.getString("attribution"))
                .build()

            providerClient.addArtwork(art)
            Result.success()
        } catch (e: InterruptedException) {
            Result.retry()

        } catch (e: ExecutionException) {
            Result.failure()

        }
    }
}