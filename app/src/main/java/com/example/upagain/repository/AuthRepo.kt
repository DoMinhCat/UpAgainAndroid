package com.example.upagain.repository

import com.example.upagain.api.ApiService
import com.example.upagain.model.LoginRequest
import com.example.upagain.model.TokenResponse
import retrofit2.Call
import retrofit2.HttpException
import retrofit2.awaitResponse

class AuthRepository(private val apiService: ApiService) {

    suspend fun login(request: LoginRequest): Result<TokenResponse> {
        return try {
            val response = apiService.login(request).awaitResponse()
            val body = response.body()

            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(HttpException(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
