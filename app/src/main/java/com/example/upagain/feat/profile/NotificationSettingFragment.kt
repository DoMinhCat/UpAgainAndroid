package com.example.upagain.feat.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.upagain.R
import com.example.upagain.api.ApiClient
import com.example.upagain.databinding.FragmentNotificationSettingBinding
import com.example.upagain.model.noti.NotiSetting
import com.example.upagain.repository.NotiSettingRepo
import com.example.upagain.util.auth.SessionManager
import com.example.upagain.util.ui.SnackbarLevel
import com.example.upagain.util.ui.setOnBackClickListener
import com.example.upagain.util.ui.showTopSnackbar
import com.example.upagain.util.ui.toggleFullScreenLoading
import com.example.upagain.viewmodel.NotiSettingViewModel
import com.example.upagain.viewmodel.UiState
import com.example.upagain.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class NotificationSettingFragment : Fragment() {
    private var _binding: FragmentNotificationSettingBinding? = null
    private val binding get() = _binding!!

    private val apiService by lazy { ApiClient.apiService }
    private val repo by lazy { NotiSettingRepo(apiService) }
    private val viewModel: NotiSettingViewModel by viewModels {
        ViewModelFactory { NotiSettingViewModel(repo, requireActivity().application) }
    }

    private var isBinding = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnBackClickListener()

        setupListeners()
        observeState()

        val accountId = SessionManager.accountId ?: return
        viewModel.loadNotificationSettings(accountId)
        viewModel.loadProAlertMaterials(accountId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupListeners() {
        val switchListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            if (isBinding) return@OnCheckedChangeListener
            val accountId = SessionManager.accountId ?: return@OnCheckedChangeListener

            when (buttonView.id) {
                R.id.switch_pro_material_available -> viewModel.updateNotificationSetting(accountId, "pro_material_available", isChecked)
                R.id.switch_pro_object_deposited -> viewModel.updateNotificationSetting(accountId, "pro_object_deposited", isChecked)
                R.id.switch_pro_object_expired -> viewModel.updateNotificationSetting(accountId, "pro_object_expired", isChecked)
                R.id.switch_pro_subscription_end -> viewModel.updateNotificationSetting(accountId, "pro_subscription_end", isChecked)
                R.id.switch_pro_code_expiring -> viewModel.updateNotificationSetting(accountId, "pro_code_expiring", isChecked)

                R.id.switch_alert_wood, R.id.switch_alert_metal, R.id.switch_alert_textile,
                R.id.switch_alert_glass, R.id.switch_alert_plastic, R.id.switch_alert_mixed,
                R.id.switch_alert_other -> {
                    viewModel.updateProAlertMaterials(accountId, getSelectedMaterialsFromUi())
                }
            }
        }

        val switches = listOf(
            binding.switchProMaterialAvailable,
            binding.switchProObjectDeposited,
            binding.switchProObjectExpired,
            binding.switchProSubscriptionEnd,
            binding.switchProCodeExpiring,
            binding.switchAlertWood,
            binding.switchAlertMetal,
            binding.switchAlertTextile,
            binding.switchAlertGlass,
            binding.switchAlertPlastic,
            binding.switchAlertMixed,
            binding.switchAlertOther
        )
        switches.forEach { it.setOnCheckedChangeListener(switchListener) }
    }

    private fun getSelectedMaterialsFromUi(): List<String> {
        val list = mutableListOf<String>()
        if (binding.switchAlertWood.isChecked) list.add("wood")
        if (binding.switchAlertMetal.isChecked) list.add("metal")
        if (binding.switchAlertTextile.isChecked) list.add("textile")
        if (binding.switchAlertGlass.isChecked) list.add("glass")
        if (binding.switchAlertPlastic.isChecked) list.add("plastic")
        if (binding.switchAlertMixed.isChecked) list.add("mixed")
        if (binding.switchAlertOther.isChecked) list.add("other")
        return list
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect settings state
                launch {
                    viewModel.notiSettingsState.collect { state ->
                        when (state) {
                            is UiState.Idle -> {}
                            is UiState.Loading -> {
                                binding.loadingOverlay.root.toggleFullScreenLoading(true)
                            }
                            is UiState.Success -> {
                                binding.loadingOverlay.root.toggleFullScreenLoading(false)
                                bindNotiSettings(state.data)
                            }
                            is UiState.Error -> {
                                binding.loadingOverlay.root.toggleFullScreenLoading(false)
                                binding.main.showTopSnackbar(R.string.snack_noti_load_fail, SnackbarLevel.ERROR)
                                Log.e("NotiSettingFragment", "Failed to load settings", state.exception)
                            }
                        }
                    }
                }

                // Collect alerts state
                launch {
                    viewModel.alertMaterialsState.collect { state ->
                        when (state) {
                            is UiState.Idle -> {}
                            is UiState.Loading -> {
                                binding.loadingOverlay.root.toggleFullScreenLoading(true)
                            }
                            is UiState.Success -> {
                                binding.loadingOverlay.root.toggleFullScreenLoading(false)
                                bindMaterialAlerts(state.data)
                            }
                            is UiState.Error -> {
                                binding.loadingOverlay.root.toggleFullScreenLoading(false)
                                Log.e("NotiSettingFragment", "Failed to load alert materials", state.exception)
                            }
                        }
                    }
                }

                // Collect update state (notification switches)
                launch {
                    viewModel.notiUpdateState.collect { state ->
                        when (state) {
                            is UiState.Idle -> {}
                            is UiState.Loading -> {
                                binding.loadingOverlay.root.toggleFullScreenLoading(true)
                            }
                            is UiState.Success -> {
                                binding.loadingOverlay.root.toggleFullScreenLoading(false)
                                binding.main.showTopSnackbar(R.string.snack_noti_update_success, SnackbarLevel.SUCCESS)
                            }
                            is UiState.Error -> {
                                binding.loadingOverlay.root.toggleFullScreenLoading(false)
                                val errMsg = state.exception?.message ?: ""
                                binding.main.showTopSnackbar(
                                    getString(R.string.snack_noti_update_fail, errMsg),
                                    SnackbarLevel.ERROR
                                )
                            }
                        }
                    }
                }

                // Collect material alerts update state
                launch {
                    viewModel.alertMaterialsUpdateState.collect { state ->
                        when (state) {
                            is UiState.Idle -> {}
                            is UiState.Loading -> {
                                binding.loadingOverlay.root.toggleFullScreenLoading(true)
                            }
                            is UiState.Success -> {
                                binding.loadingOverlay.root.toggleFullScreenLoading(false)
                                binding.main.showTopSnackbar(R.string.snack_alerts_update_success, SnackbarLevel.SUCCESS)
                            }
                            is UiState.Error -> {
                                binding.loadingOverlay.root.toggleFullScreenLoading(false)
                                val errMsg = state.exception?.message ?: ""
                                binding.main.showTopSnackbar(
                                    getString(R.string.snack_alerts_update_fail, errMsg),
                                    SnackbarLevel.ERROR
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun bindNotiSettings(settings: List<NotiSetting>) {
        isBinding = true
        settings.forEach { setting ->
            when (setting.notiType) {
                "pro_material_available" -> binding.switchProMaterialAvailable.isChecked = setting.isEnabled
                "pro_object_deposited" -> binding.switchProObjectDeposited.isChecked = setting.isEnabled
                "pro_object_expired" -> binding.switchProObjectExpired.isChecked = setting.isEnabled
                "pro_subscription_end" -> binding.switchProSubscriptionEnd.isChecked = setting.isEnabled
                "pro_code_expiring" -> binding.switchProCodeExpiring.isChecked = setting.isEnabled
            }
        }
        isBinding = false
    }

    private fun bindMaterialAlerts(materials: List<String>) {
        isBinding = true
        binding.switchAlertWood.isChecked = materials.contains("wood")
        binding.switchAlertMetal.isChecked = materials.contains("metal")
        binding.switchAlertTextile.isChecked = materials.contains("textile")
        binding.switchAlertGlass.isChecked = materials.contains("glass")
        binding.switchAlertPlastic.isChecked = materials.contains("plastic")
        binding.switchAlertMixed.isChecked = materials.contains("mixed")
        binding.switchAlertOther.isChecked = materials.contains("other")
        isBinding = false
    }

    companion object {
        @JvmStatic
        fun newInstance() = NotificationSettingFragment()
    }
}