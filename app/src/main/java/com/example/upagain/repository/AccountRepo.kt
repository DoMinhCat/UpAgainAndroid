package com.example.upagain.repository

import android.content.Context
import android.net.Uri
import com.example.upagain.api.ApiService
import com.example.upagain.model.account.AccountDetailsResponse
import com.example.upagain.model.account.AccountUpdateRequest
import com.example.upagain.model.account.PasswordUpdateRequest
import com.example.upagain.util.json.parseErrorMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import retrofit2.awaitResponse
import java.io.File
import java.io.FileOutputStream

class AccountRepo(private val apiService: ApiService, private val context: Context) {

    suspend fun getAccountDetails(idAccount: Int): Result<AccountDetailsResponse> {
        return try {
            val response = apiService.getAccountDetails(idAccount).awaitResponse()
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

    suspend fun updateAvatar(idAccount: Int, fileUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Create a temporary local file copy from the gallery Uri
            val tempFile = File(context.cacheDir, "upload_avatar.jpg")
            context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // 2. Convert the local file into an OkHttp MultipartBody payload
            val requestFile = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val multipartBody = MultipartBody.Part.createFormData("avatar", tempFile.name, requestFile)

            val response = apiService.uploadAvatar(idAccount, multipartBody).awaitResponse()
            // Delete temp file after streaming the file to server
            tempFile.delete()

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