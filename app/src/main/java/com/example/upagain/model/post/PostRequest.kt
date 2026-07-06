package com.example.upagain.model.post

import android.net.Uri
import com.google.gson.annotations.SerializedName

data class PostPaginationRequest(
    @SerializedName("page") val page: Int? = 1,
    @SerializedName("limit") val limit: Int? = 10,
    @SerializedName("search") val search: String = "",
    @SerializedName("sort") val sort: PostSortOption? = null,
    @SerializedName("category") val category: PostCategory? = null
) {
    fun toQueryMap(): Map<String, String> {
        return buildMap {
            page?.let { put("page", it.toString()) }
            limit?.let { put("limit", it.toString()) }
            if (search.isNotEmpty()) put("search", search)
            sort?.let {
                if (it != PostSortOption.MOST_RECENT_CREATION) {
                    put("sort", it.value)
                }
            }
            category?.let {
                if (it != PostCategory.ALL) {
                    put("category", it.value)
                }
            }
        }
    }
}

data class PostCreateRequest(
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String,
    @SerializedName("category") val category: PostCategory = PostCategory.PROJECT,
    @SerializedName("images") val images: List<Uri>? = emptyList()
)

data class PostUpdateRequest(
    val title: String,
    val content: String,
    val category: PostCategory,
    val endDate: String? = null,
    val newImages: List<Uri>? = emptyList(),
    val existingImages: List<String>? = emptyList()
)