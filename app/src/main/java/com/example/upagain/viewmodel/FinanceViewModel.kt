package com.example.upagain.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.upagain.model.finance.FinanceKeyEnum
import com.example.upagain.repository.FinanceRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class FinanceViewModel(private val repository: FinanceRepo) : ViewModel() {

    private val _getFinanceSettingState =
        MutableStateFlow<UiState<Double>>(UiState.Idle)
    val getFinanceSettingState: StateFlow<UiState<Double>> = _getFinanceSettingState

    fun getFinanceSetting(key: FinanceKeyEnum) {
        viewModelScope.launch {
            _getFinanceSettingState.value = UiState.Loading()

            repository.getFinanceSetting(key).onSuccess { financeSetting ->
                _getFinanceSettingState.value = UiState.Success(financeSetting)
            }.onFailure { exception ->
                val statusCode = (exception as? HttpException)?.code()
                _getFinanceSettingState.value = UiState.Error(statusCode, exception)
            }
        }
    }
}