package com.example.upagain.util.auth

import android.util.Log
import com.auth0.android.jwt.JWT

data class ParseJwtResponse(
    val id_account: Int,
    val username: String,
    val email: String,
    val role: String
)
fun parseJwt(token: String): ParseJwtResponse{
    try {
        val jwt = JWT(token)

        val id = jwt.getClaim("id_account").asInt()
            ?: jwt.getClaim("id_account").asString()?.toIntOrNull()
            ?: 0
        val email = jwt.getClaim("email").asString() ?: ""
        val username = jwt.getClaim("username").asString() ?: ""
        val role = jwt.getClaim("role").asString() ?: ""

        return ParseJwtResponse(
            id_account = id,
            email = email,
            username = username,
            role = role
        )
    } catch (e: Exception) {
        Log.e("parseJwt", "Failed to parse JWT", e)
        throw e
    }
}