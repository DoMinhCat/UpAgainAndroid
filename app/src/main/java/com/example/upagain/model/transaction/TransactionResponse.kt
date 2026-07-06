package com.example.upagain.model.transaction

import com.google.gson.annotations.SerializedName

data class TransactionResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("id_transaction") val idTransaction: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("action") val action: String,
    @SerializedName("id_item") val idItem: Int,
    @SerializedName("id_pro") val idPro: Int,
    @SerializedName("username_pro") val usernamePro: String,
    @SerializedName("reservation_expiry") val reservationExpiry: String?,
    @SerializedName("item_price") val itemPrice: Double?,
    @SerializedName("commission_rate") val commissionRate: Double?,
    @SerializedName("total_price") val totalPrice: Double?,
    @SerializedName("confirm_code") val confirmCode: String?
)

data class TransactionsPaginationResponse(
    @SerializedName("total_transactions") val totalTransactions: Int,
    @SerializedName("transactions") val transactions: List<TransactionResponse>,
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("last_page") val lastPage: Int,
    @SerializedName("limit") val limit: Int
)