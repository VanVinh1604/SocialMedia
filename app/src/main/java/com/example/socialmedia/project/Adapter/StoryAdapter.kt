package com.example.socialmedia.project.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmedia.R
import com.example.socialmedia.project.Domain.StoryTest
import de.hdodenhof.circleimageview.CircleImageView

class StoryAdapter(private val stories: List<StoryTest>) :
    RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    inner class StoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgStory: ImageView? = view.findViewById(R.id.imgStory)
        val imgAvatar: CircleImageView? = view.findViewById(R.id.imgAvatar)
        val btnAdd: ImageView? = view.findViewById(R.id.btnStory) // dùng cho layout "tạo tin"
    }

    // 🔹 Phân biệt 2 loại item: 0 = Add story, 1 = story bình thường
    override fun getItemViewType(position: Int): Int {
        return if (stories[position].isAddStory) 0 else 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val layout = if (viewType == 0)
            R.layout.item_add_story      // layout cho "Tạo tin"
        else
            R.layout.item_story   // layout cho story bình thường

        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val story = stories[position]

        if (story.isAddStory) {
            // “Tạo tin” – có thể không cần gán gì thêm nếu layout đã sẵn sàng
            holder.btnAdd?.setImageResource(R.drawable.baseline_add_24)
        } else {
            // Story thường
            holder.imgStory?.setImageResource(story.imageResId)
            holder.imgAvatar?.setImageResource(story.avatarResId)
        }
    }

    override fun getItemCount(): Int = stories.size
}
