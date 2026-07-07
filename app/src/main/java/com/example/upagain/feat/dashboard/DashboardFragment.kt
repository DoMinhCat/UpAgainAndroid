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
import com.example.upagain.model.dashboard.ProAnalyticsResponse
import com.example.upagain.repository.AccountRepo
import com.example.upagain.repository.DashboardRepo
import com.example.upagain.util.auth.SessionManager
import com.example.upagain.util.ui.SnackbarLevel
import com.example.upagain.util.ui.showTopSnackbar
import com.example.upagain.util.ui.toggleFullScreenLoading
import com.example.upagain.viewmodel.AccountViewModel
import com.example.upagain.viewmodel.DashboardViewModel
import com.example.upagain.viewmodel.UiState
import com.example.upagain.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch


class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private var justLoggedIn: Boolean = false

    private val apiService by lazy { ApiClient.apiService }

    private val accountViewModel: AccountViewModel by viewModels {
        ViewModelFactory {
            AccountViewModel(
                AccountRepo(apiService),
                requireActivity().application
            )
        }
    }

    private val dashboardRepo by lazy { DashboardRepo(apiService) }
    private val dashboardViewModel: DashboardViewModel by viewModels {
        ViewModelFactory { DashboardViewModel(dashboardRepo, requireActivity().application) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
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

        // Highlight default checked timeframe button
        updateTimeframeButtonsUi(binding.toggleTime.checkedButtonId)

        val currentId = SessionManager.accountId ?: return
        accountViewModel.getAccountDetails(currentId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            DashboardFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }

    private fun setupListeners() {
        binding.btnUpgradePremium.setOnClickListener {
            val url = "${BuildConfig.FRONTEND_BASE_URL}pricing"
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        }

        binding.toggleTime.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                updateTimeframeButtonsUi(checkedId)
                val currentId = SessionManager.accountId ?: return@addOnButtonCheckedListener
                dashboardViewModel.getProAnalytics(currentId, getSelectedTimeframeCode())
            }
        }
    }

    private fun updateTimeframeButtonsUi(checkedId: Int) {
        val buttons = listOf(binding.btn24h, binding.btn7d, binding.btn30d, binding.btnYear)
        buttons.forEach { button ->
            if (button.id == checkedId) {
                button.setBackgroundColor(resources.getColor(R.color.color_primary, null))
                button.setTextColor(resources.getColor(R.color.white, null))
            } else {
                button.setBackgroundColor(resources.getColor(android.R.color.transparent, null))
                button.setTextColor(resources.getColor(R.color.color_secondary, null))
            }
        }
    }

    private fun getSelectedTimeframeCode(): String {
        return when (binding.toggleTime.checkedButtonId) {
            R.id.btn_24h -> "24h"
            R.id.btn_7d -> "7d"
            R.id.btn_30d -> "30d"
            R.id.btn_year -> "year"
            else -> "7d"
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect Account details to verify premium status
                launch {
                    accountViewModel.accountDetailsState.collect { state ->
                        when (state) {
                            is UiState.Idle -> {}
                            is UiState.Loading -> {
                                binding.loadingOverlay.root.toggleFullScreenLoading(true)
                            }

                            is UiState.Success -> {
                                val account = state.data
                                if (account.isPremium) {
                                    showDashboardContent()
                                    // Sourced live pro analytics on verification
                                    SessionManager.accountId?.let { id ->
                                        dashboardViewModel.getProAnalytics(
                                            id,
                                            getSelectedTimeframeCode()
                                        )
                                    }
                                } else {
                                    binding.loadingOverlay.root.toggleFullScreenLoading(false)
                                    showPremiumGate()
                                }
                            }

                            is UiState.Error -> {
                                binding.loadingOverlay.root.toggleFullScreenLoading(false)
                                Log.e(
                                    "DashboardFragment",
                                    "Failed to load account",
                                    state.exception
                                )
                            }
                        }
                    }
                }

                // Collect Pro Analytics data
                launch {
                    dashboardViewModel.proAnalyticsState.collect { state ->
                        when (state) {
                            is UiState.Idle -> {}
                            is UiState.Loading -> {
                                binding.loadingOverlay.root.toggleFullScreenLoading(true)
                            }

                            is UiState.Success -> {
                                binding.loadingOverlay.root.toggleFullScreenLoading(false)
                                bindAnalytics(state.data)
                            }

                            is UiState.Error -> {
                                binding.loadingOverlay.root.toggleFullScreenLoading(false)
                                Log.e(
                                    "DashboardFragment",
                                    "Failed to load pro analytics",
                                    state.exception
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun bindAnalytics(analytics: ProAnalyticsResponse) {
        // 1. Finance Cards
        binding.tvTotalSpentVal.text = String.format("%.2f €", analytics.finance.totalSpent)
        binding.tvTotalPurchasesVal.text = analytics.finance.totalPurchases.toString()
        binding.tvPaidPurchasesVal.text = analytics.finance.paidPurchases.toString()
        binding.tvFreePurchasesVal.text =
            (analytics.finance.totalPurchases - analytics.finance.paidPurchases).toString()

        // 2. Material Usage weights & progress bars (bottom section)
        val maxWeight = analytics.impact.materialUsage.maxOfOrNull { it.weight } ?: 1.0

        bindMaterialWeight(
            analytics.impact.materialUsage.find { it.material.equals("wood", true) }?.weight ?: 0.0,
            maxWeight,
            binding.tvMaterialWoodVal,
            binding.progressMaterialWood
        )
        bindMaterialWeight(
            analytics.impact.materialUsage.find { it.material.equals("metal", true) }?.weight
                ?: 0.0,
            maxWeight,
            binding.tvMaterialMetalVal,
            binding.progressMaterialMetal
        )
        bindMaterialWeight(
            analytics.impact.materialUsage.find { it.material.equals("textile", true) }?.weight
                ?: 0.0,
            maxWeight,
            binding.tvMaterialTextileVal,
            binding.progressMaterialTextile
        )
        bindMaterialWeight(
            analytics.impact.materialUsage.find { it.material.equals("glass", true) }?.weight
                ?: 0.0,
            maxWeight,
            binding.tvMaterialGlassVal,
            binding.progressMaterialGlass
        )
        bindMaterialWeight(
            analytics.impact.materialUsage.find { it.material.equals("plastic", true) }?.weight
                ?: 0.0,
            maxWeight,
            binding.tvMaterialPlasticVal,
            binding.progressMaterialPlastic
        )
        bindMaterialWeight(
            analytics.impact.materialUsage.find { it.material.equals("mixed", true) }?.weight
                ?: 0.0,
            maxWeight,
            binding.tvMaterialMixedVal,
            binding.progressMaterialMixed
        )
        bindMaterialWeight(
            analytics.impact.materialUsage.find { it.material.equals("other", true) }?.weight
                ?: 0.0,
            maxWeight,
            binding.tvMaterialOtherVal,
            binding.progressMaterialOther
        )

        // 3. Inventory Pie Chart Legends & Bar Chart Columns
        val totalAvailable = analytics.inventory.sumOf { it.available }

        // Legends
        bindLegendText("wood", analytics, totalAvailable, binding.tvLegendWood)
        bindLegendText("metal", analytics, totalAvailable, binding.tvLegendMetal)
        bindLegendText("textile", analytics, totalAvailable, binding.tvLegendTextile)
        bindLegendText("glass", analytics, totalAvailable, binding.tvLegendGlass)
        bindLegendText("plastic", analytics, totalAvailable, binding.tvLegendPlastic)
        bindLegendText("mixed", analytics, totalAvailable, binding.tvLegendMixed)
        bindLegendText("other", analytics, totalAvailable, binding.tvLegendOther)

        // Bar Columns
        val maxQty =
            analytics.inventory.maxOfOrNull { maxOf(it.available, it.added, it.recycled) } ?: 1

        bindBarChartColumn(
            "wood",
            analytics,
            maxQty,
            binding.vBarWoodAvailable,
            binding.vBarWoodOther,
            binding.tvBarWoodQty
        )
        bindBarChartColumn(
            "metal",
            analytics,
            maxQty,
            binding.vBarMetalAvailable,
            binding.vBarMetalOther,
            binding.tvBarMetalQty
        )
        bindBarChartColumn(
            "textile",
            analytics,
            maxQty,
            binding.vBarTextileAvailable,
            binding.vBarTextileOther,
            binding.tvBarTextileQty
        )
        bindBarChartColumn(
            "glass",
            analytics,
            maxQty,
            binding.vBarGlassAvailable,
            binding.vBarGlassOther,
            binding.tvBarGlassQty
        )
        bindBarChartColumn(
            "plastic",
            analytics,
            maxQty,
            binding.vBarPlasticAvailable,
            binding.vBarPlasticOther,
            binding.tvBarPlasticQty
        )
        bindBarChartColumn(
            "mixed",
            analytics,
            maxQty,
            binding.vBarMixedAvailable,
            binding.vBarMixedOther,
            binding.tvBarMixedQty
        )
        bindBarChartColumn(
            "other",
            analytics,
            maxQty,
            binding.vBarOtherAvailable,
            binding.vBarOtherOther,
            binding.tvBarOtherQty
        )
    }

    private fun bindMaterialWeight(
        weight: Double,
        maxWeight: Double,
        textVal: android.widget.TextView,
        progressBar: com.google.android.material.progressindicator.LinearProgressIndicator
    ) {
        textVal.text = String.format("%.2f kg", weight)
        progressBar.progress = if (maxWeight > 0.0) ((weight / maxWeight) * 100).toInt() else 0
    }

    private fun bindLegendText(
        materialName: String,
        analytics: ProAnalyticsResponse,
        totalAvailable: Int,
        legendTextView: android.widget.TextView
    ) {
        val stats = analytics.inventory.find { it.material.equals(materialName, true) }
        val qty = stats?.available ?: 0
        val percent =
            if (totalAvailable > 0) ((qty.toDouble() / totalAvailable) * 100).toInt() else 0

        val localizedName = when (materialName.lowercase()) {
            "wood" -> getString(R.string.material_wood)
            "metal" -> getString(R.string.material_metal)
            "textile" -> getString(R.string.material_textile)
            "glass" -> getString(R.string.material_glass)
            "plastic" -> getString(R.string.material_plastic)
            "mixed" -> getString(R.string.material_mixed)
            else -> getString(R.string.material_other)
        }

        legendTextView.text = "● $localizedName ($percent%)"
    }

    private fun bindBarChartColumn(
        materialName: String,
        analytics: ProAnalyticsResponse,
        maxQty: Int,
        vAvailable: View,
        vOther: View,
        qtyTextView: android.widget.TextView
    ) {
        val stats = analytics.inventory.find { it.material.trim().equals(materialName.trim(), true) }
        val availableVal = stats?.available ?: 0
        val otherVal = (stats?.added ?: 0) + (stats?.recycled ?: 0)

        qtyTextView.text = (availableVal + otherVal).toString()

        // Max height: 100dp
        val maxBarHeightDp = 100
        val heightAvailableDp =
            if (maxQty > 0) ((availableVal.toDouble() / maxQty) * maxBarHeightDp).toInt() else 0
        val heightOtherDp =
            if (maxQty > 0) ((otherVal.toDouble() / maxQty) * maxBarHeightDp).toInt() else 0

        setBarHeight(vAvailable, heightAvailableDp)
        setBarHeight(vOther, heightOtherDp)
    }

    private fun setBarHeight(view: View, heightDp: Int) {
        val params = view.layoutParams
        val heightPx = (heightDp * resources.displayMetrics.density).toInt()
        params.height =
            if (heightDp > 0) heightPx.coerceAtLeast(4) else 0 // minimum height of 4 pixels if > 0 so it registers visually
        view.layoutParams = params
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