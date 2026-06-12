package com.example.upagain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class AccountDetailsResponse(
    val id: Int,
    val createdAt: String,
    val username: String,
    val email: String,
    val phone: String,
    val isPremium: Boolean
)

@Parcelize
data class SecurityData(
    val email: String,
    val password: String
) : Parcelable
