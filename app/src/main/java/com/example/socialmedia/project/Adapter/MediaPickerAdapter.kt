package com.example.socialmedia.project.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.project.Domain.Enum.MediaType
import com.example.socialmedia.project.Domain.Model.MediaItem

class MediaPickerAdapter(
    private var mediaItems: List<MediaItem>,
    private val selectedItems: MutableList<MediaItem>,
    private var maxSelection: Int,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<MediaPickerAdapter.MediaViewHolder>() {

    inner class MediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgMedia: ImageView = view.findViewById(R.id.imgMedia)
        val imgPlayIcon: ImageView = view.findViewById(R.id.imgPlayIcon)
        val tvDuration: TextView = view.findViewById(R.id.tvDuration)
        val imgCheckbox: ImageView = view.findViewById(R.id.imgCheckbox)
        val tvSelectionOrder: TextView = view.findViewById(R.id.tvSelectionOrder)
        val overlaySelected: View = view.findViewById(R.id.overlaySelected)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media_picker, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val mediaItem = mediaItems[position]
        val isSelected = selectedItems.contains(mediaItem)
        val selectionOrder = selectedItems.indexOf(mediaItem) + 1

        // Load thumbnail
        Glide.with(holder.itemView.context)
            .load(mediaItem.uri)
            .centerCrop()
            .into(holder.imgMedia)

        // Show video duration and play icon
        if (mediaItem.type == MediaType.VIDEO) {
            holder.imgPlayIcon.visibility = View.VISIBLE
            holder.tvDuration.visibility = View.VISIBLE
            holder.tvDuration.text = formatDuration(mediaItem.duration ?: 0)
        } else {
            holder.imgPlayIcon.visibility = View.GONE
            holder.tvDuration.visibility = View.GONE
        }

        // Update selection UI
        if (isSelected) {
            holder.overlaySelected.visibility = View.VISIBLE
            holder.imgCheckbox.visibility = View.GONE
            holder.tvSelectionOrder.visibility = View.VISIBLE
            holder.tvSelectionOrder.text = selectionOrder.toString()
        } else {
            holder.overlaySelected.visibility = View.GONE
            holder.imgCheckbox.visibility = View.VISIBLE
            holder.tvSelectionOrder.visibility = View.GONE
        }

        // Handle click
        holder.itemView.setOnClickListener {
            if (isSelected) {
                selectedItems.remove(mediaItem)
            } else {
                if (selectedItems.size >= maxSelection) {
                    android.widget.Toast.makeText(
                        holder.itemView.context,
                        "Chỉ được chọn tối đa $maxSelection media",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                selectedItems.add(mediaItem)
            }
            notifyItemChanged(position)
            // Update other selected items to refresh order numbers
            selectedItems.forEach { item ->
                val idx = mediaItems.indexOf(item)
                if (idx != -1 && idx != position) {
                    notifyItemChanged(idx)
                }
            }
            onSelectionChanged()
        }
    }

    override fun getItemCount() = mediaItems.size

    fun updateMediaList(newList: List<MediaItem>) {
        mediaItems = newList
        selectedItems.clear()
        notifyDataSetChanged()
        onSelectionChanged()
    }

    fun setMaxSelection(max: Int) {
        maxSelection = max
        if (selectedItems.size > max) {
            while (selectedItems.size > max) {
                selectedItems.removeAt(selectedItems.size - 1)
            }
            notifyDataSetChanged()
            onSelectionChanged()
        }
    }

    private fun formatDuration(millis: Long): String {
        val seconds = (millis / 1000).toInt()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }
}