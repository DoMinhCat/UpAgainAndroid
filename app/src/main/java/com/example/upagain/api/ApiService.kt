package com.example.upagain.api

import com.example.upagain.model.AccountDetailsResponse
import com.example.upagain.model.TokenResponse
import com.example.upagain.model.LoginRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    // AUTH
    @POST(Endpoints.LOGIN)
    fun login(@Body request: LoginRequest): Call<TokenResponse>
    @POST(Endpoints.REFRESH)
    fun refresh(): Call<TokenResponse>

    // ACCOUNT
    @GET(Endpoints.ACCOUNT_DETAILS)
    fun getAccountDetails(@Path("id") id: Int): Call<AccountDetailsResponse>
    @DELETE(Endpoints.ACCOUNT_DETAILS)
    fun deleteAccount(@Path("id") id: Int): Call<Nothing>

}
