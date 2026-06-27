package com.example.upagain

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.upagain.api.ApiClient
import com.example.upagain.databinding.FragmentSecuritySettingBinding
import com.example.upagain.model.account.AccountUpdateRequest
import com.example.upagain.model.account.PasswordUpdateRequest
import com.example.upagain.repository.AccountRepo
import com.example.upagain.util.auth.SessionManager
import com.example.upagain.util.ui.SnackbarLevel
import com.example.upagain.util.ui.dpToPx
import com.example.upagain.util.ui.hideKeyboard
import com.example.upagain.util.ui.setOnBackClickListener
import com.example.upagain.util.ui.setOnClickListenerWithCooldown
import com.example.upagain.util.ui.showTopSnackbar
import com.example.upagain.util.ui.toggleBtnLoadingState
import com.example.upagain.util.validator.EmailRule
import com.example.upagain.util.validator.FieldValidator
import com.example.upagain.util.validator.NotEmptyRule
import com.example.upagain.util.validator.PasswordRule
import com.example.upagain.util.validator.SameAsRule
import com.example.upagain.viewmodel.AccountViewModel
import com.example.upagain.viewmodel.UiState
import com.example.upagain.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch
import kotlin.getValue
import com.google.android.material.snackbar.Snackbar

class SecuritySettingFragment : Fragment() {
    // layer dependencies
    private val apiService by lazy { ApiClient.apiService }
    private val repository by lazy { AccountRepo(apiService) }

    // viewmodel
    private val appInstance by lazy { requireActivity().application }
    private val viewModel: AccountViewModel by viewModels {
        ViewModelFactory { AccountViewModel(repository, appInstance) }
    }

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
        observeAccountState()
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

    // PRIVATE ZONE
    private fun setupListeners() {
        // EMAIL FIELD
        binding.etSecurityEmail.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val email = binding.etSecurityEmail.text.toString()
                val isEmailValid = emailValidator.validate(email)
                toggleEmailErrorState(!isEmailValid)
            } else {
                toggleEmailErrorState(false)
            }
        }
        // PASSWORD FIELD
        binding.etSecurityPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val password = binding.etSecurityPassword.text.toString()
                val isPasswordValid = passwordValidator.validate(password)
                togglePasswordErrorState(!isPasswordValid)
            } else {
                togglePasswordErrorState(false)
            }
        }
        // PASSWORD CONFIRM FIELD
        binding.etSecurityConfirmPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val password = binding.etSecurityConfirmPassword.text.toString()
                val isPasswordValid = confirmPasswordValidator.validate(password)
                togglePasswordConfirmErrorState(!isPasswordValid)
            } else {
                togglePasswordConfirmErrorState(false)
            }
        }
        // SAVE EMAIL BTN
        binding.btnSaveEmail.setOnClickListenerWithCooldown {
            val email = binding.etSecurityEmail.text.toString()

            val isEmailValid = emailValidator.validate(email)
            toggleEmailErrorState(!isEmailValid)
            if (!isEmailValid) {
                return@setOnClickListenerWithCooldown
            }

            val request = AccountUpdateRequest(email = email, phone = "", username = "")
            val currentId = SessionManager.accountId ?: return@setOnClickListenerWithCooldown
            viewModel.updateAccount(currentId, request)
        }
        binding.btnSavePassword.setOnClickListenerWithCooldown {
            val password = binding.etSecurityPassword.text.toString()
            val confirmPassword = binding.etSecurityConfirmPassword.text.toString()

            val isPasswordValid = passwordValidator.validate(password)
            val isConfirmPasswordValid = confirmPasswordValidator.validate(confirmPassword)
            togglePasswordErrorState(!isPasswordValid)
            togglePasswordConfirmErrorState(!isConfirmPasswordValid)
            if (!isPasswordValid || !isConfirmPasswordValid) {
                return@setOnClickListenerWithCooldown
            }

            val currentId = SessionManager.accountId ?: return@setOnClickListenerWithCooldown
            val request = PasswordUpdateRequest(password)
            viewModel.updatePassword(currentId, request)
        }
        // BACK BTN
        binding.btnBack.setOnBackClickListener()
    }

    private fun observeAccountState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // UPDATE EMAIL
                launch {
                    viewModel.accountUpdateState.collect { state ->
                        when (state) {
                            is UiState.Idle -> {
                                toggleEmailBtnLoading(false)
                            }

                            is UiState.Loading -> {
                                toggleEmailBtnLoading(true)
                            }

                            is UiState.Success -> {
                                toggleEmailBtnLoading(false)
                                viewModel.resetAccountUpdateState()
                                activity?.hideKeyboard()
                                binding.main.showTopSnackbar(
                                    R.string.snack_email_update_success,
                                    SnackbarLevel.SUCCESS
                                )
                            }

                            is UiState.Error -> {
                                viewModel.resetAccountUpdateState()
                                toggleEmailBtnLoading(false)
                                Log.e("SecuritySettingFragment", "Update email failed", state.exception)
                                binding.main.showTopSnackbar(
                                    getString(
                                        R.string.snack_email_update_fail,
                                        state.exception.message
                                    ), SnackbarLevel.ERROR, Snackbar.LENGTH_LONG
                                )
                            }
                        }
                    }
                }
                // UPDATE PASSWORD
                launch {
                    viewModel.accountPasswordUpdateState.collect { state ->
                        when (state) {
                            is UiState.Idle -> {
                                togglePasswordBtnLoading(false)
                            }

                            is UiState.Loading -> {
                                togglePasswordBtnLoading(true)
                            }

                            is UiState.Success -> {
                                togglePasswordBtnLoading(false)
                                viewModel.resetAccountPasswordUpdateState()
                                activity?.hideKeyboard()
                                binding.main.showTopSnackbar(
                                    R.string.snack_password_update_success,
                                    SnackbarLevel.SUCCESS
                                )
                            }

                            is UiState.Error -> {
                                viewModel.resetAccountPasswordUpdateState()
                                togglePasswordBtnLoading(false)
                                Log.e("SecuritySettingFragment", "Update password failed", state.exception)
                                binding.main.showTopSnackbar(
                                    getString(
                                        R.string.snack_password_update_fail,
                                        state.exception.message
                                    ), SnackbarLevel.ERROR, Snackbar.LENGTH_LONG
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun toggleEmailBtnLoading(isLoading: Boolean) {
        toggleBtnLoadingState(binding.btnSaveEmail, binding.emailSaveLoader, isLoading, getString(R.string.btn_save))
    }
    private fun togglePasswordBtnLoading(isLoading: Boolean) {
        toggleBtnLoadingState(binding.btnSavePassword, binding.passwordSaveLoader, isLoading, getString(R.string.btn_save))
    }

    private fun toggleEmailErrorState(isError: Boolean) {
        if (isError) {
            binding.tilSecurityEmail.boxStrokeWidth = dpToPx(1.5f, resources)
            binding.tilSecurityEmail.boxStrokeWidthFocused = dpToPx(1.5f, resources)
            binding.tilSecurityEmail.isErrorEnabled = true
            binding.tilSecurityEmail.error = getString(R.string.invalid_email)
        } else {
            binding.tilSecurityEmail.error = null
            binding.tilSecurityEmail.isErrorEnabled = false
            binding.tilSecurityEmail.boxStrokeWidth = 0
            binding.tilSecurityEmail.boxStrokeWidthFocused = 0
        }
    }

    private fun togglePasswordErrorState(isError: Boolean) {
        if (isError) {
            binding.tilSecurityPassword.boxStrokeWidth = dpToPx(1.5f, resources)
            binding.tilSecurityPassword.boxStrokeWidthFocused = dpToPx(1.5f, resources)
            binding.tilSecurityPassword.isErrorEnabled = true
            binding.tilSecurityPassword.error = getString(R.string.incorrect_password)
        } else {
            binding.tilSecurityPassword.error = null
            binding.tilSecurityPassword.isErrorEnabled = false
            binding.tilSecurityPassword.boxStrokeWidth = 0
            binding.tilSecurityPassword.boxStrokeWidthFocused = 0
        }
    }

    private fun togglePasswordConfirmErrorState(isError: Boolean) {
        if (isError) {
            binding.tilSecurityConfirmPassword.boxStrokeWidth = dpToPx(1.5f, resources)
            binding.tilSecurityConfirmPassword.boxStrokeWidthFocused = dpToPx(1.5f, resources)
            binding.tilSecurityConfirmPassword.isErrorEnabled = true
            binding.tilSecurityConfirmPassword.error = getString(R.string.password_not_match)
        } else {
            binding.tilSecurityConfirmPassword.error = null
            binding.tilSecurityConfirmPassword.isErrorEnabled = false
            binding.tilSecurityConfirmPassword.boxStrokeWidth = 0
            binding.tilSecurityConfirmPassword.boxStrokeWidthFocused = 0
        }
    }
}