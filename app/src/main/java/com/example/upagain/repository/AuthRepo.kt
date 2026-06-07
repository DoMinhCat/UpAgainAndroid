package com.example.upagain.repository

import com.example.upagain.api.ApiClient.apiService
import com.example.upagain.api.ApiService
import com.example.upagain.model.LoginRequest
import com.example.upagain.model.TokenResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.awaitResponse

class AuthRepository(private val apiService: ApiService) {

    suspend fun login(request: LoginRequest): Result<TokenResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val call: Call<TokenResponse> = apiService.login(request)
                val response = call.awaitResponse()

                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Login failed: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}