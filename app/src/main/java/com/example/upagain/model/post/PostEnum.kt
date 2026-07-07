package com.example.upagain.model.post

import com.google.gson.annotations.SerializedName

enum class PostSortOption(val value: String) {
    @SerializedName("highest_view")
    MOST_VIEW("highest_view"),

    @SerializedName("highest_like")
    MOST_LIKE("highest_like"),

    @SerializedName("most_recent_creation")
    MOST_RECENT_CREATION("most_recent_creation");

    companion object {
        /**
         * Safely parses a backend sorting string.
         * Default to MOST_RECENT_CREATION if an unrecognized key is received.
         */
        fun fromString(value: String?): PostSortOption {
            return entries.find { it.value.equals(value, ignoreCase = true) }
                ?: PostSortOption.MOST_RECENT_CREATION
        }
    }
}

enum class PostCategory(val value: String) {
    @SerializedName("")
    ALL(""),

    @SerializedName("tutorial")
    TUTORIAL("tutorial"),

    @SerializedName("project")
    PROJECT("project"),

    @SerializedName("tips")
    TIPS("tips"),

    @SerializedName("news")
    NEWS("news"),

    @SerializedName("case_study")
    CASE_STUDY("case_study"),

    @SerializedName("other")
    OTHER("other");

    companion object {
        fun fromString(value: String?): PostCategory {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: ALL
        }
    }
}