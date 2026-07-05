package com.example.upagain.feat.post.fragment

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.upagain.R
import com.example.upagain.api.ApiClient
import com.example.upagain.databinding.FragmentPostNewBinding
import com.example.upagain.feat.post.adapter.PreviewImageAdapter
import com.example.upagain.model.post.PostCreateRequest
import com.example.upagain.repository.PostRepo
import com.example.upagain.util.ui.SnackbarLevel
import com.example.upagain.util.ui.dpToPx
import com.example.upagain.util.ui.hideKeyboard
import com.example.upagain.util.ui.setOnBackClickListener
import com.example.upagain.util.ui.setOnClickListenerWithCooldown
import com.example.upagain.util.ui.showTopSnackbar
import com.example.upagain.util.ui.toggleBtnLoadingState
import com.example.upagain.util.ui.toggleTilError
import com.example.upagain.util.validator.EmailRule
import com.example.upagain.util.validator.FieldValidator
import com.example.upagain.util.validator.MinLengthRule
import com.example.upagain.util.validator.NotEmptyRule
import com.example.upagain.viewmodel.PostViewModel
import com.example.upagain.viewmodel.UiState
import com.example.upagain.viewmodel.ViewModelFactory
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass.
 * Use the [PostNewFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PostNewFragment : Fragment() {
    // elements binding
    private var _binding: FragmentPostNewBinding? = null
    private val binding get() = _binding!!
    private val apiService by lazy { ApiClient.apiService }
    private val postRepository by lazy { PostRepo(apiService) }
    private val appInstance by lazy { requireActivity().application }
    private val viewModel: PostViewModel by viewModels {
        ViewModelFactory { PostViewModel(postRepository, appInstance) }
    }

    // validators
    val titleValidator = FieldValidator(listOf(NotEmptyRule(), MinLengthRule(4)))
    val contentValidator = FieldValidator(listOf(NotEmptyRule()))


    private lateinit var imagePreviewAdapter: PreviewImageAdapter

    private var chosenImages = mutableListOf<Uri>()
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            for (uri in uris) {
                if (!chosenImages.contains(uri)) {
                    chosenImages.add(uri)
                }
            }
            // Submit the final updated list to the adapter
            imagePreviewAdapter.submitList(chosenImages.toList())
            updatePreviewImagesVisibility()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostNewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        toggleTilError(binding.tilTitle, R.string.invalid_title,false)
        toggleTilError(binding.tilContent, R.string.invalid_content,false)
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
        observeState()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment PostNewFragment.
         */
        @JvmStatic
        fun newInstance() =
            PostNewFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }

    // PRIVATE ZONE
    private fun setupRecyclerView() {
        imagePreviewAdapter = PreviewImageAdapter { deletedUri ->
            chosenImages.remove(deletedUri)
            imagePreviewAdapter.submitList(chosenImages.toList())
            updatePreviewImagesVisibility()
        }

        binding.rvChosenImages.apply {
            adapter = imagePreviewAdapter
        }
    }

    private fun setupListeners() {
        // BACK
        binding.btnBack.setOnBackClickListener()
        // TITLE FIELD
        binding.etPostTitle.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val title = binding.etPostTitle.text.toString().trim()
                val isTitleValid = titleValidator.validate(title)
                toggleTilError(binding.tilTitle, R.string.invalid_title,!isTitleValid)
            } else {
                toggleTilError(binding.tilTitle, R.string.invalid_title,false)
            }
        }
        // TITLE FIELD
        binding.etPostContent.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val content = binding.etPostTitle.text.toString().trim()
                val isContentValid = contentValidator.validate(content)
                toggleTilError(binding.tilContent, R.string.invalid_content,!isContentValid)
            } else {
                toggleTilError(binding.tilContent, R.string.invalid_content,false)
            }
        }
        // CHOOSE IMAGES
        binding.layoutUploadPrompt.setOnClickListener {
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        // PUBLISH POST
        binding.btnPublish.setOnClickListenerWithCooldown {
            val title = binding.etPostTitle.text.toString().trim()
            val content = binding.etPostContent.text.toString().trim()

            val isTitleValid = titleValidator.validate(title)
            val isContentValid = contentValidator.validate(content)

            toggleTilError(binding.tilTitle, R.string.invalid_title,!isTitleValid)
            toggleTilError(binding.tilContent, R.string.invalid_content,!isContentValid)

            if (!isTitleValid || !isContentValid) {
                return@setOnClickListenerWithCooldown
            }
            val request = PostCreateRequest(title = title, content = content, images = chosenImages)
            viewModel.createPost(request)
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.createPostsState.collect { state ->
                        when (state) {
                            is UiState.Idle -> {
                                togglePublishLoadingState(false)
                            }

                            is UiState.Loading -> {
                                togglePublishLoadingState(true)
                                activity?.hideKeyboard()
                            }

                            is UiState.Success -> {
                                togglePublishLoadingState(false)
                                binding.main.showTopSnackbar(
                                    R.string.snack_create_post_success,
                                    SnackbarLevel.SUCCESS
                                )
                                viewModel.resetCreatePostState()
                            }

                            is UiState.Error -> {
                                viewModel.resetCreatePostState()
                                togglePublishLoadingState(false)
                                Log.e("PostNewFragment", "Create post failed", state.exception)
                                binding.main.showTopSnackbar(
                                    getString(
                                        R.string.snack_create_post_fail,
                                        state.exception.message
                                    ), SnackbarLevel.ERROR, Snackbar.LENGTH_LONG
                                )
                            }
                        }
                    }

                }
            }
        }
    }

    private fun updatePreviewImagesVisibility() {
        if (chosenImages.isEmpty()) {
            binding.rvChosenImages.visibility = View.GONE
        } else {
            binding.rvChosenImages.visibility = View.VISIBLE
        }
    }

    private fun togglePublishLoadingState(isLoading: Boolean) {
        toggleBtnLoadingState(binding.btnPublish, binding.publishLoader, isLoading, getString(R.string.publish))
    }
}