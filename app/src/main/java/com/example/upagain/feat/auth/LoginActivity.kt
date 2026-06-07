package com.example.upagain.feat.auth

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.View
import android.content.Intent
import android.util.Log
import androidx.activity.enableEdgeToEdge
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
import com.example.upagain.util.TokenManager
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import com.example.upagain.databinding.LoginActivityBinding
import com.example.upagain.feat.MainActivity
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import retrofit2.HttpException

class LoginActivity : AppCompatActivity() {
    // Initialize layer dependencies
    private val apiService by lazy { ApiClient.apiService }
    private val repository by lazy { AuthRepository(apiService) }

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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // LOGIN BUTTON
        binding.btnLogin.setOnClickListener {
            val email = binding.tilEmail.editText?.text?.toString()?.lowercase()?.trim() ?: ""
            val password = binding.tilPassword.editText?.text?.toString()?.trim() ?: ""

            val isEmailValid = emailValidator.validate(email)
            val isPasswordValid = passwordValidator.validate(password)

            binding.tvEmailError.visibility = if (isEmailValid) View.GONE else View.VISIBLE
            binding.tvPasswordError.visibility = if (isPasswordValid) View.GONE else View.VISIBLE
            if (!isEmailValid && !isPasswordValid) {
                return@setOnClickListener
            }

            handleLogin(email, password)
        }

        // FORGOT LINK
        binding.tvForgotPassword.setOnClickListener {
            val url = "https://upcycleconnect.org/login"
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
            val url = "https://upcycleconnect.org/register"
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())

            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Log.e("LoginActivity", "Open forgot password link failed", e)
                binding.main.showTopSnackbar(R.string.no_browsere, SnackbarLevel.ERROR)
            }
        }
    }

    private fun handleLogin(email: String, password: String) {
        toggleLoadingState(binding.btnLogin, binding.loginLoader, isLoading = true, getString(R.string.login))
        lifecycleScope.launch {
            val request = LoginRequest(email = email, password = password)
            val result = repository.login(request)

            result.onSuccess { tokenResponse ->
                val tokenManager = TokenManager.getInstance(this@LoginActivity)
                tokenManager.saveToken(tokenResponse.token)

                binding.main.showTopSnackbar(R.string.login_success, SnackbarLevel.SUCCESS, Snackbar.LENGTH_SHORT)
                toggleLoadingState(binding.btnLogin, binding.loginLoader, isLoading = false, getString(R.string.login))

                // Redirect to MainActivity
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            }
            result.onFailure { exception ->
                Log.e("LoginActivity", "Login failed", exception)

                // Extract the HTTP status code if the error came from the server
                val statusCode = (exception as? HttpException)?.code()
                when (statusCode) {
                    401 -> binding.main.showTopSnackbar(R.string.login_fail, SnackbarLevel.ERROR)
                    400 -> binding.main.showTopSnackbar(R.string.invalid_request_body, SnackbarLevel.ERROR)
                    else -> binding.main.showTopSnackbar(R.string.exception_message, SnackbarLevel.ERROR)
                }
                toggleLoadingState(binding.btnLogin, binding.loginLoader, isLoading = false, getString(R.string.login))
            }
        }
    }
}
