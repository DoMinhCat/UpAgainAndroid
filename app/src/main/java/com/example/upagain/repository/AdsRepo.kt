package com.example.upagain.repository

import com.example.upagain.api.ApiService
import com.example.upagain.model.ads.CreateAdsRequest
import com.example.upagain.model.ads.CreateAdsResponse
import com.example.upagain.util.json.parseErrorMessage
import retrofit2.awaitResponse

class AdsRepo(private val apiService: ApiService) {

    suspend fun createAds(
        id: Int,
        request: CreateAdsRequest
    ): Result<CreateAdsResponse> {
        return try {
            val response = apiService.createAds(id, request).awaitResponse()
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