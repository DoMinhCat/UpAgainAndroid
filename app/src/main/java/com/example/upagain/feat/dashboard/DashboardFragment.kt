package com.example.upagain.feat.dashboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.upagain.BuildConfig
import com.example.upagain.R
import com.example.upagain.api.ApiClient
import com.example.upagain.databinding.FragmentDashboardBinding
import com.example.upagain.repository.AccountRepo
import com.example.upagain.util.auth.SessionManager
import com.example.upagain.util.ui.SnackbarLevel
import com.example.upagain.util.ui.showTopSnackbar
import com.example.upagain.util.ui.toggleFullScreenLoading
import com.example.upagain.viewmodel.AccountViewModel
import com.example.upagain.viewmodel.UiState
import com.example.upagain.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

private const val ARG_JUST_LOGGED_IN = "key_just_logged_in"

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private var justLoggedIn: Boolean = false

    private val apiService by lazy { ApiClient.apiService }
    private val accountViewModel: AccountViewModel by viewModels {
        ViewModelFactory { AccountViewModel(AccountRepo(apiService), requireActivity().application) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            justLoggedIn = it.getBoolean(ARG_JUST_LOGGED_IN)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (justLoggedIn) {
            binding.main.showTopSnackbar(R.string.login_success, SnackbarLevel.SUCCESS)
        }

        setupListeners()
        observeState()

        val currentId = SessionManager.accountId ?: return
        accountViewModel.getAccountDetails(currentId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(justLoggedIn: Boolean) =
            DashboardFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_JUST_LOGGED_IN, justLoggedIn)
                }
            }
    }

    private fun setupListeners() {
        binding.btnUpgradePremium.setOnClickListener {
            val url = "${BuildConfig.FRONTEND_BASE_URL}pricing"
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    accountViewModel.accountDetailsState.collect { state ->
                        when (state) {
                            is UiState.Idle -> {}
                            is UiState.Loading -> {
                                binding.loadingOverlay.root.toggleFullScreenLoading(true)
                            }
                            is UiState.Success -> {
                                binding.loadingOverlay.root.toggleFullScreenLoading(false)
                                val account = state.data
                                if (account.isPremium) {
                                    showDashboardContent()
                                } else {
                                    showPremiumGate()
                                }
                            }
                            is UiState.Error -> {
                                binding.loadingOverlay.root.toggleFullScreenLoading(false)
                                Log.e("DashboardFragment", "Failed to load account", state.exception)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showDashboardContent() {
        binding.appBarDashboard.visibility = View.VISIBLE
        binding.scrollDashboardContent.visibility = View.VISIBLE
        binding.layoutPremiumGate.visibility = View.GONE
    }

    private fun showPremiumGate() {
        binding.appBarDashboard.visibility = View.GONE
        binding.scrollDashboardContent.visibility = View.GONE
        binding.layoutPremiumGate.visibility = View.VISIBLE
    }
}