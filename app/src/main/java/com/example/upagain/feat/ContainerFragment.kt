package com.example.upagain.feat

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import coil.load
import com.example.upagain.R
import com.example.upagain.api.ApiClient
import com.example.upagain.databinding.FragmentContainerBinding
import com.example.upagain.util.ui.setOnClickListenerWithCooldown
import com.example.upagain.util.validator.FieldValidator
import com.example.upagain.util.validator.MaxLengthRule
import com.example.upagain.util.validator.MinLengthRule
import com.example.upagain.util.validator.NotEmptyRule
import kotlin.getValue

/**
 * A simple [Fragment] subclass.
 * Use the [ContainerFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ContainerFragment : Fragment() {
    // elements binding
    private var _binding: FragmentContainerBinding? = null
    private val binding get() = _binding!!
    private val apiService by lazy { ApiClient.apiService }
//    private val repository by lazy { ContainerRepo(apiService, requireContext()) }
//    private val viewModel: ContainerViewModel by viewModels {
//        ViewModelFactory { Container(repository) }
//    }

    private var selectedImageUri: Uri? = null

    // Registers the system photo picker launcher for uploading barcode png
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            binding.layoutUploadPrompt.visibility = View.GONE
            binding.ivUploadThumbnail.visibility = View.VISIBLE

            // Render the cached file path or network URI smoothly using Coil
            binding.ivUploadThumbnail.load(selectedImageUri) {
                crossfade(true)
                placeholder(R.color.color_surface)
            }
        }
    }

    val digitCodeValidator =
        FieldValidator(listOf(NotEmptyRule(), MinLengthRule(6), MaxLengthRule(6)))

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
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // ! Always set up listeners and observers before API call
        setupListeners()
//        observeAccountState()

        // API call
//        val currentId = SessionManager.accountId ?: return
//        viewModel.getAccountDetails(currentId)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment ContainerFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ContainerFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }

    // PRIVATE ZONE
    private fun setupListeners() {
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
            // TODO: user can only submit 1, check if both digit code and barcode is there => return error
            val code = binding.etCode.text.toString().trim()
            val isCodeValid = digitCodeValidator.validate(code)
            if (!isCodeValid) {
                toggleTilCodeErrorState(true)
                return@setOnClickListenerWithCooldown
            } else {
                toggleTilCodeErrorState(false)
            }
        }
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
}