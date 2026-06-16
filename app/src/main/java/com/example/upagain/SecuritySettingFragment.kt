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
import coil.load
import com.example.upagain.api.ApiClient
import com.example.upagain.databinding.FragmentSecuritySettingBinding
import com.example.upagain.feat.error.ErrorActivity
import com.example.upagain.model.AccountUpdateRequest
import com.example.upagain.repository.AccountRepo
import com.example.upagain.util.auth.SessionManager
import com.example.upagain.util.datetime.formatTimestamptz
import com.example.upagain.util.image.buildImageUrl
import com.example.upagain.util.ui.SnackbarLevel
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

class SecuritySettingFragment : Fragment() {
    // layer dependencies
    private val apiService by lazy { ApiClient.apiService }

    // TODO:
    private val repository by lazy { AccountRepo(apiService) }
    // viewmodel
    private val viewModel: AccountViewModel by viewModels {
        ViewModelFactory { AccountViewModel(repository) }
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
        binding.btnSaveEmail.setOnClickListenerWithCooldown {
            val email = binding.etSecurityEmail.text.toString()

            val isEmailValid = emailValidator.validate(email)
            binding.tvErrorEmail.visibility = if (isEmailValid) View.GONE else View.VISIBLE
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
            binding.tvErrorPassword.visibility = if (isPasswordValid) View.GONE else View.VISIBLE
            binding.tvErrorConfirmPassword.visibility = if (isConfirmPasswordValid) View.GONE else View.VISIBLE
            if (!isPasswordValid || !isConfirmPasswordValid) {
                return@setOnClickListenerWithCooldown
            }
            // TODO: build request + call viewmodel to update changes
        }
        // BACK BTN
        binding.btnBack.setOnBackClickListener()
    }

    private fun observeAccountState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // update account email
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
                            binding.main.showTopSnackbar(R.string.snack_email_update_success,
                                SnackbarLevel.SUCCESS)
                        }
                        is UiState.Error -> {
                            toggleEmailBtnLoading(false)
                            Log.e("SecuritySettingFragment", "Update email failed", state.exception)
                            binding.main.showTopSnackbar(R.string.snack_email_update_fail, SnackbarLevel.ERROR)
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
}