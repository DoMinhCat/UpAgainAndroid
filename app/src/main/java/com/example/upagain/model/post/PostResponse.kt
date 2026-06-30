package com.example.upagain.model.post

import com.google.gson.annotations.SerializedName

data class PostDetailsResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String,
    @SerializedName("category") val category: PostCategory,
    @SerializedName("view_count") val viewCount: Int,
    @SerializedName("like_count") var likeCount: Int,
    @SerializedName("save_count") val saveCount: Int,
    @SerializedName("comment_count") val commentCount: Int,
    @SerializedName("id_account") val idAccount: Int,
    @SerializedName("creator") val creator: String,
    @SerializedName("creator_id") val creatorId: Int,
    @SerializedName("photos") val photos: List<String>? = emptyList(),
    @SerializedName("creator_avatar") val creatorAvatar: String?,
    @SerializedName("ads_id") val adsId: Int?,
    @SerializedName("ads_from") val adsFrom: String?,
    @SerializedName("ads_to") val adsTo: String?,
    @SerializedName("is_liked") var isLiked: Boolean,
    @SerializedName("is_saved") var isSaved: Boolean
)

data class PostPaginationResponse(
    @SerializedName("posts") val posts: List<PostDetailsResponse>? = emptyList(),
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("last_page") val lastPage: Int,
    @SerializedName("limit") val limit: Int,
    @SerializedName("total_records") val totalRecords: Int
)

data class LikePostResponse(
    @SerializedName("is_liked") val isLiked: Boolean,
)

data class ViewPostResponse(
    @SerializedName("counted") val counted: Boolean,
)

data class SavePostResponse(
    @SerializedName("is_saved") val isSaved: Boolean,
)

data class StepItem(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
)

data class ProjectStepResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("id_post") val idPost: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("photos") val photos: List<String>? = emptyList(),
    @SerializedName("order") val order: Float,
    @SerializedName("items") val items: List<StepItem>? = emptyList(),
)