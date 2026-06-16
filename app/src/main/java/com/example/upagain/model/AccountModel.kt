package com.example.upagain.model

data class AccountDetailsResponse(
    val id: Int,
    val created_at: String,
    val username: String,
    val email: String,
    val phone: String,
    val isPremium: Boolean,
    val avatar: String
)