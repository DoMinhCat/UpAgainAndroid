package com.example.upagain.feat.auth

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.View
import android.content.Intent
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.upagain.R
import com.example.upagain.api.ApiClient
import com.example.upagain.model.LoginRequest
import com.example.upagain.repository.AuthRepository
import com.example.upagain.util.validator.*
import com.example.upagain.util.ui.*
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.upagain.BuildConfig
import com.example.upagain.databinding.LoginActivityBinding
import com.example.upagain.feat.MainActivity
import com.example.upagain.util.auth.SessionManager
import com.example.upagain.viewmodel.AuthViewModel
import com.example.upagain.viewmodel.UiState
import com.example.upagain.viewmodel.ViewModelFactory

class LoginActivity : AppCompatActivity() {
    // Initialize layer dependencies
    private val apiService by lazy { ApiClient.apiService }
    private val repository by lazy { AuthRepository(apiService) }

    // form validators
    val emailValidator = FieldValidator(listOf(NotEmptyRule(), EmailRule()))
    val passwordValidator = FieldValidator(listOf(NotEmptyRule(), MinLengthRule(4)))

    // elements binding
    private lateinit var binding: LoginActivityBinding
    // viewmodel
    private val viewModel: AuthViewModel by viewModels {
        ViewModelFactory { AuthViewModel(repository) }
    }

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
        if(justLoggedOut) {
            binding.main.showTopSnackbar(R.string.logout_success, SnackbarLevel.INFO)
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
                binding.main.showTopSnackbar(R.string.no_browsere, SnackbarLevel.ERROR)
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
                binding.main.showTopSnackbar(R.string.no_browsere, SnackbarLevel.ERROR)
            }
        }
    }

    private fun observeLoginState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    when (state) {
                        is UiState.Idle -> {
                            toggleLoading(false)
                        }
                        is UiState.Loading -> {
                            toggleLoading(true)
                        }
                        is UiState.Success -> {
                            toggleLoading(false)
                            handleLoginSuccess(state.data.token)
                        }
                        is UiState.Error -> {
                            toggleLoading(false)
                            handleLoginFailure(state.statusCode, state.exception)
                        }
                    }
                }
            }
        }
    }

    private fun toggleLoading(isLoading: Boolean) {
        toggleLoadingState(binding.btnLogin, binding.loginLoader, isLoading, getString(R.string.login))
    }

    private fun handleLoginSuccess(token: String) {
        try {
            SessionManager.saveUserSession(token)
        } catch (e: Exception) {
            Log.e("LoginActivity", "Failed to save user session", e)
            SessionManager.clearSession()
            binding.main.showTopSnackbar(R.string.exception_message, SnackbarLevel.ERROR)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_JUST_LOGGED_IN", true)
        }
        startActivity(intent)
        finish()
    }

    private fun handleLoginFailure(statusCode: Int?, exception: Throwable) {
        Log.e("LoginActivity", "Login failed", exception)

        when (statusCode) {
            401 -> binding.main.showTopSnackbar(R.string.login_fail, SnackbarLevel.ERROR)
            400 -> binding.main.showTopSnackbar(R.string.invalid_request_body, SnackbarLevel.ERROR)
            else -> binding.main.showTopSnackbar(R.string.exception_message, SnackbarLevel.ERROR)
        }
        viewModel.resetState()
    }
}
