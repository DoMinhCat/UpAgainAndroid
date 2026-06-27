package com.example.upagain.feat.post

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.currentRecomposeScope
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.upagain.api.ApiClient
import com.example.upagain.databinding.FragmentPostBinding
import com.example.upagain.feat.error.ErrorActivity
import com.example.upagain.model.post.PostDetailsResponse
import com.example.upagain.model.post.PostPaginationRequest
import com.example.upagain.repository.PostRepo
import com.example.upagain.util.ui.toggleBtnLoadingState
import com.example.upagain.util.ui.toggleFullScreenLoading
import com.example.upagain.viewmodel.PostViewModel
import com.example.upagain.viewmodel.UiState
import com.example.upagain.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class PostFragment : Fragment() {
    // elements binding
    private var _binding: FragmentPostBinding? = null
    private val binding get() = _binding!!
    private val apiService by lazy { ApiClient.apiService }
    private val repository by lazy { PostRepo(apiService) }
    private val appInstance by lazy { requireActivity().application }
    private val viewModel: PostViewModel by viewModels {
        ViewModelFactory { PostViewModel(repository, appInstance) }
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
                    // TODO: navigate to Post Detail frag
                }

                override fun onLoadMoreClick() {
                    viewModel.loadPageOfAllPosts(currentPage + 1)
                }
            })
        binding.rvPosts.adapter = postAdapter
    }

    private fun setupListeners() {

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
                                loadedPosts.addAll(allPostsResponse.posts)

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
            }
        }
    }

    private fun toggleAllPostLoading(isLoading: Boolean, isFirstPage: Boolean) {
        if(isFirstPage){
            binding.loadingOverlay.root.toggleFullScreenLoading(isLoading)
        } else{
            postAdapter.toggleLoadMoreBtnLoadingState(isLoading)
        }
    }
}