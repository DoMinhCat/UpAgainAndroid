package com.example.upagain.model.account

import com.google.gson.annotations.SerializedName

data class AccountUpdateRequest(
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String
)

data class PasswordUpdateRequest(
    @SerializedName("password") val password: String
)