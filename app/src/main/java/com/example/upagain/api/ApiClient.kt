package com.example.upagain.api

import android.content.Context
import android.content.Intent
import com.example.upagain.BuildConfig
import com.example.upagain.feat.auth.LoginActivity
import com.example.upagain.feat.error.ErrorActivity
import com.example.upagain.util.auth.SessionManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
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

    // Bare client — only used for /refresh and /login, no interceptors
    private val authHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .build()
    }

    private val authRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(authHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private val authApiService: ApiService by lazy {
        authRetrofit.create(ApiService::class.java)
    }

    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(Interceptor { chain ->
                val originalRequest = chain.request()
                val path = originalRequest.url.encodedPath

                if (path == Endpoints.REFRESH || path == Endpoints.LOGIN) {
                    return@Interceptor chain.proceed(originalRequest)
                }

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
                    404 -> navigateToActivity(ErrorActivity::class.java, statusCode = 404)
                    500 -> navigateToActivity(ErrorActivity::class.java, statusCode = 500)
                    401 -> {
                        val path = request.url.encodedPath
                        if (path != Endpoints.REFRESH && path != Endpoints.LOGIN) {
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
                                    // Return a synthetic 401 — the original body is closed,
                                    // Retrofit must never see it again
                                    return@Interceptor okhttp3.Response.Builder()
                                        .request(request)
                                        .protocol(okhttp3.Protocol.HTTP_1_1)
                                        .code(401)
                                        .message("Unauthorized")
                                        .body("".toResponseBody(null))
                                        .build()
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

    // Now uses authApiService — completely separate from the httpClient init chain
    private fun refreshAccessToken(): String? {
        return try {
            val response = authApiService.refresh().execute()
            val newToken = response.body()?.token
            if (response.isSuccessful && newToken != null) {
                SessionManager.saveUserSession(newToken)
                newToken
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun handleLogout() {
        SessionManager.clearSession()
        cookieJar.clear()
        navigateToActivity(LoginActivity::class.java, clearStack = true)
    }

    private fun navigateToActivity(activityClass: Class<*>, clearStack: Boolean = false, statusCode: Int? = null) {
        val intent = Intent(appContext, activityClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (clearStack) addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            if (statusCode != null) putExtra("EXTRA_ERROR_CODE", statusCode)
        }
        appContext.startActivity(intent)
    }
}