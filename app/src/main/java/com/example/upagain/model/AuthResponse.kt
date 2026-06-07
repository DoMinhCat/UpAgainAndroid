package com.example.upagain.model

data class TokenResponse(
    val token: String
)

data class LoginRequest(
    val email: String,
    val password: String
)