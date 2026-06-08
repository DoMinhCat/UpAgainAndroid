package com.example.upagain.repository

import com.example.upagain.api.ApiService
import com.example.upagain.model.AccountDetailsResponse
import retrofit2.HttpException
import retrofit2.awaitResponse

class AccountRepo(private val apiService: ApiService) {

    suspend fun getAccountDetails(idAccount: Int): Result<AccountDetailsResponse> {
        return try {
            val response = apiService.getAccountDetails(idAccount).awaitResponse()
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