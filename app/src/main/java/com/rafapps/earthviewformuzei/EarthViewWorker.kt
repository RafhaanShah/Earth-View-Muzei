package com.rafapps.earthviewformuzei

import android.content.Context
import android.net.Uri
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
        const val WEB_URL = "https://earthview.withgoogle.com/"
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

    private fun getArtwork(response: JSONObject, imgNum: String): Artwork {
        val attribution = response.getString("attribution")
        val geocode = response.getJSONObject("geocode")
        val country = geocode.optString("country", "Unknown")
        val locality = geocode.optString("locality", "")
        val area1 = geocode.optString("administrative_area_level_1", "")
        val area2 = geocode.optString("administrative_area_level_2", "")
        val area3 = geocode.optString("administrative_area_level_3", "")

        val byline = sequenceOf(locality, area1, area2, area3)
            .filter { it.isNotEmpty() }
            .joinToString(separator = ", ") { it }

        return Artwork.Builder()
            .persistentUri(Uri.parse(IMAGE_URL + imgNum + JPG))
            .webUri(Uri.parse(WEB_URL + imgNum))
            .title(country)
            .byline(byline)
            .attribution(attribution)
            .metadata(imgNum)
            .build()
    }

    override fun doWork(): Result {
        val imgNum = EarthViewImagePicker.getNewImageNumber(applicationContext)
        val future = RequestFuture.newFuture<JSONObject>()
        val request = JsonObjectRequest(JSON_URL + imgNum + JSON, null, future, future)

        Volley.newRequestQueue(applicationContext).add(request)

        return try {
            val response = future.get(60, TimeUnit.SECONDS)
            val providerClient = ProviderContract.getProviderClient(
                applicationContext, applicationContext.getString(R.string.package_name)
            )

            providerClient.addArtwork(getArtwork(response, imgNum))
            EarthViewCacheManager.clearCache(applicationContext)
            Result.success()

        } catch (e: InterruptedException) {
            Result.retry()

        } catch (e: TimeoutException) {
            Result.retry()

        } catch (e: ExecutionException) {
            Result.failure()
        }
    }
}