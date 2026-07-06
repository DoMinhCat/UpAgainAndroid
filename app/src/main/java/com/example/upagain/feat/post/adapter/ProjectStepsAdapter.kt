package com.example.upagain.feat.post.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.upagain.databinding.ItemProjectStepBinding
import com.example.upagain.model.post.ProjectStepResponse
import com.example.upagain.util.datetime.formatTimestamptz
import com.google.android.material.tabs.TabLayoutMediator

class ProjectStepsAdapter(
    private val onStepImageClick: (String) -> Unit,
    private val listener: OnStepClickListener
) : ListAdapter<ProjectStepResponse, ProjectStepsAdapter.StepViewHolder>(DiffCallback) {

    var isEditable: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged() // Notifies the layout to update its edit items cleanly
            }
        }

    interface OnStepClickListener {
        fun onEditClick(step: ProjectStepResponse)
        fun onDeleteClick(step: ProjectStepResponse)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val binding = ItemProjectStepBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return StepViewHolder(binding, onStepImageClick, listener)
    }

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        holder.bind(getItem(position), isEditable)
    }

    class StepViewHolder(
        private val binding: ItemProjectStepBinding,
        private val onStepImageClick: (String) -> Unit,
        private val listener: OnStepClickListener
    ) : RecyclerView.ViewHolder(binding.root) {

        // Instantiated once per ViewHolder view creation block to optimize memory metrics
        private val stepCarouselAdapter = CarouselImageAdapter { url ->
            onStepImageClick(url) // Safely link item clicks back up the tree layout matrix!
        }
        private var isMediatorAttached = false

        init {
            // Bind the adapter structure early
            binding.vpStepCarousel.adapter = stepCarouselAdapter
        }

        fun bind(step: ProjectStepResponse, isEditable: Boolean) {
            // 1. Text Data Bindings
            binding.tvStepTitle.text = step.title
            binding.tvStepDescription.text = HtmlCompat.fromHtml(
                step.description,
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            binding.tvStepDate.text = formatTimestamptz(step.createdAt)

            if (isEditable) {
                binding.btnStepEdit.visibility = View.VISIBLE
                binding.btnStepDelete.visibility = View.VISIBLE

                // Assign listeners only if the elements are interactive
                binding.btnStepEdit.setOnClickListener { listener.onEditClick(step) }
                binding.btnStepDelete.setOnClickListener { listener.onDeleteClick(step) }
            } else {
                binding.btnStepEdit.visibility = View.GONE
                binding.btnStepDelete.visibility = View.GONE

                // Clear out listeners on detached non-interactive rows
                binding.btnStepEdit.setOnClickListener(null)
                binding.btnStepDelete.setOnClickListener(null)
            }

            // 2. Format Items Required Bullet List
            val materialsList = step.items.orEmpty()
            if (materialsList.isEmpty()) {
                binding.tvLabelMaterials.visibility = View.GONE
                binding.tvStepMaterialsList.visibility = View.GONE
            } else {
                binding.tvLabelMaterials.visibility = View.VISIBLE
                binding.tvStepMaterialsList.visibility = View.VISIBLE
                binding.tvStepMaterialsList.text =
                    materialsList.joinToString("\n") { "• ${it.title}" }
            }

            // 3. Handle Nested Image Carousel Layout States
            val stepImages = step.photos.orEmpty()
            if (stepImages.isEmpty()) {
                binding.layoutStepCarouselWrapper.visibility = View.GONE
            } else {
                binding.layoutStepCarouselWrapper.visibility = View.VISIBLE
                stepCarouselAdapter.submitList(stepImages)

                // Manage and update layout indicator dots links
                if (stepImages.size > 1) {
                    binding.tlStepCarouselIndicator.visibility = View.VISIBLE
                    if (!isMediatorAttached) {
                        TabLayoutMediator(
                            binding.tlStepCarouselIndicator,
                            binding.vpStepCarousel
                        ) { _, _ -> }.attach()
                        isMediatorAttached = true
                    }
                } else {
                    binding.tlStepCarouselIndicator.visibility = View.GONE
                }
            }

            // 4. Command Button Action Interceptors
            binding.btnStepEdit.setOnClickListener { listener.onEditClick(step) }
            binding.btnStepDelete.setOnClickListener { listener.onDeleteClick(step) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ProjectStepResponse>() {
        override fun areItemsTheSame(
            oldItem: ProjectStepResponse,
            newItem: ProjectStepResponse
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: ProjectStepResponse,
            newItem: ProjectStepResponse
        ): Boolean {
            return oldItem == newItem
        }
    }
}