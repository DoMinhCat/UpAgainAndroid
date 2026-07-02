package com.example.upagain.feat.post.index

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.upagain.R
import com.example.upagain.api.ApiClient
import com.example.upagain.databinding.FragmentPostBinding
import com.example.upagain.event.LikePostEvent
import com.example.upagain.event.SavePostEvent
import com.example.upagain.feat.error.ErrorActivity
import com.example.upagain.feat.post.detail.PostDetailFragment
import com.example.upagain.model.post.PostCategory
import com.example.upagain.model.post.PostDetailsResponse
import com.example.upagain.model.post.PostSortOption
import com.example.upagain.repository.CommentRepo
import com.example.upagain.repository.PostRepo
import com.example.upagain.util.ui.SnackbarLevel
import com.example.upagain.util.ui.hideKeyboard
import com.example.upagain.util.ui.showTopSnackbar
import com.example.upagain.viewmodel.PostViewModel
import com.example.upagain.viewmodel.UiState
import com.example.upagain.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class PostFragment : Fragment() {
    // elements binding
    private var _binding: FragmentPostBinding? = null
    private val binding get() = _binding!!
    private val apiService by lazy { ApiClient.apiService }
    private val postRepository by lazy { PostRepo(apiService) }
    private val appInstance by lazy { requireActivity().application }
    private val viewModel: PostViewModel by viewModels {
        ViewModelFactory { PostViewModel(postRepository, appInstance) }
    }

    // Get all posts pagination
    private var currentPage = 1
    private var loadedPosts = mutableListOf<PostDetailsResponse>()
    private lateinit var postAdapter: PostRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostBinding.inflate(inflater, container, false)
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
        observePostState()

        // API call
        viewModel.loadPageOfAllPosts(1)
    }

    // PRIVATE ZONE
    private fun setupRecyclerView() {
        // Tell Recycler View to arrange items horizontally
        binding.rvPosts.layoutManager = LinearLayoutManager(requireContext())
        // Attach the adapter
        postAdapter = PostRecyclerViewAdapter(
            loadedPosts,
            false,
            object : PostRecyclerViewAdapter.OnClickListener {
                override fun onPostClick(position: Int, post: PostDetailsResponse) {
                    val postId = loadedPosts.getOrNull(position)?.id ?: return
                    val postDetailFragment = PostDetailFragment.newInstance(postId)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, postDetailFragment)
                        .addToBackStack(null)
                        .commit()
                }

                override fun onLikeClick(position: Int, post: PostDetailsResponse) {
                    // optimistic update
                    post.isLiked = !post.isLiked
                    if (post.isLiked)
                        post.likeCount += 1
                    else post.likeCount -= 1
                    postAdapter.updateSingleItem(position, post)
                    viewModel.likePost(post.id, position)
                }

                override fun onSaveClick(position: Int, post: PostDetailsResponse) {
                    // optimistic update
                    post.isSaved = !post.isSaved
                    // tell adapter to redraw single item to sync new status
                    postAdapter.updateSingleItem(position, post)

                    viewModel.savePost(post.id, position)
                }

                override fun onLoadMoreClick() {
                    viewModel.loadPageOfAllPosts(currentPage + 1)
                }
            })
        binding.rvPosts.adapter = postAdapter
    }

    private fun setupListeners() {
        // FILTER CHIPS
        binding.chipGroupSort.setOnCheckedStateChangeListener { _, checkedChips ->
            val selectedChip = checkedChips.firstOrNull()

            if (selectedChip == null) {
                viewModel.updateSortFilter(null)
            } else {
                when (selectedChip) {
                    R.id.most_liked_chip -> {
                        viewModel.updateSortFilter(PostSortOption.MOST_LIKE)
                    }

                    R.id.most_viewed_chip -> {
                        viewModel.updateSortFilter(PostSortOption.MOST_VIEW)
                    }
                }
            }
            viewModel.loadPageOfAllPosts(1)
        }
        binding.chipGroupCategories.setOnCheckedStateChangeListener { _, checkedChips ->
            val selectedChip = checkedChips.firstOrNull()
            if (selectedChip == null) {
                viewModel.updateCategoryFilter(PostCategory.ALL)
            } else {
                when (selectedChip) {
                    R.id.tutorial_chip -> {
                        viewModel.updateCategoryFilter(PostCategory.TUTORIAL)
                    }

                    R.id.project_chip -> {
                        viewModel.updateCategoryFilter(PostCategory.PROJECT)
                    }

                    R.id.tip_chip -> {
                        viewModel.updateCategoryFilter(PostCategory.TIPS)
                    }

                    R.id.news_chip -> {
                        viewModel.updateCategoryFilter(PostCategory.NEWS)
                    }

                    R.id.case_study_chip -> {
                        viewModel.updateCategoryFilter(PostCategory.CASE_STUDY)
                    }

                    R.id.other_chip -> {
                        viewModel.updateCategoryFilter(PostCategory.OTHER)
                    }
                }
            }
            viewModel.loadPageOfAllPosts(1)
        }
        // SEARCH
        binding.etSearch.setOnEditorActionListener { textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                // when submit
                activity?.hideKeyboard()
                viewModel.updateSearchFilter(textView.text.toString())
                viewModel.loadPageOfAllPosts(1)
                true
            } else {
                false
            }
        }
        // SAVED POSTS
        binding.icSavedPosts.setOnClickListener {
            // TODO: navigate to saved posts
        }
        // MY POSTS
        binding.icMyPosts.setOnClickListener {
            // TODO: navigate to my posts
        }
    }

    private fun observePostState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // GET ALL POSTS
                launch {
                    viewModel.allPostsState.collect { state ->
                        when (state) {
                            is UiState.Idle -> {}
                            is UiState.Loading -> {
                                toggleAllPostLoading(true, state.isFirstPage)
                            }

                            is UiState.Success -> {
                                val allPostsResponse = state.data

                                currentPage = allPostsResponse.currentPage
                                if (currentPage == 1) {
                                    // if it is the default page then clear all first then load
                                    loadedPosts.clear()
                                    toggleAllPostLoading(false, isFirstPage = true)
                                }

                                // handle empty state
                                if (allPostsResponse.posts.isNullOrEmpty()) {
                                    binding.layoutPostsEmpty.visibility = View.VISIBLE
                                    binding.rvPosts.visibility = View.GONE
                                } else {
                                    binding.layoutPostsEmpty.visibility = View.GONE
                                    binding.rvPosts.visibility = View.VISIBLE
                                }

                                loadedPosts.addAll(allPostsResponse.posts ?: emptyList())

                                val hasMore =
                                    allPostsResponse.currentPage < allPostsResponse.lastPage
                                postAdapter.updateData(loadedPosts, hasMore)
                            }

                            is UiState.Error -> {
                                toggleAllPostLoading(false, isFirstPage = (currentPage == 1))
                                Log.e(
                                    "PostFragment",
                                    "Load all posts failed. Status code: ${state.statusCode}",
                                    state.exception
                                )
                                ErrorActivity.start(requireContext(), state.statusCode ?: 0)
                            }
                        }
                    }
                }
                // SAVE
                launch {
                    viewModel.savePostEvent.collect { event ->
                        when (event) {
                            is SavePostEvent.Succeeded -> {
                                // get the post at that position
                                val currentPost = loadedPosts.getOrNull(event.position)
                                if (currentPost != null && currentPost.id == event.idPost) {
                                    if (currentPost.isSaved != event.isSaved) {
                                        // sync isSaved status
                                        currentPost.isSaved = event.isSaved
                                        postAdapter.updateSingleItem(event.position, currentPost)
                                    }
                                }
                            }

                            is SavePostEvent.Rollback -> {
                                val failingPost = loadedPosts.getOrNull(event.position)
                                if (failingPost != null && failingPost.id == event.idPost) {
                                    // Revert isSaved status since update on server failed
                                    failingPost.isSaved = !failingPost.isSaved
                                    postAdapter.updateSingleItem(event.position, failingPost)

                                    Log.e(
                                        "PostFragment",
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
                // LIKE
                launch {
                    viewModel.likePostEvent.collect { event ->
                        when (event) {
                            is LikePostEvent.Succeeded -> {
                                // get the post at that position
                                val currentPost = loadedPosts.getOrNull(event.position)
                                if (currentPost != null && currentPost.id == event.idPost) {
                                    if (currentPost.isLiked != event.isLiked) {
                                        // sync isLiked status
                                        currentPost.isLiked = event.isLiked
                                        postAdapter.updateSingleItem(event.position, currentPost)
                                    }
                                }
                            }

                            is LikePostEvent.Rollback -> {
                                val failingPost = loadedPosts.getOrNull(event.position)
                                if (failingPost != null && failingPost.id == event.idPost) {
                                    // Revert isSaved status since update on server failed
                                    failingPost.isLiked = !failingPost.isLiked
                                    failingPost.likeCount -= 1
                                    postAdapter.updateSingleItem(event.position, failingPost)

                                    Log.e(
                                        "PostFragment",
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
            }
        }
    }

    private fun toggleAllPostLoading(isLoading: Boolean, isFirstPage: Boolean) {
        if (isFirstPage) {
            togglePostsAreaLoading(isLoading)
        } else {
            postAdapter.toggleLoadMoreBtnLoadingState(isLoading)
        }
    }

    private fun togglePostsAreaLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.rvPosts.visibility = View.GONE
            binding.postsLoader.visibility = View.VISIBLE
        } else {
            binding.postsLoader.visibility = View.GONE
            binding.rvPosts.visibility = View.VISIBLE
        }
    }
}



