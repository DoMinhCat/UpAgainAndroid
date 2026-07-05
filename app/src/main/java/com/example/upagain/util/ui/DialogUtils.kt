package com.example.upagain.util.ui

import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat.getString
import androidx.fragment.app.FragmentManager
import com.example.upagain.R
import com.example.upagain.databinding.DialogAdsBookingBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Locale

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

    fun showAdsBookingDialog(
        context: Context,
        fragmentManager: FragmentManager,
        postTitle: String,
        pricePerMonth: Double,
        onConfirmBooking: (startDateStr: String, durationMonths: Int, finalAmount: Double) -> Unit
    ) {
        val binding = DialogAdsBookingBinding.inflate(LayoutInflater.from(context))

        // 1. Initialize Context Info
        binding.tvBookingPostTitle.text = postTitle

        // 2. Set Up Date Picker Handling
        var selectedDateMs: Long? = null
        val dateDisplayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
        val datePayloadFormat = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)

        binding.etStartDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(context, R.string.select_start_date))
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                selectedDateMs = selection
                binding.etStartDate.setText(dateDisplayFormat.format(selection))
            }
            datePicker.show(fragmentManager, "ADS_DATE_PICKER")
        }

        // 3. Set Up Duration Selection Items (1 to 12 Months)
        val durations = (1..12).toList()
        val adapter = ArrayAdapter(
            context,
            android.R.layout.simple_dropdown_item_1line,
            durations.map { "$it ${getString(context, R.string.month)}" })
        binding.actvDuration.setAdapter(adapter)

        // Helper Calculation Engine
        fun updatePriceBreakdown(duration: Int) {
            val basePrice = pricePerMonth * duration
            val vat = basePrice * 0.20
            val processingFee = if (basePrice > 0) (basePrice * 0.015) + 0.25 else 0.0
            val totalPrice = basePrice + vat + processingFee

            binding.tvBreakdownBase.text = String.format(Locale.FRANCE, "%.2f €", basePrice)
            binding.tvBreakdownVat.text = String.format(Locale.FRANCE, "%.2f €", vat)
            binding.tvBreakdownFee.text = String.format(Locale.FRANCE, "%.2f €", processingFee)
            binding.tvBreakdownTotal.text = String.format(Locale.FRANCE, "%.2f €", totalPrice)
        }

        // Default Initialization State (1 Month Selection)
        var selectedDuration = 1
        binding.actvDuration.setText(adapter.getItem(0), false)
        updatePriceBreakdown(selectedDuration)

        binding.actvDuration.setOnItemClickListener { _, _, position, _ ->
            selectedDuration = durations[position]
            updatePriceBreakdown(selectedDuration)
        }

        // 4. Construct the Standard Styled Dialog Frame Wrapper
        val alertDialog = MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_UpAgain_MaterialAlertDialog)
            .setTitle(getString(context, R.string.book_ads_title))
            .setView(binding.root)
            .setPositiveButton(context.getString(R.string.btn_confirm), null) // Intercepted below for valid checks
            .setNegativeButton(context.getString(R.string.btn_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()

        // 5. Intercept Click validation rules to prevent closure if no date is picked
        alertDialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val dateStr = binding.etStartDate.text.toString()
            if (dateStr.isEmpty() || selectedDateMs == null) {
                binding.tilStartDate.error = getString(context, R.string.invalid_date)
                return@setOnClickListener
            } else {
                binding.tilStartDate.error = null
            }

            // Re-calculate clean values for callback payload submission
            val basePrice = pricePerMonth * selectedDuration
            val vat = basePrice * 0.20
            val processingFee = if (basePrice > 0) (basePrice * 0.015) + 0.25 else 0.0
            val finalCalculatedTotal = basePrice + vat + processingFee

            val formattedPayloadDate = datePayloadFormat.format(selectedDateMs)

            onConfirmBooking(formattedPayloadDate, selectedDuration, finalCalculatedTotal)
            alertDialog.dismiss()
        }
    }
}