package com.example.upagain.model.item

data class DepositDetailResponse(
    val container_id: Int,
    val street: String?,
    val city: String?,
    val postal_code: String?,
    val lat: Double?,
    val lng: Double?
)