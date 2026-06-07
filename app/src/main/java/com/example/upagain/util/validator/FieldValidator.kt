package com.example.upagain.util.validator

class FieldValidator<T>(private val rules: List<ValidationRule<T>>) {
    fun validate(value: T): Boolean {
        return rules.all { it.validate(value) }
    }
}