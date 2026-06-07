package com.example.upagain.feat.auth

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.content.Intent
import android.net.Uri
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.upagain.R
import com.example.upagain.api.ApiClient
import com.example.upagain.model.LoginRequest
import com.example.upagain.repository.AuthRepository
import com.example.upagain.util.ui.showLoading
import com.example.upagain.util.validator.EmailRule
import com.example.upagain.util.validator.FieldValidator
import com.example.upagain.util.validator.MinLengthRule
import com.example.upagain.util.validator.NotEmptyRule
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import androidx.core.net.toUri

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
        val emailInput = findViewById<EditText>(R.id.til_email)
        val passwordInput = findViewById<EditText>(R.id.til_password)
        val submitButton = findViewById<MaterialButton>(R.id.btn_login)
        val emailError = findViewById<TextView>(R.id.tv_email_error)
        val passwordError = findViewById<TextView>(R.id.tv_password_error)
        val forgotPasswordButton = findViewById<TextView>(R.id.tv_forgot_password)


        // LOGIN BUTTON
        submitButton.setOnClickListener {
            val email = emailInput.text.toString().lowercase().trim()
            val password = passwordInput.text.toString().trim()

            val isEmailValid = emailValidator.validate(email)
            val isPasswordValid = passwordValidator.validate(password)

            emailError.visibility = if (isEmailValid) View.GONE else View.VISIBLE
            passwordError.visibility = if (isPasswordValid) View.GONE else View.VISIBLE

            if (!isEmailValid && !isPasswordValid) {
                return@setOnClickListener
            }
            lifecycleScope.launch {
                submitButton.showLoading(true)

                val request = LoginRequest(email = email, password = password)
                val result = repository.login(request)

                // 3. Handle the Result object
                result.onSuccess { tokenResponse ->
                    // Login successful, handle token and navigate
                    Toast.makeText(this@LoginActivity, R.string.login_success, Toast.LENGTH_SHORT)
                        .show()
                    // TODO: redirect to dashboard
                }.onFailure { exception ->
                    // Login failed, show error message
                    Toast.makeText(
                        this@LoginActivity,
                        R.string.exception_message,
                        Toast.LENGTH_LONG
                    ).show()
                }
                submitButton.isEnabled = true
            }
        }

        // FORGOT BUTTON
        forgotPasswordButton.setOnClickListener {
            val url = "https://upcycleconnect.org/login"
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())

            // Verify an app can handle this intent to prevent crashes
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            }
        }
    }
}
