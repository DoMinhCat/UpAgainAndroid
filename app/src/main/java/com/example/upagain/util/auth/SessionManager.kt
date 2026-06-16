package com.example.upagain.util.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class RoleAccessDeniedException(message: String) : Exception(message)

object SessionManager {
    private const val PREF_NAME = "user_session_prefs"
    private const val KEY_TOKEN = "key_token"
    private const val KEY_USER_ID = "key_user_id"
    private const val KEY_USERNAME = "key_username"
    private const val KEY_EMAIL = "key_email"

    private lateinit var prefs: SharedPreferences

    // Fast-access memory cache
    var token: String? = null
        private set
    var accountId: Int? = null
        private set
    var username: String? = null
        private set
    var email: String? = null
        private set

    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

            // Hydrate token and data layers from disk cache
            token = prefs.getString(KEY_TOKEN, null)
            accountId = prefs.getInt(KEY_USER_ID, -1).takeIf { it != -1 }
            username = prefs.getString(KEY_USERNAME, null)
            email = prefs.getString(KEY_EMAIL, null)
        }
    }

    fun saveUserSession(jwtToken: String) {
        val parseResult = parseJwt(jwtToken)

        if (parseResult.role != "pro") {
            throw RoleAccessDeniedException("No professional account found with these credentials")
        }

        token = jwtToken
        accountId = parseResult.id_account
        username = parseResult.username
        email = parseResult.email

        prefs.edit().apply {
            putString(KEY_TOKEN, jwtToken)
            putInt(KEY_USER_ID, accountId ?: 0)
            putString(KEY_USERNAME, username)
            putString(KEY_EMAIL, email)
            apply()
        }
    }

    fun clearSession() {
        token = null
        accountId = null
        username = null
        email = null
        prefs.edit { clear() }
    }

    fun isLoggedIn(): Boolean = token != null
}