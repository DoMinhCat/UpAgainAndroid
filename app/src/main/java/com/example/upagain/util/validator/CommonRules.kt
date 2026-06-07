package com.example.upagain.util.validator

class EmailRule : ValidationRule<String> {
    private val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    override fun validate(value: String): Boolean = value.matches(emailRegex)
}

class NotEmptyRule : ValidationRule<String> {
    override fun validate(value: String): Boolean = value.isNotEmpty()
}

class MinLengthRule(private val minLength: Int) : ValidationRule<String> {
    override fun validate(value: String): Boolean = value.length >= minLength
}