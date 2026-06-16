package com.example.upagain.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.upagain.model.AccountDetailsResponse
import com.example.upagain.repository.AccountRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class AccountViewModel(private val repository: AccountRepo) : ViewModel() {
    private val _accountDetailsState = MutableStateFlow<UiState<AccountDetailsResponse>>(UiState.Loading)
    val accountDetailsState: StateFlow<UiState<AccountDetailsResponse>> = _accountDetailsState

    fun getAccountDetails(idAccount: Int) {
        viewModelScope.launch {
            _accountDetailsState.value = UiState.Loading

            repository.getAccountDetails(idAccount)
                .onSuccess { accountDetails ->
                    _accountDetailsState.value = UiState.Success(accountDetails)
                }
                .onFailure { exception ->
                    val statusCode = (exception as? HttpException)?.code()
                    _accountDetailsState.value = UiState.Error(statusCode, exception)
                }
        }
    }
}
