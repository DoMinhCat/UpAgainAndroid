package com.example.upagain.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.upagain.model.post.PostPaginationRequest
import com.example.upagain.model.post.PostPaginationResponse
import com.example.upagain.repository.PostRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class PostViewModel(private val repository: PostRepo, application: Application) : AndroidViewModel(application) {
    private val context get() = getApplication<Application>().applicationContext

    private val _allPostsState = MutableStateFlow<UiState<PostPaginationResponse>>(UiState.Loading)
    val allPostsState: StateFlow<UiState<PostPaginationResponse>> = _allPostsState

    fun getAllPosts(requestBody: PostPaginationRequest) {
        viewModelScope.launch {
            _allPostsState.value = UiState.Loading

            repository.getAllPosts(requestBody)
                .onSuccess { allPostsData ->
                    _allPostsState.value = UiState.Success(allPostsData)
                }
                .onFailure { exception ->
                    val statusCode = (exception as? HttpException)?.code()
                    _allPostsState.value = UiState.Error(statusCode, exception)
                }
        }
    }
}