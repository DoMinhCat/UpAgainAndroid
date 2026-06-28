package com.example.upagain.feat.post

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.upagain.R
import com.example.upagain.model.post.PostDetailsResponse
import com.example.upagain.util.bin.FALL_BACK_IMAGE_URL
import com.example.upagain.util.datetime.formatTimestamptz
import com.example.upagain.util.ui.setOnClickListenerWithCooldown
import com.example.upagain.util.ui.toggleBtnLoadingState
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator

/**
 * RecyclerViewAdapter pour afficher en liste (dans un RecyclerView) des prénoms
 */
class PostRecyclerViewAdapter(
    private var postsData: MutableList<PostDetailsResponse>,
    private var hasMorePages: Boolean,
    private val onClickListener: OnClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // ID for each type of row
    companion object {
        private const val TYPE_POST = 0
        private const val TYPE_LOAD_MORE = 1
    }

    private var isLoadMoreBtnLoading: Boolean = false

    interface OnClickListener {
        fun onPostClick(position: Int, post: PostDetailsResponse)
        fun onLoadMoreClick()
        fun onLikeClick(position: Int, post: PostDetailsResponse)
        fun onSaveClick(position: Int, post: PostDetailsResponse)
    }

    // Update data safely from the Fragment
    fun updateData(newPosts: List<PostDetailsResponse>, nextPagesAvailable: Boolean) {
        this.postsData = newPosts.toMutableList()
        this.hasMorePages = nextPagesAvailable
        this.isLoadMoreBtnLoading = false
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // Load the layout of the row, which is the card of post
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_POST) {
            val view = inflater.inflate(R.layout.item_post_card, parent, false)
            PostViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_post_load_more, parent, false)
            LoadMoreViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // Feed raw data from API to the card
        if (holder is PostViewHolder) {
            val post = postsData[position]
            holder.title.text = post.title
            holder.author.text = post.creator
            holder.date.text = formatTimestamptz(post.createdAt)
            holder.views.text = post.viewCount.toString()
            holder.likes.text = post.likeCount.toString()
            holder.category.text = post.category.toString().replace('_', ' ')

            // Thumbnail image
            val thumbnailUrl = post.photos?.firstOrNull() ?: FALL_BACK_IMAGE_URL
            holder.thumbnailImage.load(thumbnailUrl) {
                crossfade(true)
                placeholder(R.color.color_primary_variant)
                error(R.drawable.fall_back_image)
            }

            // like/saved status
            if (post.likeCount == 0) {
                holder.likeIcon.setImageResource(R.drawable.ic_love_outline)
            }
            if (post.isLiked) {
                holder.likeBtn.icon =
                    holder.likeBtn.context.getDrawable(R.drawable.ic_love_filled)
            }
            if (post.isSaved) {
                holder.saveBtn.icon =
                    holder.saveBtn.context.getDrawable(R.drawable.ic_bookmark_filled)
            }

            // On click listeners
            holder.itemView.setOnClickListener {
                onClickListener.onPostClick(position, post)
            }
            holder.likeBtn.setOnClickListenerWithCooldown(500L) {
                onClickListener.onLikeClick(position, post)
            }
            holder.saveBtn.setOnClickListenerWithCooldown(500L) {
                onClickListener.onSaveClick(position, post)
            }
        } else
            if (holder is LoadMoreViewHolder) {
                val context = holder.btnLoadMore.context
                val defaultText = context.getString(R.string.btn_load_more)
                val defaultIcon = context.getDrawable(R.drawable.ic_chevron_double_down)
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

    override fun getItemCount(): Int {
        return if (hasMorePages) postsData.size + 1 else postsData.size
    }


    /**
     * ViewHolder remember the elements of each row item
     */
    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnailImage: ImageView = view.findViewById(R.id.post_thumbnail)
        val category: Button = view.findViewById(R.id.post_category)
        val title: TextView = view.findViewById(R.id.post_title)
        val author: TextView = view.findViewById(R.id.post_author)
        val date: TextView = view.findViewById(R.id.post_date)
        val views: TextView = view.findViewById(R.id.post_views)
        val likes: TextView = view.findViewById(R.id.post_likes)
        val likeBtn: MaterialButton = view.findViewById(R.id.btn_like)
        val saveBtn: MaterialButton = view.findViewById(R.id.btn_save)
        val likeIcon: ImageView = view.findViewById(R.id.ic_like)
    }

    class LoadMoreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val btnLoadMore: MaterialButton = view.findViewById(R.id.btn_load_more)
        val spinnerIndicator: CircularProgressIndicator = view.findViewById(R.id.load_more_loader)
    }

    // to know if the last row is a load more or a post
    override fun getItemViewType(position: Int): Int {
        return if (position < postsData.size) TYPE_POST else TYPE_LOAD_MORE
    }

    fun toggleLoadMoreBtnLoadingState(isLoading: Boolean) {
        this.isLoadMoreBtnLoading = isLoading
        // If the load more footer is visible, notify the adapter to refresh ONLY that row
        if (hasMorePages) {
            notifyItemChanged(postsData.size)
        }
    }

    // Public method to update only one single card item (like/save)
    fun updateSingleItem(position: Int, updatedPost: PostDetailsResponse) {
        this.postsData[position] = updatedPost
        notifyItemChanged(position)
    }
}