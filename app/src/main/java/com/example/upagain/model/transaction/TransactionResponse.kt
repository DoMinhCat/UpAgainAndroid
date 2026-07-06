package com.example.upagain.model.transaction

data class TransactionResponse(
    val id: Int,
    val id_transaction: String,
    val created_at: String,
    val action: String,
    val id_item: Int,
    val id_pro: Int,
    val username_pro: String,
    val reservation_expiry: String?,
    val item_price: Double?,
    val commission_rate: Double?,
    val total_price: Double?,
    val confirm_code: String?
)

data class TransactionsPaginationResponse(
    val total_transactions: Int,
    val transactions: List<TransactionResponse>,
    val current_page: Int,
    val last_page: Int,
    val limit: Int
)