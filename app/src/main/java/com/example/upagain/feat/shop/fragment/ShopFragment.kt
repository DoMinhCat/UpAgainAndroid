package com.example.upagain.feat.shop.fragment

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.upagain.R
import com.example.upagain.api.ApiClient
import com.example.upagain.databinding.FragmentShopBinding
import com.example.upagain.feat.shop.adapter.ItemAdapter
import com.example.upagain.model.item.ItemDetailResponse
import com.example.upagain.model.item.ItemStatus
import com.example.upagain.repository.ItemRepo
import com.example.upagain.util.ui.SnackbarLevel
import com.example.upagain.util.ui.showTopSnackbar
import com.example.upagain.viewmodel.ItemViewModel
import com.example.upagain.viewmodel.UiState
import com.example.upagain.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class ShopFragment : Fragment() {

    private var _binding: FragmentShopBinding? = null
    private val binding get() = _binding!!

    private val apiService by lazy { ApiClient.apiService }
    private val itemRepository by lazy { ItemRepo(apiService) }
    private val appInstance by lazy { requireActivity().application }

    private val itemViewModel: ItemViewModel by viewModels {
        ViewModelFactory { ItemViewModel(itemRepository, appInstance) }
    }

    private lateinit var shopAdapter: ItemAdapter
    private var searchQuery = ""
    private var selectedMaterial = ""

    private var currentPage = 1
    private val loadedItems = mutableListOf<ItemDetailResponse>()

    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShopBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchRunnable?.let { handler.removeCallbacks(it) }
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeState()

        loadItems(1)
    }

    private fun setupRecyclerView() {
        shopAdapter = ItemAdapter(object : ItemAdapter.OnClickListener {
            override fun onItemClick(item: ItemDetailResponse) {
                // TODO: navigate to detail page
            }

            override fun onLoadMoreClick() {
                loadItems(currentPage + 1)
            }
        })
        binding.rvShopItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = shopAdapter
        }
    }

    private fun setupListeners() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchRunnable?.let { handler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    searchQuery = s?.toString()?.trim() ?: ""
                    loadItems(1)
                }
                handler.postDelayed(searchRunnable!!, 300)
            }
        })

        binding.chipGroupMaterials.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull()

            // Reset all chips colors to default surface background and on-background text
            val allChips = listOf(
                binding.chipWood,
                binding.chipTextile,
                binding.chipGlass,
                binding.chipMetal,
                binding.chipPlastic,
                binding.chipMixed,
                binding.chipOther
            )
            for (chip in allChips) {
                chip.chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.color_surface)
                )
                chip.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.color_on_background)
                )
            }

            selectedMaterial = if (checkedId != null) {
                val checkedChip =
                    group.findViewById<com.google.android.material.chip.Chip>(checkedId)
                checkedChip.chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.color_primary)
                )
                checkedChip.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.color_on_primary)
                )

                when (checkedId) {
                    binding.chipWood.id -> "wood"
                    binding.chipTextile.id -> "textile"
                    binding.chipGlass.id -> "glass"
                    binding.chipMetal.id -> "metal"
                    binding.chipPlastic.id -> "plastic"
                    binding.chipMixed.id -> "mixed"
                    binding.chipOther.id -> "other"
                    else -> ""
                }
            } else {
                ""
            }
            loadItems(1)
        }
    }

    private fun loadItems(page: Int = 1) {
        val options = mutableMapOf<String, String>()
        options["page"] = page.toString()
        options["limit"] = "1"
        options["status"] = ItemStatus.APPROVED.value
        if (searchQuery.isNotEmpty()) {
            options["search"] = searchQuery
        }
        if (selectedMaterial.isNotEmpty()) {
            options["material"] = selectedMaterial
        }
        itemViewModel.getAllItems(options, isFirstPage = (page == 1))
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                itemViewModel.allItemsState.collect { state ->
                    when (state) {
                        is UiState.Idle -> {
                            toggleAllItemsLoading(false, true)
                        }

                        is UiState.Loading -> {
                            toggleAllItemsLoading(true, state.isFirstPage)
                        }

                        is UiState.Success -> {
                            val itemsResponse = state.data
                            currentPage = itemsResponse.currentPage

                            if (currentPage == 1) {
                                loadedItems.clear()
                                toggleAllItemsLoading(false, isFirstPage = true)
                            } else {
                                toggleAllItemsLoading(false, isFirstPage = false)
                            }

                            // Empty state UI display check
                            if (itemsResponse.items.isNullOrEmpty() && loadedItems.isNullOrEmpty()) {
                                binding.layoutItemsEmpty.visibility = View.VISIBLE
                                binding.rvShopItems.visibility = View.GONE
                            } else {
                                binding.layoutItemsEmpty.visibility = View.GONE
                                binding.rvShopItems.visibility = View.VISIBLE
                            }

                            loadedItems.addAll(itemsResponse.items ?: emptyList())

                            val hasMore = itemsResponse.currentPage < itemsResponse.lastPage
                            shopAdapter.updatePaginationState(hasMore)
                            shopAdapter.submitList(loadedItems.toList())
                        }

                        is UiState.Error -> {
                            toggleAllItemsLoading(false, isFirstPage = (currentPage == 1))
                            Log.e("ShopFragment", "Failed to load shop items", state.exception)
                            binding.main.showTopSnackbar(
                                getString(R.string.error_load_shop_items, state.exception.message),
                                SnackbarLevel.ERROR
                            )
                        }
                    }
                }
            }
        }
    }

    private fun toggleAllItemsLoading(isLoading: Boolean, isFirstPage: Boolean) {
        if (isFirstPage) {
            toggleItemsAreaLoading(isLoading)
        } else {
            shopAdapter.toggleLoadMoreBtnLoadingState(isLoading)
        }
    }

    private fun toggleItemsAreaLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.rvShopItems.visibility = View.GONE
            binding.layoutItemsEmpty.visibility = View.GONE
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.progressBar.visibility = View.GONE
            binding.rvShopItems.visibility = View.VISIBLE
        }
    }
}