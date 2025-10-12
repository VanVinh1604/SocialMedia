package com.example.socialmedia.project.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmedia.R
import com.example.socialmedia.project.Domain.PostModel
import de.hdodenhof.circleimageview.CircleImageView

class PostAdapter(private val posts: List<PostModel>) :
    RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    inner class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgProfile: CircleImageView = view.findViewById(R.id.imgProfile)
        val txtUsername: TextView = view.findViewById(R.id.txtUsername)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvContent: TextView = view.findViewById(R.id.tvContent)
        val imgPost: ImageView = view.findViewById(R.id.imgPost)
        val tvLikesCount: TextView = view.findViewById(R.id.tvLikesCount)
        val tvCommentCount: TextView = view.findViewById(R.id.tvCommentCount)
        val tvShareCount: TextView = view.findViewById(R.id.tvShareCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        // Dữ liệu mẫu để hiển thị thiết kế
        holder.txtUsername.text = "User ${post.userId}"
        holder.tvTime.text = "2h ago"
        holder.tvContent.text = post.content
        holder.tvLikesCount.text = "${post.likeCount}"
        holder.tvCommentCount.text = "${post.shareCount}"  // nếu chưa có comment count riêng
        holder.tvShareCount.text = "${post.shareCount}"

        // Ảnh post & profile test
        holder.imgProfile.setImageResource(R.drawable.image_person)
        holder.imgPost.setImageResource(R.drawable.image_backgroud)
    }

    override fun getItemCount(): Int = posts.size
}
