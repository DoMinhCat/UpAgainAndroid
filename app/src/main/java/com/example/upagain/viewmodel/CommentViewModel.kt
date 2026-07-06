package com.example.upagain.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.upagain.event.LikeCommentEvent
import com.example.upagain.model.comment.CommentDetailsResponse
import com.example.upagain.model.comment.CommentPaginationRequest
import com.example.upagain.model.comment.CommentPaginationResponse
import com.example.upagain.model.comment.CreateCommentRequest
import com.example.upagain.repository.CommentRepo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class CommentViewModel(private val repository: CommentRepo) : ViewModel() {

    private val _allCommentsState =
        MutableStateFlow<UiState<CommentPaginationResponse>>(UiState.Loading())
    val allCommentsState: StateFlow<UiState<CommentPaginationResponse>> = _allCommentsState

    private val _createCommentState =
        MutableStateFlow<UiState<CommentDetailsResponse>>(UiState.Idle)
    val createCommentState: StateFlow<UiState<CommentDetailsResponse>> = _createCommentState

    private val _likeCommentEvent = MutableSharedFlow<LikeCommentEvent>()
    val likeCommentEvent: SharedFlow<LikeCommentEvent> = _likeCommentEvent.asSharedFlow()
    private val _deleteCommentState =
        MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val deleteCommentState: StateFlow<UiState<Unit>> = _deleteCommentState

    fun createComment(idPost: Int, requestBody: CreateCommentRequest) {
        viewModelScope.launch {
            _createCommentState.value = UiState.Loading()

            repository.createComment(idPost, requestBody)
                .onSuccess { createCommentResponse ->
                    _createCommentState.value = UiState.Success(createCommentResponse)
                }
                .onFailure { exception ->
                    val statusCode = (exception as? HttpException)?.code()
                    _createCommentState.value = UiState.Error(statusCode, exception)
                }
        }
    }

    fun getPostComments(idPost: Int, requestBody: CommentPaginationRequest, isFirstPage: Boolean) {
        viewModelScope.launch {
            _allCommentsState.value = UiState.Loading(isFirstPage = isFirstPage)

            repository.getPostComments(idPost, requestBody)
                .onSuccess { allComments ->
                    _allCommentsState.value = UiState.Success(allComments)
                }
                .onFailure { exception ->
                    val statusCode = (exception as? HttpException)?.code()
                    _allCommentsState.value = UiState.Error(statusCode, exception)
                }
        }
    }

    fun loadPageOfComments(idPost: Int, pageNumber: Int) {
        getPostComments(idPost, CommentPaginationRequest(page = pageNumber), pageNumber == 1)
    }

    fun likeComment(idCmt: Int, position: Int = -1) {
        viewModelScope.launch {
            // optimistic update for like post, no loading state needed
            repository.likeComment(idCmt)
                .onSuccess { likeCommentResponse ->
                    _likeCommentEvent.emit(
                        LikeCommentEvent.Succeeded(
                            position,
                            idCmt,
                            likeCommentResponse.isLiked
                        )
                    )
                }
                .onFailure { exception ->
                    val statusCode = (exception as? HttpException)?.code()
                    _likeCommentEvent.emit(
                        LikeCommentEvent.Rollback(
                            position,
                            idCmt,
                            statusCode,
                            exception
                        )
                    )
                }
        }
    }

    fun deleteComment(idCmt: Int, position: Int = -1) {
        viewModelScope.launch {
            _deleteCommentState.value = UiState.Loading()

            repository.deleteComment(idCmt)
                .onSuccess { deleteCommentResponse ->
                    _deleteCommentState.value = UiState.Success(deleteCommentResponse)
                }
                .onFailure { exception ->
                    val statusCode = (exception as? HttpException)?.code()
                    _deleteCommentState.value = UiState.Error(statusCode, exception)
                }
        }
    }

}