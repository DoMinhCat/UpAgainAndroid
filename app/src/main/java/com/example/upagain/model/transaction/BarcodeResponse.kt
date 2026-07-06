package com.example.upagain.model.transaction

import com.google.gson.annotations.SerializedName

data class BarcodeResponse(
    val id: Int,
    val code: String,
    val path: String,
    @SerializedName("barcode_base64") val barcodeBase64: String,
    @SerializedName("user_type") val userType: String,
    val status: String,
    @SerializedName("valid_from") val validFrom: String?,
    @SerializedName("valid_to") val validTo: String?
)
