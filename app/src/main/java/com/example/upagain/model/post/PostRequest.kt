package com.example.upagain.model.post

import com.google.gson.annotations.SerializedName

data class PostRequest (
    @SerializedName("page") val page: Int? = 1,
    @SerializedName("limit") val limit: Int? = 4, // TODO: redefine default limit based on UI space
    @SerializedName("search") val search: String = "",
    @SerializedName("sort") val sort: PostSortOption? = null,
    @SerializedName("category") val category: PostCategory? = null
) {
    fun toQueryMap(): Map<String, String> {
        return buildMap {
            if (search.isNotEmpty()) put("search", search)
            sort?.let {
                if (it != PostSortOption.MOST_RECENT_CREATION) {
                    put("sort", it.value)
                }
            }
            category?.let {
                if (it != PostCategory.OTHER) {
                    put("category", it.value)
                }
            }
        }
    }
}