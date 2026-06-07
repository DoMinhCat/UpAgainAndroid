package com.example.upagain.util.ui

import android.graphics.Color
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec
import com.google.android.material.progressindicator.IndeterminateDrawable

fun MaterialButton.showLoading(isLoading: Boolean, loadingText: String = "") {
    if (isLoading) {
        if (this.tag == null) this.tag = this.text.toString()

        val spec = CircularProgressIndicatorSpec(
            context,
            null,
            0,
            com.google.android.material.R.style.Widget_Material3_CircularProgressIndicator_Medium
        ).apply {
            indicatorColors = intArrayOf(textColors.defaultColor)
            trackColor = Color.TRANSPARENT
            indicatorSize = (20 * context.resources.displayMetrics.density).toInt()
            strokeWidth = (2.5f * context.resources.displayMetrics.density).toInt()
        }

        // Use the public IndeterminateDrawable factory method for circular indicators
        val progressDrawable = IndeterminateDrawable.createCircularDrawable(context, spec)

        this.icon = progressDrawable
        this.text = loadingText
        this.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START

        progressDrawable.start()
        this.isEnabled = false
    } else {
        // Cast to IndeterminateDrawable to safely stop the animation
        (this.icon as? IndeterminateDrawable<*>)?.stop()
        this.icon = null
        this.text = (this.tag as? String) ?: this.text
        this.isEnabled = true
    }
}