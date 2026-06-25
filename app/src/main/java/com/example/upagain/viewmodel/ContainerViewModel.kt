package com.example.upagain.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.upagain.repository.ContainerRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ContainerViewModel(private val repository: ContainerRepo, application: Application) : AndroidViewModel(application) {
    private val context get() = getApplication<Application>().applicationContext

    private val _openContainerState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val openContainerState: StateFlow<UiState<Unit>> = _openContainerState

    fun openContainer(idContainer: Int, digitCode: String?, barcodeUri: Uri?) {
        viewModelScope.launch {
            _openContainerState.value = UiState.Loading
            repository.openContainer(context, idContainer, digitCode, barcodeUri)
                .onSuccess {
                    _openContainerState.value = UiState.Success(Unit)
                }
                .onFailure { exception ->
                    _openContainerState.value = UiState.Error(null, exception)
                }
        }
    }
    fun resetOpenContainerState() {
        _openContainerState.value = UiState.Idle
    }
}