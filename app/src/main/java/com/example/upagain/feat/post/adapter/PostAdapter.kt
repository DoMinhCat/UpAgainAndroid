package com.example.upagain.feat.post.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.upagain.R
import com.example.upagain.model.post.PostDetailsResponse
import com.example.upagain.util.bin.ImageType
import com.example.upagain.util.bin.buildImageUrl
import com.example.upagain.util.datetime.compareNowWithTimestamp
import com.example.upagain.util.datetime.formatTimestamptz
import com.example.upagain.util.ui.setOnClickListenerWithCooldown
import com.example.upagain.util.ui.setPostCategoryTextAndColor
import com.example.upagain.util.ui.toggleBtnLoadingState
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator

class PostAdapter(
    private val onClickListener: OnClickListener
) : ListAdapter<PostDetailsResponse, RecyclerView.ViewHolder>(PostDiffCallback()) {

    companion object {
        private const val TYPE_POST = 0
        private const val TYPE_LOAD_MORE = 1
    }

    private var hasMorePages: Boolean = false
    private var isLoadMoreBtnLoading: Boolean = false

    interface OnClickListener {
        fun onPostClick(position: Int, post: PostDetailsResponse)
        fun onLoadMoreClick()
        fun onLikeClick(position: Int, post: PostDetailsResponse)
        fun onSaveClick(position: Int, post: PostDetailsResponse)
    }

    /**
     * Updates the layout setup for pagination state variables.
     * Call this right before or after calling [submitList].
     */
    fun updatePaginationState(nextPagesAvailable: Boolean) {
        val previousHasMore = this.hasMorePages
        this.hasMorePages = nextPagesAvailable
        this.isLoadMoreBtnLoading = false

        // Notify changes to structural elements safely
        if (previousHasMore != nextPagesAvailable) {
            if (nextPagesAvailable) {
                notifyItemInserted(super.getItemCount()) // Adds load more view row
            } else {
                notifyItemRemoved(super.getItemCount()) // Removes load more view row
            }
        }
    }

    fun toggleLoadMoreBtnLoadingState(isLoading: Boolean) {
        this.isLoadMoreBtnLoading = isLoading
        if (hasMorePages) {
            notifyItemChanged(super.getItemCount()) // Refreshes footer row explicitly
        }
    }

    override fun getItemCount(): Int {
        return if (hasMorePages) super.getItemCount() + 1 else super.getItemCount()
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < super.getItemCount()) TYPE_POST else TYPE_LOAD_MORE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_POST) {
            val view = inflater.inflate(R.layout.item_post_card, parent, false)
            PostViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_load_more, parent, false)
            LoadMoreViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PostViewHolder) {
            val post = getItem(position)

            holder.title.text = post.title
            holder.author.text = post.creator
            holder.date.text = formatTimestamptz(post.createdAt)
            holder.views.text = post.viewCount.toString()
            holder.likes.text = post.likeCount.toString()
            holder.category.setPostCategoryTextAndColor(
                holder.category.context,
                post.category.toString()
            )

            holder.sponsorStatus.visibility =
                if (post.adsId != null && (!post.adsTo.isNullOrEmpty() && compareNowWithTimestamp(
                        post.adsTo
                    ) < 0) && (!post.adsFrom.isNullOrEmpty() && compareNowWithTimestamp(
                        post.adsFrom
                    ) > 0)
                ) {
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }

            // Thumbnail image
            val thumbnailUrl = buildImageUrl(post.photos?.firstOrNull(), ImageType.MEDIA)
            holder.thumbnailImage.load(thumbnailUrl) {
                crossfade(true)
                placeholder(R.color.color_surface)
                error(R.drawable.fall_back_image)
            }

            // Default fallback design adjustments
            holder.likeIcon.setImageResource(
                if (post.likeCount == 0) R.drawable.ic_love_outline else R.drawable.ic_love_filled
            )

            holder.likeBtn.icon = AppCompatResources.getDrawable(
                holder.likeBtn.context,
                if (post.isLiked) R.drawable.ic_love_filled else R.drawable.ic_love_outline
            )

            holder.saveBtn.icon = AppCompatResources.getDrawable(
                holder.saveBtn.context,
                if (post.isSaved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_outline
            )

            // Dynamic Action Listeners avoiding stale indexing
            holder.itemView.setOnClickListener {
                val currentPos = holder.bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    onClickListener.onPostClick(currentPos, getItem(currentPos))
                }
            }

            holder.likeBtn.setOnClickListenerWithCooldown(500L) {
                val currentPos = holder.bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    onClickListener.onLikeClick(currentPos, getItem(currentPos))
                }
            }

            holder.saveBtn.setOnClickListenerWithCooldown(500L) {
                val currentPos = holder.bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    onClickListener.onSaveClick(currentPos, getItem(currentPos))
                }
            }
        } else if (holder is LoadMoreViewHolder) {
            val context = holder.btnLoadMore.context
            val defaultText = context.getString(R.string.btn_load_more)
            val defaultIcon =
                AppCompatResources.getDrawable(context, R.drawable.ic_chevron_double_down)

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

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnailImage: ImageView = view.findViewById(R.id.post_thumbnail)
        val category: MaterialButton = view.findViewById(R.id.post_category)
        val title: TextView = view.findViewById(R.id.post_title)
        val author: TextView = view.findViewById(R.id.post_author)
        val date: TextView = view.findViewById(R.id.post_date)
        val views: TextView = view.findViewById(R.id.post_views)
        val likes: TextView = view.findViewById(R.id.post_likes)
        val likeBtn: MaterialButton = view.findViewById(R.id.btn_like)
        val saveBtn: MaterialButton = view.findViewById(R.id.btn_save)
        val likeIcon: ImageView = view.findViewById(R.id.ic_like)
        val sponsorStatus: TextView = view.findViewById(R.id.tv_sponsored)
    }

    class LoadMoreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val btnLoadMore: MaterialButton = view.findViewById(R.id.btn_load_more)
        val spinnerIndicator: CircularProgressIndicator = view.findViewById(R.id.load_more_loader)
    }

    // DiffUtil Class to handle granular item animations automatically
    class PostDiffCallback : DiffUtil.ItemCallback<PostDetailsResponse>() {
        override fun areItemsTheSame(
            oldItem: PostDetailsResponse,
            newItem: PostDetailsResponse
        ): Boolean {
            return oldItem.id == newItem.id // or whatever unique ID property it contains
        }

        override fun areContentsTheSame(
            oldItem: PostDetailsResponse,
            newItem: PostDetailsResponse
        ): Boolean {
            return oldItem == newItem // Data class comparison
        }
    }
}