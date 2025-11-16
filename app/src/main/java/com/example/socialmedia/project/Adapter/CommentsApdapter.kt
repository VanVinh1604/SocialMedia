package com.example.socialmedia.project.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.project.Domain.Model.CommentsModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.*

class CommentsAdapter(
    private val comments: MutableList<CommentsModel>,
    private val reelOwnerId: String, // Thêm ID của người đăng reel
    private val onReplyClick: (CommentsModel) -> Unit,
    private val onLikeClick: (CommentsModel) -> Unit
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    private val database = FirebaseDatabase.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    inner class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgUserAvatar: CircleImageView = view.findViewById(R.id.imgUserAvatar)
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val tvAuthorLabel: TextView = view.findViewById(R.id.tvAuthorLabel)
        val imgCurrentUserStar: ImageView = view.findViewById(R.id.imgCurrentUserStar)
        val tvCommentContent: TextView = view.findViewById(R.id.tvCommentContent)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvReply: TextView = view.findViewById(R.id.tvReply)
        val imgLikeComment: ImageView = view.findViewById(R.id.imgLikeComment)
        val tvLikeCount: TextView = view.findViewById(R.id.tvLikeCount)
        val layoutReplies: LinearLayout = view.findViewById(R.id.layoutReplies)
        val tvSeeMoreReplies: TextView = view.findViewById(R.id.tvSeeMoreReplies)
        val tvHideReplies: TextView = view.findViewById(R.id.tvHideReplies)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]

        // Set content
        holder.tvCommentContent.text = comment.content
        holder.tvTime.text = getTimeAgo(comment.createdAt)

        // Load user info
        loadUserInfo(holder, comment)

        // Setup like functionality
        setupLikeButton(holder, comment)

        // Setup reply button
        holder.tvReply.setOnClickListener {
            onReplyClick(comment)
        }

        // ✅ Setup long press để edit/delete (chỉ với comment của mình)
        if (comment.userId == currentUserId) {
            holder.itemView.setOnLongClickListener {
                showCommentOptionsDialog(holder.itemView.context, comment, position)
                true
            }
        } else {
            holder.itemView.setOnLongClickListener(null)
        }

        // Load replies
        loadReplies(holder, comment)
    }

    private fun loadUserInfo(holder: CommentViewHolder, comment: CommentsModel) {
        database.reference.child("InfoUser").child(comment.userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val fullName = snapshot.child("fullName").getValue(String::class.java) ?: "Ẩn danh"
                    val profileUrl = snapshot.child("profilePictureUrl").getValue(String::class.java)

                    holder.tvUserName.text = fullName

                    Glide.with(holder.itemView.context)
                        .load(profileUrl)
                        .placeholder(R.drawable.image_avata_user)
                        .circleCrop()
                        .into(holder.imgUserAvatar)

                    // Hiển thị label "Tác giả" nếu comment.userId == reelOwnerId
                    if (comment.userId == reelOwnerId) {
                        holder.tvAuthorLabel.visibility = View.VISIBLE
                    } else {
                        holder.tvAuthorLabel.visibility = View.GONE
                    }

                    // Show star if current user
                    holder.imgCurrentUserStar.visibility =
                        if (comment.userId == currentUserId) View.VISIBLE else View.GONE
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun setupLikeButton(holder: CommentViewHolder, comment: CommentsModel) {
        val commentRef = database.reference.child("Comments").child(comment.commentId)

        // Load like status
        commentRef.child("likedUsers").child(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val isLiked = snapshot.getValue(Boolean::class.java) ?: false
                    holder.imgLikeComment.setImageResource(
                        if (isLiked) R.drawable.ic_favorite_red else R.drawable.ic_favorite
                    )
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // Load like count
        commentRef.child("likeCount")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val count = snapshot.getValue(Int::class.java) ?: 0
                    holder.tvLikeCount.text = count.toString()
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // Handle like click
        holder.imgLikeComment.setOnClickListener {
            commentRef.child("likedUsers").child(currentUserId)
                .get().addOnSuccessListener { snapshot ->
                    val isLiked = snapshot.getValue(Boolean::class.java) ?: false
                    val currentCount = holder.tvLikeCount.text.toString().toIntOrNull() ?: 0

                    if (isLiked) {
                        // Unlike
                        commentRef.child("likedUsers").child(currentUserId).removeValue()
                        commentRef.child("likeCount").setValue((currentCount - 1).coerceAtLeast(0))
                        holder.imgLikeComment.setImageResource(R.drawable.ic_favorite)
                    } else {
                        // Like
                        commentRef.child("likedUsers").child(currentUserId).setValue(true)
                        commentRef.child("likeCount").setValue(currentCount + 1)
                        holder.imgLikeComment.setImageResource(R.drawable.ic_favorite_red)
                    }

                    onLikeClick(comment)
                }
        }
    }

    private fun loadReplies(holder: CommentViewHolder, comment: CommentsModel) {
        // Query replies by parentCommentId
        database.reference.child("Comments")
            .orderByChild("parentCommentId")
            .equalTo(comment.commentId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val replies = mutableListOf<CommentsModel>()

                    for (replySnapshot in snapshot.children) {
                        val reply = replySnapshot.getValue(CommentsModel::class.java)
                        if (reply != null) {
                            replies.add(reply)
                        }
                    }

                    // Sort by oldest first
                    replies.sortBy { it.createdAt }

                    if (replies.isNotEmpty()) {
                        displayReplies(holder, replies)
                    } else {
                        holder.layoutReplies.visibility = View.GONE
                        holder.tvSeeMoreReplies.visibility = View.GONE
                        holder.tvHideReplies.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun displayReplies(holder: CommentViewHolder, replies: List<CommentsModel>) {
        holder.layoutReplies.removeAllViews()
        holder.layoutReplies.visibility = View.VISIBLE

        if (replies.size == 1) {
            // Show single reply
            val replyView = createReplyView(holder.itemView.context, replies[0])
            holder.layoutReplies.addView(replyView)
            holder.tvSeeMoreReplies.visibility = View.GONE
            holder.tvHideReplies.visibility = View.GONE
        } else {
            // Show first reply only
            val firstReplyView = createReplyView(holder.itemView.context, replies[0])
            holder.layoutReplies.addView(firstReplyView)

            holder.tvSeeMoreReplies.visibility = View.VISIBLE
            holder.tvSeeMoreReplies.text = "Xem ${replies.size - 1} câu trả lời khác"
            holder.tvHideReplies.visibility = View.GONE

            holder.tvSeeMoreReplies.setOnClickListener {
                // Show all replies
                holder.layoutReplies.removeAllViews()
                replies.forEach { reply ->
                    val replyView = createReplyView(holder.itemView.context, reply)
                    holder.layoutReplies.addView(replyView)
                }
                holder.tvSeeMoreReplies.visibility = View.GONE
                holder.tvHideReplies.visibility = View.VISIBLE
            }

            holder.tvHideReplies.setOnClickListener {
                // Show only first reply
                holder.layoutReplies.removeAllViews()
                val firstView = createReplyView(holder.itemView.context, replies[0])
                holder.layoutReplies.addView(firstView)
                holder.tvSeeMoreReplies.visibility = View.VISIBLE
                holder.tvHideReplies.visibility = View.GONE
            }
        }
    }

    private fun createReplyView(context: android.content.Context, reply: CommentsModel): View {
        val replyView = LayoutInflater.from(context)
            .inflate(R.layout.item_comment_reply, null, false)

        val imgReplyAvatar: CircleImageView = replyView.findViewById(R.id.imgReplyAvatar)
        val tvReplyUserName: TextView = replyView.findViewById(R.id.tvReplyUserName)
        val tvReplyAuthorLabel: TextView = replyView.findViewById(R.id.tvReplyAuthorLabel)
        val imgReplyStar: ImageView = replyView.findViewById(R.id.imgReplyStar)
        val tvReplyContent: TextView = replyView.findViewById(R.id.tvReplyContent)
        val tvReplyTime: TextView = replyView.findViewById(R.id.tvReplyTime)
        val btnReplyToReply: TextView = replyView.findViewById(R.id.btnReplyToReply)
        val imgLikeReply: ImageView = replyView.findViewById(R.id.imgLikeReply)
        val tvReplyLikeCount: TextView = replyView.findViewById(R.id.tvReplyLikeCount)

        // Set content
        tvReplyContent.text = reply.content
        tvReplyTime.text = getTimeAgo(reply.createdAt)

        // ✅ Long press để edit/delete reply (chỉ với reply của mình)
        if (reply.userId == currentUserId) {
            replyView.setOnLongClickListener {
                showReplyOptionsDialog(context, reply)
                true
            }
        }

        // Load user info
        database.reference.child("InfoUser").child(reply.userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val fullName = snapshot.child("fullName").getValue(String::class.java) ?: "Ẩn danh"
                    val profileUrl = snapshot.child("profilePictureUrl").getValue(String::class.java)

                    tvReplyUserName.text = fullName
                    Glide.with(context)
                        .load(profileUrl)
                        .placeholder(R.drawable.image_avata_user)
                        .circleCrop()
                        .into(imgReplyAvatar)

                    // Hiển thị label "Tác giả" cho reply nếu cần
                    if (reply.userId == reelOwnerId) {
                        tvReplyAuthorLabel.visibility = View.VISIBLE
                    } else {
                        tvReplyAuthorLabel.visibility = View.GONE
                    }

                    imgReplyStar.visibility =
                        if (reply.userId == currentUserId) View.VISIBLE else View.GONE
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // Setup like for reply
        val replyRef = database.reference.child("Comments").child(reply.commentId)

        replyRef.child("likedUsers").child(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val isLiked = snapshot.getValue(Boolean::class.java) ?: false
                    imgLikeReply.setImageResource(
                        if (isLiked) R.drawable.ic_favorite_red else R.drawable.ic_favorite
                    )
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        replyRef.child("likeCount")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val count = snapshot.getValue(Int::class.java) ?: 0
                    tvReplyLikeCount.text = count.toString()
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        imgLikeReply.setOnClickListener {
            replyRef.child("likedUsers").child(currentUserId)
                .get().addOnSuccessListener { snapshot ->
                    val isLiked = snapshot.getValue(Boolean::class.java) ?: false
                    val currentCount = tvReplyLikeCount.text.toString().toIntOrNull() ?: 0

                    if (isLiked) {
                        replyRef.child("likedUsers").child(currentUserId).removeValue()
                        replyRef.child("likeCount").setValue((currentCount - 1).coerceAtLeast(0))
                        imgLikeReply.setImageResource(R.drawable.ic_favorite)
                    } else {
                        replyRef.child("likedUsers").child(currentUserId).setValue(true)
                        replyRef.child("likeCount").setValue(currentCount + 1)
                        imgLikeReply.setImageResource(R.drawable.ic_favorite_red)
                    }
                }
        }

        btnReplyToReply.setOnClickListener {
            onReplyClick(reply)
        }

        return replyView
    }

    // ✅ Dialog options cho reply
    private fun showReplyOptionsDialog(context: android.content.Context, reply: CommentsModel) {
        val options = arrayOf("Chỉnh sửa", "Xóa")

        val builder = android.app.AlertDialog.Builder(context)
        builder.setTitle("Tùy chọn")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> showEditReplyDialog(context, reply)
                1 -> showDeleteReplyConfirmDialog(context, reply)
            }
        }
        builder.show()
    }

    // ✅ Dialog chỉnh sửa reply
    private fun showEditReplyDialog(context: android.content.Context, reply: CommentsModel) {
        val builder = android.app.AlertDialog.Builder(context)
        builder.setTitle("Chỉnh sửa câu trả lời")

        val input = android.widget.EditText(context)
        input.setText(reply.content)
        input.setSelection(reply.content.length)
        builder.setView(input)

        builder.setPositiveButton("Lưu") { dialog, _ ->
            val newContent = input.text.toString().trim()
            if (newContent.isNotEmpty() && newContent != reply.content) {
                updateReply(reply, newContent)
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Hủy") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()

        input.requestFocus()
        val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    // ✅ Dialog xác nhận xóa reply
    private fun showDeleteReplyConfirmDialog(context: android.content.Context, reply: CommentsModel) {
        val builder = android.app.AlertDialog.Builder(context)
        builder.setTitle("Xóa câu trả lời")
        builder.setMessage("Bạn có chắc muốn xóa câu trả lời này?")

        builder.setPositiveButton("Xóa") { dialog, _ ->
            deleteReply(reply)
            dialog.dismiss()
        }

        builder.setNegativeButton("Hủy") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    // ✅ Cập nhật reply
    private fun updateReply(reply: CommentsModel, newContent: String) {
        database.reference.child("Comments").child(reply.commentId)
            .child("content").setValue(newContent)
            .addOnSuccessListener {
                reply.content = newContent
                notifyDataSetChanged() // Refresh để hiển thị nội dung mới
                android.widget.Toast.makeText(
                    null,
                    "Đã cập nhật câu trả lời",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("CommentsAdapter", "Failed to update reply", e)
            }
    }

    // ✅ Xóa reply
    private fun deleteReply(reply: CommentsModel) {
        database.reference.child("Comments").child(reply.commentId)
            .removeValue()
            .addOnSuccessListener {
                // Giảm reply count của parent comment
                reply.parentCommentId?.let { parentId ->
                    database.reference.child("Comments").child(parentId)
                        .child("replyCount")
                        .setValue(com.google.firebase.database.ServerValue.increment(-1))
                }

                // Giảm comment count trong Reel
                database.reference.child("Reels").child(reply.commentableId)
                    .child("commentCount")
                    .setValue(com.google.firebase.database.ServerValue.increment(-1))

                notifyDataSetChanged() // Refresh để ẩn reply đã xóa
            }
            .addOnFailureListener { e ->
                android.util.Log.e("CommentsAdapter", "Failed to delete reply", e)
            }
    }

    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> "Vừa xong"
            diff < 3600000 -> "${diff / 60000} phút trước"
            diff < 86400000 -> "${diff / 3600000} giờ trước"
            diff < 604800000 -> "${diff / 86400000} ngày trước"
            else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    // ✅ Hiển thị dialog options (Edit/Delete)
    private fun showCommentOptionsDialog(context: android.content.Context, comment: CommentsModel, position: Int) {
        val options = arrayOf("Chỉnh sửa", "Xóa")

        val builder = android.app.AlertDialog.Builder(context)
        builder.setTitle("Tùy chọn")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> showEditCommentDialog(context, comment, position)
                1 -> showDeleteConfirmDialog(context, comment, position)
            }
        }
        builder.show()
    }

    // ✅ Dialog chỉnh sửa comment
    private fun showEditCommentDialog(context: android.content.Context, comment: CommentsModel, position: Int) {
        val builder = android.app.AlertDialog.Builder(context)
        builder.setTitle("Chỉnh sửa bình luận")

        val input = android.widget.EditText(context)
        input.setText(comment.content)
        input.setSelection(comment.content.length) // Đặt cursor ở cuối
        builder.setView(input)

        builder.setPositiveButton("Lưu") { dialog, _ ->
            val newContent = input.text.toString().trim()
            if (newContent.isNotEmpty() && newContent != comment.content) {
                updateComment(comment, newContent, position)
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Hủy") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()

        // Hiển thị bàn phím
        input.requestFocus()
        val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    // ✅ Dialog xác nhận xóa
    private fun showDeleteConfirmDialog(context: android.content.Context, comment: CommentsModel, position: Int) {
        val builder = android.app.AlertDialog.Builder(context)
        builder.setTitle("Xóa bình luận")
        builder.setMessage("Bạn có chắc muốn xóa bình luận này?")

        builder.setPositiveButton("Xóa") { dialog, _ ->
            deleteComment(comment, position)
            dialog.dismiss()
        }

        builder.setNegativeButton("Hủy") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    // ✅ Cập nhật comment
    private fun updateComment(comment: CommentsModel, newContent: String, position: Int) {
        database.reference.child("Comments").child(comment.commentId)
            .child("content").setValue(newContent)
            .addOnSuccessListener {
                comment.content = newContent
                notifyItemChanged(position)
                android.widget.Toast.makeText(
                    comments[position].let { null } ?: return@addOnSuccessListener,
                    "Đã cập nhật bình luận",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("CommentsAdapter", "Failed to update comment", e)
            }
    }

    // ✅ Xóa comment
    private fun deleteComment(comment: CommentsModel, position: Int) {
        database.reference.child("Comments").child(comment.commentId)
            .removeValue()
            .addOnSuccessListener {
                // Giảm comment count trong Reel
                database.reference.child("Reels").child(comment.commentableId)
                    .child("commentCount")
                    .setValue(com.google.firebase.database.ServerValue.increment(-1))

                // Nếu là reply, giảm reply count của parent
                comment.parentCommentId?.let { parentId ->
                    database.reference.child("Comments").child(parentId)
                        .child("replyCount")
                        .setValue(com.google.firebase.database.ServerValue.increment(-1))
                }

                comments.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, comments.size)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("CommentsAdapter", "Failed to delete comment", e)
            }
    }

    override fun getItemCount(): Int = comments.size
}