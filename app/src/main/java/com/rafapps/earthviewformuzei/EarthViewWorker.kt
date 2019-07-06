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
import java.util.concurrent.TimeoutException


class EarthViewWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    companion object {

        const val JSON_URL = "https://www.gstatic.com/prettyearth/assets/data/v2/"
        const val IMAGE_URL = "https://www.gstatic.com/prettyearth/assets/full/"
        const val JSON = ".json"
        const val JPG = ".jpg"

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

        val imgNum = EarthViewImagePicker.getImageNumber(applicationContext)
        val future = RequestFuture.newFuture<JSONObject>()
        val request = JsonObjectRequest(JSON_URL + imgNum + JSON, null, future, future)
        //request.setShouldCache(false)

        Log.v("EarthView", "Image Number: $imgNum")

        Volley.newRequestQueue(applicationContext).add(request)

        return try {
            val response = future.get(60, TimeUnit.SECONDS)
            val providerClient = ProviderContract.getProviderClient(
                applicationContext, applicationContext.getString(R.string.package_name)
            )

            val attribution = response.getString("attribution")
            val geocode = response.getJSONObject("geocode")
            val country = geocode.optString("country", "")
            val locality = geocode.optString("locality", "")
            val area = geocode.optString("administrative_area_level_1", "")

            val title = sequenceOf(locality, area, country)
                .filter { it.isNotEmpty() }
                .joinToString(separator = ", ") { it }

            Log.v("EarthView", "Area: $title")

            val art = Artwork.Builder()
                .persistentUri(Uri.parse(IMAGE_URL + imgNum + JPG))
                .webUri(Uri.parse(IMAGE_URL + imgNum + JPG))
                .title(title)
                .byline(attribution)
                .build()

            providerClient.addArtwork(art)

            Log.v("EarthView", "Success")
            Result.success()

        } catch (e: InterruptedException) {
            Log.v("EarthView Interrupted", e.toString())
            Result.retry()

        } catch (e: TimeoutException) {
            Log.v("EarthView Timeout", e.toString())
            Result.retry()

        } catch (e: ExecutionException) {
            Log.v("EarthView Execution", e.toString())
            Result.failure()

        }
    }
}