package com.example.upagain.feat.post

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.upagain.R
import com.example.upagain.api.ApiClient
import com.example.upagain.databinding.FragmentPostBinding
import com.example.upagain.databinding.FragmentProfileBinding
import com.example.upagain.repository.AccountRepo
import com.example.upagain.repository.PostRepo
import com.example.upagain.viewmodel.AccountViewModel
import com.example.upagain.viewmodel.ViewModelFactory
import kotlin.getValue


/**
 * A simple [Fragment] subclass.
 * Use the [PostFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PostFragment : Fragment() {
    // elements binding
    private var _binding: FragmentPostBinding? = null
    private val binding get() = _binding!!
    private val apiService by lazy { ApiClient.apiService }
    private val repository by lazy { PostRepo(apiService) }
    private val appInstance by lazy { requireActivity().application }
    // TODO
//    private val viewModel: PostViewModel by viewModels {
//        ViewModelFactory { PostViewModel(repository, appInstance) }
//    }

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

    // PRIVATE ZONE
}