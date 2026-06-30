package com.example.upagain.util.bin

import android.util.Log
import androidx.core.net.toUri
import com.example.upagain.BuildConfig
import com.example.upagain.api.Endpoints

const val FALL_BACK_IMAGE_URL = "https://upcycleconnect.org/banners/guest-banner1-dark.png"

enum class ImageType() {
    AVATAR,
    MEDIA
}
fun buildImageUrl(imagePath: String?, type: ImageType): String {
    if (imagePath == null) {
        if (type == ImageType.MEDIA) {
            return FALL_BACK_IMAGE_URL
        }
        return ""
    }
    val sanitizedBase = BuildConfig.API_BASE_URL.removeSuffix("/")
    val imageUrl = sanitizedBase.toUri()
        .buildUpon()
        .appendEncodedPath(Endpoints.IMAGES)
        .appendQueryParameter("path", imagePath)
        .build()
        .toString()
    return imageUrl
}