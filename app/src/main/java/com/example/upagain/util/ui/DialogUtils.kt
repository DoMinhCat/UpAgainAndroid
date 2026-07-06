package com.example.upagain.util.ui

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import androidx.core.content.ContextCompat.getString
import androidx.core.net.toUri
import androidx.fragment.app.FragmentManager
import com.example.upagain.R
import com.example.upagain.databinding.DialogAdsBookingBinding
import com.example.upagain.databinding.DialogCreateStepBinding
import com.example.upagain.databinding.DialogEditPostBinding
import com.example.upagain.feat.post.adapter.PreviewImageAdapter
import com.example.upagain.model.item.ItemDetailResponse
import com.example.upagain.model.post.PostDetailsResponse
import com.example.upagain.model.post.ProjectStepResponse
import com.example.upagain.model.post.StepItem
import com.example.upagain.util.bin.ImageType
import com.example.upagain.util.bin.buildImageUrl
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
        MaterialAlertDialogBuilder(
            context,
            R.style.ThemeOverlay_UpAgain_MaterialAlertDialog_Destructive
        )
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
        onConfirmBooking: (startDateStr: String, durationMonths: Int) -> Unit
    ) {
        val binding = DialogAdsBookingBinding.inflate(LayoutInflater.from(context))

        // 1. Initialize Context Info
        binding.tvBookingPostTitle.text = postTitle

        // 2. Set Up Date Picker Handling
        var selectedDateMs: Long? = null
        val dateDisplayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
        val datePayloadFormat = SimpleDateFormat("yyyy-MM-dd'T00:00:00Z'", Locale.FRANCE)

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
        val alertDialog = MaterialAlertDialogBuilder(
            context,
            R.style.ThemeOverlay_UpAgain_MaterialAlertDialog_Standard
        )
            .setTitle(getString(context, R.string.book_ads_title))
            .setView(binding.root)
            .setPositiveButton(
                context.getString(R.string.btn_confirm),
                null
            ) // Intercepted below for valid checks
            .setNegativeButton(context.getString(R.string.btn_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()

        // 5. Intercept Click validation rules to prevent closure if no date is picked
        alertDialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val dateStr = binding.etStartDate.text.toString()
            val localSelectedDateMs = selectedDateMs

            // Check Rule 1: Date text must not be blank
            if (dateStr.isEmpty() || localSelectedDateMs == null) {
                binding.tilStartDate.error = getString(context, R.string.invalid_date)
                return@setOnClickListener
            }

            // Check Rule 2: Date must not be in the past
            // MaterialDatePicker operates strictly in UTC midnight values.
            val todayUtcMs = MaterialDatePicker.todayInUtcMilliseconds()
            if (localSelectedDateMs < todayUtcMs) {
                // Ensure you add this string entry to your strings.xml file
                binding.tilStartDate.error = context.getString(R.string.invalid_date)
                return@setOnClickListener
            }

            binding.tilStartDate.error = null
            val formattedPayloadDate = datePayloadFormat.format(selectedDateMs)

            onConfirmBooking(formattedPayloadDate, selectedDuration)
            alertDialog.dismiss()
        }
    }

    fun showEditPostDialog(
        context: Context,
        post: PostDetailsResponse,
        onAddImageClick: (onUrisPicked: (List<Uri>) -> Unit) -> Unit,
        onConfirmEdit: (title: String, content: String, newImages: List<Uri>, existingImages: List<String>) -> Unit
    ) {
        val binding = DialogEditPostBinding.inflate(LayoutInflater.from(context))

        // Prepopulate text fields
        binding.etEditTitle.setText(post.title)
        binding.etEditContent.setText(post.content)

        // Initialize chosen images list with remote image paths parsed as Uris
        val chosenImages = mutableListOf<Uri>()
        post.photos?.forEach { path ->
            val remoteUri = buildImageUrl(path, ImageType.MEDIA).toUri()
            chosenImages.add(remoteUri)
        }

        lateinit var imagePreviewAdapter: PreviewImageAdapter
        imagePreviewAdapter = PreviewImageAdapter { deletedUri ->
            chosenImages.remove(deletedUri)
            imagePreviewAdapter.submitList(chosenImages.toList())
            if (chosenImages.isEmpty()) {
                binding.rvEditChosenImages.visibility = View.GONE
            } else {
                binding.rvEditChosenImages.visibility = View.VISIBLE
            }
        }
        binding.rvEditChosenImages.adapter = imagePreviewAdapter
        imagePreviewAdapter.submitList(chosenImages.toList())

        fun updatePreviewImagesVisibility() {
            if (chosenImages.isEmpty()) {
                binding.rvEditChosenImages.visibility = View.GONE
            } else {
                binding.rvEditChosenImages.visibility = View.VISIBLE
            }
        }
        updatePreviewImagesVisibility()

        binding.layoutEditUploadPrompt.setOnClickListener {
            onAddImageClick { newUris ->
                newUris.forEach { uri ->
                    if (!chosenImages.contains(uri)) {
                        chosenImages.add(uri)
                    }
                }
                imagePreviewAdapter.submitList(chosenImages.toList())
                updatePreviewImagesVisibility()
            }
        }

        val alertDialog = MaterialAlertDialogBuilder(
            context,
            R.style.ThemeOverlay_UpAgain_MaterialAlertDialog_Standard
        )
            .setTitle(context.getString(R.string.edit_post))
            .setView(binding.root)
            .setPositiveButton(context.getString(R.string.btn_confirm), null)
            .setNegativeButton(context.getString(R.string.btn_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()

        alertDialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val title = binding.etEditTitle.text.toString().trim()
            val content = binding.etEditContent.text.toString().trim()

            var isValid = true

            if (title.isEmpty()) {
                binding.tilEditTitle.error = context.getString(R.string.invalid_title)
                isValid = false
            } else {
                binding.tilEditTitle.error = null
            }

            if (content.isEmpty()) {
                binding.tilEditContent.error = context.getString(R.string.invalid_content)
                isValid = false
            } else {
                binding.tilEditContent.error = null
            }

            if (!isValid) return@setOnClickListener

            // Differentiate local Uris from remaining remote image paths using query parameter "path"
            val existingImages = mutableListOf<String>()
            val newImages = mutableListOf<Uri>()

            chosenImages.forEach { uri ->
                val path = uri.getQueryParameter("path")
                if (path != null) {
                    existingImages.add(path)
                } else {
                    newImages.add(uri)
                }
            }

            onConfirmEdit(title, content, newImages, existingImages)
            alertDialog.dismiss()
        }
    }

    fun showStepDialog(
        context: Context,
        step: ProjectStepResponse? = null,
        allAvailableItems: List<ItemDetailResponse> = emptyList(),
        onAddImageClick: (onUrisPicked: (List<Uri>) -> Unit) -> Unit,
        onConfirmStep: (title: String, description: String, newImages: List<Uri>, existingImages: List<String>, itemIds: List<Int>) -> Unit
    ) {
        val binding = DialogCreateStepBinding.inflate(LayoutInflater.from(context))

        // Prepopulate text fields if editing
        if (step != null) {
            binding.etStepTitle.setText(step.title)
            binding.etStepDesc.setText(step.description)
        }

        // Initialize chosen images list with remote image paths parsed as Uris if editing
        val chosenImages = mutableListOf<Uri>()
        step?.photos?.forEach { path ->
            val remoteUri = Uri.parse(buildImageUrl(path, ImageType.MEDIA))
            chosenImages.add(remoteUri)
        }

        // Initialize associated items list
        val selectedItemIds = mutableListOf<Int>()
        step?.items?.forEach { item ->
            selectedItemIds.add(item.id)
        }

        // Summary updater for associated items
        fun updateItemsSummary() {
            val selectedItemTitles = allAvailableItems
                .filter { selectedItemIds.contains(it.id) }
                .map { it.title }

            if (selectedItemTitles.isEmpty()) {
                binding.tvSelectedItemsSummary.text = context.getString(R.string.none_selected)
            } else {
                binding.tvSelectedItemsSummary.text = selectedItemTitles.joinToString(", ")
            }
        }
        updateItemsSummary()

        class DropdownAdapter(
            context: Context,
            private val items: List<StepItem>,
            private val selectedIds: List<Int>
        ) : ArrayAdapter<StepItem>(context, R.layout.item_dropdown_multiselect, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as CheckedTextView
                val item = items[position]
                view.text = item.title
                view.isChecked = selectedIds.contains(item.id)
                return view
            }

            override fun getFilter(): android.widget.Filter {
                return object : android.widget.Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        val results = FilterResults()
                        results.values = items
                        results.count = items.size
                        return results
                    }

                    override fun publishResults(
                        constraint: CharSequence?,
                        results: FilterResults?
                    ) {
                        notifyDataSetChanged()
                    }
                }
            }
        }

        val dropdownAdapter = DropdownAdapter(context, allAvailableItems, selectedItemIds)
        binding.actvSelectItems.setAdapter(dropdownAdapter)

        binding.actvSelectItems.setOnItemClickListener { _, _, position, _ ->
            val itemId = allAvailableItems[position].id
            if (selectedItemIds.contains(itemId)) {
                selectedItemIds.remove(itemId)
            } else {
                selectedItemIds.add(itemId)
            }
            dropdownAdapter.notifyDataSetChanged()
            binding.actvSelectItems.post {
                binding.actvSelectItems.showDropDown()
            }
            updateItemsSummary()
        }

        lateinit var imagePreviewAdapter: PreviewImageAdapter
        imagePreviewAdapter = PreviewImageAdapter { deletedUri ->
            chosenImages.remove(deletedUri)
            imagePreviewAdapter.submitList(chosenImages.toList())
            if (chosenImages.isEmpty()) {
                binding.rvStepChosenImages.visibility = View.GONE
            } else {
                binding.rvStepChosenImages.visibility = View.VISIBLE
            }
        }
        binding.rvStepChosenImages.adapter = imagePreviewAdapter
        imagePreviewAdapter.submitList(chosenImages.toList())

        fun updatePreviewImagesVisibility() {
            if (chosenImages.isEmpty()) {
                binding.rvStepChosenImages.visibility = View.GONE
            } else {
                binding.rvStepChosenImages.visibility = View.VISIBLE
            }
        }
        updatePreviewImagesVisibility()

        binding.layoutStepUploadPrompt.setOnClickListener {
            onAddImageClick { newUris ->
                newUris.forEach { uri ->
                    if (!chosenImages.contains(uri)) {
                        chosenImages.add(uri)
                    }
                }
                imagePreviewAdapter.submitList(chosenImages.toList())
                updatePreviewImagesVisibility()
            }
        }

        val alertDialog = MaterialAlertDialogBuilder(
            context,
            R.style.ThemeOverlay_UpAgain_MaterialAlertDialog_Standard
        )
            .setTitle(
                if (step == null) context.getString(R.string.add_step) else context.getString(
                    R.string.edit_post
                )
            )
            .setView(binding.root)
            .setPositiveButton(context.getString(R.string.btn_confirm), null)
            .setNegativeButton(context.getString(R.string.btn_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()

        alertDialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val title = binding.etStepTitle.text.toString().trim()
            val description = binding.etStepDesc.text.toString().trim()

            var isValid = true

            if (title.isEmpty()) {
                binding.tilStepTitle.error = context.getString(R.string.invalid_step_title)
                isValid = false
            } else {
                binding.tilStepTitle.error = null
            }

            if (description.isEmpty()) {
                binding.tilStepDesc.error = context.getString(R.string.invalid_step_description)
                isValid = false
            } else {
                binding.tilStepDesc.error = null
            }

            if (!isValid) return@setOnClickListener

            // Differentiate local Uris from remaining remote image paths using query parameter "path"
            val existingImages = mutableListOf<String>()
            val newImages = mutableListOf<Uri>()

            chosenImages.forEach { uri ->
                val path = uri.getQueryParameter("path")
                if (path != null) {
                    existingImages.add(path)
                } else {
                    newImages.add(uri)
                }
            }

            onConfirmStep(title, description, newImages, existingImages, selectedItemIds)
            alertDialog.dismiss()
        }
    }
}