package com.example.upagain.util.ui

import android.graphics.Color
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec
import com.google.android.material.progressindicator.IndeterminateDrawable

fun MaterialButton.showLoading(isLoading: Boolean, loadingText: String = "") {
    if (isLoading) {
        if (this.tag == null) this.tag = this.text.toString()

        val density = context.resources.displayMetrics.density
        val spec = CircularProgressIndicatorSpec(
            context,
            null,
            0,
            com.google.android.material.R.style.Widget_Material3_CircularProgressIndicator_Medium
        ).apply {
            indicatorColors = intArrayOf(textColors.defaultColor)
            trackColor = Color.TRANSPARENT
            indicatorSize = (20 * density).toInt()
            strokeWidth = (2.5f * density).toInt()
        }

        val progressDrawable = IndeterminateDrawable.createCircularDrawable(context, spec)

        // Fixed properties using standard public APIs
        this.iconPadding = (8 * density).toInt()
        this.icon = progressDrawable
        this.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START

        if (loadingText.isNotEmpty()) {
            this.text = loadingText
        }

        progressDrawable.start()
        this.isEnabled = false
    } else {
        this.stopLoading()
    }
}

fun MaterialButton.stopLoading() {
    (this.icon as? IndeterminateDrawable<*>)?.stop()
    this.icon = null // Setting icon to null automatically handles hiding it
    this.text = (this.tag as? String) ?: this.text
    this.tag = null
    this.isEnabled = true
}