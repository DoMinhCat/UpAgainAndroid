package com.example.upagain.util.ui

import android.content.Context
import com.example.upagain.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object DialogUtils {

    /*
    * Show a confirmation dialog that confirms a destructive action.
    *
    * @param context The context of the activity or fragment.
    * @param title The title of the dialog.
    * @param message The message to display in the dialog.
    * @param confirmButtonText The text for the confirm button.
    * @param cancelButtonText The text for the cancel button.
     */
    fun showDestructiveConfirmationDialog(
        context: Context,
        title: String,
        message: String = context.getString(R.string.confirm_message),
        confirmButtonText: String = context.getString(R.string.btn_confirm),
        cancelButtonText: String = context.getString(R.string.btn_cancel),
        onConfirm: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_UpAgain_MaterialAlertDialog)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(confirmButtonText) { dialog, _ ->
                onConfirm()
                dialog.dismiss()
            }
            .setNegativeButton(cancelButtonText) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}