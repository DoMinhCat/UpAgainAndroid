package com.example.upagain.repository

import com.example.upagain.api.ApiService
import com.example.upagain.model.noti.NotiSetting
import com.example.upagain.model.noti.UpdateNotiSettingRequest
import com.example.upagain.model.noti.UpdateMaterialAlertsRequest
import com.example.upagain.util.json.parseErrorMessage
import retrofit2.awaitResponse

class NotiSettingRepo(private val apiService: ApiService) {

    suspend fun getNotificationSettings(idAccount: Int): Result<List<NotiSetting>> {
        return try {
            val response = apiService.getNotificationSettings(idAccount).awaitResponse()
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

    suspend fun updateNotificationSetting(
        idAccount: Int,
        notiType: String,
        isEnabled: Boolean
    ): Result<Unit> {
        return try {
            val payload = UpdateNotiSettingRequest(notiType, isEnabled)
            val response = apiService.updateNotificationSetting(idAccount, payload).awaitResponse()

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

    suspend fun getProAlertMaterials(idAccount: Int): Result<List<String>> {
        return try {
            val response = apiService.getProAlertMaterials(idAccount).awaitResponse()
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

    suspend fun updateProAlertMaterials(idAccount: Int, materials: List<String>): Result<Unit> {
        return try {
            val payload = UpdateMaterialAlertsRequest(materials)
            val response = apiService.updateProAlertMaterials(idAccount, payload).awaitResponse()

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
