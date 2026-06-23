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