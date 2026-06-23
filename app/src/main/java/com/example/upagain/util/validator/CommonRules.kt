package com.example.upagain.util.validator

class NotEmptyRule : ValidationRule<String> {
    override fun validate(value: String): Boolean = value.isNotEmpty()
}

class MinLengthRule(private val minLength: Int) : ValidationRule<String> {
    override fun validate(value: String): Boolean = value.length >= minLength
}

class MaxLengthRule(private val maxLength: Int) : ValidationRule<String> {
    override fun validate(value: String): Boolean = value.length <= maxLength
}

class SameAsRule(private val targetTextProvider: () -> String) : ValidationRule<String> {
    override fun validate(value: String): Boolean {
        return value == targetTextProvider()
    }
}

class OnlyNumberRule : ValidationRule<String> {
    override fun validate(value: String): Boolean {
        return value.all { it.isDigit() }
    }
}