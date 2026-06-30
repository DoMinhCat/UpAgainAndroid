package com.example.upagain.feat.post.detail

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.upagain.api.ApiClient
import com.example.upagain.databinding.FragmentPostDetailBinding
import com.example.upagain.feat.error.ErrorActivity
import com.example.upagain.repository.PostRepo
import com.example.upagain.util.datetime.formatTimestamptz
import com.example.upagain.util.ui.setOnBackClickListener
import com.example.upagain.util.ui.setPostCategoryTextAndColor
import com.example.upagain.util.ui.toggleFullScreenLoading
import com.example.upagain.viewmodel.PostViewModel
import com.example.upagain.viewmodel.UiState
import com.example.upagain.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

private const val ARG_POST_ID = "arg_post_id"

class PostDetailFragment : Fragment() {
    private var idPost: Int? = null

    // elements binding
    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!
    private val apiService by lazy { ApiClient.apiService }
    private val repository by lazy { PostRepo(apiService) }
    private val appInstance by lazy { requireActivity().application }
    private val viewModel: PostViewModel by viewModels {
        ViewModelFactory { PostViewModel(repository, appInstance) }
    }

    private lateinit var commentAdapter: CommentRecyclerViewAdapter
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
            viewModel.getPostDetails(id)
            viewModel.loadPageOfComments(id, 1)
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
        // init empty, data will be passed in observer once api response arrive
        commentAdapter = CommentRecyclerViewAdapter(
            false,
            object : CommentRecyclerViewAdapter.OnClickListener {
                override fun onLoadMoreClick() {
                    idPost?.let { id ->
                        viewModel.loadPageOfComments(id, currentCommentPage + 1)
                    }
                }
            })
        binding.rvComments.adapter = commentAdapter
    }

    private fun setupListeners() {
        // BACK
        binding.btnBack.setOnBackClickListener()
        //
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.postDetailState.collect { state ->
                        when (state) {
                            is UiState.Idle -> {}
                            is UiState.Loading -> {
                                binding.main.toggleFullScreenLoading(true)
                            }

                            is UiState.Success -> {
                                binding.main.toggleFullScreenLoading(false)
                                val post = state.data

                                // TODO: show images in carousel

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
                                binding.tvCommentCount.text = post.commentCount.toString()
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
                launch {
                    viewModel.allCommentsState.collect { state ->
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
            }
        }
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
}