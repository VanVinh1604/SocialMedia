package com.example.socialmedia.project.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.databinding.ItemCommentBinding
import com.example.socialmedia.project.Domain.Model.CommentModel
import com.example.socialmedia.project.Helper.TimeUtils
import com.example.socialmedia.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CommentAdapter(
    private val comments: List<CommentModel>,
    private val currentUserId: String,
    private val postAuthorId: String,
    private val onReplyClick: (comment: CommentModel) -> Unit
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

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
        b.tvReply.setOnClickListener { onReplyClick(comment) }

        b.root.setOnLongClickListener {

            when {
                // Người viết comment → sửa + xoá
                comment.userId == currentUserId -> {
                    showUserCommentMenu(b.root.context, comment)
                }

                // Chủ bài post → chỉ được xoá
                currentUserId == postAuthorId -> {
                    showAuthorDeleteMenu(b.root.context, comment)
                }
            }

            true
        }


        // Lấy info user
        comment.userId?.let { userId ->
            FirebaseDatabase.getInstance().getReference("InfoUser").child(userId)
                .get().addOnSuccessListener { snapshot ->
                    val fullName = snapshot.child("fullName").getValue(String::class.java) ?: "Ẩn danh"
                    val profileUrl = snapshot.child("profilePictureUrl").getValue(String::class.java)

                    b.tvUserName.text = fullName
                    Glide.with(b.root.context)
                        .load(profileUrl ?: R.drawable.image_avata_user)
                        .placeholder(R.drawable.image_avata_user)
                        .circleCrop()
                        .into(b.imgUserAvatar)

                    b.tvAuthorLabel.visibility = if (userId == postAuthorId) View.VISIBLE else View.GONE
                    b.imgCurrentUserStar.visibility = if (userId == currentUserId) View.VISIBLE else View.GONE
                }
        }

        val commentRef = FirebaseDatabase.getInstance()
            .getReference("comments")
            .child(comment.commentableId)
            .child(comment.commentId)
        val userLikeRef = commentRef.child("likedUsers").child(currentUserId)

        // Lấy trạng thái like + số like
        userLikeRef.get().addOnSuccessListener { snap ->
            val hasLiked = snap.getValue(Boolean::class.java) ?: false
            b.imgLikeComment.setImageResource(
                if (hasLiked) R.drawable.ic_favorite_red else R.drawable.ic_favorite
            )
        }
        commentRef.child("likeCount").get().addOnSuccessListener { snap ->
            val likeCount = snap.getValue(Int::class.java) ?: 0
            b.tvLikeCount.text = likeCount.toString()
        }

        // Click like update UI + Firebase
        b.imgLikeComment.setOnClickListener {
            userLikeRef.get().addOnSuccessListener { snap ->
                val hasLiked = snap.getValue(Boolean::class.java) ?: false
                val currentCount = b.tvLikeCount.text.toString().toInt()
                val newCount = if (!hasLiked) currentCount + 1 else currentCount - 1

                commentRef.child("likeCount").setValue(newCount.coerceAtLeast(0))
                if (!hasLiked) userLikeRef.setValue(true) else userLikeRef.removeValue()

                b.tvLikeCount.text = newCount.coerceAtLeast(0).toString()
                b.imgLikeComment.setImageResource(
                    if (!hasLiked) R.drawable.ic_favorite_red else R.drawable.ic_favorite
                )
            }
        }

        // Load replies
        val repliesRef = FirebaseDatabase.getInstance()
            .getReference("comments")
            .child(comment.commentableId)
            .child(comment.commentId)
            .child("replies")

        repliesRef.get().addOnSuccessListener { snapshot ->
            val replyList = snapshot.children.mapNotNull { it.getValue(CommentModel::class.java) }
            val repliesCount = replyList.size

            b.layoutReplies.removeAllViews()

            if (repliesCount > 0) {
                // Reply đầu tiên
                val firstReply = replyList[0]
                val replyAdapter = ReplyAdapter(listOf(firstReply), postAuthorId, currentUserId, onReplyClick)
                val rvReplies = RecyclerView(b.root.context)
                rvReplies.layoutManager = LinearLayoutManager(b.root.context)
                rvReplies.adapter = replyAdapter
                b.layoutReplies.addView(rvReplies)

                if (repliesCount > 1) {
                    val tvSeeMore = b.tvSeeMoreReplies
                    tvSeeMore.visibility = View.VISIBLE
                    tvSeeMore.text = "Xem ${repliesCount - 1} câu trả lời khác"
                    if (tvSeeMore.parent == null) b.layoutReplies.addView(tvSeeMore)

                    tvSeeMore.setOnClickListener {
                        b.layoutFirstReply.visibility = View.GONE
                        rvReplies.adapter = ReplyAdapter(replyList, postAuthorId, currentUserId, onReplyClick)
                        rvReplies.visibility = View.VISIBLE
                        tvSeeMore.visibility = View.GONE
                        b.tvHideReplies.visibility = View.VISIBLE
                    }

                    b.tvHideReplies.setOnClickListener {
                        rvReplies.visibility = View.GONE
                        b.layoutFirstReply.visibility = View.VISIBLE
                        b.tvHideReplies.visibility = View.GONE
                        if (replyList.size > 1) tvSeeMore.visibility = View.VISIBLE
                    }
                } else {
                    b.tvSeeMoreReplies.visibility = View.GONE
                }
            } else {
                b.tvSeeMoreReplies.visibility = View.GONE
            }
        }
    }

    private fun showUserCommentMenu(context: android.content.Context, item: CommentModel) {
        val options = arrayOf("Chỉnh sửa", "Xóa")

        val builder = android.app.AlertDialog.Builder(context)
        builder.setTitle("Tùy chọn")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> showEditDialog(context, item, false)
                1 -> showDeleteDialog(context, item, false)
            }
        }
        builder.show()
    }

    private fun showAuthorDeleteMenu(context: android.content.Context, item: CommentModel) {

        val builder = android.app.AlertDialog.Builder(context)
        builder.setTitle("Tùy chọn")
        builder.setItems(arrayOf("Xóa")) { _, _ ->
            showDeleteDialog(context, item, false)
        }
        builder.show()
    }


    private fun showEditDialog(context: android.content.Context, item: CommentModel, isReply: Boolean) {
        val builder = android.app.AlertDialog.Builder(context)
        builder.setTitle("Chỉnh sửa")

        val input = android.widget.EditText(context)
        input.setText(item.content)
        input.setSelection(item.content.length)
        builder.setView(input)

        builder.setPositiveButton("Lưu") { dialog, _ ->
            val newContent = input.text.toString().trim()
            if (newContent.isNotEmpty() && newContent != item.content) {
                if (isReply) updateReply(item, newContent)
                else updateComment(item, newContent)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Hủy") { d, _ -> d.dismiss() }
        builder.show()
    }

    private fun showDeleteDialog(context: android.content.Context, item: CommentModel, isReply: Boolean) {
        val builder = android.app.AlertDialog.Builder(context)
        builder.setTitle("Xóa")
        builder.setMessage("Bạn có chắc muốn xóa?")

        builder.setPositiveButton("Xóa") { dialog, _ ->
            if (isReply) deleteReply(item)
            else deleteComment(item)
            dialog.dismiss()
        }
        builder.setNegativeButton("Hủy") { d, _ -> d.dismiss() }
        builder.show()
    }

    private fun updateComment(comment: CommentModel, newContent: String) {
        FirebaseDatabase.getInstance()
            .getReference("comments")
            .child(comment.commentableId)
            .child(comment.commentId)
            .child("content")
            .setValue(newContent)
    }

    private fun deleteComment(comment: CommentModel) {
        FirebaseDatabase.getInstance()
            .getReference("comments")
            .child(comment.commentableId)
            .child(comment.commentId)
            .removeValue()
    }

    private fun updateReply(reply: CommentModel, newContent: String) {
        FirebaseDatabase.getInstance()
            .getReference("comments")
            .child(reply.commentableId)
            .child(reply.parentCommentId!!)
            .child("replies")
            .child(reply.commentId)
            .child("content")
            .setValue(newContent)
    }

    private fun deleteReply(reply: CommentModel) {
        FirebaseDatabase.getInstance()
            .getReference("comments")
            .child(reply.commentableId)
            .child(reply.parentCommentId!!)
            .child("replies")
            .child(reply.commentId)
            .removeValue()
    }

    override fun getItemCount(): Int = comments.size
}
