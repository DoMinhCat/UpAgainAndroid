package com.example.upagain.repository

import android.content.Context
import android.net.Uri
import com.example.upagain.api.ApiService
import com.example.upagain.model.account.AccountDetailsResponse
import com.example.upagain.model.account.AccountUpdateRequest
import com.example.upagain.model.account.PasswordUpdateRequest
import com.example.upagain.util.bin.getFileExtensionAndMime
import com.example.upagain.util.bin.streamUriToTempFile
import com.example.upagain.util.json.parseErrorMessage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.awaitResponse
import java.io.File
import java.util.UUID

class AccountRepo(private val apiService: ApiService) {

    suspend fun getAccountDetails(idAccount: Int): Result<AccountDetailsResponse> {
        return try {
            val response = apiService.getAccountDetails(idAccount).awaitResponse()
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

    suspend fun updatePassword(idAccount: Int, request: PasswordUpdateRequest): Result<Unit> {
        return try {
            val response = apiService.updatePassword(idAccount, request).awaitResponse()

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

    suspend fun updateAvatar(context: Context, idAccount: Int, fileUri: Uri): Result<Unit> {
        val (mimeType, extension) = getFileExtensionAndMime(context, fileUri)
        val tempFile = File(context.cacheDir, "upload_${UUID.randomUUID()}.$extension")

        return try {
            val streamSuccess = streamUriToTempFile(context, fileUri, tempFile)
            if (!streamSuccess) return Result.failure(Exception("Failed to stream URI data"))

            val requestFile = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
            val multipartBody =
                MultipartBody.Part.createFormData("avatar", tempFile.name, requestFile)

            val response = apiService.uploadAvatar(idAccount, multipartBody).awaitResponse()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errMessage = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }
}