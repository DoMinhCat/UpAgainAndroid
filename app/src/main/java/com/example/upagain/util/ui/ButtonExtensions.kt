package com.example.upagain.util.ui

import android.os.SystemClock
import android.view.View
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import android.os.Handler
import android.os.Looper

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