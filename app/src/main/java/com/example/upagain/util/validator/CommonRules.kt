package com.example.upagain.util.validator

class NotEmptyRule : ValidationRule<String> {
    override fun validate(value: String): Boolean = value.isNotEmpty()
}

class MinLengthRule(private val minLength: Int) : ValidationRule<String> {
    override fun validate(value: String): Boolean = value.length >= minLength
}

class SameAsRule(private val otherString: String) : ValidationRule<String> {
    override fun validate(value: String): Boolean = value == otherString
}