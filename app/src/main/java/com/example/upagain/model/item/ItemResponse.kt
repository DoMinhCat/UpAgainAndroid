package com.example.upagain.model.item

import com.example.upagain.model.post.StepItem
import com.google.gson.annotations.SerializedName

data class MyItemsResponse(
    @SerializedName("items") val items: List<StepItem>,
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("last_page") val lastPage: Int,
    @SerializedName("limit") val limit: Int,
    @SerializedName("total_records") val totalRecords: Int
)