package com.example.upagain.repository

import com.example.upagain.api.ApiService
import com.example.upagain.model.account.AccountDetailsResponse
import com.example.upagain.model.finance.FinanceKeyEnum
import com.example.upagain.util.json.parseErrorMessage
import retrofit2.awaitResponse

class FinanceRepo(private val apiService: ApiService) {

    suspend fun getFinanceSetting(key: FinanceKeyEnum): Result<Float> {
        return try {
            val response = apiService.getFinanceSetting(key).awaitResponse()
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