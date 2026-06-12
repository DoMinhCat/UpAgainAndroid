package com.example.upagain

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.upagain.api.ApiClient

class UpAgainApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        ApiClient.initialize(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(ApiClient.getHttpClient())
            .build()
    }
}
