package com.rafapps.earthviewformuzei

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.android.volley.Request.Method.GET
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.RequestFuture
import com.android.volley.toolbox.Volley
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.ProviderContract
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class EarthViewWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    companion object {

        private val TAG = this::class.simpleName

        const val JSON_URL = "https://www.gstatic.com/prettyearth/assets/data/v3/"
        const val JSON = ".json"
        const val JPG = ".jpg"
        const val IMAGE = "image"

        internal fun enqueueLoad(context: Context) {
            Log.d(TAG, "enqueueLoad")
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

    private fun getArtwork(response: JSONObject, imgNum: String): Artwork? {
        Log.d(TAG, "getArtwork $imgNum, response length ${response.length()}")
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

        val imageData = response.optString("dataUri")
        val file = saveImage(imgNum, imageData) ?: return null

        return Artwork.Builder()
            .attribution(attribution)
            .byline(region)
            .metadata(imgNum)
            .title(country)
            .token(imgNum)
            .persistentUri(Uri.fromFile(file))
            .build()
    }

    override fun doWork(): Result {
        Log.d(TAG, "doWork")
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
            val artwork = getArtwork(response, imgNum) ?: return Result.failure()

            ProviderContract
                .getProviderClient(applicationContext, EarthViewArtProvider::class.java)
                .addArtwork(artwork)
            EarthViewCacheManager.clearCache(applicationContext)

            Log.d(TAG, "doWork success")
            Result.success()

        } catch (e: InterruptedException) {
            Result.retry()

        } catch (e: TimeoutException) {
            Result.retry()

        } catch (e: ExecutionException) {
            Log.d(TAG, "doWork failure")
            Result.failure()
        }
    }

    private fun saveImage(id: String, imageData: String): File? {
        val base64Data = imageData.split(",").getOrNull(1) ?: return null
        val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)

        val cacheDir = applicationContext.cacheDir
        val file = File(cacheDir, "${IMAGE}-${id}${JPG}")

        runCatching {
            FileOutputStream(file).use { outputStream ->
                outputStream.write(imageBytes)
            }
        }.onFailure {
            Log.e(TAG, "saveImage", it)
            return null
        }

        Log.d(TAG, "saveImage ${file.path}")
        return file
    }
}
