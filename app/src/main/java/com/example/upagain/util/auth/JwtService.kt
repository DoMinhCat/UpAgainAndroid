package com.example.upagain.util.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.fragment.app.Fragment
import com.auth0.android.jwt.JWT
import com.example.upagain.feat.auth.LoginActivity

data class ParseJwtResponse(
    val id: Int,
    val username: String,
    val email: String
)
fun parseJwt(token: String): ParseJwtResponse{
    try {
        val jwt = JWT(token)

        return ParseJwtResponse(
            id = jwt.getClaim("id").asInt() ?: 0,
            email = jwt.getClaim("email").asString() ?: "",
            username = jwt.getClaim("username").asString() ?: ""
        )
    } catch (e: Exception) {
        Log.e("parseJwt", "Failed to parse JWT", e)
        throw e
    }
}