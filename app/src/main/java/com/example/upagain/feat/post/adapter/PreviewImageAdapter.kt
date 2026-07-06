package com.example.upagain.feat.post.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.upagain.databinding.ItemChosenImagePreviewBinding

import coil.load

class PreviewImageAdapter(
    private val onDeleteClick: (Uri) -> Unit
) : ListAdapter<Uri, PreviewImageAdapter.ImageViewHolder>(UriDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemChosenImagePreviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ImageViewHolder(
        private val binding: ItemChosenImagePreviewBinding,
        private val onDeleteClick: (Uri) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(uri: Uri) {
            // Display the image thumbnail using Coil to support both remote and local Uris
            binding.ivPreviewImage.load(uri) {
                crossfade(true)
            }

            // Handle the floating delete button click
            binding.btnRemovePreview.setOnClickListener {
                onDeleteClick(uri)
            }
        }
    }

    companion object {
        private val UriDiffCallback = object : DiffUtil.ItemCallback<Uri>() {
            override fun areItemsTheSame(oldItem: Uri, newItem: Uri): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Uri, newItem: Uri): Boolean {
                return oldItem == newItem
            }
        }
    }
}