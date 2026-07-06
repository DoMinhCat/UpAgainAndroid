package com.example.upagain.repository

import android.content.Context
import com.example.upagain.api.ApiService
import com.example.upagain.model.post.LikePostResponse
import com.example.upagain.model.post.PostCreateRequest
import com.example.upagain.model.post.PostDetailsResponse
import com.example.upagain.model.post.PostPaginationRequest
import com.example.upagain.model.post.PostPaginationResponse
import com.example.upagain.model.post.PostStepCreateRequest
import com.example.upagain.model.post.PostStepUpdateRequest
import com.example.upagain.model.post.PostUpdateRequest
import com.example.upagain.model.post.ProjectStepResponse
import com.example.upagain.model.post.SavePostResponse
import com.example.upagain.model.post.ViewPostResponse
import com.example.upagain.util.bin.getFileExtensionAndMime
import com.example.upagain.util.bin.streamUriToTempFile
import com.example.upagain.util.json.parseErrorMessage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.awaitResponse
import java.io.File
import java.util.UUID

class PostRepo(private val apiService: ApiService) {


    suspend fun createPost(
        context: Context,
        request: PostCreateRequest,
    ): Result<Unit> {
        val trackedTempFiles = mutableListOf<File>()

        return try {
            // put strings into form value
            val textMediaType = "text/plain".toMediaTypeOrNull()
            val titlePart = request.title.toRequestBody(textMediaType)
            val contentPart = request.content.toRequestBody(textMediaType)
            val categoryPart =
                request.category.value.toRequestBody(textMediaType)

            // handle file uploads
            val imageParts = mutableListOf<MultipartBody.Part>()
            if (!request.images.isNullOrEmpty()) {
                request.images.forEach { imageUri ->
                    val (mimeType, extension) = getFileExtensionAndMime(context, imageUri)
                    val localTempFile =
                        File(context.cacheDir, "post_upload_${UUID.randomUUID()}.$extension")
                    trackedTempFiles.add(localTempFile)

                    val streamSuccess = streamUriToTempFile(context, imageUri, localTempFile)
                    if (!streamSuccess) throw Exception("Failed to stream URI data")

                    val requestFile = localTempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                    val filePart =
                        MultipartBody.Part.createFormData("images", localTempFile.name, requestFile)
                    imageParts.add(filePart)
                }
            }

            val response = apiService.createPost(
                title = titlePart,
                content = contentPart,
                category = categoryPart,
                images = imageParts
            ).awaitResponse()

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errMessage = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            trackedTempFiles.forEach { file ->
                if (file.exists()) file.delete()
            }
        }
    }

    suspend fun updatePost(
        context: Context,
        id: Int,
        request: PostUpdateRequest
    ): Result<Unit> {
        val trackedTempFiles = mutableListOf<File>()

        return try {
            val textMediaType = "text/plain".toMediaTypeOrNull()
            val titlePart = request.title.toRequestBody(textMediaType)
            val contentPart = request.content.toRequestBody(textMediaType)
            val categoryPart = request.category.value.toRequestBody(textMediaType)
            val endDatePart = request.endDate?.toRequestBody(textMediaType)

            // handle new file uploads
            val newImageParts = mutableListOf<MultipartBody.Part>()
            if (!request.newImages.isNullOrEmpty()) {
                request.newImages.forEach { imageUri ->
                    val (mimeType, extension) = getFileExtensionAndMime(context, imageUri)
                    val localTempFile =
                        File(context.cacheDir, "post_update_${UUID.randomUUID()}.$extension")
                    trackedTempFiles.add(localTempFile)

                    val streamSuccess = streamUriToTempFile(context, imageUri, localTempFile)
                    if (!streamSuccess) throw Exception("Failed to stream URI data")

                    val requestFile = localTempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                    val filePart =
                        MultipartBody.Part.createFormData("new_images", localTempFile.name, requestFile)
                    newImageParts.add(filePart)
                }
            }

            // handle existing images
            val existingImageParts = mutableListOf<MultipartBody.Part>()
            if (!request.existingImages.isNullOrEmpty()) {
                request.existingImages.forEach { path ->
                    val filePart = MultipartBody.Part.createFormData("existing_images", path)
                    existingImageParts.add(filePart)
                }
            }

            val response = apiService.updatePost(
                id = id,
                title = titlePart,
                content = contentPart,
                category = categoryPart,
                endDate = endDatePart,
                newImages = newImageParts,
                existingImages = existingImageParts
            ).awaitResponse()

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errMessage = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            trackedTempFiles.forEach { file ->
                if (file.exists()) file.delete()
            }
        }
    }

    suspend fun createProjectStep(
        context: Context,
        idPost: Int,
        request: PostStepCreateRequest
    ): Result<Unit> {
        val trackedTempFiles = mutableListOf<File>()

        return try {
            val textMediaType = "text/plain".toMediaTypeOrNull()
            val titlePart = request.title.toRequestBody(textMediaType)
            val descriptionPart = request.description.toRequestBody(textMediaType)

            // handle file uploads
            val imageParts = mutableListOf<MultipartBody.Part>()
            if (!request.images.isNullOrEmpty()) {
                request.images.forEach { imageUri ->
                    val (mimeType, extension) = getFileExtensionAndMime(context, imageUri)
                    val localTempFile =
                        File(context.cacheDir, "step_upload_${UUID.randomUUID()}.$extension")
                    trackedTempFiles.add(localTempFile)

                    val streamSuccess = streamUriToTempFile(context, imageUri, localTempFile)
                    if (!streamSuccess) throw Exception("Failed to stream URI data")

                    val requestFile = localTempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                    val filePart =
                        MultipartBody.Part.createFormData("images", localTempFile.name, requestFile)
                    imageParts.add(filePart)
                }
            }

            val itemIdParts = request.itemIds?.map { id ->
                MultipartBody.Part.createFormData("item_ids", id.toString())
            } ?: emptyList()

            val response = apiService.createProjectStep(
                idPost = idPost,
                title = titlePart,
                description = descriptionPart,
                itemIds = itemIdParts,
                images = imageParts
            ).awaitResponse()

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errMessage = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            trackedTempFiles.forEach { file ->
                if (file.exists()) file.delete()
            }
        }
    }

    suspend fun updateProjectStep(
        context: Context,
        idStep: Int,
        request: PostStepUpdateRequest
    ): Result<Unit> {
        val trackedTempFiles = mutableListOf<File>()

        return try {
            val textMediaType = "text/plain".toMediaTypeOrNull()
            val titlePart = request.title.toRequestBody(textMediaType)
            val descriptionPart = request.description.toRequestBody(textMediaType)

            val existingImagesParts = request.existingImages?.map { path ->
                MultipartBody.Part.createFormData("existing_images", path)
            } ?: emptyList()

            // handle file uploads
            val imageParts = mutableListOf<MultipartBody.Part>()
            if (!request.newImages.isNullOrEmpty()) {
                request.newImages.forEach { imageUri ->
                    val (mimeType, extension) = getFileExtensionAndMime(context, imageUri)
                    val localTempFile =
                        File(context.cacheDir, "step_upload_${UUID.randomUUID()}.$extension")
                    trackedTempFiles.add(localTempFile)

                    val streamSuccess = streamUriToTempFile(context, imageUri, localTempFile)
                    if (!streamSuccess) throw Exception("Failed to stream URI data")

                    val requestFile = localTempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                    val filePart =
                        MultipartBody.Part.createFormData("new_images", localTempFile.name, requestFile)
                    imageParts.add(filePart)
                }
            }

            val itemIdParts = request.itemIds?.map { id ->
                MultipartBody.Part.createFormData("item_ids", id.toString())
            } ?: emptyList()

            val response = apiService.updateProjectStep(
                idStep = idStep,
                title = titlePart,
                description = descriptionPart,
                existingImages = existingImagesParts,
                newImages = imageParts,
                itemIds = itemIdParts
            ).awaitResponse()

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errMessage = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            trackedTempFiles.forEach { file ->
                if (file.exists()) file.delete()
            }
        }
    }


    suspend fun getAllPosts(requestBody: PostPaginationRequest): Result<PostPaginationResponse> {
        return try {
            val response = apiService.getAllPosts(requestBody.toQueryMap()).awaitResponse()
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

    suspend fun getMyPosts(filters: PostPaginationRequest): Result<PostPaginationResponse> {
        return try {
            val response = apiService.getMyPosts(filters.toQueryMap()).awaitResponse()
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

    suspend fun getSavedPosts(filters: PostPaginationRequest): Result<PostPaginationResponse> {
        return try {
            val response = apiService.getSavedPosts(filters.toQueryMap()).awaitResponse()
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

    suspend fun getPostDetails(id: Int): Result<PostDetailsResponse> {
        return try {
            val response = apiService.getPostDetails(id).awaitResponse()
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

    suspend fun getProjectSteps(idPost: Int): Result<List<ProjectStepResponse>> {
        return try {
            val response = apiService.getProjectSteps(idPost).awaitResponse()

            if (response.isSuccessful) {
                val stepsList = response.body() ?: emptyList()
                Result.success(stepsList)
                Result.success(stepsList)
            } else {
                val errMessage = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errMessage))
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
                val errMessage = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errMessage))
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
                val errMessage = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errMessage))
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
                val errMessage = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePost(id: Int): Result<Unit> {
        return try {
            val response = apiService.deletePost(id).awaitResponse()

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

    suspend fun deleteProjectStep(idStep: Int): Result<Unit> {
        return try {
            val response = apiService.deleteProjectStep(idStep).awaitResponse()

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