package com.example.upagain.repository

import com.example.upagain.api.ApiService
import com.example.upagain.model.AccountDetailsResponse
import com.example.upagain.model.AccountUpdateRequest
import com.example.upagain.util.json.parseErrorMessage
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

    suspend fun updateAccount(idAccount: Int, request: AccountUpdateRequest): Result<Unit> {
        return try {
            val response = apiService.updateAccount(idAccount, request).awaitResponse()

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errMessage = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(idAccount: Int): Result<Unit> {
        return try {
            val response = apiService.deleteAccount(idAccount).awaitResponse()

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errMessage = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}