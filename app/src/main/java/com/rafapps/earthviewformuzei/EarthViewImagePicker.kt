package com.rafapps.earthviewformuzei

import android.content.Context
import android.text.TextUtils
import androidx.preference.PreferenceManager
import kotlin.random.Random

class EarthViewImagePicker {

    companion object {

        private const val maxNumberOfPastImages = 100
        private const val PREFERENCE_PREVIOUS_IMAGES = "PREFERENCE_PREVIOUS_IMAGES"

        fun getNewImageNumber(context: Context): String {
            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
            val stringList = sharedPrefs.getString(PREFERENCE_PREVIOUS_IMAGES, "")
            var newStringList = ""
            var img: String

            if (TextUtils.isEmpty(stringList)) {
                img = imageIDs[Random.nextInt(imageIDs.size)]
                newStringList += img

            } else {
                val list = mutableListOf<String>()
                val set = mutableListOf<String>()
                list.addAll(TextUtils.split(stringList, ";"))
                set.addAll(list)
                img = imageIDs[Random.nextInt(imageIDs.size)]

                while (set.contains(img)) {
                    img = imageIDs[Random.nextInt(imageIDs.size)]
                }

                if (list.size >= maxNumberOfPastImages) {
                    list.removeAt(0)
                }

                list.add(img)
                newStringList = TextUtils.join(";", list)
            }

            sharedPrefs.edit().putString(PREFERENCE_PREVIOUS_IMAGES, newStringList).apply()
            return img
        }
    }
}
