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
import com.example.upagain.model.post.ProjectStepResponse
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
//    private val context get() = getApplication<Application>().applicationContext

    private val _allPostsState =
        MutableStateFlow<UiState<PostPaginationResponse>>(UiState.Loading())
    val allPostsState: StateFlow<UiState<PostPaginationResponse>> = _allPostsState
    private val _savedPostsState =
        MutableStateFlow<UiState<PostPaginationResponse>>(UiState.Loading())
    val savedPostsState: StateFlow<UiState<PostPaginationResponse>> = _savedPostsState
    private val _myPostsState = MutableStateFlow<UiState<PostPaginationResponse>>(UiState.Loading())
    val myPostsState: StateFlow<UiState<PostPaginationResponse>> = _myPostsState
    private val _postDetailState = MutableStateFlow<UiState<PostDetailsResponse>>(UiState.Loading())
    val postDetailState: StateFlow<UiState<PostDetailsResponse>> = _postDetailState
    private val _projectStepsState =
        MutableStateFlow<UiState<List<ProjectStepResponse>>>(UiState.Idle)
    val projectStepsState: StateFlow<UiState<List<ProjectStepResponse>>> = _projectStepsState

    private val _savePostEvent = MutableSharedFlow<SavePostEvent>()
    val savePostEvent: SharedFlow<SavePostEvent> = _savePostEvent.asSharedFlow()
    private val _likePostEvent = MutableSharedFlow<LikePostEvent>()
    val likePostEvent: SharedFlow<LikePostEvent> = _likePostEvent.asSharedFlow()

    private var currentGetAllFilters = PostPaginationRequest(page = 1)
    private var currentGetSavedFilters = PostPaginationRequest(page = 1)
    private var currentGetMyPostsFilters = PostPaginationRequest(page = 1)

    // GET ALL POST METHODS
    fun getAllPosts(requestBody: PostPaginationRequest, isFirstPage: Boolean) {
        viewModelScope.launch {
            // only show full screen load if loading the first page
            _allPostsState.value = UiState.Loading(isFirstPage = isFirstPage)

            repository.getAllPosts(requestBody).onSuccess { allPostsData ->
                    _allPostsState.value = UiState.Success(allPostsData)
                }.onFailure { exception ->
                    val statusCode = (exception as? HttpException)?.code()
                    _allPostsState.value = UiState.Error(statusCode, exception)
                }
        }
    }

    fun loadPageOfAllPosts(pageNumber: Int) {
        currentGetAllFilters = currentGetAllFilters.copy(page = pageNumber)
        getAllPosts(currentGetAllFilters, pageNumber == 1)
    }

    // GET SAVED POST METHODS
    fun getSavedPosts(requestBody: PostPaginationRequest, isFirstPage: Boolean) {
        viewModelScope.launch {
            // only show full screen load if loading the first page
            _savedPostsState.value = UiState.Loading(isFirstPage = isFirstPage)

            repository.getSavedPosts(requestBody).onSuccess { allPostsData ->
                    _savedPostsState.value = UiState.Success(allPostsData)
                }.onFailure { exception ->
                    val statusCode = (exception as? HttpException)?.code()
                    _savedPostsState.value = UiState.Error(statusCode, exception)
                }
        }
    }

    fun loadPageOfSavedPosts(pageNumber: Int) {
        currentGetSavedFilters = currentGetSavedFilters.copy(page = pageNumber)
        getSavedPosts(currentGetSavedFilters, pageNumber == 1)
    }

    fun updateSavedPostsCategoryFilter(categoryOption: PostCategory) {
        currentGetSavedFilters = currentGetSavedFilters.copy(category = categoryOption, page = 1)
    }

    fun updateSortFilter(sortOption: PostSortOption?) {
        currentGetAllFilters = currentGetAllFilters.copy(sort = sortOption, page = 1)
    }

    fun updateAllPostsCategoryFilter(categoryOption: PostCategory) {
        currentGetAllFilters = currentGetAllFilters.copy(category = categoryOption, page = 1)
    }

    fun updateSearchAllPostsFilter(query: String) {
        currentGetAllFilters = currentGetAllFilters.copy(search = query, page = 1)
    }

    // GET MY POST METHODS
    fun getMyPosts(requestBody: PostPaginationRequest, isFirstPage: Boolean) {
        viewModelScope.launch {
            // only show full screen load if loading the first page
            _myPostsState.value = UiState.Loading(isFirstPage = isFirstPage)

            repository.getMyPosts(requestBody).onSuccess { myPostsData ->
                    _myPostsState.value = UiState.Success(myPostsData)
                }.onFailure { exception ->
                    val statusCode = (exception as? HttpException)?.code()
                    _myPostsState.value = UiState.Error(statusCode, exception)
                }
        }
    }

    fun loadPageOfMyPosts(pageNumber: Int) {
        currentGetMyPostsFilters = currentGetMyPostsFilters.copy(page = pageNumber)
        getMyPosts(currentGetMyPostsFilters, pageNumber == 1)
    }

    fun updateSearchMyPostsFilter(query: String) {
        currentGetMyPostsFilters = currentGetMyPostsFilters.copy(search = query, page = 1)
    }

    // OTHER METHODS
    fun getPostDetails(idPost: Int) {
        viewModelScope.launch {
            _postDetailState.value = UiState.Loading()

            repository.getPostDetails(idPost).onSuccess { postDetails ->
                    _postDetailState.value = UiState.Success(postDetails)
                    val category = postDetails.category
                    if (category == PostCategory.PROJECT) {
                        getProjectSteps(idPost)
                    }
                }.onFailure { exception ->
                    val statusCode = (exception as? HttpException)?.code()
                    _postDetailState.value = UiState.Error(statusCode, exception)
                }
        }
    }

    fun getProjectSteps(idPost: Int) {
        viewModelScope.launch {
            _projectStepsState.value = UiState.Loading()

            repository.getProjectSteps(idPost).onSuccess { steps ->
                    _projectStepsState.value = UiState.Success(steps)
                }.onFailure { exception ->
                    val statusCode = (exception as? HttpException)?.code()
                    _projectStepsState.value = UiState.Error(statusCode, exception)
                }
        }
    }

    fun savePost(id: Int, position: Int = -1) {
        // for optimistic update, emit success or fallback event to tell fragment to sync the data correspondingly
        viewModelScope.launch {
            // optimistic update for save post, no loading state needed
            repository.savePost(id).onSuccess { savePostResponse ->
                    _savePostEvent.emit(
                        SavePostEvent.Succeeded(
                            position, id, savePostResponse.isSaved
                        )
                    )
                }.onFailure { exception ->
                    val statusCode = (exception as? HttpException)?.code()
                    // Broadcast error containing exact instructions on what index row to roll back
                    _savePostEvent.emit(SavePostEvent.Rollback(position, id, statusCode, exception))
                }
        }
    }

    fun likePost(id: Int, position: Int = -1) {
        viewModelScope.launch {
            // optimistic update for like post, no loading state needed
            repository.likePost(id).onSuccess { likePostResponse ->
                    _likePostEvent.emit(
                        LikePostEvent.Succeeded(
                            position, id, likePostResponse.isLiked
                        )
                    )
                }.onFailure { exception ->
                    val statusCode = (exception as? HttpException)?.code()
                    _likePostEvent.emit(LikePostEvent.Rollback(position, id, statusCode, exception))
                }
        }
    }
}