package com.example.upagain.model.item

import com.google.gson.annotations.SerializedName

data class ItemPaginationRequest(
    @SerializedName("page") val page: Int? = 1,
    @SerializedName("limit") val limit: Int? = 10,
    @SerializedName("search") val search: String = "",
    @SerializedName("sort") val sort: ItemSortOption? = null,
    @SerializedName("status") val status: String,
    @SerializedName("material") val material: String
) {
    fun toQueryMap(): Map<String, String> {
        return buildMap {
            page?.let { put("page", it.toString()) }
            limit?.let { put("limit", it.toString()) }
            if (search.isNotEmpty()) put("search", search)
            sort?.let {
                if (it != ItemSortOption.MOST_RECENT_CREATION) {
                    put("sort", it.value)
                }
            }
            status.let { put("status", it) }
            material.let { put("material", it) }
        }
    }
}
