package com.example.upagain.api

import com.example.upagain.model.AccountDetailsResponse
import com.example.upagain.model.AccountUpdateRequest
import com.example.upagain.model.TokenResponse
import com.example.upagain.model.LoginRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    // AUTH
    @POST(Endpoints.LOGIN)
    fun login(@Body request: LoginRequest): Call<TokenResponse>
    @POST(Endpoints.REFRESH)
    fun refresh(): Call<TokenResponse>

    // ACCOUNT
    @GET(Endpoints.ACCOUNT_DETAILS)
    fun getAccountDetails(@Path("id") id: Int): Call<AccountDetailsResponse>
    @PATCH(Endpoints.ACCOUNT_UPDATE)
    fun updateAccount(@Path("id") id: Int, @Body request: AccountUpdateRequest): Call<Unit>
    @DELETE(Endpoints.ACCOUNT_DETAILS)
    fun deleteAccount(@Path("id") id: Int): Call<Unit>
}
