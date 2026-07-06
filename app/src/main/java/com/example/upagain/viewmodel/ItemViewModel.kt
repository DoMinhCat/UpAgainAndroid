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

    private val _itemDetailState = MutableStateFlow<UiState<com.example.upagain.model.item.ItemDetailResponse>>(UiState.Idle)
    val itemDetailState: StateFlow<UiState<com.example.upagain.model.item.ItemDetailResponse>> = _itemDetailState

    private val _listingDetailState = MutableStateFlow<UiState<com.example.upagain.model.item.ListingDetailResponse>>(UiState.Idle)
    val listingDetailState: StateFlow<UiState<com.example.upagain.model.item.ListingDetailResponse>> = _listingDetailState

    private val _depositDetailState = MutableStateFlow<UiState<com.example.upagain.model.item.DepositDetailResponse>>(UiState.Idle)
    val depositDetailState: StateFlow<UiState<com.example.upagain.model.item.DepositDetailResponse>> = _depositDetailState

    private val _latestTransactionState = MutableStateFlow<UiState<com.example.upagain.model.transaction.TransactionResponse>>(UiState.Idle)
    val latestTransactionState: StateFlow<UiState<com.example.upagain.model.transaction.TransactionResponse>> = _latestTransactionState

    private val _purchaseState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val purchaseState: StateFlow<UiState<String>> = _purchaseState

    private val _reserveState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val reserveState: StateFlow<UiState<Unit>> = _reserveState

    private val _cancelReserveState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val cancelReserveState: StateFlow<UiState<Unit>> = _cancelReserveState

    fun fetchItemDetailsComplete(id: Int) {
        viewModelScope.launch {
            _itemDetailState.value = UiState.Loading()
            _listingDetailState.value = UiState.Idle
            _depositDetailState.value = UiState.Idle
            
            repository.getItemDetails(id).onSuccess { itemResponse ->
                _itemDetailState.value = UiState.Success(itemResponse)
                
                if (itemResponse.category == "listing") {
                    _listingDetailState.value = UiState.Loading()
                    repository.getListingDetails(id).onSuccess { listingResponse ->
                        _listingDetailState.value = UiState.Success(listingResponse)
                    }.onFailure { ex ->
                        _listingDetailState.value = UiState.Error((ex as? HttpException)?.code(), ex)
                    }
                } else if (itemResponse.category == "deposit") {
                    _depositDetailState.value = UiState.Loading()
                    repository.getDepositDetails(id).onSuccess { depositResponse ->
                        _depositDetailState.value = UiState.Success(depositResponse)
                    }.onFailure { ex ->
                        _depositDetailState.value = UiState.Error((ex as? HttpException)?.code(), ex)
                    }
                }
            }.onFailure { exception ->
                _itemDetailState.value = UiState.Error((exception as? HttpException)?.code(), exception)
            }
        }
    }

    fun getLatestTransactionOfPro(id: Int) {
        viewModelScope.launch {
            _latestTransactionState.value = UiState.Loading()
            repository.getLatestTransactionOfPro(id).onSuccess { response ->
                _latestTransactionState.value = UiState.Success(response)
            }.onFailure { exception ->
                _latestTransactionState.value = UiState.Error((exception as? HttpException)?.code(), exception)
            }
        }
    }

    fun purchaseItem(id: Int, payload: com.example.upagain.model.transaction.ItemPurchaseRequest) {
        viewModelScope.launch {
            _purchaseState.value = UiState.Loading()
            repository.purchaseItem(id, payload).onSuccess { response ->
                _purchaseState.value = UiState.Success(response)
            }.onFailure { exception ->
                _purchaseState.value = UiState.Error((exception as? HttpException)?.code(), exception)
            }
        }
    }

    fun resetPurchaseState() {
        _purchaseState.value = UiState.Idle
    }

    fun reserveItem(id: Int) {
        viewModelScope.launch {
            _reserveState.value = UiState.Loading()
            repository.reserveItem(id).onSuccess { response ->
                _reserveState.value = UiState.Success(response)
            }.onFailure { exception ->
                _reserveState.value = UiState.Error((exception as? HttpException)?.code(), exception)
            }
        }
    }
    
    fun resetReserveState() {
        _reserveState.value = UiState.Idle
    }

    fun cancelReservation(id: Int) {
        viewModelScope.launch {
            _cancelReserveState.value = UiState.Loading()
            repository.cancelItemReservation(id).onSuccess { response ->
                _cancelReserveState.value = UiState.Success(response)
            }.onFailure { exception ->
                _cancelReserveState.value = UiState.Error((exception as? HttpException)?.code(), exception)
            }
        }
    }

    fun resetCancelReservationState() {
        _cancelReserveState.value = UiState.Idle
    }
}