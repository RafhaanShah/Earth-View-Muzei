package com.rafapps.earthviewformuzei

import android.content.Context
import android.net.Uri
import androidx.work.*
import com.android.volley.Request.Method.GET
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.RequestFuture
import com.android.volley.toolbox.Volley
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.ProviderContract
import org.json.JSONObject
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class EarthViewWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    companion object {

        const val JSON_URL = "https://www.gstatic.com/prettyearth/assets/data/v3/"
        const val IMAGE_URL = "https://earthview.withgoogle.com/download/"
        const val WEB_URL = "https://g.co/ev/"
        const val JSON = ".json"
        const val JPG = ".jpg"

        internal fun enqueueLoad(context: Context) {
            val workManager = WorkManager.getInstance(context)
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
        val attribution = response.optString("attribution")
        val geocode = response.optJSONObject("geocode")

        val optCountry = response.optString("country")
        val country =
            optCountry.ifEmpty { geocode?.optString("country") ?: "" }

        val optRegion = response.optString("region")
        val region =
            optRegion.ifEmpty {
                listOf(
                    geocode?.optString("locality") ?: "",
                    geocode?.optString("administrative_area_level_2") ?: "",
                    geocode?.optString("administrative_area_level_1") ?: ""
                ).firstOrNull { it.isNotEmpty() } ?: ""
            }

        return Artwork.Builder()
            .attribution(attribution)
            .byline(region)
            .metadata(imgNum)
            .title(country)
            .token(imgNum)
            .persistentUri(Uri.parse(IMAGE_URL + imgNum + JPG))
            .webUri(Uri.parse(WEB_URL + imgNum))
            .build()
    }

    override fun doWork(): Result {
        val imgNum = EarthViewImagePicker.getNewImageNumber(applicationContext)
        val future = RequestFuture.newFuture<JSONObject>()
        val request = JsonObjectRequest(
            GET,
            JSON_URL + imgNum + JSON,
            null,
            future,
            future
        )

        Volley.newRequestQueue(applicationContext).add(request)

        return try {
            val response = future.get(60, TimeUnit.SECONDS)

            ProviderContract.getProviderClient(applicationContext, EarthViewArtProvider::class.java)
                .addArtwork(getArtwork(response, imgNum))
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
