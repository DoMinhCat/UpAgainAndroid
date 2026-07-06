package com.example.upagain.model.transaction

data class ItemPurchaseRequest(
    val origin_url: String,
    val paid: Boolean
)