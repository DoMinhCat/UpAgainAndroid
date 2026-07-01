package com.example.upagain.feat.post.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.upagain.R
import com.example.upagain.databinding.ItemCarouselImageBinding
import com.example.upagain.util.bin.ImageType
import com.example.upagain.util.bin.buildImageUrl

class CarouselImageAdapter(private val onImageClick: (String) -> Unit) :
    ListAdapter<String, CarouselImageAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCarouselImageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onImageClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemCarouselImageBinding,
        private val onImageClick: (String) -> Unit
    ) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(imageUrl: String) {
            binding.ivCarouselItem.load(buildImageUrl(imageUrl, ImageType.MEDIA)) {
                crossfade(true)
                placeholder(R.drawable.fall_back_image)
                error(R.drawable.fall_back_image)
            }
            val resolvedUrl = buildImageUrl(imageUrl, ImageType.MEDIA)
            binding.root.setOnClickListener {
                onImageClick(resolvedUrl)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
            oldItem == newItem
    }
}