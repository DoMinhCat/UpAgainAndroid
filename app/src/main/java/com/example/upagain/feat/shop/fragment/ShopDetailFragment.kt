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

    private lateinit var carouselAdapter: com.example.upagain.feat.post.adapter.CarouselImageAdapter

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
        
        carouselAdapter = com.example.upagain.feat.post.adapter.CarouselImageAdapter {
            // Handle image click
        }
        binding.vpCarousel.adapter = carouselAdapter

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
                        getString(R.string.snack_payment_verification_fail, "Payment status not success"),
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
                    getString(R.string.snack_payment_verification_fail, e.message ?: ""),
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
                // GET ITEM DETAIL
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
                                binding.chipMaterial.setChipBackgroundColorResource(com.example.upagain.util.ui.getItemMaterialColor(item.material))
                                binding.tvItemInfo.text = getString(R.string.item_info_format, item.state.replace("_", ""), item.weight.toString())
                                
                                binding.tvPrice.text = if (item.price > 0) "${item.price} €" else getString(R.string.free)
                                binding.tvScore.text = (item.score ?: 0).toString()
                                binding.tvDescription.text = androidx.core.text.HtmlCompat.fromHtml(
                                    item.description ?: "",
                                    androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
                                )
                                
                                val photosList = item.images.orEmpty()
                                carouselAdapter.submitList(photosList)
                                if (photosList.size > 1) {
                                    binding.ivDefaultCarouselPlaceholder.visibility = View.GONE
                                    binding.tlCarouselIndicator.visibility = View.VISIBLE
                                    com.google.android.material.tabs.TabLayoutMediator(
                                        binding.tlCarouselIndicator, binding.vpCarousel
                                    ) { _, _ ->
                                    }.attach()
                                } else {
                                    binding.vpCarousel.visibility = View.GONE
                                    binding.tlCarouselIndicator.visibility = View.GONE
                                    binding.ivDefaultCarouselPlaceholder.visibility = View.VISIBLE
                                }

                                val tx = (itemViewModel.latestTransactionState.value as? UiState.Success)?.data
                                Log.d("ShopDetailFragment", "itemDetailState loaded category=${item.category}. Current tx action=${tx?.action}")
                                updateAccessCodes(tx, item)
                            }

                            is UiState.Error -> {
                                binding.loadingOverlay.root.toggleFullScreenLoading(false)
                                Log.e("ShopDetailFragment", "Failed to load item detail", state.exception)
                            }
                        }
                    }
                }

                // GET LISTING DETAILS
                launch {
                    itemViewModel.listingDetailState.collect { state ->
                        if (state is UiState.Success) {
                            val listing = state.data
                            binding.tvLocation.text = "${listing.city ?: ""}, ${listing.postal_code ?: ""}"
                        } else if (state is UiState.Error) {
                            Log.e("ShopDetailFragment", "Failed to load listing details", state.exception)
                        }
                    }
                }

                // GET DEPOSIT DETAILS
                launch {
                    itemViewModel.depositDetailState.collect { state ->
                        if (state is UiState.Success) {
                            val deposit = state.data
                            binding.tvLocation.text = "${deposit.street ?: ""}, ${deposit.postal_code ?: ""} ${deposit.city ?: ""}"
                        } else if (state is UiState.Error) {
                            Log.e("ShopDetailFragment", "Failed to load deposit details", state.exception)
                        }
                    }
                }

                launch {
                    itemViewModel.latestTransactionState.collect { state ->
                        when (state) {
                            is UiState.Success -> {
                                val tx = state.data
                                val item = (itemViewModel.itemDetailState.value as? UiState.Success)?.data
                                Log.d("ShopDetailFragment", "latestTransactionState loaded action=${tx.action}, confirmCode=${tx.confirmCode}. Current item category=${item?.category}")
                                updateAccessCodes(tx, item)
                            }

                            is UiState.Error -> {
                                Log.e("ShopDetailFragment", "Failed to load transaction state", state.exception)
                                binding.btnActionReserve.visibility = View.VISIBLE
                                binding.btnActionPurchase.visibility = View.VISIBLE
                                binding.btnActionCancelReserve.visibility = View.GONE
                                binding.chipPurchasedStatus.visibility = View.GONE
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
                                Log.e("ShopDetailFragment", "Purchase action failed", state.exception)
                                binding.main.showTopSnackbar(
                                    getString(R.string.snack_purchase_fail, state.exception.message ?: ""),
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
                                Log.e("ShopDetailFragment", "Reservation action failed", state.exception)
                                binding.main.showTopSnackbar(
                                    getString(R.string.snack_reserve_fail, state.exception.message ?: ""),
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
                                Log.e("ShopDetailFragment", "Cancel reservation action failed", state.exception)
                                binding.main.showTopSnackbar(
                                    getString(R.string.snack_cancel_fail, state.exception.message ?: ""),
                                    SnackbarLevel.ERROR
                                )
                            }

                            else -> {}
                        }
                    }
                }

                launch {
                    itemViewModel.depositCodesState.collect { state ->
                        when (state) {
                            is UiState.Loading -> {}
                            is UiState.Success -> {
                                val codes = state.data
                                Log.d("ShopDetailFragment", "depositCodesState success: size=${codes.size}, codes=${codes.map { "id=${it.id}, type=${it.userType}, status=${it.status}" }}")
                                val proCode = codes.find { it.userType == "pro" }
                                if (proCode != null && proCode.barcodeBase64.isNotEmpty() && proCode.status == "active") {
                                    try {
                                        val base64Str = proCode.barcodeBase64.substringAfter("base64,")
                                        val decodedString = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
                                        val decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                                        binding.ivBarcode.setImageBitmap(decodedByte)
                                        binding.ivBarcode.visibility = View.VISIBLE
                                        binding.tvConfirmationCode.visibility = View.GONE
                                        binding.tvWaitingDropoffMessage.visibility = View.GONE
                                    } catch (e: Exception) {
                                        Log.e("ShopDetailFragment", "Failed to decode barcode", e)
                                        binding.tvWaitingDropoffMessage.visibility = View.VISIBLE
                                        binding.ivBarcode.visibility = View.GONE
                                    }
                                } else {
                                    binding.tvWaitingDropoffMessage.visibility = View.VISIBLE
                                    binding.ivBarcode.visibility = View.GONE
                                    binding.tvConfirmationCode.visibility = View.GONE
                                }
                            }
                            is UiState.Error -> {
                                Log.e("ShopDetailFragment", "Failed to load deposit codes", state.exception)
                                binding.tvWaitingDropoffMessage.visibility = View.VISIBLE
                                binding.ivBarcode.visibility = View.GONE
                                binding.tvConfirmationCode.visibility = View.GONE
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    private fun updateAccessCodes(
        tx: com.example.upagain.model.transaction.TransactionResponse?,
        item: com.example.upagain.model.item.ItemDetailResponse?
    ) {
        if (tx == null || item == null) {
            Log.d("ShopDetailFragment", "updateAccessCodes: skipped because tx is null (${tx == null}) or item is null (${item == null})")
            return
        }
        Log.d("ShopDetailFragment", "updateAccessCodes: updating action=${tx.action}, category=${item.category}")

        if (tx.action == "purchased") {
            binding.btnActionReserve.visibility = View.GONE
            binding.btnActionPurchase.visibility = View.GONE
            binding.btnActionCancelReserve.visibility = View.GONE
            binding.chipPurchasedStatus.visibility = View.VISIBLE

            // Display access codes
            binding.layoutAccessCodes.visibility = View.VISIBLE
            if (item.category == "listing") {
                binding.tvConfirmationCode.text = tx.confirmCode ?: ""
                binding.tvConfirmationCode.visibility = View.VISIBLE
                binding.tvWaitingDropoffMessage.visibility = View.GONE
                binding.ivBarcode.visibility = View.GONE
            } else if (item.category == "deposit") {
                idItem?.let { itemViewModel.getDepositCodes(it) }
            }
        } else if (tx.action == "reserved") {
            binding.btnActionReserve.visibility = View.GONE
            binding.btnActionPurchase.visibility = View.VISIBLE
            binding.btnActionCancelReserve.visibility = View.VISIBLE
            binding.chipPurchasedStatus.visibility = View.GONE
            binding.layoutAccessCodes.visibility = View.GONE
        } else {
            binding.btnActionReserve.visibility = View.VISIBLE
            binding.btnActionPurchase.visibility = View.VISIBLE
            binding.btnActionCancelReserve.visibility = View.GONE
            binding.chipPurchasedStatus.visibility = View.GONE
            binding.layoutAccessCodes.visibility = View.GONE
        }
    }
}