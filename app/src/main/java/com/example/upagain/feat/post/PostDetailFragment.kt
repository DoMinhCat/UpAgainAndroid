package com.example.upagain.feat.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.upagain.api.ApiClient
import com.example.upagain.databinding.FragmentPostDetailBinding
import com.example.upagain.repository.PostRepo
import com.example.upagain.viewmodel.PostViewModel
import com.example.upagain.viewmodel.ViewModelFactory

private const val ARG_POST_ID = "arg_post_id"

class PostDetailFragment : Fragment() {
    private var postId: Int? = null

    // elements binding
    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!
    private val apiService by lazy { ApiClient.apiService }
    private val repository by lazy { PostRepo(apiService) }
    private val appInstance by lazy { requireActivity().application }
    private val viewModel: PostViewModel by viewModels {
        ViewModelFactory { PostViewModel(repository, appInstance) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            postId = it.getInt(ARG_POST_ID)
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

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param postId The ID of the post to display.
         * @return A new instance of fragment PostDetailFragment.
         */
        @JvmStatic
        fun newInstance(postId: Int) =
            PostDetailFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_POST_ID, postId)
                }
            }
    }
}