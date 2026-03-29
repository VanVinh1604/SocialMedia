package com.example.socialmedia.project.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.ItemPostMediaBinding
import com.example.socialmedia.project.Domain.Enum.MediaType
import com.example.socialmedia.project.Domain.Model.PostMediaModel

class MediaViewPagerAdapter :
    ListAdapter<PostMediaModel, MediaViewPagerAdapter.MediaViewHolder>(MediaDiffCallback()) {

    inner class MediaViewHolder(val binding: ItemPostMediaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(media: PostMediaModel) {
            val b = binding

            // Hiển thị icon play nếu là video
            b.ivPlayIcon.visibility = if (media.mediaType == MediaType.VIDEO) View.VISIBLE else View.GONE

            // Load ảnh / thumbnail
            Glide.with(b.root.context)
                .load(media.thumbnailUrl ?: media.mediaUrl)
                .placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_placeholder)
                .into(b.imgMedia) // Sửa lỗi: dùng ID 'imgMedia' của bạn
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding =
            ItemPostMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val media = getItem(position)
        holder.bind(media)
    }
}

class MediaDiffCallback : DiffUtil.ItemCallback<PostMediaModel>() {
    override fun areItemsTheSame(oldItem: PostMediaModel, newItem: PostMediaModel): Boolean {
        return oldItem.mediaId == newItem.mediaId
    }

    override fun areContentsTheSame(oldItem: PostMediaModel, newItem: PostMediaModel): Boolean {
        return oldItem == newItem
    }
}