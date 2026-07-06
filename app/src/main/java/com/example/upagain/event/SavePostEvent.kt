package com.example.upagain.event

sealed class SavePostEvent {
    data class Rollback(
        val position: Int,
        val idPost: Int,
        val statusCode: Int?,
        val exception: Throwable
    ) : SavePostEvent()

    data class Succeeded(val position: Int, val idPost: Int, val isSaved: Boolean) : SavePostEvent()
}