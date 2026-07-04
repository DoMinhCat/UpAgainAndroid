package com.example.upagain.repository

import com.example.upagain.api.ApiService
import com.example.upagain.model.comment.CreateCommentRequest
import com.example.upagain.model.comment.CommentDetailsResponse
import com.example.upagain.model.comment.CommentPaginationRequest
import com.example.upagain.model.comment.CommentPaginationResponse
import com.example.upagain.model.comment.LikeCommentResponse
import retrofit2.HttpException
import retrofit2.awaitResponse

class CommentRepo(private val apiService: ApiService) {
    suspend fun getPostComments(
        id: Int,
        request: CommentPaginationRequest
    ): Result<CommentPaginationResponse> {
        return try {
            val response = apiService.getPostComments(id, request.toQueryMap()).awaitResponse()
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

    suspend fun createComment(idPost: Int, request: CreateCommentRequest): Result<CommentDetailsResponse> {
        return try {
            val response = apiService.createComment(idPost, request).awaitResponse()
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

    suspend fun likeComment(idCmt: Int): Result<LikeCommentResponse> {
        return try {
            val response = apiService.likeComment(idCmt).awaitResponse()
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

    suspend fun deleteComment(idCmt: Int): Result<Unit> {
        return try {
            val response = apiService.deleteComment(idCmt).awaitResponse()
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