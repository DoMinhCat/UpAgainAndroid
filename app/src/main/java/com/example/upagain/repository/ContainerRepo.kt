package com.example.upagain.repository

import android.content.Context
import android.net.Uri
import com.example.upagain.api.ApiService
import com.example.upagain.util.bin.getFileExtensionAndMime
import com.example.upagain.util.bin.streamUriToTempFile
import com.example.upagain.util.json.parseErrorMessage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.awaitResponse
import java.io.File
import java.util.UUID

class ContainerRepo(private val apiService: ApiService) {
    suspend fun openContainer(
        context: Context,
        idContainer: Int,
        digitCode: String?,
        barcodeUri: Uri?
    ): Result<Unit> {
        var tempFile: File? = null

        return try {
            var filePart: MultipartBody.Part? = null
            var codePart: RequestBody? = null

            if (!digitCode.isNullOrEmpty()) {
                codePart = digitCode.toRequestBody("text/plain".toMediaTypeOrNull())
            } else if (barcodeUri != null) {
                val (mimeType, extension) = getFileExtensionAndMime(context, barcodeUri)

                val localTempFile = File(context.cacheDir, "upload_${UUID.randomUUID()}.$extension")
                tempFile = localTempFile

                val streamSuccess = streamUriToTempFile(context, barcodeUri, localTempFile)
                if (!streamSuccess) return Result.failure(Exception("Failed to stream URI data"))

                val requestFile = localTempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                filePart =
                    MultipartBody.Part.createFormData("barcode", localTempFile.name, requestFile)
            } else {
                return Result.failure(Exception("Either access code or barcode image is required."))
            }

            val response = apiService.openContainer(idContainer, filePart, codePart).awaitResponse()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errMessage = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            tempFile?.let {
                if (it.exists()) {
                    it.delete()
                }
            }
        }
    }
}