package com.example.upagain.util.validator

class EmailRule : ValidationRule<String> {
    private val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    override fun validate(value: String): Boolean = value.matches(emailRegex)
}

class PasswordRule : ValidationRule<String> {
    private val passwordRegex = Regex($$"^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,}$")
    override fun validate(value: String): Boolean = value.matches(passwordRegex)
}