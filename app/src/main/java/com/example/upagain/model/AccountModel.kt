package com.example.upagain.model

import com.google.gson.annotations.SerializedName

data class AccountDetailsResponse(
    val id: Int,
    @SerializedName("created_at") val createdAt: String,
    val username: String,
    val email: String,
    val phone: String,
    @SerializedName("is_premium") val isPremium: Boolean,
    val avatar: String
)

data class AccountUpdateRequest(
    val username: String,
    val email: String,
    val phone: String
)

data class PasswordUpdateRequest(
    val password: String
)