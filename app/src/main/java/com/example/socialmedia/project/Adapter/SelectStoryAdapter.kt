package com.example.socialmedia.project.Adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.ItemSelectStoryBinding
import com.example.socialmedia.project.Domain.Model.StoryModel

class SelectStoryAdapter(
    private var stories: List<StoryModel>,
    private val onSelectionChanged: (Int) -> Unit // Trả về số lượng đã chọn
) : RecyclerView.Adapter<SelectStoryAdapter.SelectViewHolder>() {

    // Danh sách các Story ID đã được chọn
    val selectedStoryIds = mutableListOf<String>()

    // Lưu URL ảnh đầu tiên được chọn để làm ảnh bìa
    var firstSelectedImageUrl: String = ""

    inner class SelectViewHolder(val binding: ItemSelectStoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectViewHolder {
        val binding = ItemSelectStoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SelectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SelectViewHolder, position: Int) {
        val story = stories[position]

        // Load ảnh (Dùng mediaUrl hoặc thumbnailUrl)
        val url = story.mediaUrl // Hoặc thumbnailUrl nếu có
        Glide.with(holder.itemView.context).load(url).centerCrop().into(holder.binding.ivStoryThumb)

        // Kiểm tra xem có đang được chọn không
        val isSelected = selectedStoryIds.contains(story.storyId)

        if (isSelected) {
            holder.binding.viewOverlay.visibility = android.view.View.VISIBLE
            holder.binding.ivCheckMark.setBackgroundResource(R.drawable.btn_gradient) // Nếu có drawable gradient
            holder.binding.ivCheckMark.setColorFilter(Color.WHITE)
        } else {
            holder.binding.viewOverlay.visibility = android.view.View.GONE
            holder.binding.ivCheckMark.setBackgroundResource(R.drawable.circle_border_gray) // Tròn xám
            holder.binding.ivCheckMark.setColorFilter(Color.TRANSPARENT)
        }

        // Sự kiện Click
        holder.itemView.setOnClickListener {
            if (isSelected) {
                selectedStoryIds.remove(story.storyId)
            } else {
                selectedStoryIds.add(story.storyId)
                if (selectedStoryIds.size == 1) {
                    firstSelectedImageUrl = url
                }
            }
            notifyItemChanged(position)
            onSelectionChanged(selectedStoryIds.size)
        }
    }

    override fun getItemCount(): Int = stories.size
}