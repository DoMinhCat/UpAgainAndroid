package com.example.upagain.util.ui

import android.content.res.Resources

/**
 * Quick helper method to convert float density points
 * cleanly into exact target pixel layout bounds.
 */
fun dpToPx(dp: Float, resources: Resources): Int {
    val density = resources.displayMetrics.density
    return (dp * density).toInt()
}