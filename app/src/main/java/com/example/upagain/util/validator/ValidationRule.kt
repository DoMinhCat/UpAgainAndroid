package com.example.upagain.util.validator

interface ValidationRule<T> {
    fun validate(value: T): Boolean
}