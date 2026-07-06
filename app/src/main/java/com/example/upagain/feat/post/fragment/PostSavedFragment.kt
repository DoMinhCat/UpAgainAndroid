package com.example.upagain.feat.post.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.upagain.R
import com.example.upagain.api.ApiClient
import com.example.upagain.databinding.FragmentPostSavedBinding
import com.example.upagain.event.LikePostEvent
import com.example.upagain.event.SavePostEvent
import com.example.upagain.feat.error.ErrorActivity
import com.example.upagain.feat.post.adapter.PostAdapter
import com.example.upagain.model.post.PostCategory
import com.example.upagain.model.post.PostDetailsResponse
import com.example.upagain.repository.PostRepo
import com.example.upagain.util.ui.SnackbarLevel
import com.example.upagain.util.ui.setOnBackClickListener
import com.example.upagain.util.ui.showTopSnackbar
import com.example.upagain.viewmodel.PostViewModel
import com.example.upagain.viewmodel.UiState
import com.example.upagain.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass.
 * Use the [PostSavedFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PostSavedFragment : Fragment() {
    private var _binding: FragmentPostSavedBinding? = null
    private val binding get() = _binding!!
    private val apiService by lazy { ApiClient.apiService }
    private val repository by lazy { PostRepo(apiService) }
    private val appInstance by lazy { requireActivity().application }
    private val viewModel: PostViewModel by viewModels {
        ViewModelFactory { PostViewModel(repository, appInstance) }
    }

    private var currentPage = 1
    private var savedPosts = mutableListOf<PostDetailsResponse>()
    private lateinit var postAdapter: PostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostSavedBinding.inflate(inflater, container, false)
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
        viewModel.loadPageOfSavedPosts(currentPage)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment PostSavedFragment.
         */
        @JvmStatic
        fun newInstance() =
            PostSavedFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }

    // PRIVATE ZONE
    private fun setupRecyclerView() {
        binding.rvPosts.layoutManager = LinearLayoutManager(requireContext())
        // Attach the adapter
        postAdapter = PostAdapter(
            object : PostAdapter.OnClickListener {
                override fun onPostClick(position: Int, post: PostDetailsResponse) {
                    val postId = postAdapter.currentList.getOrNull(position)?.id ?: return
                    val postDetailFragment = PostDetailFragment.Companion.newInstance(postId)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, postDetailFragment)
                        .addToBackStack(null)
                        .commit()
                }

                override fun onLikeClick(position: Int, post: PostDetailsResponse) {
                    // optimistic update
                    val updatedPost = post.copy(
                        isLiked = !post.isLiked,
                        likeCount = if (!post.isLiked) post.likeCount + 1 else post.likeCount - 1
                    )

                    val newList = postAdapter.currentList.toMutableList()
                    if (position in newList.indices) {
                        newList[position] = updatedPost
                        postAdapter.submitList(newList)
                    }
                    viewModel.likePost(updatedPost.id, position)
                }

                override fun onSaveClick(position: Int, post: PostDetailsResponse) {
                    val targetSavedState = !post.isSaved
                    val newList = postAdapter.currentList.toMutableList()

                    if (!targetSavedState) {
                        if (position in newList.indices) {
                            newList.removeAt(position)
                            postAdapter.submitList(newList)
                        }
                    } else {
                        // If toggling on a reference update, use .copy()
                        val updatedPost = post.copy(isSaved = true)
                        if (position in newList.indices) {
                            newList[position] = updatedPost
                            postAdapter.submitList(newList)
                        }
                    }
                    viewModel.savePost(post.id, position)
                }

                override fun onLoadMoreClick() {
                    viewModel.loadPageOfSavedPosts(currentPage + 1)
                }
            })
        binding.rvPosts.adapter = postAdapter
    }

    private fun setupListeners() {
        // FILTER CHIPS
        binding.chipGroupCategories.setOnCheckedStateChangeListener { _, checkedChips ->
            val selectedChip = checkedChips.firstOrNull()
            if (selectedChip == null) {
                viewModel.updateSavedPostsCategoryFilter(PostCategory.ALL)
            } else {
                when (selectedChip) {
                    R.id.tutorial_chip -> {
                        viewModel.updateSavedPostsCategoryFilter(PostCategory.TUTORIAL)
                    }

                    R.id.project_chip -> {
                        viewModel.updateSavedPostsCategoryFilter(PostCategory.PROJECT)
                    }

                    R.id.tip_chip -> {
                        viewModel.updateSavedPostsCategoryFilter(PostCategory.TIPS)
                    }

                    R.id.news_chip -> {
                        viewModel.updateSavedPostsCategoryFilter(PostCategory.NEWS)
                    }

                    R.id.case_study_chip -> {
                        viewModel.updateSavedPostsCategoryFilter(PostCategory.CASE_STUDY)
                    }

                    R.id.other_chip -> {
                        viewModel.updateSavedPostsCategoryFilter(PostCategory.OTHER)
                    }
                }
            }
            viewModel.loadPageOfSavedPosts(1)
        }
        // BACK BTN
        binding.btnBack.setOnBackClickListener()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // GET ALL SAVED POSTS
                launch {
                    viewModel.savedPostsState.collect { state ->
                        when (state) {
                            is UiState.Idle -> {}
                            is UiState.Loading -> {
                                toggleAllPostLoading(true, state.isFirstPage)
                            }

                            is UiState.Success -> {
                                val savedPostsResponse = state.data

                                currentPage = savedPostsResponse.currentPage
                                if (currentPage == 1) {
                                    // if it is the default page then clear all first then load
                                    savedPosts.clear()
                                    toggleAllPostLoading(false, isFirstPage = true)
                                } else {
                                    toggleAllPostLoading(false, isFirstPage = false)
                                }

                                // handle empty state
                                if (savedPostsResponse.posts.isNullOrEmpty()) {
                                    binding.layoutPostsEmpty.visibility = View.VISIBLE
                                    binding.rvPosts.visibility = View.GONE
                                } else {
                                    binding.layoutPostsEmpty.visibility = View.GONE
                                    binding.rvPosts.visibility = View.VISIBLE
                                }

                                savedPosts.addAll(savedPostsResponse.posts ?: emptyList())

                                val hasMore =
                                    savedPostsResponse.currentPage < savedPostsResponse.lastPage
                                postAdapter.updatePaginationState(hasMore)
                                postAdapter.submitList(savedPosts.toList())
                            }

                            is UiState.Error -> {
                                toggleAllPostLoading(false, isFirstPage = (currentPage == 1))
                                Log.e(
                                    "PostSavedFragment",
                                    "Load saved posts failed. Status code: ${state.statusCode}",
                                    state.exception
                                )
                                ErrorActivity.Companion.start(
                                    requireContext(),
                                    state.statusCode ?: 0
                                )
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
                                val currentPost = postAdapter.currentList.getOrNull(event.position)
                                if (currentPost != null && currentPost.id == event.idPost) {
                                    if (currentPost.isSaved != event.isSaved) {
                                        // sync isSaved status
                                        currentPost.isSaved = event.isSaved

                                        val newList = postAdapter.currentList.toMutableList()
                                        newList[event.position] = currentPost
                                        postAdapter.submitList(newList)
                                    }
                                    // remove the item if unsaved
                                    if (!event.isSaved) {
                                        savedPosts.remove(currentPost)
                                        val newList = postAdapter.currentList.toMutableList()
                                        newList.removeAt(event.position)
                                        postAdapter.submitList(newList)
                                    }
                                }
                            }

                            is SavePostEvent.Rollback -> {
                                val failingPost = postAdapter.currentList.getOrNull(event.position)
                                if (failingPost != null && failingPost.id == event.idPost) {
                                    // Revert isSaved status since update on server failed
                                    failingPost.isSaved = !failingPost.isSaved

                                    val newList = postAdapter.currentList.toMutableList()
                                    newList[event.position] = failingPost
                                    postAdapter.submitList(newList)

                                    Log.e(
                                        "PostSavedFragment",
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
                                val currentPost = postAdapter.currentList.getOrNull(event.position)
                                if (currentPost != null && currentPost.id == event.idPost) {
                                    if (currentPost.isLiked != event.isLiked) {
                                        // sync isLiked status
                                        currentPost.isLiked = event.isLiked

                                        val newList = postAdapter.currentList.toMutableList()
                                        newList[event.position] = currentPost
                                        postAdapter.submitList(newList)
                                    }
                                }
                            }

                            is LikePostEvent.Rollback -> {
                                val failingPost = postAdapter.currentList.getOrNull(event.position)
                                if (failingPost != null && failingPost.id == event.idPost) {
                                    // Revert isSaved status since update on server failed
                                    failingPost.isLiked = !failingPost.isLiked
                                    failingPost.likeCount -= 1

                                    val newList = postAdapter.currentList.toMutableList()
                                    newList[event.position] = failingPost
                                    postAdapter.submitList(newList)

                                    Log.e(
                                        "PostSavedFragment",
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
            binding.rvPosts.visibility = View.VISIBLE
            binding.postsLoader.visibility = View.GONE
        }
    }
}