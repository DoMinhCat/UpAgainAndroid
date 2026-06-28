package com.example.upagain.event

sealed class LikePostEvent {
    data class Rollback(val position: Int, val postId: Int, val statusCode: Int?, val exception: Throwable) : LikePostEvent()
    data class Succeeded(val position: Int, val postId: Int, val isLiked: Boolean) : LikePostEvent()
}