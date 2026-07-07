package com.example.upagain.model.dashboard

import com.google.gson.annotations.SerializedName

data class MaterialInventoryStats(
    @SerializedName("material") val material: String,
    @SerializedName("available") val available: Int,
    @SerializedName("added") val added: Int,
    @SerializedName("recycled") val recycled: Int
)

data class MaterialUsageStats(
    @SerializedName("material") val material: String,
    @SerializedName("weight") val weight: Double
)

data class DashboardImpact(
    @SerializedName("total_co2") val totalCo2: Double,
    @SerializedName("material_usage") val materialUsage: List<MaterialUsageStats>
)

data class DashboardFinance(
    @SerializedName("total_purchases") val totalPurchases: Int,
    @SerializedName("paid_purchases") val paidPurchases: Int,
    @SerializedName("total_spent") val totalSpent: Double
)

data class ProAnalyticsResponse(
    @SerializedName("inventory") val inventory: List<MaterialInventoryStats>,
    @SerializedName("impact") val impact: DashboardImpact,
    @SerializedName("finance") val finance: DashboardFinance
)
