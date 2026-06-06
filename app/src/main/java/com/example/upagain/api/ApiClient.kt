package com.example.upagain.api

import com.google.gson.Gson
import com.google.gson.Strictness
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.GsonBuilder
import com.example.upagain.api.ApiService
import com.example.upagain.BuildConfig

object ApiClient {
    private const val BASE_URL = BuildConfig.API_BASE_URL

    private val gson: Gson by lazy {
        GsonBuilder().setStrictness(Strictness.LENIENT).create()
    }
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder().build()
    }
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder().baseUrl(BASE_URL).client(httpClient).addConverterFactory(GsonConverterFactory.create(gson)).build()
    }
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}