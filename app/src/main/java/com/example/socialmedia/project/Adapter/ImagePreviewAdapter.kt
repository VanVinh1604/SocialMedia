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
    private val onItemClick: (Uri?) -> Unit // Uri? = null => mở camera
) : RecyclerView.Adapter<ImagePreviewAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val binding: ItemImagePreviewBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImagePreviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        if (position == 0) {
            holder.binding.imgPreview.setImageResource(0)
            holder.binding.iconVideo.visibility = View.VISIBLE
            holder.binding.root.setOnClickListener { onItemClick(null) }
        } else {
            val uri = items[position - 1] // trừ 1 vì ô đầu tiên là camera
            Glide.with(holder.itemView)
                .load(uri)
                .centerCrop()
                .into(holder.binding.imgPreview)

            holder.binding.overlaySelected.visibility = if (selectedItems.contains(uri)) View.VISIBLE else View.GONE
            holder.binding.iconVideo.visibility = if (uri.toString().endsWith("mp4")) View.VISIBLE else View.GONE

            holder.binding.root.setOnClickListener {
                if (selectedItems.contains(uri)) selectedItems.remove(uri)
                else selectedItems.add(uri)
                onItemClick(uri)
                notifyItemChanged(position)
            }
        }
    }

    override fun getItemCount(): Int = items.size + 1
}
