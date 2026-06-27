package com.example.upagain.viewmodel

sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    data class Loading(val isFirstPage: Boolean = true) : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val statusCode: Int?, val exception: Throwable) : UiState<Nothing>
}