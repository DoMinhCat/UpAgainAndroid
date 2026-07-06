package com.example.upagain.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.upagain.model.LoginRequest
import com.example.upagain.model.TokenResponse
import com.example.upagain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _loginState = MutableStateFlow<UiState<TokenResponse>>(UiState.Idle)
    val loginState: StateFlow<UiState<TokenResponse>> = _loginState

    fun login(request: LoginRequest) {
        viewModelScope.launch {
            _loginState.value = UiState.Loading()

            repository.login(request)
                .onSuccess { tokenResponse ->
                    _loginState.value = UiState.Success(tokenResponse)
                }
                .onFailure { exception ->
                    val statusCode = (exception as? HttpException)?.code()
                    _loginState.value = UiState.Error(statusCode, exception)
                }
        }
    }

    fun resetState() {
        _loginState.value = UiState.Idle
    }
}