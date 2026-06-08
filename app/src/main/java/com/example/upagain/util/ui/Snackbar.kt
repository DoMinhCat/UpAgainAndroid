package com.example.upagain.util.ui

import android.animation.ObjectAnimator
import android.view.Gravity
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.example.upagain.R
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar

enum class SnackbarLevel {
    SUCCESS,
    ERROR,
    INFO
}
private data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

fun View.showTopSnackbar(
    @StringRes resId: Int,
    level: SnackbarLevel = SnackbarLevel.INFO, // Defaults to your current setup
    length: Int = Snackbar.LENGTH_SHORT
) {
    val message = context.getString(resId)
    val snackbar = Snackbar.make(this, message, length)
    val snackbarView = snackbar.view
    val context = snackbarView.context

    // 2. Resolve colors dynamically based on the level passed
    val (backgroundColor, textColor, progressColor, iconRes) = when (level) {
        SnackbarLevel.SUCCESS -> Quadruple(
            ContextCompat.getColor(context, R.color.color_surface),
            ContextCompat.getColor(context, R.color.color_success),
            ContextCompat.getColor(context, R.color.color_success),
            R.drawable.ic_check_circle
        )
        SnackbarLevel.ERROR -> Quadruple(
            ContextCompat.getColor(context, R.color.color_surface),
            ContextCompat.getColor(context, R.color.color_error),
            ContextCompat.getColor(context, R.color.color_error),
            R.drawable.ic_error_circle
        )
        SnackbarLevel.INFO -> Quadruple(
            ContextCompat.getColor(context, R.color.color_surface),
            ContextCompat.getColor(context, R.color.color_on_surface),
            ContextCompat.getColor(context, R.color.blue),
            R.drawable.ic_info_circle
        )
    }

    // 2. Locate internal layout and map structures
    val snackbarText = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
    val defaultTextParent = snackbarText.parent as LinearLayout

    // Set root structure to vertical to support lower progress bar integration
    defaultTextParent.orientation = LinearLayout.VERTICAL

    val paddingSm = context.resources.getDimensionPixelSize(R.dimen.space_sm)
    val paddingMd = context.resources.getDimensionPixelSize(R.dimen.space_md)
    defaultTextParent.setPadding(paddingMd, paddingMd, paddingMd, paddingSm)

    // 3. Reconstruct text wrapper line to dynamically position the level status icon on the left
    val textRowLayout = LinearLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    // Isolate text field from default parent and update attributes
    defaultTextParent.removeView(snackbarText)
    snackbarText.setTextColor(textColor)
    snackbarText.setTextAppearance(R.style.TextAppearance_UpAgain_BodyMedium)
    snackbarText.layoutParams = LinearLayout.LayoutParams(
        0,
        LinearLayout.LayoutParams.WRAP_CONTENT,
        1f
    )

    // Construct the context state icon
    val iconView = ImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            context.resources.getDimensionPixelSize(R.dimen.icon_sm),
            context.resources.getDimensionPixelSize(R.dimen.icon_sm)
        ).apply {
            marginEnd = paddingMd
        }
        setImageResource(iconRes)
//        imageTintList = android.content.res.ColorStateList.valueOf(progressColor)
    }

    // Stitch combined components into the vertical display hierarchy
    textRowLayout.addView(iconView)
    textRowLayout.addView(snackbarText)
    defaultTextParent.addView(textRowLayout, 0) // Attach text row at top position

    // 4. Position layout container at Top position
    val params = snackbarView.layoutParams
    val marginLg = context.resources.getDimensionPixelSize(R.dimen.space_lg)
    val marginTop = context.resources.getDimensionPixelSize(R.dimen.space_xl)

    if (params is CoordinatorLayout.LayoutParams) {
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.setMargins(marginLg, marginTop, marginLg, 0)
    } else if (params is FrameLayout.LayoutParams) {
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.setMargins(marginLg, marginTop, marginLg, 0)
    }
    snackbarView.layoutParams = params

    // 5. Apply surface container configurations
    snackbarView.background = ContextCompat.getDrawable(context, R.drawable.input_round)
    snackbarView.backgroundTintList = android.content.res.ColorStateList.valueOf(backgroundColor)
    snackbarView.elevation = context.resources.getDimension(R.dimen.space_xs)

    // 6. Build the linear timeout progress tracking bar
    val progressBar = LinearProgressIndicator(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = paddingSm
        }
        max = 1000
        progress = 1000
        trackColor = ContextCompat.getColor(context, R.color.color_background)
        setIndicatorColor(progressColor)
        trackThickness = context.resources.getDimensionPixelSize(R.dimen.space_xs)
        trackCornerRadius = 4
        trackStopIndicatorSize = 0
    }
    defaultTextParent.addView(progressBar)

    // 7. Calculate specific layout execution lengths
    val durationMs = when (length) {
        Snackbar.LENGTH_LONG -> 5500L
        else -> 2750L
    }

    // 8. Bind smooth execution logic to the viewing cycle
    snackbar.addCallback(object : Snackbar.Callback() {
        override fun onShown(sb: Snackbar?) {
            ObjectAnimator.ofInt(progressBar, "progress", 1000, 0).apply {
                duration = durationMs
                interpolator = LinearInterpolator()
                start()
            }
        }
    })

    snackbar.show()
}