package com.example.socialmedia.project.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.ItemCommentReplyBinding
import com.example.socialmedia.project.Domain.Model.CommentModel
import com.example.socialmedia.project.Helper.TimeUtils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ReplyAdapter(
    private val replies: List<CommentModel>,
    private val postAuthorId: String,
    private val currentUserId: String,
    private val onReplyClick: (comment: CommentModel) -> Unit
) : RecyclerView.Adapter<ReplyAdapter.ReplyViewHolder>() {

    inner class ReplyViewHolder(val binding: ItemCommentReplyBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(reply: CommentModel) {
            binding.tvReplyContent.text = reply.content
            binding.tvReplyTime.text = TimeUtils.getTimeAgo(reply.createdAt)

            // Lấy thông tin user
            FirebaseDatabase.getInstance().getReference("InfoUser")
                .child(reply.userId).get().addOnSuccessListener { snap ->
                    val fullName = snap.child("fullName").getValue(String::class.java) ?: "Ẩn danh"
                    val profileUrl = snap.child("profilePictureUrl").getValue(String::class.java)

                    binding.tvReplyUserName.text = fullName
                    Glide.with(binding.root.context)
                        .load(profileUrl ?: R.drawable.image_avata_user)
                        .placeholder(R.drawable.image_avata_user)
                        .circleCrop()
                        .into(binding.imgReplyAvatar)

                    binding.btnReplyToReply.setOnClickListener {
                        // Luôn trả về comment gốc để không tạo cấp 2
                        onReplyClick(reply.copy(parentCommentId = reply.parentCommentId))
                    }

                    if (reply.userId == postAuthorId) {
                        binding.tvReplyAuthorLabel.visibility = View.VISIBLE
                        binding.imgReplyStar.visibility =
                            if (postAuthorId == currentUserId) View.VISIBLE else View.GONE
                    } else if (reply.userId == currentUserId) {
                        binding.tvReplyAuthorLabel.visibility = View.GONE
                        binding.imgReplyStar.visibility = View.VISIBLE
                    } else {
                        binding.tvReplyAuthorLabel.visibility = View.GONE
                        binding.imgReplyStar.visibility = View.GONE
                    }

                }

            val replyRef = FirebaseDatabase.getInstance()
                .getReference("comments")
                .child(reply.commentableId)
                .child(reply.parentCommentId ?: "")
                .child("replies")
                .child(reply.commentId)

            val userLikeRef = replyRef.child("likedUsers").child(currentUserId)

            // Lấy trạng thái like + số like một lần, chỉ để hiển thị UI
            userLikeRef.get().addOnSuccessListener { snap ->
                val hasLiked = snap.getValue(Boolean::class.java) ?: false
                binding.imgLikeReply.setImageResource(
                    if (hasLiked) R.drawable.ic_favorite_red else R.drawable.ic_favorite
                )
            }
            replyRef.child("likeCount").get().addOnSuccessListener { snap ->
                val likeCount = snap.getValue(Int::class.java) ?: 0
                binding.tvReplyLikeCount.text = likeCount.toString()
            }

            // Click like chỉ update UI + Firebase, không chỉnh reply object
            binding.imgLikeReply.setOnClickListener {
                userLikeRef.get().addOnSuccessListener { snap ->
                    val hasLiked = snap.getValue(Boolean::class.java) ?: false
                    val currentCount = binding.tvReplyLikeCount.text.toString().toInt()
                    val newCount = if (!hasLiked) currentCount + 1 else currentCount - 1

                    replyRef.child("likeCount").setValue(newCount.coerceAtLeast(0))
                    if (!hasLiked) userLikeRef.setValue(true) else userLikeRef.removeValue()

                    binding.tvReplyLikeCount.text = newCount.coerceAtLeast(0).toString()
                    binding.imgLikeReply.setImageResource(
                        if (!hasLiked) R.drawable.ic_favorite_red else R.drawable.ic_favorite
                    )
                }
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplyViewHolder {
        val binding = ItemCommentReplyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReplyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReplyViewHolder, position: Int) {
        holder.bind(replies[position])
    }

    override fun getItemCount(): Int = replies.size
}

