package com.example.upagain.feat

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.example.upagain.R
import com.example.upagain.api.ApiClient
import com.example.upagain.databinding.FragmentContainerBinding
import com.example.upagain.repository.ContainerRepo
import com.example.upagain.util.ui.SnackbarLevel
import com.example.upagain.util.ui.hideKeyboard
import com.example.upagain.util.ui.setOnClickListenerWithCooldown
import com.example.upagain.util.ui.showTopSnackbar
import com.example.upagain.util.ui.toggleBtnLoadingState
import com.example.upagain.util.validator.FieldValidator
import com.example.upagain.util.validator.MaxLengthRule
import com.example.upagain.util.validator.MinLengthRule
import com.example.upagain.util.validator.NotEmptyRule
import com.example.upagain.util.validator.NumberRangeRule
import com.example.upagain.util.validator.OnlyNumberRule
import com.example.upagain.viewmodel.ContainerViewModel
import com.example.upagain.viewmodel.UiState
import com.example.upagain.viewmodel.ViewModelFactory
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ContainerFragment : Fragment() {
    // elements binding
    private var _binding: FragmentContainerBinding? = null
    private val binding get() = _binding!!
    private val apiService by lazy { ApiClient.apiService }
    private val repository by lazy { ContainerRepo(apiService) }
    private val appInstance by lazy { requireActivity().application }
    private val viewModel: ContainerViewModel by viewModels {
        ViewModelFactory { ContainerViewModel(repository, appInstance) }
    }

    private var barcodeUri: Uri? = null

    // Registers the system photo picker launcher for uploading barcode png
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            barcodeUri = uri
            binding.layoutUploadPrompt.visibility = View.GONE
            binding.ivUploadThumbnail.visibility = View.VISIBLE

            // Render the cached file path or network URI smoothly using Coil
            binding.ivUploadThumbnail.load(barcodeUri) {
                crossfade(true)
                placeholder(R.color.color_surface)
            }
        }
    }

    val digitCodeValidator =
        FieldValidator(listOf(NotEmptyRule(), MinLengthRule(6), MaxLengthRule(6)))
    val idValidator =
        FieldValidator(listOf(NotEmptyRule(), OnlyNumberRule(), NumberRangeRule(1)))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContainerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        toggleTilIdErrorState(false)
        toggleTilCodeErrorState(false)
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // ! Always set up listeners and observers before API call
        setupListeners()
        observeState()

        // API call
    }

    // PRIVATE ZONE
    private fun setupListeners() {
        // ID FIELD
        binding.etContainerId.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val id = binding.etContainerId.text.toString().trim()
                val idIdValid = idValidator.validate(id)
                if (!idIdValid) {
                    toggleTilIdErrorState(true)
                } else {
                    toggleTilIdErrorState(false)
                }
            } else {
                toggleTilIdErrorState(false)
            }
        }
        // DIGIT CODE FIELD
        binding.etCode.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val code = binding.etCode.text.toString().trim()
                val isCodeValid = digitCodeValidator.validate(code)
                if (!isCodeValid) {
                    toggleTilCodeErrorState(true)
                } else {
                    toggleTilCodeErrorState(false)
                }
            } else {
                toggleTilCodeErrorState(false)
            }
        }
        // UPLOAD BARCODE ZONE
        binding.layoutUploadContainer.setOnClickListener {
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        // SUBMIT BTN
        binding.btnSubmit.setOnClickListenerWithCooldown {
            val code = binding.etCode.text.toString().trim()
            val idContainerStr = binding.etContainerId.text.toString().trim()
            if (!isValidToSubmit(idContainerStr, code, barcodeUri)) {
                return@setOnClickListenerWithCooldown
            }

            // validation passed
            val idContainer = idContainerStr.toIntOrNull() ?: 0
            viewModel.openContainer(idContainer, code, barcodeUri)
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.openContainerState.collect { state ->
                        when (state) {
                            is UiState.Idle -> {
                                toggleSubmitBtnLoadingState(false)
                            }

                            is UiState.Loading -> {
                                toggleSubmitBtnLoadingState(true)
                            }

                            is UiState.Success -> {
                                toggleSubmitBtnLoadingState(false)
                                activity?.hideKeyboard()
                                binding.main.showTopSnackbar(
                                    R.string.snack_container_open_success,
                                    SnackbarLevel.SUCCESS
                                )
                                viewModel.resetOpenContainerState()
                            }

                            is UiState.Error -> {
                                toggleSubmitBtnLoadingState(false)
                                Log.e(
                                    "ContainerFragment",
                                    "Open container failed",
                                    state.exception
                                )
                                binding.main.showTopSnackbar(
                                    getString(
                                        R.string.snack_container_open_fail,
                                        state.exception.message
                                    ), SnackbarLevel.ERROR, Snackbar.LENGTH_LONG
                                )
                                viewModel.resetOpenContainerState()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isValidToSubmit(idContainer: String, code: String?, barcodeUri: Uri?): Boolean {
        var isValidToSubmit = true
        // check ID
        val isIdValid = idValidator.validate(idContainer)
        if (!isIdValid) {
            (true)
            isValidToSubmit = false
        } else {
            toggleTilIdErrorState(false)
        }
        val hasBarcode = barcodeUri != null
        val hasDigitCode = code != null
        // only 1 method can be selected at a time
        if (hasDigitCode && hasBarcode) {
            binding.main.showTopSnackbar(
                R.string.only_one_method,
                SnackbarLevel.ERROR,
                Snackbar.LENGTH_LONG
            )
            isValidToSubmit = false
        }
        // at least 1 method chosen
        if (!hasDigitCode && !hasBarcode) {
            binding.main.showTopSnackbar(
                R.string.no_method_chosen,
                SnackbarLevel.ERROR,
                Snackbar.LENGTH_LONG
            )
            isValidToSubmit = false
        }
        // validate digit code
        if (hasDigitCode) {
            val isCodeValid = digitCodeValidator.validate(code)
            if (!isCodeValid) {
                toggleTilCodeErrorState(true)
                isValidToSubmit = false
            } else {
                toggleTilCodeErrorState(false)
            }
        }
        return isValidToSubmit
    }

    private fun toggleTilCodeErrorState(isError: Boolean) {
        binding.tilCode.isErrorEnabled = isError
        if (isError) {
            binding.tilCode.isErrorEnabled = true
            binding.tilCode.error = getString(R.string.invalid_digit_code)
            return
        } else {
            binding.tilCode.error = null
            binding.tilCode.isErrorEnabled = false
        }
    }

    private fun toggleTilIdErrorState(isError: Boolean) {
        binding.tilContainerId.isErrorEnabled = isError
        if (isError) {
            binding.tilContainerId.isErrorEnabled = true
            binding.tilContainerId.error = getString(R.string.invalid_container_id)
            return
        } else {
            binding.tilContainerId.error = null
            binding.tilContainerId.isErrorEnabled = false
        }
    }

    private fun toggleSubmitBtnLoadingState(isLoading: Boolean) {
        toggleBtnLoadingState(
            binding.btnSubmit,
            binding.submitLoader,
            isLoading,
            getString(R.string.btn_open_container)
        )
    }
}