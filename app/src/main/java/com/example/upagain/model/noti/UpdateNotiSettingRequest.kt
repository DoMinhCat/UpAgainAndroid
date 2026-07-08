package com.example.upagain.model.noti

import com.google.gson.annotations.SerializedName

data class UpdateNotiSettingRequest(
    @SerializedName("noti_type") val notiType: String,
    @SerializedName("is_enabled") val isEnabled: Boolean
)
