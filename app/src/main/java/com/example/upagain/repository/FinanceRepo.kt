package com.example.upagain.repository

import com.example.upagain.api.ApiService
import com.example.upagain.model.finance.FinanceKeyEnum
import com.example.upagain.util.json.parseErrorMessage
import retrofit2.awaitResponse

class FinanceRepo(private val apiService: ApiService) {

    suspend fun getFinanceSetting(key: FinanceKeyEnum): Result<Double> {
        return try {
            val response = apiService.getFinanceSetting(key).awaitResponse()
            val body = response.body()

            if (response.isSuccessful && body != null) {
                val rawString = body.string().trim()
                val price = rawString.toDoubleOrNull() ?: 0.0
                Result.success(price)
            } else {
                val errMessage = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}