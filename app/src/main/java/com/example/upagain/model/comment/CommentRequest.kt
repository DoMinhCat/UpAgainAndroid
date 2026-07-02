package com.example.upagain.model.comment

import com.google.gson.annotations.SerializedName

data class CommentPaginationRequest(
    @SerializedName("page") val page: Int? = 1,
    @SerializedName("limit") val limit: Int? = 10,
) {
    fun toQueryMap(): Map<String, String> {
        return buildMap {
            page?.let { put("page", it.toString()) }
            limit?.let { put("limit", it.toString()) }
        }
    }
}

data class CommentCreateRequest(
    @SerializedName("content") val content: String
)
