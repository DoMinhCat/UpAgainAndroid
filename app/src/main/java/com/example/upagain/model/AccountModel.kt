package com.example.upagain.model

data class AccountDetailsResponse(
    val id: Int,
    val createdAt: String,
    val username: String,
    val email: String,
    val phone: String,
    val isPremium: Boolean
)