package com.example.socialmedia.project.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.ItemProfilePostBinding
import com.example.socialmedia.project.Domain.Model.PostModel

class ProfilePostAdapter(
    private val onPostClick: (PostModel) -> Unit
) : ListAdapter<PostModel, ProfilePostAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemProfilePostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(private val binding: ItemProfilePostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: PostModel) {
            // 1. Load Ảnh
            val imageUrl = if (post.isReel) {
                post.thumbnail
            } else {
                post.mediaList.firstOrNull()?.mediaUrl
            }

            Glide.with(binding.root.context)
                .load(imageUrl)
                .placeholder(android.R.color.darker_gray)
                .error(android.R.color.darker_gray)
                .centerCrop()
                .into(binding.ivPostThumbnail)

            // 2. Hiện Icon (Reels hoặc Nhiều ảnh)
            if (post.isReel) {
                binding.ivTypeIcon.visibility = View.VISIBLE
                binding.ivTypeIcon.setImageResource(R.drawable.ic_reels)
            } else {
                binding.ivTypeIcon.visibility = View.GONE
            }

            // [ĐÃ XÓA] Code set text cho tvLikeCount

            // 3. Click sự kiện
            binding.root.setOnClickListener {
                onPostClick(post)
            }
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<PostModel>() {
        override fun areItemsTheSame(oldItem: PostModel, newItem: PostModel): Boolean {
            return oldItem.postId == newItem.postId
        }
        override fun areContentsTheSame(oldItem: PostModel, newItem: PostModel): Boolean {
            return oldItem == newItem
        }
    }
}