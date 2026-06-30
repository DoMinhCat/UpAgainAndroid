package com.example.upagain.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.upagain.event.LikePostEvent
import com.example.upagain.event.SavePostEvent
import com.example.upagain.model.post.PostCategory
import com.example.upagain.model.post.PostDetailsResponse
import com.example.upagain.model.post.PostPaginationRequest
import com.example.upagain.model.post.PostPaginationResponse
import com.example.upagain.model.post.PostSortOption
import com.example.upagain.repository.PostRepo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class PostViewModel(private val repository: PostRepo, application: Application) :
    AndroidViewModel(application) {
    private val context get() = getApplication<Application>().applicationContext

    private val _allPostsState = MutableStateFlow<UiState<PostPaginationResponse>>(UiState.Loading())
    val allPostsState: StateFlow<UiState<PostPaginationResponse>> = _allPostsState
    private val _postDetailState = MutableStateFlow<UiState<PostDetailsResponse>>(UiState.Loading())
    val postDetailState: StateFlow<UiState<PostDetailsResponse>> = _postDetailState

    private val _savePostEvent = MutableSharedFlow<SavePostEvent>()
    val savePostEvent: SharedFlow<SavePostEvent> = _savePostEvent.asSharedFlow()
    private val _likePostEvent = MutableSharedFlow<LikePostEvent>()
    val likePostEvent: SharedFlow<LikePostEvent> = _likePostEvent.asSharedFlow()

    private var currentFilters = PostPaginationRequest(page = 1)

    // GET ALL POST METHODS
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

    fun updateSortFilter(sortOption: PostSortOption?) {
        currentFilters = currentFilters.copy(sort = sortOption, page = 1)
    }

    fun updateCategoryFilter(categoryOption: PostCategory?) {
        currentFilters = currentFilters.copy(category = categoryOption, page = 1)
    }

    fun updateSearchFilter(query: String) {
        currentFilters = currentFilters.copy(search = query, page = 1)
    }

    // OTHER METHODS
    fun getPostDetails(idPost: Int) {
        viewModelScope.launch {
            _postDetailState.value = UiState.Loading()

            repository.getPostDetails(idPost)
                .onSuccess { postDetails ->
                    _postDetailState.value = UiState.Success(postDetails)
                }
                .onFailure { exception ->
                    val statusCode = (exception as? HttpException)?.code()
                    _postDetailState.value = UiState.Error(statusCode, exception)
                }
        }
    }
    fun savePost(id: Int, position: Int) {
        // for optimistic update, emit success or fallback event to tell fragment to sync the data correspondingly
        viewModelScope.launch {
            // optimistic update for save post, no loading state needed
            repository.savePost(id)
                .onSuccess { savePostResponse ->
                    _savePostEvent.emit(SavePostEvent.Succeeded(position, id, savePostResponse.isSaved))
                }
                .onFailure { exception ->
                    val statusCode = (exception as? HttpException)?.code()
                    // Broadcast error containing exact instructions on what index row to roll back
                    _savePostEvent.emit(SavePostEvent.Rollback(position, id, statusCode, exception))
                }
        }
    }

    fun likePost(id: Int, position: Int) {
        viewModelScope.launch {
            // optimistic update for like post, no loading state needed
            repository.likePost(id)
                .onSuccess { likePostResponse ->
                    _likePostEvent.emit(LikePostEvent.Succeeded(position, id, likePostResponse.isLiked))
                }
                .onFailure { exception ->
                    val statusCode = (exception as? HttpException)?.code()
                    _likePostEvent.emit(LikePostEvent.Rollback(position, id, statusCode, exception))
                }
        }
    }
}