package com.example.upagain.repository

import com.example.upagain.api.ApiService
import com.example.upagain.model.comment.CommentPaginationRequest
import com.example.upagain.model.comment.CommentPaginationResponse
import com.example.upagain.model.post.LikePostResponse
import com.example.upagain.model.post.PostCreateRequest
import com.example.upagain.model.post.PostDetailsResponse
import com.example.upagain.model.post.PostPaginationRequest
import com.example.upagain.model.post.PostPaginationResponse
import com.example.upagain.model.post.SavePostResponse
import com.example.upagain.model.post.ViewPostResponse
import retrofit2.HttpException
import retrofit2.awaitResponse

class PostRepo(private val apiService: ApiService) {

    suspend fun getAllPosts(requestBody: PostPaginationRequest): Result<PostPaginationResponse> {
        return try {
            val response = apiService.getAllPosts(requestBody.toQueryMap()).awaitResponse()
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

    suspend fun getMyPosts(filters: PostPaginationRequest): Result<PostPaginationResponse> {
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

    suspend fun getSavedPosts(filters: PostPaginationRequest): Result<PostPaginationResponse> {
        return try {
            val response = apiService.getSavedPosts(filters.toQueryMap()).awaitResponse()
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

    suspend fun getPostDetails(id: Int): Result<PostDetailsResponse> {
        return try {
            val response = apiService.getPostDetails(id).awaitResponse()
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

    suspend fun getPostComments(id: Int, request: CommentPaginationRequest): Result<CommentPaginationResponse> {
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

    suspend fun savePost(id: Int): Result<SavePostResponse> {
        return try {
            val response = apiService.savePost(id).awaitResponse()
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

    suspend fun likePost(id: Int): Result<LikePostResponse> {
        return try {
            val response = apiService.likePost(id).awaitResponse()
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

    suspend fun viewPost(id: Int): Result<ViewPostResponse> {
        return try {
            val response = apiService.viewPost(id).awaitResponse()
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

    suspend fun deletePost(id: Int): Result<Unit> {
        return try {
            val response = apiService.deletePost(id).awaitResponse()
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

    suspend fun createPost(request: PostCreateRequest): Result<Unit> {
        return try {
            val response = apiService.createPost(request).awaitResponse()
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