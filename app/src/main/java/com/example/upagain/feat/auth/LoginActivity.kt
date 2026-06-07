package com.example.upagain.feat.auth

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.View
import android.content.Intent
import android.util.Log
import android.widget.TextView
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
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import com.example.upagain.feat.MainActivity
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.delay
import retrofit2.HttpException

class LoginActivity : AppCompatActivity() {
    // Initialize layer dependencies
    private val apiService by lazy { ApiClient.apiService }
    private val repository by lazy { AuthRepository(apiService) }

    // form validators
    val emailValidator = FieldValidator(listOf(NotEmptyRule(), EmailRule()))
    val passwordValidator = FieldValidator(listOf(NotEmptyRule(), MinLengthRule(4)))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.login_activity)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // get buttons and input fields
        val emailInput = findViewById<TextInputLayout>(R.id.til_email)
        val passwordInput = findViewById<TextInputLayout>(R.id.til_password)
        val submitButton = findViewById<MaterialButton>(R.id.btn_login)
        val emailError = findViewById<TextView>(R.id.tv_email_error)
        val passwordError = findViewById<TextView>(R.id.tv_password_error)
        val forgotPasswordButton = findViewById<TextView>(R.id.tv_forgot_password)
        val mainView = findViewById<View>(R.id.main)
        val loader = findViewById<CircularProgressIndicator>(R.id.login_loader)

        // LOGIN BUTTON
        submitButton.setOnClickListener {
            val email = emailInput.editText?.text?.toString()?.lowercase()?.trim() ?: ""
            val password = passwordInput.editText?.text?.toString()?.trim() ?: ""

            val isEmailValid = emailValidator.validate(email)
            val isPasswordValid = passwordValidator.validate(password)

            emailError.visibility = if (isEmailValid) View.GONE else View.VISIBLE
            passwordError.visibility = if (isPasswordValid) View.GONE else View.VISIBLE
            if (!isEmailValid && !isPasswordValid) {
                return@setOnClickListener
            }

            toggleLoadingState(submitButton, loader, isLoading = true, getString(R.string.login))
            lifecycleScope.launch {
                val request = LoginRequest(email = email, password = password)
                val result = repository.login(request)

                result.onSuccess { tokenResponse ->
                    val tokenManager = TokenManager.getInstance(this@LoginActivity)
                    tokenManager.saveToken(tokenResponse.token)

                    mainView.showTopSnackbar(R.string.login_success, SnackbarLevel.SUCCESS, Snackbar.LENGTH_SHORT)

                    // Redirect to MainActivity
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
                result.onFailure { exception ->
                    Log.e("LoginActivity", "Login failed", exception)

                    // Extract the HTTP status code if the error came from the server
                    val statusCode = (exception as? HttpException)?.code()
                    when (statusCode) {
                        401 -> mainView.showTopSnackbar(R.string.login_fail, SnackbarLevel.ERROR)
                        400 -> mainView.showTopSnackbar(R.string.invalid_request_body, SnackbarLevel.ERROR)
                        else -> mainView.showTopSnackbar(R.string.exception_message, SnackbarLevel.ERROR)
                    }
                    toggleLoadingState(submitButton, loader, isLoading = false, getString(R.string.login))
                }
            }
        }

        // FORGOT LINK
        forgotPasswordButton.setOnClickListener {
            val url = "https://upcycleconnect.org/login"
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())

            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Log.e("LoginActivity", "Open forgot password link failed", e)
                mainView.showTopSnackbar(R.string.no_browsere, SnackbarLevel.ERROR)
            }
        }

        // SIGN UP LINK
        forgotPasswordButton.setOnClickListener {
            val url = "https://upcycleconnect.org/register"
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())

            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Log.e("LoginActivity", "Open forgot password link failed", e)
                mainView.showTopSnackbar(R.string.no_browsere, SnackbarLevel.ERROR)
            }
        }
    }
}
