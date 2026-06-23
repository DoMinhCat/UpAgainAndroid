package com.example.upagain.feat

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.upagain.R
import com.example.upagain.api.ApiClient
import com.example.upagain.databinding.FragmentContainerBinding
import com.example.upagain.databinding.FragmentProfileBinding
import com.example.upagain.repository.AccountRepo
import com.example.upagain.util.ui.setOnClickListenerWithCooldown
import com.example.upagain.util.validator.FieldValidator
import com.example.upagain.util.validator.MaxLengthRule
import com.example.upagain.util.validator.MinLengthRule
import com.example.upagain.util.validator.NotEmptyRule
import com.example.upagain.util.validator.PhoneRule
import com.example.upagain.viewmodel.AccountViewModel
import com.example.upagain.viewmodel.ViewModelFactory
import kotlin.getValue

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

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

    val digitCodeValidator = FieldValidator(listOf(NotEmptyRule(), MinLengthRule(6), MaxLengthRule(6)))

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

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ContainerFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ContainerFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    // PRIVATE ZONE
    private fun setUpListeners() {
        val code = binding.etCode.text.toString()

        // DIGIT CODE FIELD
        binding.etCode.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val isCodeValid = digitCodeValidator.validate(code)
                if (!isCodeValid) {
                    binding.tilCode.error = getString(R.string.invalid_digit_code)
                }
            } else {
                // Field Refocused: Clear previous validation errors immediately for fluid UX
                binding.etCode.error = null
            }
        }
        // SUBMIT BTN
        binding.btnSubmit.setOnClickListenerWithCooldown {
            // TODO: user can only submit 1, check if both digit code and barcode is there => return error

            val isCodeValid = digitCodeValidator.validate(code)
            if (!isCodeValid) {
                binding.tilCode.error = getString(R.string.invalid_digit_code)
                return@setOnClickListenerWithCooldown
            } else {
                binding.tilCode.error = null
            }
        }
    }
}