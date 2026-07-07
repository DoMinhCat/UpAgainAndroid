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

    suspend fun getMyItemsPaginated(options: Map<String, String>): Result<MyItemsResponse> {
        return try {
            val response = apiService.getMyItemsPaginated(options).awaitResponse()
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

    suspend fun getItemDetails(id: Int): Result<com.example.upagain.model.item.ItemDetailResponse> {
        return try {
            val response = apiService.getItemDetails(id).awaitResponse()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception(parseErrorMessage(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getListingDetails(id: Int): Result<com.example.upagain.model.item.ListingDetailResponse> {
        return try {
            val response = apiService.getListingDetails(id).awaitResponse()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception(parseErrorMessage(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDepositDetails(id: Int): Result<com.example.upagain.model.item.DepositDetailResponse> {
        return try {
            val response = apiService.getDepositDetails(id).awaitResponse()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception(parseErrorMessage(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteItem(id: Int): Result<Unit> {
        return try {
            val response = apiService.deleteItem(id).awaitResponse()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseErrorMessage(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getItemTransactions(
        id: Int,
        page: Int? = null,
        limit: Int? = null
    ): Result<com.example.upagain.model.transaction.TransactionsPaginationResponse> {
        return try {
            val response = apiService.getItemTransactions(id, page, limit).awaitResponse()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception(parseErrorMessage(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLatestTransactionOfPro(id: Int): Result<com.example.upagain.model.transaction.TransactionResponse> {
        return try {
            val response = apiService.getLatestTransactionOfPro(id).awaitResponse()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception(parseErrorMessage(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reserveItem(id: Int): Result<Unit> {
        return try {
            val response = apiService.reserveItem(id).awaitResponse()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseErrorMessage(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelItemReservation(id: Int): Result<Unit> {
        return try {
            val response = apiService.cancelItemReservation(id).awaitResponse()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseErrorMessage(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun purchaseItem(
        id: Int,
        payload: com.example.upagain.model.transaction.ItemPurchaseRequest
    ): Result<String> {
        return try {
            val response = apiService.purchaseItem(id, payload).awaitResponse()
            if (response.isSuccessful) {
                val bodyString = response.body()?.string() ?: ""
                Result.success(bodyString)
            } else {
                Result.failure(Exception(parseErrorMessage(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDepositCodes(id: Int): Result<List<com.example.upagain.model.transaction.BarcodeResponse>> {
        return try {
            val response = apiService.getDepositCodes(id).awaitResponse()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception(parseErrorMessage(response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}