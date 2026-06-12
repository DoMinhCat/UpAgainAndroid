package com.example.upagain.feat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.upagain.R
import com.example.upagain.SecuritySettingFragment
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val languages = resources.getStringArray(R.array.languages_array)
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, languages)
        binding.actvLanguageSelector.setAdapter(adapter)

        // ! Always set up listeners and observers before API call
        setupListeners()
        observeAccountState()

        // API call
        val currentId = SessionManager.userId ?: return
        viewModel.getAccountDetails(currentId)
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
         * @return A new instance of fragment ProfileFragment.
         */
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }

    // PRIVATE ZONE

    fun setupListeners() {
        // LOG OUT BUTTON
        binding.btnLogout.setOnClickListener {
            handleLogOut()
        }
        // LANGUAGE
        binding.actvLanguageSelector.setOnItemClickListener { parent, _, position, _ ->
            val selectedLanguage = parent.getItemAtPosition(position) as String
            // TODO: Execute app language change logic here
        }
        // SECURITY
        binding.btnSettingSecurity.setOnClickListener {
            val emailToPass = binding.tvProfileEmail.text.toString()
            val securityFragment = SecuritySettingFragment.newInstance(emailToPass)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, securityFragment)
                .addToBackStack(null)
                .commit()
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