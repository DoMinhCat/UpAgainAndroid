package com.example.upagain.feat.auth

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.upagain.BuildConfig
import com.example.upagain.R
import com.example.upagain.api.ApiClient
import com.example.upagain.databinding.LoginActivityBinding
import com.example.upagain.feat.MainActivity
import com.example.upagain.model.LoginRequest
import com.example.upagain.repository.AuthRepository
import com.example.upagain.util.auth.RoleAccessDeniedException
import com.example.upagain.util.auth.SessionManager
import com.example.upagain.util.ui.SnackbarLevel
import com.example.upagain.util.ui.setOnClickListenerWithCooldown
import com.example.upagain.util.ui.showTopSnackbar
import com.example.upagain.util.ui.toggleBtnLoadingState
import com.example.upagain.util.validator.EmailRule
import com.example.upagain.util.validator.FieldValidator
import com.example.upagain.util.validator.MinLengthRule
import com.example.upagain.util.validator.NotEmptyRule
import com.example.upagain.viewmodel.AuthViewModel
import com.example.upagain.viewmodel.UiState
import com.example.upagain.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    // Initialize layer dependencies
    private val apiService by lazy { ApiClient.apiService }
    private val repository by lazy { AuthRepository(apiService) }

    // viewmodel
    private val viewModel: AuthViewModel by viewModels {
        ViewModelFactory { AuthViewModel(repository) }
    }

    // form validators
    val emailValidator = FieldValidator(listOf(NotEmptyRule(), EmailRule()))
    val passwordValidator = FieldValidator(listOf(NotEmptyRule(), MinLengthRule(4)))

    // elements binding
    private lateinit var binding: LoginActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginActivityBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // GET INTENT TO SHOW NOTI
        val justLoggedOut = intent.getBooleanExtra("EXTRA_JUST_LOGGED_OUT", false)
        val justDeleted = intent.getBooleanExtra("EXTRA_ACCOUNT_DELETED", false)
        if (justLoggedOut) {
            binding.main.showTopSnackbar(R.string.logout_success, SnackbarLevel.INFO)
        }
        if (justDeleted) {
            binding.main.showTopSnackbar(
                R.string.snack_account_delete_success,
                SnackbarLevel.SUCCESS
            )
        }

        setupListeners()
        observeLoginState()
    }

    // PRIVATE ZONE
    private fun setupListeners() {
        // LOGIN BUTTON
        binding.btnLogin.setOnClickListenerWithCooldown(cooldownMillis = 1200L) {
            val email = binding.tilEmail.editText?.text.toString()
            val password = binding.tilPassword.editText?.text.toString()

            // Frontend validation
            val isEmailValid = emailValidator.validate(email)
            val isPasswordValid = passwordValidator.validate(password)
            binding.tvEmailError.visibility = if (isEmailValid) View.GONE else View.VISIBLE
            binding.tvPasswordError.visibility = if (isPasswordValid) View.GONE else View.VISIBLE
            if (!isEmailValid || !isPasswordValid) {
                return@setOnClickListenerWithCooldown
            }

            val request = LoginRequest(email = email, password = password)
            viewModel.login(request)
        }

        // FORGOT LINK
        binding.tvForgotPassword.setOnClickListener {
            val url = BuildConfig.FRONTEND_BASE_URL + "login"
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())

            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Log.e("LoginActivity", "Open forgot password link failed", e)
                binding.main.showTopSnackbar(R.string.no_browser, SnackbarLevel.ERROR)
            }
        }

        // SIGN UP LINK
        binding.tvSignUp.setOnClickListener {
            val url = BuildConfig.FRONTEND_BASE_URL + "register"
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())

            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Log.e("LoginActivity", "Open sign up link failed", e)
                binding.main.showTopSnackbar(R.string.no_browser, SnackbarLevel.ERROR)
            }
        }
    }

    private fun observeLoginState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    when (state) {
                        is UiState.Idle -> {
                            toggleBtnLoading(false)
                        }

                        is UiState.Loading -> {
                            toggleBtnLoading(true)
                        }

                        is UiState.Success -> {
                            toggleBtnLoading(false)
                            handleLoginSuccess(state.data.token)
                        }

                        is UiState.Error -> {
                            toggleBtnLoading(false)
                            handleLoginFailure(state.statusCode, state.exception)
                        }
                    }
                }
            }
        }
    }

    private fun toggleBtnLoading(isLoading: Boolean) {
        toggleBtnLoadingState(
            binding.btnLogin,
            binding.loginLoader,
            isLoading,
            getString(R.string.login)
        )
    }

    private fun handleLoginSuccess(token: String) {
        try {
            SessionManager.saveUserSession(token)

            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("EXTRA_JUST_LOGGED_IN", true)
            }
            startActivity(intent)
            finish()
        } catch (e: RoleAccessDeniedException) {
            Log.e("LoginActivity", "Access denied: ${e.message}")
            SessionManager.clearSession()
            binding.main.showTopSnackbar(R.string.error_not_pro, SnackbarLevel.ERROR)
            viewModel.resetState()
        } catch (e: Exception) {
            Log.e("LoginActivity", "Failed to save user session", e)
            SessionManager.clearSession()
            binding.main.showTopSnackbar(R.string.exception_message, SnackbarLevel.ERROR)
            viewModel.resetState()
        }
    }

    private fun handleLoginFailure(statusCode: Int?, exception: Throwable) {
        Log.e("LoginActivity", "Login failed with status code $statusCode", exception)
        // no redirection here, just show snack bar noti
        when (statusCode) {
            401 -> binding.main.showTopSnackbar(R.string.login_fail, SnackbarLevel.ERROR)
            400 -> binding.main.showTopSnackbar(R.string.invalid_request_body, SnackbarLevel.ERROR)
            404 -> binding.main.showTopSnackbar(R.string.login_fail, SnackbarLevel.ERROR)
            else -> binding.main.showTopSnackbar(R.string.exception_message, SnackbarLevel.ERROR)
        }
        viewModel.resetState()
    }
}
