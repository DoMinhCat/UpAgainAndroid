package com.example.upagain.feat.post.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.upagain.R
import com.example.upagain.model.comment.CommentDetailsResponse
import com.example.upagain.util.bin.ImageType
import com.example.upagain.util.bin.buildImageUrl
import com.example.upagain.util.datetime.formatTimestamptz
import com.example.upagain.util.ui.toggleBtnLoadingState
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.CircularProgressIndicator

class CommentRecyclerViewAdapter(
    private var hasMorePages: Boolean,
    private val onClickListener: OnClickListener
) : ListAdapter<CommentDetailsResponse, RecyclerView.ViewHolder>(
    CommentDiffCallback()
) {

    companion object {
        private const val TYPE_COMMENT = 0
        private const val TYPE_LOAD_MORE = 1
    }

    private var isLoadMoreBtnLoading: Boolean = false

    interface OnClickListener {
        fun onLoadMoreClick()
    }

    override fun getItemViewType(position: Int): Int {
        return if (hasMorePages && position == currentList.size) {
            TYPE_LOAD_MORE
        } else {
            TYPE_COMMENT
        }
    }

    override fun getItemCount(): Int {
        val baseCount = currentList.size
        return if (hasMorePages) baseCount + 1 else baseCount
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_COMMENT) {
            val view = inflater.inflate(R.layout.item_comment, parent, false)
            CommentViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_load_more, parent, false)
            LoadMoreViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is CommentViewHolder) {
            if (position < currentList.size) {
                val comment = getItem(position)

                holder.username.text = comment.username
                holder.content.text = comment.content
                holder.createdAt.text = formatTimestamptz(comment.createdAt)
                holder.likeNb.text = comment.likeCount.toString()
                val avatarUrl = buildImageUrl(comment.userAvatar, ImageType.AVATAR)
                holder.avatar.load(avatarUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_avatar_unknown)
                    error(R.drawable.ic_avatar_unknown)
                }
            }
        } else if (holder is LoadMoreViewHolder) {
            val context = holder.btnLoadMore.context
            val defaultText = context.getString(R.string.btn_load_more)
            val defaultIcon = AppCompatResources.getDrawable(context, R.drawable.ic_chevron_double_down)

            if (isLoadMoreBtnLoading) {
                toggleBtnLoadingState(
                    holder.btnLoadMore,
                    holder.spinnerIndicator,
                    true,
                    defaultText,
                    defaultIcon
                )
            } else {
                toggleBtnLoadingState(
                    holder.btnLoadMore,
                    holder.spinnerIndicator,
                    false,
                    defaultText,
                    defaultIcon
                )
                holder.btnLoadMore.setOnClickListener {
                    onClickListener.onLoadMoreClick()
                }
            }
        }
    }

    fun toggleLoadMoreBtnLoadingState(isLoading: Boolean) {
        this.isLoadMoreBtnLoading = isLoading
        if (hasMorePages) {
            notifyItemChanged(currentList.size)
        }
    }

    fun updatePaginationStatus(hasMore: Boolean) {
        if (this.hasMorePages == hasMore) return

        this.hasMorePages = hasMore
        if (!hasMore) {
            // remove Load more btn
            notifyItemRemoved(currentList.size)
        } else {
            notifyItemInserted(currentList.size)
        }
    }

    class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ShapeableImageView = view.findViewById(R.id.iv_cmt_avatar)
        val username: TextView = view.findViewById(R.id.tv_cmt_user_name)
        val createdAt: TextView = view.findViewById(R.id.tv_cmt_time)
        val content: TextView = view.findViewById(R.id.tv_cmt_content)
        val likeBtn: MaterialButton = view.findViewById(R.id.btn_cmt_like)
        val likeNb: TextView = view.findViewById(R.id.tv_cmt_like_nb)
    }

    class LoadMoreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val btnLoadMore: MaterialButton = view.findViewById(R.id.btn_load_more)
        val spinnerIndicator: CircularProgressIndicator = view.findViewById(R.id.load_more_loader)
    }
}

class CommentDiffCallback : DiffUtil.ItemCallback<CommentDetailsResponse>() {
    override fun areItemsTheSame(
        oldItem: CommentDetailsResponse,
        newItem: CommentDetailsResponse
    ): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: CommentDetailsResponse,
        newItem: CommentDetailsResponse
    ): Boolean = oldItem == newItem
}