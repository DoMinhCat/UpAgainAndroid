package com.example.upagain.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.upagain.model.LoginRequest
import com.example.upagain.model.post.PostPaginationRequest
import com.example.upagain.model.post.PostPaginationResponse
import com.example.upagain.model.post.SavePostResponse
import com.example.upagain.repository.PostRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class PostViewModel(private val repository: PostRepo, application: Application) :
    AndroidViewModel(application) {
    private val context get() = getApplication<Application>().applicationContext

    private val _allPostsState = MutableStateFlow<UiState<PostPaginationResponse>>(UiState.Loading())
    val allPostsState: StateFlow<UiState<PostPaginationResponse>> = _allPostsState
    private val _savePostsState = MutableStateFlow<UiState<SavePostResponse>>(UiState.Idle)
    val savePostsState: StateFlow<UiState<SavePostResponse>> = _savePostsState

    private var currentFilters = PostPaginationRequest(page = 1)
    fun getAllPosts(requestBody: PostPaginationRequest, isFirstPage: Boolean) {
        viewModelScope.launch {
            // only show full screen load if loading the first page
            _allPostsState.value = UiState.Loading(isFirstPage = isFirstPage)

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

    fun loadPageOfAllPosts(pageNumber: Int) {
        currentFilters = currentFilters.copy(page = pageNumber)
        getAllPosts(currentFilters, pageNumber == 1)
    }

    fun savePost(id: Int) {
        viewModelScope.launch {
            _savePostsState.value = UiState.Loading()

            repository.savePost(id)
                .onSuccess { savePostResponse ->
                    _savePostsState.value = UiState.Success(savePostResponse)
                }
                .onFailure { exception ->
                    val statusCode = (exception as? HttpException)?.code()
                    _savePostsState.value = UiState.Error(statusCode, exception)
                }
        }
    }
}