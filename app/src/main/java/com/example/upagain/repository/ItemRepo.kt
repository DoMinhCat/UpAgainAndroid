package com.example.upagain.repository

import com.example.upagain.api.ApiService
import com.example.upagain.model.item.MyItemsResponse
import com.example.upagain.util.json.parseErrorMessage
import retrofit2.awaitResponse

class ItemRepo(private val apiService: ApiService) {
    suspend fun getMyItems(page: Int? = 1, limit: Int? = 100): Result<MyItemsResponse> {
        return try {
            val response = apiService.getMyItems(page, limit).awaitResponse()
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

    suspend fun getAllItems(options: Map<String, String>): Result<MyItemsResponse> {
        return try {
            val response = apiService.getAllItems(options).awaitResponse()
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