package com.example.socialmedia.project.Adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.ItemStoryHighlightBinding
import com.example.socialmedia.project.Domain.Model.StoryHighlightModel

class StoryHighlightAdapter(
    private var highlights: List<StoryHighlightModel>,
    private val onClick: (StoryHighlightModel) -> Unit
) : RecyclerView.Adapter<StoryHighlightAdapter.HighlightViewHolder>() {

    inner class HighlightViewHolder(val binding: ItemStoryHighlightBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HighlightViewHolder {
        val binding = ItemStoryHighlightBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HighlightViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HighlightViewHolder, position: Int) {
        val item = highlights[position]

        // === 1. LOGIC HIỂN THỊ ===
        if (item.id == "ADD_NEW") {
            holder.binding.tvHighlightName.text = "Mới"
            holder.binding.ivHighlightCover.setImageResource(R.drawable.ic_add)
            holder.binding.ivHighlightCover.setPadding(15,15,15,15)
        } else {
            holder.binding.tvHighlightName.text = item.name
            holder.binding.ivHighlightCover.setPadding(3,3,3,3)

            Glide.with(holder.itemView.context)
                .load(item.coverUrl)
                .placeholder(R.drawable.image_avata_user)
                .centerCrop()
                .into(holder.binding.ivHighlightCover)
        }

        // === 2. LOGIC CLICK (ĐÃ SỬA LỖI ID) ===
        holder.itemView.setOnClickListener { view ->
            if (item.id == "ADD_NEW") {
                onClick(item)
            } else {
                // Tạo Bundle chứa ID
                val bundle = Bundle()
                bundle.putString("highlightId", item.id)
                bundle.putString("userId", item.userId)

                // --- PHẦN SỬA LỖI ---
                // Thử điều hướng bằng action của ProfileFragment
                try {
                    view.findNavController().navigate(
                        R.id.action_profileFragment_to_highlightViewerFragment,
                        bundle
                    )
                } catch (e: Exception) {
                    // Nếu lỗi (nghĩa là đang ở PersonalProfileFragment), thử action còn lại
                    try {
                        view.findNavController().navigate(
                            R.id.action_personalProfileFragment_to_highlightViewerFragment,
                            bundle
                        )
                    } catch (e2: Exception) {
                        // Nếu vẫn lỗi thì báo Toast để kiểm tra lại nav_graph
                        Toast.makeText(view.context, "Lỗi điều hướng: Không tìm thấy Action ID phù hợp", Toast.LENGTH_SHORT).show()
                        e2.printStackTrace()
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = highlights.size

    fun submitList(newList: List<StoryHighlightModel>) {
        highlights = newList
        notifyDataSetChanged()
    }
}