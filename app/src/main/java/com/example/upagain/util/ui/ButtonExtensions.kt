package com.example.upagain.util.ui

import android.graphics.Color
import android.view.View
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec
import com.google.android.material.progressindicator.IndeterminateDrawable

fun toggleLoadingState(
    button: MaterialButton,
    loader: CircularProgressIndicator,
    isLoading: Boolean,
    defaultText: String
) {
    if (isLoading) {
        button.text = "" // Hides the text completely
        button.isEnabled = false
        loader.visibility = View.VISIBLE
    } else {
        button.text = defaultText // Restores the button text
        button.isEnabled = true
        loader.visibility = View.GONE
    }
}