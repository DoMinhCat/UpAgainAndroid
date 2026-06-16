package com.example.upagain.util.ui

import android.app.Activity
import android.content.Context
import android.view.inputmethod.InputMethodManager

/**
 * Dismisses the software keyboard and clears active focus from the window hierarchy.
 */
fun Activity.hideKeyboard() {
    val currentFocusedView = currentFocus
    if (currentFocusedView != null) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocusedView.windowToken, 0)
        currentFocusedView.clearFocus()
    }
}