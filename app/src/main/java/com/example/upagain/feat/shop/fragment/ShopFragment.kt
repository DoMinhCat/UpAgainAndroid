package com.example.upagain.feat.shop.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.upagain.api.ApiClient
import com.example.upagain.databinding.FragmentShopBinding
import com.example.upagain.feat.shop.adapter.ItemAdapter
import com.example.upagain.model.item.ItemDetailResponse
import com.example.upagain.repository.ItemRepo
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

        loadItems()
    }

    private fun setupRecyclerView() {
        shopAdapter = ItemAdapter(object : ItemAdapter.OnClickListener {
            override fun onItemClick(item: ItemDetailResponse) {
                Toast.makeText(requireContext(), item.title, Toast.LENGTH_SHORT).show()
            }
        })
        binding.rvShopItems.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
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
                    loadItems()
                }
                handler.postDelayed(searchRunnable!!, 300)
            }
        })

        binding.chipGroupMaterials.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            selectedMaterial = if (checkedId != null) {
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
            loadItems()
        }
    }

    private fun loadItems() {
        val options = mutableMapOf<String, String>()
        options["page"] = "1"
        options["limit"] = "100"
        options["status"] = "approved" // approved items are active on shop catalog
        if (searchQuery.isNotEmpty()) {
            options["search"] = searchQuery
        }
        if (selectedMaterial.isNotEmpty()) {
            options["material"] = selectedMaterial
        }
        itemViewModel.getAllItems(options)
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                itemViewModel.allItemsState.collect { state ->
                    when (state) {
                        is UiState.Idle -> {
                            binding.progressBar.visibility = View.GONE
                        }
                        is UiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        is UiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            shopAdapter.submitList(state.data.items)
                        }
                        is UiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            Log.e("ShopFragment", "Failed to load shop items", state.exception)
                            Toast.makeText(
                                requireContext(),
                                "Failed to load shop items: ${state.exception.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }
}