package com.example.upagain.feat.post

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.upagain.R
import com.example.upagain.model.post.PostDetailsResponse
import com.example.upagain.util.datetime.formatTimestamptz

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

    interface OnClickListener {
        fun onPostClick(position: Int, post: PostDetailsResponse)
        fun onLoadMoreClick()
    }

    // Update data safely from the Fragment
    fun updateData(newPosts: List<PostDetailsResponse>, nextPagesAvailable: Boolean) {
        this.postsData = newPosts.toMutableList()
        this.hasMorePages = nextPagesAvailable
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
            holder.category.text = post.category.toString()

            holder.itemView.setOnClickListener {
                onClickListener.onPostClick(position, post)
            }
        } else if (holder is LoadMoreViewHolder) {
            holder.btnLoadMore.setOnClickListener {
                onClickListener.onLoadMoreClick()
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
    }

    class LoadMoreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val btnLoadMore: Button = view.findViewById(R.id.btn_load_more)
    }

    // to know if the last row is a load more or a post
    override fun getItemViewType(position: Int): Int {
        return if (position < postsData.size) TYPE_POST else TYPE_LOAD_MORE
    }
}