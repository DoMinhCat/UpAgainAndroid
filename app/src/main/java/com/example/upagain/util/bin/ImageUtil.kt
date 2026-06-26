package com.example.upagain.util.bin

import android.util.Log
import androidx.core.net.toUri
import com.example.upagain.BuildConfig
import com.example.upagain.api.Endpoints

fun buildImageUrl(imagePath: String): String {
    val sanitizedBase = BuildConfig.API_BASE_URL.removeSuffix("/")
    val imageUrl = sanitizedBase.toUri()
        .buildUpon()
        .appendEncodedPath(Endpoints.IMAGES)
        .appendQueryParameter("path", imagePath)
        .build()
        .toString()
    Log.v("buildImageUrl", "Image URL: $imageUrl")
    return imageUrl
}