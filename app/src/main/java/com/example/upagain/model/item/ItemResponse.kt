package com.example.upagain.model.item

import com.google.gson.annotations.SerializedName

data class ItemDetailResponse(
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("weight") val weight: Float,
    @SerializedName("state") val state: String,
    @SerializedName("id_user") val idUser: Int,
    @SerializedName("username") val username: String,
    @SerializedName("creator_avatar") val creatorAvatar: String?,
    @SerializedName("category") val category: String,
    @SerializedName("material") val material: String,
    @SerializedName("price") val price: Float,
    @SerializedName("status") val status: String,
    @SerializedName("refuse_reason") val refuseReason: String?,
    @SerializedName("images") val images: List<String>,
    @SerializedName("street") val street: String,
    @SerializedName("score") val score: Int,
)

data class MyItemsResponse(
    @SerializedName("items") val items: List<ItemDetailResponse>? = emptyList(),
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("last_page") val lastPage: Int,
    @SerializedName("limit") val limit: Int,
    @SerializedName("total_records") val totalRecords: Int
)