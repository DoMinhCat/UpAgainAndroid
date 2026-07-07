package com.example.upagain.model.finance

import com.google.gson.annotations.SerializedName

enum class FinanceKeyEnum(val key: String) {
    @SerializedName("trial_days")
    TRIAL_DAYS("trial_days"),

    @SerializedName("commission_rate")
    COMMISSION_RATE("commission_rate"),

    @SerializedName("ads_price_per_month")
    ADS_PRICE_PER_MONTH("ads_price_per_month"),

    @SerializedName("subscription_price")
    SUBSCRIPTION_PRICE("subscription_price");

    override fun toString(): String = key
}
