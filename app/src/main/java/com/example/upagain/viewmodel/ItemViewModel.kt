package com.example.upagain.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.upagain.model.item.MyItemsResponse
import com.example.upagain.repository.ItemRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class ItemViewModel(private val repository: ItemRepo, application: Application) :
    AndroidViewModel(application) {
    private val context get() = getApplication<Application>().applicationContext

    private val _myItemsState =
        MutableStateFlow<UiState<MyItemsResponse>>(UiState.Idle)
    val myItemsState: StateFlow<UiState<MyItemsResponse>> = _myItemsState

    private val _allItemsState =
        MutableStateFlow<UiState<MyItemsResponse>>(UiState.Loading())
    val allItemsState: StateFlow<UiState<MyItemsResponse>> = _allItemsState

    fun getMyItems(page: Int? = 1, limit: Int? = 100) {
        viewModelScope.launch {
            _myItemsState.value = UiState.Loading()

            repository.getMyItems(page, limit).onSuccess { response ->
                _myItemsState.value = UiState.Success(response)
            }.onFailure { exception ->
                val statusCode = (exception as? HttpException)?.code()
                _myItemsState.value = UiState.Error(statusCode, exception)
            }
        }
    }

    fun getMyItemsPaginated(options: Map<String, String>, isFirstPage: Boolean = true) {
        viewModelScope.launch {
            _myItemsState.value = UiState.Loading(isFirstPage)

            repository.getMyItemsPaginated(options).onSuccess { response ->
                _myItemsState.value = UiState.Success(response)
            }.onFailure { exception ->
                val statusCode = (exception as? HttpException)?.code()
                _myItemsState.value = UiState.Error(statusCode, exception)
            }
        }
    }

    fun resetMyItemsState() {
        _myItemsState.value = UiState.Idle
    }

    fun getAllItems(options: Map<String, String>, isFirstPage: Boolean = true) {
        viewModelScope.launch {
            _allItemsState.value = UiState.Loading(isFirstPage)

            repository.getAllItems(options).onSuccess { response ->
                _allItemsState.value = UiState.Success(response)
            }.onFailure { exception ->
                val statusCode = (exception as? HttpException)?.code()
                _allItemsState.value = UiState.Error(statusCode, exception)
            }
        }
    }

    fun resetAllItemsState() {
        _allItemsState.value = UiState.Idle
    }
}