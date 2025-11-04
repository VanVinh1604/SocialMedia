package com.example.socialmedia.project.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.socialmedia.databinding.ItemExploreImageBinding
import com.example.socialmedia.project.Domain.Model.PostMediaModel

class ExploreGridAdapter(
    private val onPostClick: (PostMediaModel) -> Unit
) : ListAdapter<PostMediaModel, ExploreGridAdapter.ImageViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemExploreImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding, onPostClick)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // Tính toán span size cho GridLayoutManager
    fun getSpanSize(position: Int): Int {
        // Mỗi 10 ảnh là 1 nhóm
        val positionInGroup = position % 10

        return when (positionInGroup) {
            0, 1, 2, 3 -> 1  // 4 ảnh nhỏ bên trái (hàng 1-2)
            4 -> 2           // 1 ảnh lớn bên phải (hàng 1-2)
            5 -> 2           // 1 ảnh lớn bên trái (hàng 3-4)
            6, 7, 8, 9 -> 1  // 4 ảnh nhỏ bên phải (hàng 3-4)
            else -> 1
        }
    }

    class ImageViewHolder(
        private val binding: ItemExploreImageBinding,
        private val onPostClick: (PostMediaModel) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(postMedia: PostMediaModel) {
            // Load ảnh với Glide
            Glide.with(binding.root.context)
                .load(postMedia.mediaUrl)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.imageView)

            // Click listener
            binding.root.setOnClickListener {
                onPostClick(postMedia)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PostMediaModel>() {
        override fun areItemsTheSame(oldItem: PostMediaModel, newItem: PostMediaModel): Boolean {
            return oldItem.mediaId == newItem.mediaId
        }

        override fun areContentsTheSame(oldItem: PostMediaModel, newItem: PostMediaModel): Boolean {
            return oldItem == newItem
        }
    }
}