package com.example.upagain.model.ads

import com.google.gson.annotations.SerializedName

data class CreateAdsRequest(
    @SerializedName("from") val from: String,
    @SerializedName("duration") val duration: Int,
    @SerializedName("id_post") val idPost: Int,
    @SerializedName("origin_url") val originUrl: String,
    @SerializedName("paid") val isPaid: Boolean,
)
