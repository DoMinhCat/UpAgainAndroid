package com.example.upagain.util.bin

import androidx.core.net.toUri
import com.example.upagain.BuildConfig
import com.example.upagain.api.Endpoints

fun buildImageUrl(imagePath: String): String {
    val sanitizedBase = BuildConfig.API_BASE_URL.removeSuffix("/")
    val imageUrl = sanitizedBase.toUri()
        .buildUpon()
        .path(Endpoints.IMAGES)
        .appendQueryParameter("path", imagePath)
        .build()
        .toString()
    return imageUrl
}