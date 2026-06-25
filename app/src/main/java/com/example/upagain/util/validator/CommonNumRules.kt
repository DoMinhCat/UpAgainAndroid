package com.example.upagain.util.validator

class OnlyNumberRule : ValidationRule<String> {
    override fun validate(value: String): Boolean {
        return value.all { it.isDigit() }
    }
}

class NumberRangeRule(
    private val minValue: Int? = null,
    private val maxValue: Int? = null
) : ValidationRule<String> {

    override fun validate(value: String): Boolean {
        if (value.isEmpty() || !value.all { it.isDigit() }) return false
        val numericValue = value.toIntOrNull() ?: return false

        val satisfiesMin = minValue == null || numericValue >= minValue
        val satisfiesMax = maxValue == null || numericValue <= maxValue
        return satisfiesMin && satisfiesMax
    }
}