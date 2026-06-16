package com.example.upagain

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.upagain.api.ApiClient
import com.example.upagain.databinding.FragmentSecuritySettingBinding
import com.example.upagain.repository.AuthRepository
import com.example.upagain.util.ui.setOnBackClickListener
import com.example.upagain.util.ui.setOnClickListenerWithCooldown
import com.example.upagain.util.validator.EmailRule
import com.example.upagain.util.validator.FieldValidator
import com.example.upagain.util.validator.MinLengthRule
import com.example.upagain.util.validator.NotEmptyRule
import com.example.upagain.util.validator.PasswordRule
import com.example.upagain.util.validator.SameAsRule
import com.example.upagain.viewmodel.AuthViewModel
import com.example.upagain.viewmodel.ViewModelFactory
import kotlin.getValue

class SecuritySettingFragment : Fragment() {
    // layer dependencies
    private val apiService by lazy { ApiClient.apiService }

    // TODO:
//    private val repository by lazy { AuthRepository(apiService) }
    // viewmodel
//    private val viewModel: AuthViewModel by viewModels {
//        ViewModelFactory { AuthViewModel(repository) }
//    }

    // form validators
    val emailValidator = FieldValidator(listOf(NotEmptyRule(), EmailRule()))
    val passwordValidator = FieldValidator(listOf(NotEmptyRule(), PasswordRule()))
    val confirmPasswordValidator = FieldValidator(
        listOf(
            NotEmptyRule(),
            SameAsRule { binding.etSecurityPassword.text.toString() }
        )
    )
    // binding
    private var _binding: FragmentSecuritySettingBinding? = null
    private val binding get() = _binding!!

    private var userEmail: String? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userEmail = it.getString(ARG_USER_EMAIL)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecuritySettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.etSecurityEmail.setText(userEmail)

        setupListeners()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_USER_EMAIL = "ARG_USER_EMAIL"

        @JvmStatic
        fun newInstance(email: String) =
            SecuritySettingFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USER_EMAIL, email)
                }
            }
    }

    fun setupListeners() {
        // SAVE CHANGES
        binding.btnSaveSecurity.setOnClickListenerWithCooldown {
            val email = binding.etSecurityEmail.text.toString()
            val password = binding.etSecurityPassword.text.toString()
            val confirmPassword = binding.etSecurityConfirmPassword.text.toString()

            val isEmailValid = emailValidator.validate(email)
            val isPasswordValid = passwordValidator.validate(password)
            val isConfirmPasswordValid = confirmPasswordValidator.validate(confirmPassword)
            binding.tvErrorEmail.visibility = if (isEmailValid) View.GONE else View.VISIBLE
            binding.tvErrorPassword.visibility = if (isPasswordValid) View.GONE else View.VISIBLE
            binding.tvErrorConfirmPassword.visibility = if (isConfirmPasswordValid) View.GONE else View.VISIBLE
            if (!isEmailValid || !isPasswordValid || !isConfirmPasswordValid) {
                return@setOnClickListenerWithCooldown
            }
            // TODO: build request + call viewmodel to update changes
        }
        // BACK
        binding.btnBack.setOnBackClickListener()
    }
}