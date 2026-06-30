package com.example.upagain.model.comment

import com.google.gson.annotations.SerializedName

data class CommentDetailsResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("content") val content: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("like_count") val likeCount: Int,
    @SerializedName("id_post") val idPost: Int,
    @SerializedName("id_account") val idAccount: Int,
    @SerializedName("user_avatar") val userAvatar: String? = null,
    @SerializedName("user_name") val username: String,
    @SerializedName("is_liked") val isLiked: Boolean
)

data class CommentPaginationResponse(
    @SerializedName("total_comments") val totalComments: Int,
    @SerializedName("comments") val comments: List<CommentDetailsResponse>?,
    @SerializedName("comments") val currentPage: Int,
    @SerializedName("last_page") val lastPage: Int,
    @SerializedName("limit") val limit: Int,
)
