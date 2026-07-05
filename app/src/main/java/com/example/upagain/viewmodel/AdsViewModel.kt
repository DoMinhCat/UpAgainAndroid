package com.example.upagain.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.upagain.model.ads.CreateAdsRequest
import com.example.upagain.model.ads.CreateAdsResponse
import com.example.upagain.repository.AdsRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class AdsViewModel(private val repository: AdsRepo) : ViewModel() {
    private val _createAdsState =
        MutableStateFlow<UiState<CreateAdsResponse>>(UiState.Idle)
    val createAdsState: StateFlow<UiState<CreateAdsResponse>> = _createAdsState

    fun createAds(request: CreateAdsRequest) {
        viewModelScope.launch {
            _createAdsState.value = UiState.Loading()

            repository.createAds(request)
                .onSuccess { createAdsResponse ->
                    _createAdsState.value = UiState.Success(createAdsResponse)
                }
                .onFailure { exception ->
                    val statusCode = (exception as? HttpException)?.code()
                    _createAdsState.value = UiState.Error(statusCode, exception)
                }
        }
    }

    fun resetCreateAdsState() {
        _createAdsState.value = UiState.Idle
    }

}