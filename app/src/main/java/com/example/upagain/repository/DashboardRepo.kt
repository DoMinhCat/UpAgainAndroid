package com.example.upagain.repository

import com.example.upagain.api.ApiService
import com.example.upagain.model.dashboard.ProAnalyticsResponse
import com.example.upagain.util.json.parseErrorMessage
import retrofit2.awaitResponse

class DashboardRepo(private val apiService: ApiService) {

    suspend fun getProAnalytics(idAccount: Int): Result<ProAnalyticsResponse> {
        return try {
            val response = apiService.getProAnalytics(idAccount).awaitResponse()
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
