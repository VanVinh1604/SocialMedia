package com.example.socialmedia.project.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.databinding.ItemCommentBinding
import com.example.socialmedia.project.Domain.Model.CommentModel
import com.example.socialmedia.project.Helper.TimeUtils
import com.example.socialmedia.R
import com.google.firebase.database.FirebaseDatabase

class CommentAdapter(private val comments: List<CommentModel>,
                     private val currentUserId: String,
                     private val postAuthorId: String) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(val binding: ItemCommentBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        val b = holder.binding

        b.tvCommentContent.text = comment.content
        b.tvTime.text = TimeUtils.getTimeAgo(comment.createdAt)

        comment.userId?.let { userId ->
            val userRef = FirebaseDatabase.getInstance().getReference("InfoUser").child(userId)
            userRef.get().addOnSuccessListener { snapshot ->
                val fullName = snapshot.child("fullName").getValue(String::class.java) ?: "Ẩn danh"
                val profileUrl = snapshot.child("profilePictureUrl").getValue(String::class.java)

                b.tvUserName.text = fullName
                Glide.with(b.root.context)
                    .load(profileUrl ?: R.drawable.image_avata_user)
                    .placeholder(R.drawable.image_avata_user)
                    .circleCrop()
                    .into(b.imgUserAvatar)

                // 🔹 Xử lý nhãn tác giả / ngôi sao

                b.tvAuthorLabel.visibility = if (userId == postAuthorId) View.VISIBLE else View.GONE
                b.imgCurrentUserStar.visibility = if (userId == currentUserId) View.VISIBLE else View.GONE
            }
        }
    }


    override fun getItemCount(): Int = comments.size
}
