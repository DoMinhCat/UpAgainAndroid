package com.example.upagain.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.upagain.model.AccountDetailsResponse
import com.example.upagain.repository.AccountRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class AccountViewModel(private val repository: AccountRepo) : ViewModel() {
    private val _accountState = MutableStateFlow<UiState<AccountDetailsResponse>>(UiState.Idle)
    val accountState: StateFlow<UiState<AccountDetailsResponse>> = _accountState

    fun getAccountDetails(idAccount: Int) {
        viewModelScope.launch {
            _accountState.value = UiState.Loading

            repository.getAccountDetails(idAccount)
                .onSuccess { accountDetails ->
                    _accountState.value = UiState.Success(accountDetails)
                }
                .onFailure { exception ->
                    val statusCode = (exception as? HttpException)?.code()
                    _accountState.value = UiState.Error(statusCode, exception)
                }
        }
    }

    fun resetState() {
        _accountState.value = UiState.Idle
    }
}
