package com.example.socialmedia.project.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.project.Domain.Model.StoryViewerItem

class StoryViewerBottomSheetAdapter(
   var viewers: List<StoryViewerItem>
) : RecyclerView.Adapter<StoryViewerBottomSheetAdapter.ViewerViewHolder>() {

    inner class ViewerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgAvatar: de.hdodenhof.circleimageview.CircleImageView = view.findViewById(R.id.imgAvatar)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val imgHeart: ImageView = view.findViewById(R.id.imgHeart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_story_viewer, parent, false)
        return ViewerViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewerViewHolder, position: Int) {
        val viewer = viewers[position]
        holder.tvName.text = viewer.userName
        Glide.with(holder.itemView.context)
            .load(viewer.userAvatar ?: R.drawable.default_avatar)
            .circleCrop()
            .into(holder.imgAvatar)
        holder.imgHeart.visibility = if (viewer.hasLiked) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int = viewers.size
}
