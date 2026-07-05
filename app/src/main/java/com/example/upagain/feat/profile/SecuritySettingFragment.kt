package com.example.upagain.feat.profile

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
import com.example.upagain.R
import com.example.upagain.api.ApiClient
import com.example.upagain.databinding.FragmentSecuritySettingBinding
import com.example.upagain.model.account.AccountUpdateRequest
import com.example.upagain.model.account.PasswordUpdateRequest
import com.example.upagain.repository.AccountRepo
import com.example.upagain.util.auth.SessionManager
import com.example.upagain.util.ui.SnackbarLevel
import com.example.upagain.util.ui.hideKeyboard
import com.example.upagain.util.ui.setOnBackClickListener
import com.example.upagain.util.ui.setOnClickListenerWithCooldown
import com.example.upagain.util.ui.showTopSnackbar
import com.example.upagain.util.ui.toggleBtnLoadingState
import com.example.upagain.util.ui.toggleTilError
import com.example.upagain.util.validator.EmailRule
import com.example.upagain.util.validator.FieldValidator
import com.example.upagain.util.validator.NotEmptyRule
import com.example.upagain.util.validator.PasswordRule
import com.example.upagain.util.validator.SameAsRule
import com.example.upagain.viewmodel.AccountViewModel
import com.example.upagain.viewmodel.UiState
import com.example.upagain.viewmodel.ViewModelFactory
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

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
                toggleTilError(binding.tilSecurityEmail, R.string.invalid_email, !isEmailValid)

            } else {
                toggleTilError(binding.tilSecurityEmail, R.string.invalid_email, false)
            }
        }
        // PASSWORD FIELD
        binding.etSecurityPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val password = binding.etSecurityPassword.text.toString()
                val isPasswordValid = passwordValidator.validate(password)
                toggleTilError(
                    binding.tilSecurityPassword,
                    R.string.incorrect_password,
                    !isPasswordValid
                )

            } else {
                toggleTilError(binding.tilSecurityPassword, R.string.incorrect_password, false)
            }
        }
        // PASSWORD CONFIRM FIELD
        binding.etSecurityConfirmPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val password = binding.etSecurityConfirmPassword.text.toString()
                val isPasswordValid = confirmPasswordValidator.validate(password)
                toggleTilError(
                    binding.tilSecurityConfirmPassword,
                    R.string.password_not_match,
                    !isPasswordValid
                )

            } else {
                toggleTilError(
                    binding.tilSecurityConfirmPassword,
                    R.string.password_not_match,
                    false
                )
            }
        }
        // SAVE EMAIL BTN
        binding.btnSaveEmail.setOnClickListenerWithCooldown {
            val email = binding.etSecurityEmail.text.toString()

            val isEmailValid = emailValidator.validate(email)
            toggleTilError(binding.tilSecurityEmail, R.string.invalid_email, !isEmailValid)
            if (!isEmailValid) {
                return@setOnClickListenerWithCooldown
            }

            val request = AccountUpdateRequest(email = email, phone = "", username = "")
            val currentId = SessionManager.accountId ?: return@setOnClickListenerWithCooldown
            viewModel.updateAccount(currentId, request)
        }
        // SAVE PASSWORD
        binding.btnSavePassword.setOnClickListenerWithCooldown {
            val password = binding.etSecurityPassword.text.toString()
            val confirmPassword = binding.etSecurityConfirmPassword.text.toString()

            val isPasswordValid = passwordValidator.validate(password)
            val isConfirmPasswordValid = confirmPasswordValidator.validate(confirmPassword)

            toggleTilError(
                binding.tilSecurityPassword,
                R.string.incorrect_password,
                !isPasswordValid
            )
            toggleTilError(
                binding.tilSecurityConfirmPassword,
                R.string.password_not_match,
                !isConfirmPasswordValid
            )

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
                                Log.e(
                                    "SecuritySettingFragment",
                                    "Update email failed",
                                    state.exception
                                )
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
                                Log.e(
                                    "SecuritySettingFragment",
                                    "Update password failed",
                                    state.exception
                                )
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
        toggleBtnLoadingState(
            binding.btnSaveEmail,
            binding.emailSaveLoader,
            isLoading,
            getString(R.string.btn_save)
        )
    }

    private fun togglePasswordBtnLoading(isLoading: Boolean) {
        toggleBtnLoadingState(
            binding.btnSavePassword, binding.passwordSaveLoader, isLoading, getString(
                R.string.btn_save
            )
        )
    }
}