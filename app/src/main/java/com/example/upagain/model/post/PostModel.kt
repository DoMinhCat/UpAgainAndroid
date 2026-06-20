package com.example.upagain.model.post

import com.google.gson.annotations.SerializedName

enum class PostCategory(val value: String) {
    @SerializedName("tutorial") TUTORIAL("tutorial"),
    @SerializedName("project") PROJECT("project"),
    @SerializedName("tips") TIPS("tips"),
    @SerializedName("news") NEWS("news"),
    @SerializedName("case_study") CASE_STUDY("case_study"),
    @SerializedName("other") OTHER("other");

    companion object {
        fun fromString(value: String?): PostCategory {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: OTHER
        }
    }
}

data class PostDetailsResponse(
    // TODO: remove unused fields
    @SerializedName("id") val id: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String,
    @SerializedName("category") val category: PostCategory,
    @SerializedName("view_count") val viewCount: Int,
    @SerializedName("like_count") val likeCount: Int,
    @SerializedName("save_count") val saveCount: Int,
    @SerializedName("comment_count") val commentCount: Int,
    @SerializedName("id_account") val idAccount: Int,
    @SerializedName("creator") val creator: String,
    @SerializedName("creator_id") val creatorId: Int,
    @SerializedName("photos") val photos: List<String>,
    @SerializedName("creator_avatar") val creatorAvatar: String?,
    @SerializedName("ads_id") val adsId: Int?,
    @SerializedName("ads_from") val adsFrom: String?,
    @SerializedName("ads_to") val adsTo: String?,
    @SerializedName("is_liked") val isLiked: Boolean,
    @SerializedName("is_saved") val isSaved: Boolean
)

data class PostPaginationResponse (
    @SerializedName("posts") val posts: List<PostDetailsResponse>,
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("last_page") val lastPage: Int,
    @SerializedName("limit") val limit: Int,
    @SerializedName("total_records") val totalRecords: Int
)
