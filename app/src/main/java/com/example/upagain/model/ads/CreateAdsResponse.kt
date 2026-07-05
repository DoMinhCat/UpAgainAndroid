package com.example.upagain.model.ads

import com.google.gson.annotations.SerializedName

data class CreateAdsResponse(
    @SerializedName("checkout_url") val checkoutUrl: String,
)