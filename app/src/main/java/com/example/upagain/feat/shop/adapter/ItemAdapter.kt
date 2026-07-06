package com.example.upagain.feat.shop.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.upagain.databinding.ItemShopCardBinding
import com.example.upagain.model.item.ItemDetailResponse
import com.example.upagain.util.bin.ImageType
import com.example.upagain.util.bin.buildImageUrl
import com.example.upagain.util.datetime.formatTimestamptz
import java.util.Locale

class ItemAdapter(
    private val onClickListener: OnClickListener
) : ListAdapter<ItemDetailResponse, ItemAdapter.ItemViewHolder>(ItemDiffCallback()) {

    interface OnClickListener {
        fun onItemClick(item: ItemDetailResponse)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemShopCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, onClickListener)
    }

    class ItemViewHolder(
        private val binding: ItemShopCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ItemDetailResponse, listener: OnClickListener) {
            binding.itemTitle.text = item.title
            binding.itemAuthor.text = item.username
            binding.itemDate.text = try {
                formatTimestamptz(item.createdAt)
            } catch (e: Exception) {
                item.createdAt
            }
            binding.itemPrice.text = String.format(Locale.getDefault(), "%.2f €", item.price)

            binding.itemCategory1.text = item.category
            binding.itemCategory2.text = item.material

            val firstImage = item.images.firstOrNull()
            if (firstImage != null) {
                binding.itemImage.load(buildImageUrl(firstImage, ImageType.MEDIA))
            } else {
                binding.itemImage.setImageDrawable(null)
            }

            binding.btnView.setOnClickListener {
                listener.onItemClick(item)
            }
            itemView.setOnClickListener {
                listener.onItemClick(item)
            }
        }
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<ItemDetailResponse>() {
        override fun areItemsTheSame(oldItem: ItemDetailResponse, newItem: ItemDetailResponse): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ItemDetailResponse, newItem: ItemDetailResponse): Boolean {
            return oldItem == newItem
        }
    }
}
