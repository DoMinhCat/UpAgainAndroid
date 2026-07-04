package com.example.upagain.feat.post.detail

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.upagain.R
import com.example.upagain.api.ApiClient
import com.example.upagain.databinding.FragmentPostDetailBinding
import com.example.upagain.event.LikeCommentEvent
import com.example.upagain.event.LikePostEvent
import com.example.upagain.event.SavePostEvent
import com.example.upagain.feat.error.ErrorActivity
import com.example.upagain.model.comment.CommentDetailsResponse
import com.example.upagain.model.comment.CreateCommentRequest
import com.example.upagain.model.post.PostDetailsResponse
import com.example.upagain.model.post.ProjectStepResponse
import com.example.upagain.repository.CommentRepo
import com.example.upagain.repository.PostRepo
import com.example.upagain.util.auth.SessionManager
import com.example.upagain.util.bin.ImageType
import com.example.upagain.util.bin.buildImageUrl
import com.example.upagain.util.datetime.formatTimestamptz
import com.example.upagain.util.ui.SnackbarLevel
import com.example.upagain.util.ui.setOnBackClickListener
import com.example.upagain.util.ui.setOnClickListenerWithCooldown
import com.example.upagain.util.ui.setPostCategoryTextAndColor
import com.example.upagain.util.ui.showTopSnackbar
import com.example.upagain.util.ui.toggleBtnLoadingState
import com.example.upagain.util.ui.toggleFullScreenLoading
import com.example.upagain.viewmodel.CommentViewModel
import com.example.upagain.viewmodel.PostViewModel
import com.example.upagain.viewmodel.UiState
import com.example.upagain.viewmodel.ViewModelFactory
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

private const val ARG_POST_ID = "arg_post_id"

class PostDetailFragment : Fragment() {
    private var idPost: Int? = null

    // elements binding
    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!
    private val apiService by lazy { ApiClient.apiService }
    private val postRepository by lazy { PostRepo(apiService) }
    private val commentRepository by lazy { CommentRepo(apiService) }
    private val appInstance by lazy { requireActivity().application }
    private val postViewModel: PostViewModel by viewModels {
        ViewModelFactory { PostViewModel(postRepository, appInstance) }
    }
    private val commentViewModel: CommentViewModel by viewModels {
        ViewModelFactory { CommentViewModel(commentRepository) }
    }

    private lateinit var commentAdapter: CommentRecyclerViewAdapter
    private lateinit var carouselAdapter: CarouselImageAdapter
    private lateinit var stepsAdapter: ProjectStepsAdapter
    private var currentCommentPage = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            idPost = it.getInt(ARG_POST_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // ! Always set up listeners and observers before API call
        setupRecyclerView()
        setupListeners()
        observeState()

        // API call
        idPost?.let { id ->
            postViewModel.getPostDetails(id)
            commentViewModel.loadPageOfComments(id, 1)
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param idPost The ID of the post to display.
         * @return A new instance of fragment PostDetailFragment.
         */
        @JvmStatic
        fun newInstance(idPost: Int) =
            PostDetailFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_POST_ID, idPost)
                }
            }
    }

    // PRIVATE ZONE
    private fun setupRecyclerView() {
        binding.rvComments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProjectSteps.layoutManager = LinearLayoutManager(requireContext())

        // init empty, data will be passed in observer once api response arrive
        commentAdapter = CommentRecyclerViewAdapter(
            SessionManager.accountId ?: -1,
            false,
            object : CommentRecyclerViewAdapter.OnClickListener {
                override fun onLoadMoreClick() {
                    idPost?.let { id ->
                        commentViewModel.loadPageOfComments(id, currentCommentPage + 1)
                    }
                }

                override fun onDeleteClick(commentId: Int, position: Int) {
                    commentViewModel.deleteComment(commentId)
                }

                override fun onLikeClick(position: Int, comment: CommentDetailsResponse) {
                    // optimistic update
                    comment.isLiked = !comment.isLiked
                    if (comment.isLiked)
                        comment.likeCount += 1
                    else comment.likeCount -= 1
                    commentAdapter.notifyItemChanged(position)
                    commentViewModel.likeComment(comment.id, position)
                }
            })
        stepsAdapter = ProjectStepsAdapter(
            isEditable = false,
            onStepImageClick = { absoluteUrl ->
                showFullScreenImageDialog(absoluteUrl) // Both adapters open the exact same fullscreen viewer!
            },
            listener = object : ProjectStepsAdapter.OnStepClickListener {
                override fun onEditClick(step: ProjectStepResponse) {
                    // TODO
                    // Handle step edit navigation or action dialog sheet
                }

                override fun onDeleteClick(step: ProjectStepResponse) {
                    // TODO
                    // Trigger verification dialog or network delete routine
                }
            },
        )
        carouselAdapter = CarouselImageAdapter { absoluteUrl ->
            showFullScreenImageDialog(absoluteUrl)
        }

        binding.rvProjectSteps.adapter = stepsAdapter
        binding.rvComments.adapter = commentAdapter
        binding.vpCarousel.adapter = carouselAdapter
    }

    private fun setupListeners() {
        // BACK
        binding.btnBack.setOnBackClickListener()
        // LIKE
        binding.btnActionLike.setOnClickListenerWithCooldown(500L) {
            val post = getPostData()
            if (post != null) {
                post.isLiked = !post.isLiked
                toggleLikeIconAndCount(post.isLiked, post)

                postViewModel.likePost(post.id)
            } else {
                // The post hasn't loaded yet or failed
            }
        }
        // SAVE
        binding.btnActionSave.setOnClickListenerWithCooldown(500L) {
            val post = getPostData()
            if (post != null) {
                // optimistic update
                post.isSaved = !post.isSaved
                toggleSaveIconAndText(post.isSaved)
                postViewModel.savePost(post.id)
            } else {
                // The post hasn't loaded yet or failed
            }
        }
        // SEND COMMENT
        binding.btnSendComment.setOnClickListenerWithCooldown {
            idPost?.let { id ->
                val commentBody = binding.etWriteComment.text.toString().trim()
                if (commentBody.isEmpty()) {
                    return@setOnClickListenerWithCooldown
                }
                commentViewModel.createComment(
                    id,
                    CreateCommentRequest(commentBody)
                )
            }
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // GET POST DETAIL
                launch {
                    postViewModel.postDetailState.collect { state ->
                        when (state) {
                            is UiState.Idle -> {}
                            is UiState.Loading -> {
                                binding.loadingOverlay.root.toggleFullScreenLoading(true)
                            }

                            is UiState.Success -> {
                                binding.loadingOverlay.root.toggleFullScreenLoading(false)
                                val post = state.data

                                // show image in carousel
                                val photosList = post.photos.orEmpty()
                                carouselAdapter.submitList(photosList)
                                if (photosList.size > 1) {
                                    binding.tlCarouselIndicator.visibility = View.VISIBLE
                                    TabLayoutMediator(
                                        binding.tlCarouselIndicator,
                                        binding.vpCarousel
                                    ) { _, _ ->
                                    }.attach()
                                } else {
                                    // Hide indicator entirely if there is only 1 or 0 images
                                    binding.tlCarouselIndicator.visibility = View.GONE
                                }

                                binding.ivAuthorAvatar.load(
                                    buildImageUrl(
                                        post.creatorAvatar,
                                        ImageType.AVATAR
                                    )
                                ) {
                                    crossfade(true)
                                    placeholder(R.drawable.ic_avatar_unknown)
                                    error(R.drawable.ic_avatar_unknown)
                                }

                                // feed text data
                                binding.tvLikeCount.text = post.likeCount.toString()
                                binding.postCategory.setPostCategoryTextAndColor(
                                    requireContext(),
                                    post.category.toString()
                                )
                                binding.tvTitle.text = post.title
                                binding.tvAuthorName.text = post.creator
                                binding.tvPostTime.text = formatTimestamptz(post.createdAt)
                                binding.tvHtmlContentBody.text = HtmlCompat.fromHtml(
                                    post.content,
                                    HtmlCompat.FROM_HTML_MODE_LEGACY
                                )
                                binding.tvCommentCount.text =
                                    getString(R.string.comment_count, post.commentCount)
                                toggleLikeIconAndCount(post.isLiked, null)
                                toggleSaveIconAndText(post.isSaved)
                            }

                            is UiState.Error -> {
                                binding.main.toggleFullScreenLoading(false)
                                Log.e(
                                    "PostDetailFragment",
                                    "Load post details failed. Status code: ${state.statusCode}",
                                    state.exception
                                )
                                ErrorActivity.start(requireContext(), state.statusCode ?: 0)
                            }
                        }
                    }

                }
                // GET COMMENTS
                launch {
                    commentViewModel.allCommentsState.collect { state ->
                        when (state) {
                            is UiState.Idle -> {}
                            is UiState.Loading -> {
                                toggleCommentLoadingState(true, state.isFirstPage)
                            }

                            is UiState.Success -> {
                                val commentsResponse = state.data
                                val newComments = commentsResponse.comments.orEmpty()

                                // handle loading state
                                if (commentsResponse.currentPage == 1) {
                                    toggleCommentLoadingState(isLoading = false, isFirstPage = true)
                                } else {
                                    commentAdapter.toggleLoadMoreBtnLoadingState(false)
                                }

                                val combinedList = if (commentsResponse.currentPage == 1) {
                                    newComments
                                } else {
                                    // append the new page comments to existing comments
                                    commentAdapter.currentList + newComments
                                }
                                // tell adapter to draw load more btn at the bottom or not
                                val hasMore =
                                    commentsResponse.currentPage < commentsResponse.lastPage
                                commentAdapter.updatePaginationStatus(hasMore)

                                commentAdapter.submitList(combinedList)

                                // handle empty state
                                if (combinedList.isEmpty()) {
                                    binding.layoutCommentsEmpty.visibility = View.VISIBLE
                                    binding.rvComments.visibility = View.GONE
                                } else {
                                    binding.layoutCommentsEmpty.visibility = View.GONE
                                    binding.rvComments.visibility = View.VISIBLE
                                }
                                currentCommentPage = commentsResponse.currentPage
                            }

                            is UiState.Error -> {
                                commentAdapter.toggleLoadMoreBtnLoadingState(false)
                                toggleCommentLoadingState(
                                    false,
                                    isFirstPage = (currentCommentPage == 1)
                                )
                                Log.e(
                                    "PostDetailFragment",
                                    "Load post's comments failed. Status code: ${state.statusCode}",
                                    state.exception
                                )
                                ErrorActivity.start(requireContext(), state.statusCode ?: 0)
                            }
                        }
                    }
                }
                // SAVE POST
                launch {
                    postViewModel.savePostEvent.collect { event ->
                        when (event) {
                            is SavePostEvent.Succeeded -> {
                                toggleSaveIconAndText(event.isSaved)
                            }

                            is SavePostEvent.Rollback -> {
                                val post = getPostData()
                                if (post != null) {
                                    // Revert isSaved status since update on server failed
                                    post.isSaved = !post.isSaved
                                    toggleSaveIconAndText(post.isSaved)
                                    Log.e(
                                        "PostDetailFragment",
                                        "Save failed for Post ${event.idPost}. Status code: ${event.statusCode}",
                                        event.exception
                                    )
                                }
                                binding.main.showTopSnackbar(
                                    R.string.err_save_post_msg,
                                    SnackbarLevel.ERROR
                                )
                            }
                        }
                    }
                }
                // LIKE POST
                launch {
                    postViewModel.likePostEvent.collect { event ->
                        when (event) {
                            is LikePostEvent.Succeeded -> {
                                // Do nothing, data and state already updated optimistically
                            }

                            is LikePostEvent.Rollback -> {
                                val post = getPostData()
                                if (post != null) {
                                    // Revert isLiked status since update on server failed
                                    post.isLiked = !post.isLiked
                                    toggleLikeIconAndCount(post.isLiked, post)
                                    post.likeCount -= 1
                                    Log.e(
                                        "PostDetailFragment",
                                        "Like failed for Post ${event.idPost}. Status code: ${event.statusCode}",
                                        event.exception
                                    )
                                }
                                binding.main.showTopSnackbar(
                                    R.string.err_like_post_msg,
                                    SnackbarLevel.ERROR
                                )
                            }
                        }
                    }
                }
                // PROJECT STEPS
                launch {
                    postViewModel.projectStepsState.collect { state ->
                        when (state) {
                            is UiState.Idle -> {
                                binding.layoutProjectStepsContainer.visibility = View.GONE
                            }

                            is UiState.Loading -> {
                                // Keep the area frame wrapper visible, but shift view focal state to the loader spinner
                                binding.layoutProjectStepsContainer.visibility = View.VISIBLE
                                binding.stepsLoader.visibility = View.VISIBLE
                                binding.rvProjectSteps.visibility = View.GONE
                                binding.layoutStepsError.visibility = View.GONE
                            }

                            is UiState.Success -> {
                                val steps = state.data

                                // Hide the loader container spinner
                                binding.stepsLoader.visibility = View.GONE
                                binding.layoutStepsError.visibility = View.GONE

                                if (steps.isEmpty()) {
                                    binding.rvProjectSteps.visibility = View.GONE
                                    binding.layoutStepsEmpty.visibility = View.VISIBLE
                                } else {
                                    binding.rvProjectSteps.visibility = View.VISIBLE
                                    stepsAdapter.submitList(steps)
                                }
                            }

                            is UiState.Error -> {
                                // Clear the loading indicator widget
                                binding.stepsLoader.visibility = View.GONE
                                binding.rvProjectSteps.visibility = View.GONE

                                binding.layoutStepsError.visibility = View.VISIBLE
                                binding.ivStepsErrorImage.setImageResource(R.drawable.ic_error)

                                Log.e(
                                    "PostDetailFragment",
                                    "Failed to load project steps. Status code: ${state.statusCode}",
                                    state.exception
                                )
                            }
                        }
                    }
                }
                // NEW COMMENT
                launch {
                    commentViewModel.createCommentState.collect { state ->
                        when (state) {
                            is UiState.Idle -> {}

                            is UiState.Loading -> {
                                toggleSendCommentBtnLoading(true)
                            }

                            is UiState.Success -> {
                                toggleSendCommentBtnLoading(false)

                                binding.etWriteComment.text?.clear()
                                currentCommentPage = 1
                                // reload comments
                                idPost?.let { id ->
                                    commentViewModel.loadPageOfComments(id, 1)
                                }
                                // update count
                                getPostData()?.commentCount += 1
                                binding.tvCommentCount.text =
                                    getString(R.string.comment_count, getPostData()?.commentCount)
                            }

                            is UiState.Error -> {
                                toggleSendCommentBtnLoading(false)
                                Log.e(
                                    "PostDetailFragment",
                                    "Send comment failed. Status code: ${state.statusCode}",
                                    state.exception
                                )
                                binding.main.showTopSnackbar(
                                    R.string.err_create_comment_msg,
                                    SnackbarLevel.ERROR
                                )
                            }
                        }
                    }
                }
                // LIKE COMMENT
                launch {
                    commentViewModel.likeCommentEvent.collect { event ->
                        when (event) {
                            is LikeCommentEvent.Succeeded -> {
                                // optimistic update succeeded, do nothing
                            }

                            is LikeCommentEvent.Rollback -> {
                                val failedPosition =
                                    event.position // Assuming your state wraps the targeted index row

                                if (failedPosition < commentAdapter.currentList.size) {
                                    val comment = commentAdapter.currentList[failedPosition]
                                    comment.isLiked = !comment.isLiked
                                    if (comment.isLiked) {
                                        comment.likeCount += 1
                                    } else {
                                        comment.likeCount -= 1
                                    }

                                    commentAdapter.notifyItemChanged(failedPosition)
                                }
                                binding.main.showTopSnackbar(
                                    R.string.err_like_comment_msg,
                                    SnackbarLevel.ERROR
                                )
                                Log.e(
                                    "PostDetailFragment",
                                    "Like failed for Comment ${event.idComment}. Status code: ${event.statusCode}",
                                    event.exception
                                )
                            }
                        }
                    }
                }
                // DELETE COMMENT
                launch {
                    commentViewModel.deleteCommentState.collect { state ->
                        when (state) {
                            is UiState.Idle -> {}

                            is UiState.Loading -> {}

                            is UiState.Success -> {
                                // refetch comments
                                currentCommentPage = 1
                                idPost?.let { id ->
                                    commentViewModel.loadPageOfComments(id, 1)
                                }
                                // update count
                                getPostData()?.commentCount -= 1
                                binding.tvCommentCount.text =
                                    getString(R.string.comment_count, getPostData()?.commentCount)
                            }

                            is UiState.Error -> {
                                Log.e(
                                    "PostDetailFragment",
                                    "Failed to delete comment. Status code: ${state.statusCode}",
                                    state.exception
                                )
                                binding.main.showTopSnackbar(
                                    R.string.err_delete_comment_msg,
                                    SnackbarLevel.ERROR
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getPostData(): PostDetailsResponse? {
        val currentState = postViewModel.postDetailState.value
        if (currentState is UiState.Success) {
            return currentState.data
        }
        return null
    }

    private fun toggleCommentLoadingState(isLoading: Boolean, isFirstPage: Boolean) {
        if (isFirstPage) {
            toggleCommentAreaLoading(isLoading)
        } else {
            commentAdapter.toggleLoadMoreBtnLoadingState(isLoading)
        }
    }

    private fun toggleCommentAreaLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.rvComments.visibility = View.INVISIBLE
            binding.commentsLoader.visibility = View.VISIBLE
        } else {
            binding.rvComments.visibility = View.VISIBLE
            binding.commentsLoader.visibility = View.GONE
        }
    }

    private fun toggleSendCommentBtnLoading(isLoading: Boolean) {
        toggleBtnLoadingState(
            binding.btnSendComment,
            binding.sendCommentLoader,
            isLoading,
            "",
            AppCompatResources.getDrawable(requireContext(), R.drawable.ic_send)
        )
    }

    private fun toggleSaveIconAndText(isSaved: Boolean) {
        if (isSaved) {
            binding.btnActionSave.icon = AppCompatResources.getDrawable(
                binding.btnActionSave.context,
                R.drawable.ic_bookmark_filled
            )
            binding.tvSaveLabel.text = getString(R.string.btn_saved)
        } else {
            binding.btnActionSave.icon = AppCompatResources.getDrawable(
                binding.btnActionSave.context,
                R.drawable.ic_bookmark_outline
            )
            binding.tvSaveLabel.text = getString(R.string.btn_save)
        }
    }

    private fun toggleLikeIconAndCount(isLiked: Boolean, post: PostDetailsResponse?) {
        if (isLiked) {
            binding.btnActionLike.icon = AppCompatResources.getDrawable(
                binding.btnActionLike.context,
                R.drawable.ic_love_filled
            )
            if (post != null) {
                post.likeCount += 1
                binding.tvLikeCount.text = post.likeCount.toString()
            }
        } else {
            binding.btnActionLike.icon = AppCompatResources.getDrawable(
                binding.btnActionLike.context,
                R.drawable.ic_love_outline
            )
            if (post != null) {
                post.likeCount -= 1
                binding.tvLikeCount.text = post.likeCount.toString()
            }
        }
    }

    private fun showFullScreenImageDialog(absoluteImageUrl: String) {
        // Create an unstyled fullscreen dialog wrapper window context
        val dialog =
            android.app.Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_fullscreen_image)
        dialog.setCancelable(true)

        val imageView =
            dialog.findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.iv_fullscreen_target)
        val btnClose = dialog.findViewById<android.widget.ImageView>(R.id.btn_close_fullscreen)

        // Populate image immediately
        imageView.load(absoluteImageUrl) {
            crossfade(true)
            placeholder(R.color.color_surface)
            error(R.drawable.fall_back_image)
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}