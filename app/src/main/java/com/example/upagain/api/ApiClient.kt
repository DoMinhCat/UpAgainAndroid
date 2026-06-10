package com.example.upagain.api

import android.content.Context
import android.content.Intent
import com.example.upagain.BuildConfig
import com.example.upagain.feat.auth.LoginActivity
import com.example.upagain.feat.error.InternalServerErrorActivity
import com.example.upagain.feat.error.NotFoundActivity
import com.example.upagain.util.auth.SessionManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = BuildConfig.API_BASE_URL

    private lateinit var appContext: Context
    private lateinit var cookieJar: PersistentCookieJar

    private val gson: Gson by lazy {
        GsonBuilder().setStrictness(Strictness.LENIENT).create()
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
        cookieJar = PersistentCookieJar(appContext)
    }

    private val httpClient: OkHttpClient by lazy {
        // AUTO INJECT JWT IN SHAREDPREF INTO OUTGOING REQUESTS
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(Interceptor { chain ->
                val originalRequest = chain.request()
                val token = SessionManager.token
                val newRequest = if (!token.isNullOrEmpty()) {
                    originalRequest.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    originalRequest
                }
                chain.proceed(newRequest)
            })
            .addInterceptor(Interceptor { chain ->
                val request = chain.request()
                var response = chain.proceed(request)

                when (response.code) {
                    404 -> {
                        navigateToActivity(NotFoundActivity::class.java)
                    }
                    500 -> {
                        navigateToActivity(InternalServerErrorActivity::class.java)
                    }
                    401 -> {
                        val path = request.url.encodedPath
                        val isRefresh = path == Endpoints.REFRESH
                        val isLogin = path == Endpoints.LOGIN

                        if (!isRefresh && !isLogin) {
                            val alreadyRetried = request.header("X-Retry") != null
                            if (!alreadyRetried) {
                                response.close()
                                val newToken = refreshAccessToken()
                                if (newToken != null) {
                                    val retryRequest = request.newBuilder()
                                        .header("Authorization", "Bearer $newToken")
                                        .header("X-Retry", "true")
                                        .build()
                                    response = chain.proceed(retryRequest)
                                } else {
                                    handleLogout()
                                }
                            }
                        }
                    }
                }
                response
            })
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    private fun refreshAccessToken(): String? {
        try {
            val response = apiService.refresh().execute()
            val newToken = response.body()?.token

            if (response.isSuccessful && newToken != null) {
                SessionManager.saveUserSession(newToken)
                return newToken
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun handleLogout() {
        SessionManager.clearSession()
        cookieJar.clear()
        navigateToActivity(LoginActivity::class.java, clearStack = true)
    }

    private fun navigateToActivity(activityClass: Class<*>, clearStack: Boolean = false) {
        val intent = Intent(appContext, activityClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (clearStack) {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        }
        appContext.startActivity(intent)
    }
}