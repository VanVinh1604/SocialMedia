package com.example.socialmedia.project.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.ItemPostMediaBinding
import com.example.socialmedia.project.Domain.Enum.MediaType
import com.example.socialmedia.project.Domain.Model.PostMediaModel

class MediaAdapter(private val mediaList: List<PostMediaModel>) :
    RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    inner class MediaViewHolder(val binding: ItemPostMediaBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding =
            ItemPostMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val media = mediaList[position]
        val b = holder.binding

        // Hiển thị icon play nếu là video
        b.ivPlayIcon.visibility = if (media.mediaType == MediaType.VIDEO) View.VISIBLE else View.GONE

        // Load ảnh / thumbnail
        Glide.with(b.root.context)
            .load(media.thumbnailUrl ?: media.mediaUrl)
            .placeholder(R.drawable.image_placeholder)
            .error(R.drawable.image_placeholder)
            .into(b.imgMedia)

        // Nếu chỉ 1 media → full width, giữ tỉ lệ
        if (mediaList.size == 1) {
            val params = b.imgMedia.layoutParams
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            b.imgMedia.layoutParams = params
            b.imgMedia.adjustViewBounds = true
        } else {
            // nhiều media → giữ kích thước nhỏ, scroll ngang
            val params = b.imgMedia.layoutParams
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            b.imgMedia.layoutParams = params
        }
    }
    fun updateMedia(newList: List<PostMediaModel>) {
        (mediaList as MutableList).apply {
            clear()
            addAll(newList)
        }
        notifyDataSetChanged()
    }


    override fun getItemCount(): Int = mediaList.size
}
