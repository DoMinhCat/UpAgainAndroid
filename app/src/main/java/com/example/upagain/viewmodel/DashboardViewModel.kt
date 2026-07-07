package com.example.upagain.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.upagain.model.dashboard.ProAnalyticsResponse
import com.example.upagain.repository.DashboardRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class DashboardViewModel(private val repository: DashboardRepo, application: Application) :
    AndroidViewModel(application) {

    private val _proAnalyticsState =
        MutableStateFlow<UiState<ProAnalyticsResponse>>(UiState.Idle)
    val proAnalyticsState: StateFlow<UiState<ProAnalyticsResponse>> = _proAnalyticsState

    fun getProAnalytics(idAccount: Int) {
        viewModelScope.launch {
            _proAnalyticsState.value = UiState.Loading()

            repository.getProAnalytics(idAccount)
                .onSuccess { analytics ->
                    _proAnalyticsState.value = UiState.Success(analytics)
                }
                .onFailure { exception ->
                    val statusCode = (exception as? HttpException)?.code()
                    _proAnalyticsState.value = UiState.Error(statusCode, exception)
                }
        }
    }
}
