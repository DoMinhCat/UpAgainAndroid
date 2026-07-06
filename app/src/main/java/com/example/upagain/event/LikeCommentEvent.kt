package com.example.upagain.event

sealed class LikeCommentEvent {
    data class Rollback(
        val position: Int,
        val idComment: Int,
        val statusCode: Int?,
        val exception: Throwable
    ) : LikeCommentEvent()

    data class Succeeded(val position: Int, val idComment: Int, val isLiked: Boolean) :
        LikeCommentEvent()

}