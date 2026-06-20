package com.example.upagain.repository

import android.content.Context
import com.example.upagain.api.ApiService
import com.example.upagain.model.post.PostFilter
import com.example.upagain.model.post.PostPaginationResponse
import retrofit2.HttpException
import retrofit2.awaitResponse

class PostRepo(private val apiService: ApiService, private val context: Context) {

    suspend fun getAllPosts(filters: PostFilter): Result<PostPaginationResponse> {
        return try {
            val response = apiService.getAllPosts(filters.toQueryMap()).awaitResponse()
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