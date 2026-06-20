package com.example.upagain.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.upagain.model.account.AccountDetailsResponse
import com.example.upagain.model.account.AccountUpdateRequest
import com.example.upagain.model.account.PasswordUpdateRequest
import com.example.upagain.repository.AccountRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class AccountViewModel(private val repository: AccountRepo) : ViewModel() {
    private val _accountDetailsState =
        MutableStateFlow<UiState<AccountDetailsResponse>>(UiState.Loading)
    val accountDetailsState: StateFlow<UiState<AccountDetailsResponse>> = _accountDetailsState
    private val _accountUpdateState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val accountUpdateState: StateFlow<UiState<Unit>> = _accountUpdateState
    private val _accountDeleteState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val accountDeleteState: StateFlow<UiState<Unit>> = _accountDeleteState
    private val _accountPasswordUpdateState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val accountPasswordUpdateState: StateFlow<UiState<Unit>> = _accountPasswordUpdateState
    private val _accountAvatarUploadState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val accountAvatarUploadState: StateFlow<UiState<Unit>> = _accountAvatarUploadState


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

    fun updateAccount(idAccount: Int, request: AccountUpdateRequest) {
        viewModelScope.launch {
            _accountUpdateState.value = UiState.Loading

            repository.updateAccount(idAccount, request)
                .onSuccess {
                    _accountUpdateState.value = UiState.Success(Unit)
                }
                .onFailure { exception ->
                    val statusCode = (exception as? HttpException)?.code()
                    _accountUpdateState.value = UiState.Error(statusCode, exception)
                }
        }
    }

    fun deleteAccount(idAccount: Int) {
        viewModelScope.launch {
            _accountDeleteState.value = UiState.Loading

            repository.deleteAccount(idAccount)
                .onSuccess {
                    _accountDeleteState.value = UiState.Success(Unit)
                }
                .onFailure { exception ->
                    val statusCode = (exception as? HttpException)?.code()
                    _accountDeleteState.value = UiState.Error(statusCode, exception)
                }
        }
    }

    fun updatePassword(idAccount: Int, request: PasswordUpdateRequest) {
        viewModelScope.launch {
            _accountPasswordUpdateState.value = UiState.Loading

            repository.updatePassword(idAccount, request)
                .onSuccess {
                    _accountPasswordUpdateState.value = UiState.Success(Unit)
                }
                .onFailure { exception ->
                    val statusCode = (exception as? HttpException)?.code()
                    _accountPasswordUpdateState.value = UiState.Error(statusCode, exception)
                }
        }
    }

    fun uploadAvatar(idAccount: Int, fileUri: android.net.Uri) {
        viewModelScope.launch {
            _accountAvatarUploadState.value = UiState.Loading
            repository.updateAvatar(idAccount, fileUri)
                .onSuccess {
                    _accountAvatarUploadState.value = UiState.Success(Unit)
                }
                .onFailure { exception ->
                    _accountAvatarUploadState.value = UiState.Error(null, exception)
                }
        }
    }
}
