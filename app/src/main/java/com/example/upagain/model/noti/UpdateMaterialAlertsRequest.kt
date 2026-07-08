package com.example.upagain.model.noti

import com.google.gson.annotations.SerializedName

data class UpdateMaterialAlertsRequest(
    @SerializedName("materials") val materials: List<String>
)
