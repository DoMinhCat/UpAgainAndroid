package com.example.upagain.util.ui

import android.os.SystemClock
import android.view.View
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import android.os.Handler
import android.os.Looper
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner

/**
 * Toggle loading state of a button
 *
 * @param button The button to toggle the loading state for.
 * @param loader The loader view to show/hide.
 * @param isLoading Whether the loading state is active or not.
 * @param defaultText The default text to restore the button to.
 */
fun toggleBtnLoadingState(
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

/**
 * Reusable toggle to swap an icon or image with a loading spinner.
 * Safely maintains parent container dimensions using View.INVISIBLE.
 *
 * @param componentZone The parent interactive container layout (e.g., Row or Button).
 * @param staticIcon The image or icon view that should vanish during loading.
 * @param loader The progress indicator view that spins.
 * @param isLoading Triggers the operational toggle state.
 */
fun View.toggleIconLoadingState(
    componentZone: View,
    staticIcon: View,
    loader: View,
    isLoading: Boolean
) {
    // 1. Disable or enable container interactions to prevent double-clicks
    componentZone.isClickable = !isLoading
    componentZone.isFocusable = !isLoading

    staticIcon.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
    loader.visibility = if (isLoading) View.VISIBLE else View.GONE
}

fun View.setOnClickListenerWithCooldown(cooldownMillis: Long = 1000L, action: () -> Unit) {
    this.setOnClickListener(object : View.OnClickListener {
        private var lastClickTime: Long = 0

        override fun onClick(v: View) {
            val currentTime = SystemClock.elapsedRealtime()
            if (currentTime - lastClickTime < cooldownMillis) {
                return // Reject click spam instantly
            }
            lastClickTime = currentTime
            v.isEnabled = false
            action()
            Handler(Looper.getMainLooper()).postDelayed({
                v.isEnabled = true
            }, cooldownMillis)
        }
    })
}

/**
 * Automatically binds an onClick listener to execute the system back navigation action.
 * Works seamlessly in both Activities and Fragments.
 */
fun View.setOnBackClickListener() {
    setOnClickListener {
        findViewTreeOnBackPressedDispatcherOwner()?.onBackPressedDispatcher?.onBackPressed()
    }
}

/**
 * Universally toggles full-screen loading visibility.
 * Can be called using any included layout's root view.
 */
fun View.toggleFullScreenLoading(isLoading: Boolean) {
    this.visibility = if (isLoading) View.VISIBLE else View.GONE
}