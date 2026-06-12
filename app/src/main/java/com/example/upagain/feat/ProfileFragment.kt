package com.example.upagain.feat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.upagain.api.ApiClient
import com.example.upagain.databinding.FragmentProfileBinding
import com.example.upagain.feat.auth.LoginActivity
import com.example.upagain.feat.error.ErrorActivity
import com.example.upagain.repository.AccountRepo
import com.example.upagain.util.auth.SessionManager
import com.example.upagain.util.datetime.formatTimestamptz
import com.example.upagain.viewmodel.AccountViewModel
import com.example.upagain.viewmodel.UiState
import com.example.upagain.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch
import kotlin.getValue

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProfileFragment : Fragment() {
    // elements binding
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val apiService by lazy { ApiClient.apiService }
    private val repository by lazy { AccountRepo(apiService) }
    private val viewModel: AccountViewModel by viewModels {
        ViewModelFactory { AccountViewModel(repository) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ! Always set up listeners and observers before API call
        setupListeners()
        observeAccountState()

        viewModel.getAccountDetails(SessionManager.userId ?: 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // clear reference to prevent memory leaks
        _binding = null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ProfileFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    // PRIVATE ZONE

    fun setupListeners() {
        // LOG OUT BUTTON
        binding.btnLogout.setOnClickListener {
            handleLogOut()
        }
    }
    private fun handleLogOut() {
        SessionManager.clearSession()
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            // Clear activity task stack history so back button cannot re-enter profile
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_JUST_LOGGED_OUT", true)
        }
        startActivity(intent)
        activity?.finish()
    }

    private fun observeAccountState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.accountDetailsState.collect { state ->
                    when (state) {
                        is UiState.Idle -> {}
                        is UiState.Loading -> {
                            toggleLoading(true)
                        }
                        is UiState.Success -> {
                            toggleLoading(false)
                            val account = state.data
                            // update UI with account details
                            binding.tvUsername.text = account.username
                            binding.tvMemberSince.text = formatTimestamptz(account.createdAt)
                            binding.tvPlanType.text = if (account.isPremium) "Premium" else "Freemium"
                            binding.etProfileName.setText(account.username)
                            binding.tvProfileEmail.text = account.email
                            binding.etProfilePhone.setText(account.phone)
                        }
                        is UiState.Error -> {
                            toggleLoading(false)
                            Log.e("ProfileFragment", "Load account details failed", state.exception)
                            ErrorActivity.start(requireContext(), state.statusCode ?: 0)
                        }
                    }
                }
            }
        }
    }
    fun toggleLoading(isLoading: Boolean) {
        binding.loadingOverlay.root.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

}