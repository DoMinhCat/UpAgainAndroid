package com.example.upagain.feat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.upagain.model.account.AccountUpdateRequest
import com.example.upagain.repository.AccountRepo
import com.example.upagain.util.auth.SessionManager
import com.example.upagain.util.bin.ImageType
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
import com.example.upagain.util.bin.buildImageUrl
import com.example.upagain.util.locale.LocaleManager
import com.example.upagain.util.ui.dpToPx
import com.example.upagain.util.ui.hideKeyboard
import com.example.upagain.util.ui.toggleFullScreenLoading
import com.example.upagain.util.ui.toggleIconLoadingState
import com.google.android.material.snackbar.Snackbar

// TODO: notification settings fragment
class ProfileFragment : Fragment() {
    // elements binding
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val apiService by lazy { ApiClient.apiService }
    private val repository by lazy { AccountRepo(apiService) }
    private val appInstance by lazy { requireActivity().application }
    private val viewModel: AccountViewModel by viewModels {
        ViewModelFactory { AccountViewModel(repository, appInstance) }
    }

    // Registers the system photo picker launcher
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            // Image selected successfully, send to viewmodel for processing
            val accountId = SessionManager.accountId ?: return@registerForActivityResult
            viewModel.uploadAvatar(accountId, uri)
        }
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
        // Use a no-filter adapter so selection doesn't collapse the list
        val languageAdapter = object : ArrayAdapter<String>(
            requireContext(),
            R.layout.item_dropdown, // your custom item layout
            languages
        ) {
            override fun getFilter() = object : android.widget.Filter() {
                override fun performFiltering(c: CharSequence?) =
                    FilterResults().apply { values = languages; count = languages.size }

                override fun publishResults(c: CharSequence?, r: FilterResults?) {
                    notifyDataSetChanged()
                }
            }
        }
        binding.actvLanguageSelector.setAdapter(languageAdapter)

        // ! Always set up listeners and observers before API call
        setupListeners()
        observeAccountState()

        // API call
        val currentId = SessionManager.accountId ?: return
        viewModel.getAccountDetails(currentId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        toggleErrorState(binding.tilProfilePhone, false)
        toggleErrorState(binding.tilProfileName, false)
        _binding = null
    }

    // PRIVATE ZONE
    private fun setupListeners() {
        // LOG OUT BUTTON
        binding.btnLogout.setOnClickListener {
            handleLogOut()
        }
        // LANGUAGE
        binding.actvLanguageSelector.setOnItemClickListener { parent, _, position, _ ->
            val selectedLanguage = parent.getItemAtPosition(position) as String
            LocaleManager.setLocaleByDisplayName(selectedLanguage)
            // setText(..., false) prevents filtering the adapter to a single item
            binding.actvLanguageSelector.setText(selectedLanguage, false)
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
        // USERNAME FIELD
        binding.etProfileName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val username = binding.etProfileName.text.toString()
                val isUsernameValid = usernameValidator.validate(username)
                toggleErrorState(binding.tilProfileName, !isUsernameValid)
            } else {
                toggleErrorState(binding.tilProfileName, false)
            }
        }
        // PHONE FIELD
        binding.etProfilePhone.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val phone = binding.etProfilePhone.text.toString()
                val isPhoneValid = phoneValidator.validate(phone)
                toggleErrorState(binding.tilProfilePhone, !isPhoneValid)
            } else {
                toggleErrorState(binding.tilProfilePhone, false)
            }
        }

        // SAVE CHANGES
        binding.btnSaveProfile.setOnClickListenerWithCooldown {
            val email = binding.tvProfileEmail.text.toString()
            val username = binding.etProfileName.text.toString()
            val phone = binding.etProfilePhone.text.toString()
            val isUsernameValid = usernameValidator.validate(username)
            val isPhoneValid = phoneValidator.validate(phone)
            toggleErrorState(binding.tilProfileName, !isUsernameValid)
            toggleErrorState(binding.tilProfilePhone, !isPhoneValid)
            if (!isUsernameValid || !isPhoneValid) {
                return@setOnClickListenerWithCooldown
            }
            val request = AccountUpdateRequest(username, email, phone)
            val currentId = SessionManager.accountId ?: return@setOnClickListenerWithCooldown
            viewModel.updateAccount(currentId, request)
        }
        // DELETE ACCOUNT
        binding.btnSettingDelete.setOnClickListenerWithCooldown {
            DialogUtils.showDestructiveConfirmationDialog(
                context = requireContext(),
                title = getString(R.string.confirm_del_account),
            ) {
                val currentUserId =
                    SessionManager.accountId ?: return@showDestructiveConfirmationDialog
                viewModel.deleteAccount(currentUserId)
            }
        }
        // UPDATE AVATAR
        binding.ivAvatar.setOnClickListener {
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    private fun observeAccountState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // GET ACCOUNT DETAILS
                launch {
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
                                binding.ivAvatar.load(buildImageUrl(account.avatar, ImageType.AVATAR)) {
                                    crossfade(true)
                                    placeholder(R.drawable.ic_avatar_unknown)
                                    error(R.drawable.ic_avatar_unknown)

                                    listener(
                                        onStart = { _ ->
                                            binding.avatarLoader.visibility = View.VISIBLE
                                        },
                                        onSuccess = { _, _ ->
                                            binding.avatarLoader.visibility = View.GONE
                                        },
                                        onError = { _, result ->
                                            binding.avatarLoader.visibility = View.GONE
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
                                    "Load account details failed. Status code: ${state.statusCode}",
                                    state.exception
                                )
                                ErrorActivity.start(requireContext(), state.statusCode ?: 0)
                            }
                        }
                    }
                }
                // UPDATE ACCOUNT
                launch {
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
                                viewModel.resetAccountUpdateState()
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
                                viewModel.resetAccountUpdateState()
                            }
                        }
                    }
                }
                // DELETE ACCOUNT
                launch {
                    viewModel.accountDeleteState.collect { state ->
                        when (state) {
                            is UiState.Loading -> toggleDeleteAccountLoading(true)
                            is UiState.Success -> {
                                toggleDeleteAccountLoading(false)
                                viewModel.resetAccountDeleteState()
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
                                viewModel.resetAccountDeleteState()
                            }

                            is UiState.Idle -> {
                                toggleDeleteAccountLoading(false)
                            }
                        }
                    }
                }
                // UPDATE AVATAR
                launch {
                    viewModel.accountAvatarUploadState.collect { state ->
                        when (state) {
                            is UiState.Loading -> toggleAvatarLoading(true)
                            is UiState.Success -> {
                                toggleAvatarLoading(false)
                                viewModel.resetAccountAvatarUploadState()
                                binding.main.showTopSnackbar(
                                    R.string.snack_avatar_update_success,
                                    SnackbarLevel.SUCCESS
                                )
                                // Re-fetch user profile data to load the new avatar
                                val accountId = SessionManager.accountId ?: return@collect
                                viewModel.getAccountDetails(accountId)
                            }

                            is UiState.Error -> {
                                viewModel.resetAccountAvatarUploadState()
                                toggleAvatarLoading(false)
                                Log.e(
                                    "ProfileFragment",
                                    "Avatar upload failed. Status Code: ${state.statusCode}",
                                    state.exception
                                )
                                binding.main.showTopSnackbar(
                                    getString(
                                        R.string.snack_avatar_update_fail,
                                        state.exception.message
                                    ),
                                    SnackbarLevel.ERROR
                                )
                            }

                            is UiState.Idle -> {
                                toggleAvatarLoading(false)
                            }
                        }
                    }
                }
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

    private fun loadSecurityFragment() {
        toggleErrorState(binding.tilProfilePhone, false)
        toggleErrorState(binding.tilProfileName, false)
        val emailToPass = binding.tvProfileEmail.text.toString()
        val securityFragment = SecuritySettingFragment.newInstance(emailToPass)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, securityFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun toggleDeleteAccountLoading(isLoading: Boolean) {
        binding.btnSettingDelete.toggleIconLoadingState(
            componentZone = binding.btnSettingDelete,
            staticIcon = binding.ivDeleteIcon,
            loader = binding.deleteAccountLoader,
            isLoading = isLoading
        )
    }

    private fun toggleAvatarLoading(isLoading: Boolean) {
        binding.ivAvatar.toggleIconLoadingState(
            componentZone = binding.ivAvatar,
            staticIcon = binding.ivAvatar,
            loader = binding.avatarLoader,
            isLoading = isLoading
        )
    }

    private fun toggleErrorState(
        til: com.google.android.material.textfield.TextInputLayout,
        isError: Boolean
    ) {
        val errorString =
            if (til == binding.tilProfileName) getString(R.string.invalid_username) else getString(R.string.invalid_phone)
        if (isError) {
            til.boxStrokeWidth = dpToPx(1.5f, resources)
            til.boxStrokeWidthFocused = dpToPx(1.5f, resources)
            til.isErrorEnabled = true
            til.error = errorString
        } else {
            til.error = null
            til.isErrorEnabled = false
            til.boxStrokeWidth = 0
            til.boxStrokeWidthFocused = 0
        }
    }
}