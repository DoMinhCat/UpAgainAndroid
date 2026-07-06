package com.example.upagain.feat.shop.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.upagain.R
import com.example.upagain.databinding.ItemShopCardBinding
import com.example.upagain.model.item.ItemDetailResponse
import com.example.upagain.util.bin.ImageType
import com.example.upagain.util.bin.buildImageUrl
import com.example.upagain.util.datetime.formatTimestamptz
import com.example.upagain.util.ui.getItemMaterialColor
import com.example.upagain.util.ui.toggleBtnLoadingState
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.util.Locale

class ItemAdapter(
    private val onClickListener: OnClickListener
) : ListAdapter<ItemDetailResponse, RecyclerView.ViewHolder>(ItemDiffCallback()) {

    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_LOAD_MORE = 1
    }

    private var hasMorePages: Boolean = false
    private var isLoadMoreBtnLoading: Boolean = false

    interface OnClickListener {
        fun onItemClick(item: ItemDetailResponse)
        fun onLoadMoreClick()
    }

    fun updatePaginationState(nextPagesAvailable: Boolean) {
        val previousHasMore = this.hasMorePages
        this.hasMorePages = nextPagesAvailable
        this.isLoadMoreBtnLoading = false

        if (previousHasMore != nextPagesAvailable) {
            if (nextPagesAvailable) {
                notifyItemInserted(super.getItemCount())
            } else {
                notifyItemRemoved(super.getItemCount())
            }
        }
    }

    fun toggleLoadMoreBtnLoadingState(isLoading: Boolean) {
        this.isLoadMoreBtnLoading = isLoading
        if (hasMorePages) {
            notifyItemChanged(super.getItemCount())
        }
    }

    override fun getItemCount(): Int {
        return if (hasMorePages) super.getItemCount() + 1 else super.getItemCount()
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < super.getItemCount()) TYPE_ITEM else TYPE_LOAD_MORE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_ITEM) {
            val binding = ItemShopCardBinding.inflate(inflater, parent, false)
            ItemViewHolder(binding)
        } else {
            val view = inflater.inflate(R.layout.item_load_more, parent, false)
            LoadMoreViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder) {
            val item = getItem(position)
            holder.bind(item, onClickListener)
        } else if (holder is LoadMoreViewHolder) {
            val context = holder.btnLoadMore.context
            val defaultText = context.getString(R.string.btn_load_more)
            val defaultIcon =
                AppCompatResources.getDrawable(context, R.drawable.ic_chevron_double_down)

            toggleBtnLoadingState(
                holder.btnLoadMore,
                holder.spinnerIndicator,
                isLoadMoreBtnLoading,
                defaultText,
                defaultIcon
            )

            holder.btnLoadMore.setOnClickListener {
                onClickListener.onLoadMoreClick()
            }
        }
    }

    class ItemViewHolder(
        private val binding: ItemShopCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ItemDetailResponse, listener: OnClickListener) {
            val context = binding.root.context

            binding.itemTitle.text = item.title
            binding.itemAuthor.text = item.username
            binding.itemDate.text = try {
                formatTimestamptz(item.createdAt)
            } catch (e: Exception) {
                item.createdAt
            }
            binding.itemPrice.text = String.format(Locale.getDefault(), "%.2f €", item.price)

            // Capitalize category
            binding.itemCategory1.text = item.category.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
            }

            // Material Text & Dynamic Background Color
            binding.itemCategory2.text = item.material.uppercase(Locale.getDefault())
            val materialColorRes = getItemMaterialColor(item.material)
            binding.itemCategory2.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, materialColorRes)
            )

            // Condition Translation
            val conditionRes = when (item.state.lowercase(Locale.ROOT)) {
                "brand_new" -> R.string.state_brand_new
                "very_good" -> R.string.state_very_good
                "good" -> R.string.state_good
                "fair" -> R.string.state_fair
                "poor" -> R.string.state_poor
                else -> null
            }
            val conditionText =
                if (conditionRes != null) context.getString(conditionRes) else item.state
            binding.itemState.text = context.getString(R.string.label_condition, conditionText)

            // Status Translation
            val statusRes = when (item.status.lowercase(Locale.ROOT)) {
                "pending" -> R.string.status_pending
                "approved" -> R.string.status_approved
                "refused" -> R.string.status_refused
                else -> null
            }
            val statusText = if (statusRes != null) context.getString(statusRes) else item.status
            binding.itemStatus.text = context.getString(R.string.label_status, statusText)

            val firstImage = item.images.firstOrNull()
            if (firstImage != null) {
                binding.itemImage.load(buildImageUrl(firstImage, ImageType.MEDIA)) {
                    crossfade(true)
                    placeholder(R.color.color_surface)
                    error(R.drawable.fall_back_image)
                }
            } else {
                binding.itemImage.setImageDrawable(null)
            }

            itemView.setOnClickListener {
                listener.onItemClick(item)
            }
        }
    }

    class LoadMoreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val btnLoadMore: MaterialButton = view.findViewById(R.id.btn_load_more)
        val spinnerIndicator: CircularProgressIndicator = view.findViewById(R.id.load_more_loader)
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<ItemDetailResponse>() {
        override fun areItemsTheSame(
            oldItem: ItemDetailResponse,
            newItem: ItemDetailResponse
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: ItemDetailResponse,
            newItem: ItemDetailResponse
        ): Boolean {
            return oldItem == newItem
        }
    }
}
