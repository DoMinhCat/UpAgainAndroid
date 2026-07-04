package com.example.upagain.feat.post.fragment

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
import com.example.upagain.databinding.FragmentPostMeBinding
import com.example.upagain.event.LikePostEvent
import com.example.upagain.event.SavePostEvent
import com.example.upagain.feat.error.ErrorActivity
import com.example.upagain.feat.post.adapter.PostAdapter
import com.example.upagain.model.post.PostCategory
import com.example.upagain.model.post.PostDetailsResponse
import com.example.upagain.repository.PostRepo
import com.example.upagain.util.ui.SnackbarLevel
import com.example.upagain.util.ui.hideKeyboard
import com.example.upagain.util.ui.showTopSnackbar
import com.example.upagain.viewmodel.PostViewModel
import com.example.upagain.viewmodel.UiState
import com.example.upagain.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class PostMeFragment : Fragment() {
    // elements binding
    private var _binding: FragmentPostMeBinding? = null
    private val binding get() = _binding!!
    private val apiService by lazy { ApiClient.apiService }
    private val repository by lazy { PostRepo(apiService) }
    private val appInstance by lazy { requireActivity().application }
    private val viewModel: PostViewModel by viewModels {
        ViewModelFactory { PostViewModel(repository, appInstance) }
    }

    private var currentPage = 1
    private var myPosts = mutableListOf<PostDetailsResponse>()
    private lateinit var postAdapter: PostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostMeBinding.inflate(inflater, container, false)
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
        viewModel.loadPageOfMyPosts(1)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment PostMeFragment.
         */
        @JvmStatic
        fun newInstance() =
            PostMeFragment().apply {
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
                    // optimistic update
                    val updatedPost = post.copy(isSaved = !post.isSaved)

                    val newList = postAdapter.currentList.toMutableList()
                    if (position in newList.indices) {
                        newList[position] = updatedPost
                        postAdapter.submitList(newList)
                    }
                    viewModel.savePost(updatedPost.id, position)
                }

                override fun onLoadMoreClick() {
                    viewModel.loadPageOfAllPosts(currentPage + 1)
                }
            })
        binding.rvPosts.adapter = postAdapter
    }

    private fun setupListeners() {
        // FILTER CHIPS
        binding.chipGroupCategories.setOnCheckedStateChangeListener { _, checkedChips ->
            val selectedChip = checkedChips.firstOrNull()
            if (selectedChip == null) {
                viewModel.updateAllPostsCategoryFilter(PostCategory.ALL)
            } else {
                when (selectedChip) {
                    R.id.tutorial_chip -> {
                        viewModel.updateAllPostsCategoryFilter(PostCategory.TUTORIAL)
                    }

                    R.id.project_chip -> {
                        viewModel.updateAllPostsCategoryFilter(PostCategory.PROJECT)
                    }

                    R.id.tip_chip -> {
                        viewModel.updateAllPostsCategoryFilter(PostCategory.TIPS)
                    }

                    R.id.news_chip -> {
                        viewModel.updateAllPostsCategoryFilter(PostCategory.NEWS)
                    }

                    R.id.case_study_chip -> {
                        viewModel.updateAllPostsCategoryFilter(PostCategory.CASE_STUDY)
                    }

                    R.id.other_chip -> {
                        viewModel.updateAllPostsCategoryFilter(PostCategory.OTHER)
                    }
                }
            }
            viewModel.loadPageOfMyPosts(1)
        }
        // SEARCH
        binding.etSearch.setOnEditorActionListener { textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                // when submit
                activity?.hideKeyboard()
                viewModel.updateSearchAllPostsFilter(textView.text.toString())
                viewModel.loadPageOfMyPosts(1)
                true
            } else {
                false
            }
        }
    }

    private fun observePostState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // GET MY POSTS
                launch {
                    viewModel.myPostsState.collect { state ->
                        when (state) {
                            is UiState.Idle -> {}
                            is UiState.Loading -> {
                                toggleAllPostLoading(true, state.isFirstPage)
                            }

                            is UiState.Success -> {
                                val myPostsResponse = state.data

                                currentPage = myPostsResponse.currentPage
                                if (currentPage == 1) {
                                    // if it is the default page then clear all first then load
                                    myPosts.clear()
                                    toggleAllPostLoading(false, isFirstPage = true)
                                } else {
                                    toggleAllPostLoading(false, isFirstPage = false)
                                }

                                // handle empty state
                                if (myPostsResponse.posts.isNullOrEmpty()) {
                                    binding.layoutPostsEmpty.visibility = View.VISIBLE
                                    binding.rvPosts.visibility = View.GONE
                                } else {
                                    binding.layoutPostsEmpty.visibility = View.GONE
                                    binding.rvPosts.visibility = View.VISIBLE
                                }

                                myPosts.addAll(myPostsResponse.posts ?: emptyList())

                                val hasMore =
                                    myPostsResponse.currentPage < myPostsResponse.lastPage
                                postAdapter.updatePaginationState(hasMore)
                                postAdapter.submitList(myPosts.toList())
                            }

                            is UiState.Error -> {
                                toggleAllPostLoading(false, isFirstPage = (currentPage == 1))
                                Log.e(
                                    "PostMeFragment",
                                    "Load all posts failed. Status code: ${state.statusCode}",
                                    state.exception
                                )
                                ErrorActivity.Companion.start(requireContext(), state.statusCode ?: 0)
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
                                        "PostMeFragment",
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
                                        "PostMeFragment",
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