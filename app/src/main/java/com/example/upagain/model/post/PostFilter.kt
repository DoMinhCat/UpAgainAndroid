package com.example.upagain.model.post

import com.google.gson.annotations.SerializedName

enum class PostSortOption(val value: String) {
    @SerializedName("earliest_start_date") EARLIEST_START_DATE("earliest_start_date"),
    @SerializedName("latest_start_date") LATEST_START_DATE("latest_start_date"),
    @SerializedName("most_recent_creation") MOST_RECENT_CREATION("most_recent_creation"),
    @SerializedName("oldest_creation") OLDEST_CREATION("oldest_creation"),
    @SerializedName("highest_price") HIGHEST_PRICE("highest_price"),
    @SerializedName("lowest_price") LOWEST_PRICE("lowest_price"),
    @SerializedName("random") RANDOM("random"),
    @SerializedName("most_popular") MOST_POPULAR("most_popular");

    companion object {
        /**
         * Safely parses a backend sorting string.
         * Default to MOST_RECENT_CREATION if an unrecognized key is received.
         */
        fun fromString(value: String?): PostSortOption {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: MOST_RECENT_CREATION
        }
    }
}

data class PostFilter (
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