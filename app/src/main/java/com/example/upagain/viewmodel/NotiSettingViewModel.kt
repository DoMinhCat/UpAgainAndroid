package com.example.upagain.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.upagain.model.noti.NotiSetting
import com.example.upagain.repository.NotiSettingRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class NotiSettingViewModel(private val repository: NotiSettingRepo, application: Application) :
    AndroidViewModel(application) {

    private val _notiSettingsState = MutableStateFlow<UiState<List<NotiSetting>>>(UiState.Idle)
    val notiSettingsState: StateFlow<UiState<List<NotiSetting>>> = _notiSettingsState

    private val _notiUpdateState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val notiUpdateState: StateFlow<UiState<Unit>> = _notiUpdateState

    private val _alertMaterialsState = MutableStateFlow<UiState<List<String>>>(UiState.Idle)
    val alertMaterialsState: StateFlow<UiState<List<String>>> = _alertMaterialsState

    private val _alertMaterialsUpdateState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val alertMaterialsUpdateState: StateFlow<UiState<Unit>> = _alertMaterialsUpdateState

    fun loadNotificationSettings(idAccount: Int) {
        viewModelScope.launch {
            _notiSettingsState.value = UiState.Loading()
            repository.getNotificationSettings(idAccount)
                .onSuccess { settings ->
                    _notiSettingsState.value = UiState.Success(settings)
                }
                .onFailure { exception ->
                    val statusCode = (exception as? HttpException)?.code()
                    _notiSettingsState.value = UiState.Error(statusCode, exception)
                }
        }
    }

    fun updateNotificationSetting(idAccount: Int, notiType: String, isEnabled: Boolean) {
        viewModelScope.launch {
            _notiUpdateState.value = UiState.Loading()
            repository.updateNotificationSetting(idAccount, notiType, isEnabled)
                .onSuccess {
                    _notiUpdateState.value = UiState.Success(Unit)
                }
                .onFailure { exception ->
                    val statusCode = (exception as? HttpException)?.code()
                    _notiUpdateState.value = UiState.Error(statusCode, exception)
                }
        }
    }

    fun loadProAlertMaterials(idAccount: Int) {
        viewModelScope.launch {
            _alertMaterialsState.value = UiState.Loading()
            repository.getProAlertMaterials(idAccount)
                .onSuccess { materials ->
                    _alertMaterialsState.value = UiState.Success(materials)
                }
                .onFailure { exception ->
                    val statusCode = (exception as? HttpException)?.code()
                    _alertMaterialsState.value = UiState.Error(statusCode, exception)
                }
        }
    }

    fun updateProAlertMaterials(idAccount: Int, materials: List<String>) {
        viewModelScope.launch {
            _alertMaterialsUpdateState.value = UiState.Loading()
            repository.updateProAlertMaterials(idAccount, materials)
                .onSuccess {
                    _alertMaterialsUpdateState.value = UiState.Success(Unit)
                    // Refresh materials state locally to sync UI
                    _alertMaterialsState.value = UiState.Success(materials)
                }
                .onFailure { exception ->
                    val statusCode = (exception as? HttpException)?.code()
                    _alertMaterialsUpdateState.value = UiState.Error(statusCode, exception)
                }
        }
    }
}
