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
import coil.load
import com.example.upagain.R
import com.example.upagain.SecuritySettingFragment
import com.example.upagain.api.ApiClient
import com.example.upagain.databinding.FragmentProfileBinding
import com.example.upagain.feat.auth.LoginActivity
import com.example.upagain.feat.error.ErrorActivity
import com.example.upagain.repository.AccountRepo
import com.example.upagain.util.auth.SessionManager
import com.example.upagain.util.datetime.formatTimestamptz
import com.example.upagain.util.ui.DialogUtils
import com.example.upagain.util.ui.SnackbarLevel
import com.example.upagain.util.ui.setOnClickListenerWithCooldown
import com.example.upagain.util.ui.showTopSnackbar
import com.example.upagain.util.ui.toggleBtnLoadingState
import com.example.upagain.util.validator.FieldValidator
import com.example.upagain.util.validator.MaxLengthRule
import com.example.upagain.util.validator.MinLengthRule
import com.example.upagain.util.validator.NotEmptyRule
import com.example.upagain.util.validator.PhoneRule
import com.example.upagain.viewmodel.AccountViewModel
import com.example.upagain.viewmodel.UiState
import com.example.upagain.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch
import kotlin.getValue
import com.example.upagain.util.image.buildImageUrl

class ProfileFragment : Fragment() {
    // elements binding
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val apiService by lazy { ApiClient.apiService }
    private val repository by lazy { AccountRepo(apiService) }
    private val viewModel: AccountViewModel by viewModels {
        ViewModelFactory { AccountViewModel(repository) }
    }

    // validators
    val usernameValidator = FieldValidator(listOf(NotEmptyRule(), MinLengthRule(4), MaxLengthRule(20)))
    val phoneValidator = FieldValidator(listOf(NotEmptyRule(), PhoneRule()))

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
        val currentId = SessionManager.accountId ?: return
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
        // SAVE CHANGES
        binding.btnSaveProfile.setOnClickListenerWithCooldown {
            val username = binding.etProfileName.text.toString()
            val phone = binding.etProfilePhone.text.toString()
            val isUsernameValid = usernameValidator.validate(username)
            val isPhoneValid = phoneValidator.validate(phone)
            binding.tvUsernameError.visibility = if (isUsernameValid) View.GONE else View.VISIBLE
            binding.tvPhoneError.visibility = if (isPhoneValid) View.GONE else View.VISIBLE
            if (!isUsernameValid || !isPhoneValid) {
                return@setOnClickListenerWithCooldown
            }
            // TODO: call view model to update account details
//            val currentId = SessionManager.userId ?: return@setOnClickListenerWithCooldown
//            viewModel.updateAccountDetails(currentId, username, phone)
        }
        // DELETE ACCOUNT
        binding.btnSettingDelete.setOnClickListener {
            DialogUtils.showDestructiveConfirmationDialog(
                context = requireContext(),
                title = getString(R.string.confirm_del_account),
            ) {
                val currentUserId = SessionManager.accountId ?: return@showDestructiveConfirmationDialog
                // TODO: call view model to delete account
                // e.g., viewModel.deleteUserAccount()
            }
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
                // get account details
                viewModel.accountDetailsState.collect { state ->
                    when (state) {
                        is UiState.Idle -> {}
                        is UiState.Loading -> {
                            toggleFullScreenLoading(true)
                        }
                        is UiState.Success -> {
                            toggleFullScreenLoading(false)
                            val account = state.data
                            // update UI with account details
                            binding.tvUsername.text = account.username
                            binding.tvMemberSince.text = formatTimestamptz(account.createdAt)
                            binding.tvPlanType.text = if (account.isPremium) "Premium" else "Freemium"
                            binding.etProfileName.setText(account.username)
                            binding.tvProfileEmail.text = account.email
                            binding.etProfilePhone.setText(account.phone)

                            // build url and let coil handle image serving for avatar
                            binding.ivAvatar.load(buildImageUrl(account.avatar)) {
                                crossfade(true)
                                placeholder(R.drawable.ic_avatar_unknown)
                                error(R.drawable.ic_avatar_unknown)

                                listener(
                                    onSuccess = { _, _ ->
                                        // Image loaded successfully
                                    },
                                    onError = { _, result ->
                                        val exception = result.throwable
                                        val statusCode = (exception as? coil.network.HttpException)?.response?.code

                                        Log.e("ProfileFragment", "Failed to serve user's avatar. Status Code: $statusCode", exception)
                                        when (statusCode) {
                                            404 -> {
                                                binding.main.showTopSnackbar(R.string.error_media_msg, SnackbarLevel.ERROR)
                                            }
                                            else -> {
                                                binding.main.showTopSnackbar(
                                                    R.string.error_media_msg,
                                                    SnackbarLevel.ERROR
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        is UiState.Error -> {
                            toggleFullScreenLoading(false)
                            Log.e("ProfileFragment", "Load account details failed", state.exception)
                            ErrorActivity.start(requireContext(), state.statusCode ?: 0)
                        }
                    }
                }
                // del account
                /*
                viewModel.deleteAccountState.collect { state ->
                    when (state) {
                        is UiState.Loading -> toggleLoading(true)
                        is UiState.Success -> {
                            toggleLoading(false)
                            SessionManager.clearSession()

                            // 🗺️ Step 3: Execute explicit navigation routing
                            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                putExtra("EXTRA_ACCOUNT_DELETED", true)
                            }
                            startActivity(intent)
                            activity?.finish()
                        }
                        is UiState.Error -> {
                            toggleLoading(false)
                            ErrorActivity.start(requireContext(), state.statusCode ?: 500)
                        }
                        is UiState.Idle -> { /* No-op */ }
                    }
                }
                */
            }
        }
    }
    fun toggleFullScreenLoading(isLoading: Boolean) {
        binding.loadingOverlay.root.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
    private fun toggleBtnLoading(isLoading: Boolean) {
        toggleBtnLoadingState(binding.btnSaveProfile, binding.saveLoader, isLoading, getString(R.string.login))
    }

}