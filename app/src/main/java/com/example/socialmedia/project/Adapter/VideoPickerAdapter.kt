package com.example.socialmedia.project.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.project.Domain.Model.MediaItem

class VideoPickerAdapter(
    private val videos: MutableList<MediaItem>,
    private val selectedVideos: MutableList<MediaItem>,
    private val maxSelection: Int,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<VideoPickerAdapter.VideoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video_picker, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(videos[position])
    }

    override fun getItemCount() = videos.size

    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val videoThumbnail: ImageView = itemView.findViewById(R.id.videoThumbnail)
        private val checkOverlay: View = itemView.findViewById(R.id.checkOverlay)
        private val checkIcon: ImageView = itemView.findViewById(R.id.checkIcon)
        private val durationText: TextView = itemView.findViewById(R.id.durationText)
        private val playIcon: ImageView = itemView.findViewById(R.id.playIcon)

        fun bind(mediaItem: MediaItem) {
            // Load video thumbnail
            Glide.with(itemView.context)
                .load(mediaItem.uri)
                .centerCrop()
                .placeholder(R.drawable.ic_video_placeholder)
                .into(videoThumbnail)

            // Show play icon
            playIcon.visibility = View.VISIBLE

            // Format and show duration
            val seconds = (mediaItem.duration ?: 0) / 1000
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            durationText.text = String.format("%d:%02d", minutes, remainingSeconds)

            // Check if selected
            val isSelected = selectedVideos.any { it.uri == mediaItem.uri }
            checkOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE
            checkIcon.visibility = if (isSelected) View.VISIBLE else View.GONE

            // Handle click
            itemView.setOnClickListener {
                if (isSelected) {
                    // Deselect
                    selectedVideos.removeAll { it.uri == mediaItem.uri }
                    notifyItemChanged(adapterPosition)
                    onSelectionChanged()
                } else {
                    // Select if not exceeded max
                    if (selectedVideos.size < maxSelection) {
                        selectedVideos.add(mediaItem)
                        notifyItemChanged(adapterPosition)
                        onSelectionChanged()
                    } else {
                        android.widget.Toast.makeText(
                            itemView.context,
                            "Chỉ được chọn tối đa $maxSelection video",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}