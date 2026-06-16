package com.example.upagain.util.json

import org.json.JSONObject

fun parseErrorMessage(jsonString: String?): String {
    val parsedMessage = try {
        val jsonObject = JSONObject(jsonString ?: "")
        jsonObject.getString("message")
    } catch (e: Exception) {
        jsonString ?: "Unknown backend error"
    }
    return parsedMessage
}