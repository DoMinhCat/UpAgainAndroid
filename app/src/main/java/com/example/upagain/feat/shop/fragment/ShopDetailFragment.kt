package com.example.upagain.feat.shop.fragment

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
import com.example.upagain.databinding.FragmentShopDetailBinding
import com.example.upagain.model.transaction.ItemPurchaseRequest
import com.example.upagain.repository.ItemRepo
import com.example.upagain.util.ui.SnackbarLevel
import com.example.upagain.util.ui.setOnBackClickListener
import com.example.upagain.util.ui.setOnClickListenerWithCooldown
import com.example.upagain.util.ui.showTopSnackbar
import com.example.upagain.util.ui.toggleFullScreenLoading
import com.example.upagain.viewmodel.ItemViewModel
import com.example.upagain.viewmodel.UiState
import com.example.upagain.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

private const val ARG_ITEM_ID = "arg_item_id"
private const val ARG_STRIPE_URL = "arg_stripe_url"

class ShopDetailFragment : Fragment() {
    private var idItem: Int? = null
    private var stripeUrl: String? = null

    private var _binding: FragmentShopDetailBinding? = null
    private val binding get() = _binding!!

    private val apiService by lazy { ApiClient.apiService }
    private val itemViewModel: ItemViewModel by viewModels {
        ViewModelFactory { ItemViewModel(ItemRepo(apiService), requireActivity().application) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            idItem = it.getInt(ARG_ITEM_ID)
            stripeUrl = it.getString(ARG_STRIPE_URL)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShopDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeState()

        idItem?.let { id ->
            itemViewModel.fetchItemDetailsComplete(id)
            itemViewModel.getLatestTransactionOfPro(id)
        }

        if (!stripeUrl.isNullOrEmpty()) {
            onPaymentSuccessReturned(stripeUrl!!)
            arguments?.remove(ARG_STRIPE_URL)
            stripeUrl = null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(idItem: Int, stripeUrl: String = "") = ShopDetailFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_ITEM_ID, idItem)
                putString(ARG_STRIPE_URL, stripeUrl)
            }
        }
    }

    fun onPaymentSuccessReturned(returnUrlString: String) {
        lifecycleScope.launch {
            try {
                val uri = returnUrlString.toUri()
                val paymentStatus = uri.getQueryParameter("payment")

                if (paymentStatus != "success") {
                    binding.main.showTopSnackbar(
                        getString(R.string.snack_payment_verification_fail),
                        SnackbarLevel.ERROR
                    )
                    return@launch
                }

                idItem?.let { id ->
                    itemViewModel.purchaseItem(
                        id,
                        ItemPurchaseRequest(
                            origin_url = BuildConfig.PAYMENT_DEEPLINK,
                            paid = true
                        )
                    )
                }

            } catch (e: Exception) {
                Log.e(
                    "ShopDetailFragment",
                    "Error executing 2nd purchase request",
                    e
                )
                binding.main.showTopSnackbar(
                    getString(R.string.snack_payment_verification_fail),
                    SnackbarLevel.ERROR
                )
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnBackClickListener()

        binding.btnActionReserve.setOnClickListenerWithCooldown {
            idItem?.let { itemViewModel.reserveItem(it) }
        }

        binding.btnActionPurchase.setOnClickListenerWithCooldown {
            idItem?.let { id ->
                itemViewModel.purchaseItem(
                    id,
                    ItemPurchaseRequest(
                        origin_url = BuildConfig.PAYMENT_DEEPLINK + "?frag=ShopDetailFragment&id_item=$idItem",
                        paid = false
                    )
                )
            }
        }

        binding.btnActionCancelReserve.setOnClickListenerWithCooldown {
            idItem?.let { itemViewModel.cancelReservation(it) }
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    itemViewModel.itemDetailState.collect { state ->
                        when (state) {
                            is UiState.Idle -> {}
                            is UiState.Loading -> binding.loadingOverlay.root.toggleFullScreenLoading(
                                true
                            )

                            is UiState.Success -> {
                                binding.loadingOverlay.root.toggleFullScreenLoading(false)
                                val item = state.data
                                binding.tvTitle.text = item.title
                                binding.chipMaterial.text = item.material.uppercase()
                                binding.tvItemInfo.text = getString(R.string.item_info_format, item.state, item.weight.toString())
                            }

                            is UiState.Error -> {
                                binding.loadingOverlay.root.toggleFullScreenLoading(false)
                            }
                        }
                    }
                }

                launch {
                    itemViewModel.latestTransactionState.collect { state ->
                        when (state) {
                            is UiState.Success -> {
                                val tx = state.data
                                if (tx.action == "reserved") {
                                    binding.btnActionReserve.visibility = View.GONE
                                    binding.btnActionPurchase.visibility = View.VISIBLE
                                    binding.btnActionCancelReserve.visibility = View.VISIBLE
                                } else {
                                    binding.btnActionReserve.visibility = View.VISIBLE
                                    binding.btnActionPurchase.visibility = View.VISIBLE
                                    binding.btnActionCancelReserve.visibility = View.GONE
                                }
                            }

                            is UiState.Error -> {
                                binding.btnActionReserve.visibility = View.VISIBLE
                                binding.btnActionPurchase.visibility = View.VISIBLE
                                binding.btnActionCancelReserve.visibility = View.GONE
                            }

                            else -> {}
                        }
                    }
                }

                launch {
                    itemViewModel.purchaseState.collect { state ->
                        when (state) {
                            is UiState.Loading -> binding.loadingOverlay.root.toggleFullScreenLoading(
                                true
                            )

                            is UiState.Success -> {
                                itemViewModel.resetPurchaseState()
                                binding.loadingOverlay.root.toggleFullScreenLoading(false)

                                val rawResponse = state.data
                                if (rawResponse.contains("checkout_url")) {
                                    val url = rawResponse.substringAfter("\"checkout_url\":\"")
                                        .substringBefore("\"")
                                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                    startActivity(intent)
                                } else {
                                    binding.main.showTopSnackbar(
                                        getString(R.string.snack_purchase_success),
                                        SnackbarLevel.SUCCESS
                                    )
                                    idItem?.let { itemViewModel.getLatestTransactionOfPro(it) }
                                }
                            }

                            is UiState.Error -> {
                                itemViewModel.resetPurchaseState()
                                binding.loadingOverlay.root.toggleFullScreenLoading(false)
                                binding.main.showTopSnackbar(
                                    getString(R.string.snack_purchase_fail),
                                    SnackbarLevel.ERROR
                                )
                            }

                            else -> {}
                        }
                    }
                }

                launch {
                    itemViewModel.reserveState.collect { state ->
                        when (state) {
                            is UiState.Loading -> binding.loadingOverlay.root.toggleFullScreenLoading(
                                true
                            )

                            is UiState.Success -> {
                                itemViewModel.resetReserveState()
                                binding.loadingOverlay.root.toggleFullScreenLoading(false)
                                binding.main.showTopSnackbar(getString(R.string.snack_reserve_success), SnackbarLevel.SUCCESS)
                                idItem?.let { itemViewModel.getLatestTransactionOfPro(it) }
                            }

                            is UiState.Error -> {
                                itemViewModel.resetReserveState()
                                binding.loadingOverlay.root.toggleFullScreenLoading(false)
                                binding.main.showTopSnackbar(
                                    getString(R.string.snack_reserve_fail),
                                    SnackbarLevel.ERROR
                                )
                            }

                            else -> {}
                        }
                    }
                }

                launch {
                    itemViewModel.cancelReserveState.collect { state ->
                        when (state) {
                            is UiState.Loading -> binding.loadingOverlay.root.toggleFullScreenLoading(
                                true
                            )

                            is UiState.Success -> {
                                itemViewModel.resetCancelReservationState()
                                binding.loadingOverlay.root.toggleFullScreenLoading(false)
                                binding.main.showTopSnackbar(
                                    getString(R.string.snack_cancel_success),
                                    SnackbarLevel.SUCCESS
                                )
                                idItem?.let { itemViewModel.getLatestTransactionOfPro(it) }
                            }

                            is UiState.Error -> {
                                itemViewModel.resetCancelReservationState()
                                binding.loadingOverlay.root.toggleFullScreenLoading(false)
                                binding.main.showTopSnackbar(
                                    getString(R.string.snack_cancel_fail),
                                    SnackbarLevel.ERROR
                                )
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
    }
}