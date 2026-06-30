package com.example.upagain.feat.post.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.upagain.R
import com.example.upagain.model.comment.CommentDetailsResponse
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView

class CommentRecyclerViewAdapter(
    // TODO: pass data
) : ListAdapter<CommentDetailsResponse, CommentRecyclerViewAdapter.CommentViewHolder>(
    CommentDiffCallback()
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ShapeableImageView = view.findViewById(R.id.iv_cmt_avatar)
        val username: TextView = view.findViewById(R.id.tv_cmt_user_name)
        val createdAt: TextView = view.findViewById(R.id.tv_cmt_time)
        val content: TextView = view.findViewById(R.id.tv_cmt_content)
        val likeBtn: MaterialButton = view.findViewById(R.id.btn_cmt_like)
        val likeNb: TextView = view.findViewById(R.id.tv_cmt_like_nb)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = getItem(position)
    }
}

class CommentDiffCallback : DiffUtil.ItemCallback<CommentDetailsResponse>() {
    override fun areItemsTheSame(
        oldItem: CommentDetailsResponse,
        newItem: CommentDetailsResponse
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: CommentDetailsResponse,
        newItem: CommentDetailsResponse
    ): Boolean {
        return oldItem == newItem
    }
}