package com.example.upagain.repository

import com.example.upagain.api.ApiService
import com.example.upagain.model.LoginRequest
import com.example.upagain.model.TokenResponse
import com.example.upagain.util.json.parseErrorMessage
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
                val errMessage = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
