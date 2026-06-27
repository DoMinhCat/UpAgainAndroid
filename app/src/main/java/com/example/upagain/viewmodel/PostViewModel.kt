package com.example.upagain.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.upagain.repository.PostRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PostViewModel(private val repository: PostRepo, application: Application) : AndroidViewModel(application) {
    private val context get() = getApplication<Application>().applicationContext

//    private val _openContainerState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
//    val openContainerState: StateFlow<UiState<Unit>> = _openContainerState
}