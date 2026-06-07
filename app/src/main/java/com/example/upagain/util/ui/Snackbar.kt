package com.example.upagain.util.ui

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar

fun View.showTopSnackbar(@StringRes resId: Int, length: Int = Snackbar.LENGTH_SHORT) {
    val message = context.getString(resId)
    val snackbar = Snackbar.make(this, message, length)
    val snackbarView = snackbar.view
    val params = snackbarView.layoutParams

    if (params is CoordinatorLayout.LayoutParams) {
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
    } else if (params is FrameLayout.LayoutParams) {
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
    }

    snackbarView.layoutParams = params
    snackbar.show()
}