package com.example.upagain.repository

import com.example.upagain.api.ApiService
import com.example.upagain.model.LoginRequest
import com.example.upagain.model.TokenResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class AuthRepository(private val apiService: ApiService) {
    // suspend allow running in coroutine
    suspend fun loginUser(request: LoginRequest): Result<TokenResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<TokenResponse> = apiService.login(request)
                
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