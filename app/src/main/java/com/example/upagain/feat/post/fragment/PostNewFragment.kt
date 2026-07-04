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
import com.example.upagain.repository.PostRepo
import com.example.upagain.util.ui.SnackbarLevel
import com.example.upagain.util.ui.hideKeyboard
import com.example.upagain.util.ui.showTopSnackbar
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

//    private lateinit var imagePreviewAdapter: SelectedImagesAdapter

    private var chosenImages = mutableListOf<Uri>()
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            chosenImages.add(uri)
//            imagePreviewAdapter.notifyItemInserted(chosenImages.size - 1)
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
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() =
            PostNewFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }

    // PRIVATE ZONE
    private fun setupRecyclerView() {

    }

    private fun setupListeners() {
        // CHOOSE IMAGES
        binding.layoutUploadPrompt.setOnClickListener {
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    // colect
                }
            }
        }

    }

    private fun updatePreviewImagesVisibility() {
        if (chosenImages.isEmpty()) {
            binding.rvChosenImages.visibility = View.GONE
        } else {
            binding.rvChosenImages.visibility = View.VISIBLE
            binding.layoutUploadPrompt.visibility = View.VISIBLE
        }
    }
}