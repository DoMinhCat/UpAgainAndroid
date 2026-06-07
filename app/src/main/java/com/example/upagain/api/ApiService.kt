package com.example.upagain.api

import com.example.upagain.model.AuthResponse
import retrofit2.Call
import retrofit2.http.POST

interface ApiService {
    @POST(Endpoints.REFRESH)
    fun refresh(): Call<TokenResponse>
}
