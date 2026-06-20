package com.example.upagain.model.account

import com.google.gson.annotations.SerializedName

data class AccountDetailsResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("is_premium") val isPremium: Boolean,
    @SerializedName("avatar") val avatar: String
)