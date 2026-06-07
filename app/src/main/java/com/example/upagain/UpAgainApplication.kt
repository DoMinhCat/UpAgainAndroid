package com.example.upagain

import android.app.Application
import com.example.upagain.api.ApiClient

class UpAgainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.initialize(this)
    }
}
