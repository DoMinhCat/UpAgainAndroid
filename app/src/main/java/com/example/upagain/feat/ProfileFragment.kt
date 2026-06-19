package com.example.upagain.feat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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
import com.example.upagain.model.AccountUpdateRequest
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
import com.example.upagain.util.ui.hideKeyboard
import com.example.upagain.util.ui.toggleFullScreenLoading
import com.google.android.material.snackbar.Snackbar

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
    val usernameValidator =
        FieldValidator(listOf(NotEmptyRule(), MinLengthRule(4), MaxLengthRule(20)))
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
            loadSecurityFragment()
        }
        // EMAIL FIELD
        binding.tvProfileEmail.setOnClickListener {
            // navigate to security fragment to edit
            loadSecurityFragment()
        }
        // SAVE CHANGES
        binding.btnSaveProfile.setOnClickListenerWithCooldown {
            val email = binding.tvProfileEmail.text.toString()
            val username = binding.etProfileName.text.toString()
            val phone = binding.etProfilePhone.text.toString()
            val isUsernameValid = usernameValidator.validate(username)
            val isPhoneValid = phoneValidator.validate(phone)
            binding.tvUsernameError.visibility = if (isUsernameValid) View.GONE else View.VISIBLE
            binding.tvPhoneError.visibility = if (isPhoneValid) View.GONE else View.VISIBLE
            if (!isUsernameValid || !isPhoneValid) {
                return@setOnClickListenerWithCooldown
            }
            val request = AccountUpdateRequest(username, email, phone)
            val currentId = SessionManager.accountId ?: return@setOnClickListenerWithCooldown
            viewModel.updateAccount(currentId, request)
            // TODO: observe state for this and add loading spinner for button
        }
        // DELETE ACCOUNT
        binding.btnSettingDelete.setOnClickListenerWithCooldown {
            DialogUtils.showDestructiveConfirmationDialog(
                context = requireContext(),
                title = getString(R.string.confirm_del_account),
            ) {
                val currentUserId =
                    SessionManager.accountId ?: return@showDestructiveConfirmationDialog
                // TODO: call view model to delete account
                viewModel.deleteAccount(currentUserId)
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
                launch {
                    // GET ACCOUNT DETAILS
                    viewModel.accountDetailsState.collect { state ->
                        when (state) {
                            is UiState.Idle -> {}
                            is UiState.Loading -> {
                                binding.loadingOverlay.root.toggleFullScreenLoading(true)
                            }

                            is UiState.Success -> {
                                binding.loadingOverlay.root.toggleFullScreenLoading(false)

                                val account = state.data
                                // update UI with account details
                                binding.tvUsername.text = account.username
                                binding.tvMemberSince.text = formatTimestamptz(account.createdAt)
                                binding.tvPlanType.text =
                                    if (account.isPremium) "Premium" else "Freemium"
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
                                            val statusCode =
                                                (exception as? coil.network.HttpException)?.response?.code

                                            Log.e(
                                                "ProfileFragment",
                                                "Failed to serve user's avatar. Status Code: $statusCode",
                                                exception
                                            )
                                            when (statusCode) {
                                                404 -> {
                                                    binding.main.showTopSnackbar(
                                                        R.string.error_media_msg,
                                                        SnackbarLevel.ERROR
                                                    )
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
                                binding.loadingOverlay.root.toggleFullScreenLoading(false)
                                Log.e(
                                    "ProfileFragment",
                                    "Load account details failed",
                                    state.exception
                                )
                                ErrorActivity.start(requireContext(), state.statusCode ?: 0)
                            }
                        }
                    }
                }
                launch {
                    // UPDATE ACCOUNT
                    viewModel.accountUpdateState.collect { state ->
                        when (state) {
                            is UiState.Idle -> {
                                toggleBtnLoadingState(
                                    binding.btnSaveProfile,
                                    binding.saveLoader,
                                    false,
                                    getString(R.string.btn_save)
                                )
                            }

                            is UiState.Loading -> {
                                toggleBtnLoadingState(
                                    binding.btnSaveProfile,
                                    binding.saveLoader,
                                    true,
                                    getString(R.string.btn_save)
                                )
                            }

                            is UiState.Success -> {
                                toggleBtnLoadingState(
                                    binding.btnSaveProfile,
                                    binding.saveLoader,
                                    false,
                                    getString(R.string.btn_save)
                                )
                                activity?.hideKeyboard()
                                binding.main.showTopSnackbar(
                                    R.string.snack_account_details_update_success,
                                    SnackbarLevel.SUCCESS
                                )
                            }

                            is UiState.Error -> {
                                toggleBtnLoadingState(
                                    binding.btnSaveProfile,
                                    binding.saveLoader,
                                    false,
                                    getString(R.string.btn_save)
                                )
                                Log.e(
                                    "ProfileFragment",
                                    "Update account details failed",
                                    state.exception
                                )
                                binding.main.showTopSnackbar(
                                    getString(
                                        R.string.snack_account_details_update_fail,
                                        state.exception.message
                                    ), SnackbarLevel.ERROR, Snackbar.LENGTH_LONG
                                )
                            }
                        }
                    }
                }
                launch {
                    // DELETE ACCOUNT
                    viewModel.accountDeleteState.collect { state ->
                        when (state) {
                            is UiState.Loading -> toggleDeleteAccountLoading(true)
                            is UiState.Success -> {
                                toggleDeleteAccountLoading(false)
                                SessionManager.clearSession()

                                val intent =
                                    Intent(requireContext(), LoginActivity::class.java).apply {
                                        flags =
                                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        putExtra("EXTRA_ACCOUNT_DELETED", true)
                                    }
                                startActivity(intent)
                                activity?.finish()
                            }

                            is UiState.Error -> {
                                toggleDeleteAccountLoading(false)
                                Log.e(
                                    "ProfileFragment",
                                    "Delete account failed",
                                    state.exception
                                )
                                binding.main.showTopSnackbar(
                                    getString(
                                        R.string.snack_account_delete_fail,
                                        state.exception.message
                                    ), SnackbarLevel.ERROR
                                )
                            }

                            is UiState.Idle -> {
                                toggleDeleteAccountLoading(false)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadSecurityFragment() {
        val emailToPass = binding.tvProfileEmail.text.toString()
        val securityFragment = SecuritySettingFragment.newInstance(emailToPass)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, securityFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun toggleDeleteAccountLoading(isLoading: Boolean) {
        binding.btnSettingDelete.isClickable = !isLoading
        binding.btnSettingDelete.isFocusable = !isLoading

        // Swap icon visibility state structures
        binding.ivDeleteIcon.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
        binding.deleteAccountLoader.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}