package com.example.upagain.api

import com.example.upagain.model.TokenResponse
import com.example.upagain.model.LoginRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    // AUTH
    @POST(Endpoints.LOGIN)
    fun login(@Body request: LoginRequest): Call<TokenResponse>
    
    @POST(Endpoints.REFRESH)
    fun refresh(): Call<TokenResponse>
}
