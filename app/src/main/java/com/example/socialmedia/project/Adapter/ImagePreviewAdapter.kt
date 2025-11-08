package com.example.socialmedia.project.Adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.ItemImagePreviewBinding

class ImagePreviewAdapter(
    private val items: List<Uri>,
    private val selectedItems: MutableList<Uri>,
    private val onItemClick: (Uri?) -> Unit
) : RecyclerView.Adapter<ImagePreviewAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val binding: ItemImagePreviewBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImagePreviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        // Luôn giữ hình vuông
        parent.post {
            val width = parent.measuredWidth / 3 - 8
            binding.root.layoutParams.height = width
        }

        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        if (position == 0) {
            // Camera / add story
            holder.binding.iconVideo.visibility = View.VISIBLE
            holder.binding.overlaySelected.visibility = View.GONE
            holder.binding.root.setOnClickListener { onItemClick(null) }
        } else {
            val uri = items[position - 1]
            Glide.with(holder.itemView)
                .load(uri)
                .centerCrop()
                .into(holder.binding.imgPreview)

            // Overlay nếu đang chọn
            holder.binding.overlaySelected.visibility =
                if (selectedItems.contains(uri)) View.VISIBLE else View.GONE

            // Icon video
            holder.binding.iconVideo.apply {
                visibility = if (uri.toString().endsWith("mp4")) View.VISIBLE else View.GONE
                setImageResource(R.drawable.clapper)
            }

            holder.binding.root.setOnClickListener {
                val previousSelection = if (selectedItems.isNotEmpty()) selectedItems[0] else null
                selectedItems.clear() // chỉ chọn 1 ảnh
                selectedItems.add(uri)

                // Cập nhật 2 item liên quan
                previousSelection?.let { prev ->
                    val prevIndex = items.indexOf(prev) + 1
                    if (prevIndex >= 0) notifyItemChanged(prevIndex)
                }
                notifyItemChanged(position)

                onItemClick(uri)
            }
        }
    }

    override fun getItemCount(): Int = items.size + 1
}
