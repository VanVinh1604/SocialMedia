package com.example.socialmedia.project.Adapter

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.ItemPostBinding
import com.example.socialmedia.project.Domain.Model.PostModel
import com.example.socialmedia.project.Helper.TimeUtils
import com.google.firebase.database.*

class PostAdapter(
    private var postList: List<PostModel>,
    private val currentUserId: String,
    var onLikesClickListener: ((postId: String) -> Unit)? = null,
    var onCommentClickListener: ((postId: String, postAuthorId: String) -> Unit)? = null
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val commentListeners = mutableMapOf<String, ValueEventListener>()

    inner class PostViewHolder(val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]
        val b = holder.binding

        // 🧹 Reset UI tránh lỗi view cũ
        b.tvCommentCount.text = "0 comments"
        b.tvLikesCount.text = "${post.likeCount} likes"
        b.ivLike.setImageResource(R.drawable.ic_favorite)
        b.rvMediaList.visibility = View.GONE

        // 📝 Nội dung bài viết
        b.tvContent.text = post.caption ?: ""
        b.tvTime.text = TimeUtils.getTimeAgo(post.createdAt)

        // 🎯 Click listeners
        b.tvLikesCount.setOnClickListener { onLikesClickListener?.invoke(post.postId) }
        b.layoutComment.setOnClickListener {
            val authorId = post.userId ?: return@setOnClickListener
            onCommentClickListener?.invoke(post.postId, authorId)
        }

        // 💬 Đếm comment + replies (dùng ValueEventListener)
        val commentsRef = FirebaseDatabase.getInstance().getReference("comments").child(post.postId)
        val existing = commentListeners.remove(post.postId)
        if (existing != null) commentsRef.removeEventListener(existing)

        val commentListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalComments = 0
                for (child in snapshot.children) {
                    totalComments++
                    totalComments += child.child("replies").childrenCount.toInt()
                }
                b.tvCommentCount.text = "$totalComments comments"
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        commentsRef.addValueEventListener(commentListener)
        commentListeners[post.postId] = commentListener

        // 📸 Media
        if (post.mediaList.isNotEmpty()) {
            b.rvMediaList.visibility = View.VISIBLE
            if (b.rvMediaList.layoutManager == null) {
                b.rvMediaList.layoutManager = LinearLayoutManager(b.root.context, LinearLayoutManager.HORIZONTAL, false)
            }

            if (b.rvMediaList.onFlingListener == null) {
                PagerSnapHelper().attachToRecyclerView(b.rvMediaList)
            }

            if (b.rvMediaList.adapter == null) {
                b.rvMediaList.adapter = MediaAdapter(post.mediaList)
            } else {
                (b.rvMediaList.adapter as MediaAdapter).updateMedia(post.mediaList)
            }

            // 🔘 Tạo dot indicator
            b.dotsContainer.removeAllViews()
            for (i in post.mediaList.indices) {
                val dot = ImageView(b.root.context)
                dot.setImageResource(if (i == 0) R.drawable.dot_selected else R.drawable.dot_unselected)
                val params = LinearLayout.LayoutParams(16, 16)
                params.setMargins(4, 0, 4, 0)
                dot.layoutParams = params
                b.dotsContainer.addView(dot)
            }
        }

        // 👤 Load user info
        val userId = post.userId ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("InfoUser").child(userId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("fullName").getValue(String::class.java) ?: "Ẩn danh"
                val avatar = snapshot.child("profilePictureUrl").getValue(String::class.java)
                b.txtUsername.text = name
                Glide.with(b.root.context)
                    .load(avatar ?: R.drawable.image_avata_user)
                    .placeholder(R.drawable.image_avata_user)
                    .circleCrop()
                    .into(b.imgProfile)
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // ❤️ Like system
        val postRef = FirebaseDatabase.getInstance().getReference("Posts").child(post.postId)
        postRef.child("likeCount").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                post.likeCount = snapshot.getValue(Int::class.java) ?: 0
                updateLikeUI(post, b)
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        postRef.child("likedUsers").child(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    post.isLikedByCurrentUser = snapshot.getValue(Boolean::class.java) ?: false
                    updateLikeUI(post, b)
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        b.ivLike.setOnClickListener {
            val likedUsersRef = postRef.child("likedUsers").child(currentUserId)
            likedUsersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val hasLiked = snapshot.getValue(Boolean::class.java) ?: false
                    if (!hasLiked) {
                        // Thả tym
                        likedUsersRef.setValue(true)
                        postRef.child("likeCount").setValue(post.likeCount + 1)
                        post.likeCount += 1
                        post.isLikedByCurrentUser = true
                    } else {
                        // Bỏ tym
                        likedUsersRef.removeValue()
                        postRef.child("likeCount").setValue((post.likeCount - 1).coerceAtLeast(0))
                        post.likeCount = (post.likeCount - 1).coerceAtLeast(0)
                        post.isLikedByCurrentUser = false
                    }

                    // ✅ Cập nhật ngay giao diện mà không cần chờ Firebase callback
                    updateLikeUI(post, b)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }

    }

    private fun updateLikeUI(post: PostModel, b: ItemPostBinding) {
        b.ivLike.setImageResource(
            if (post.isLikedByCurrentUser) R.drawable.ic_favorite_red else R.drawable.ic_favorite
        )
        b.tvLikesCount.text = "${post.likeCount} likes"
    }

    override fun onViewRecycled(holder: PostViewHolder) {
        super.onViewRecycled(holder)
        val pos = holder.bindingAdapterPosition
        if (pos == RecyclerView.NO_POSITION) return
        val post = postList.getOrNull(pos) ?: return
        val listener = commentListeners.remove(post.postId)
        if (listener != null) {
            FirebaseDatabase.getInstance().getReference("comments").child(post.postId)
                .removeEventListener(listener)
        }
    }

    override fun getItemCount(): Int = postList.size

    fun updatePosts(newList: List<PostModel>) {
        postList = newList
        notifyDataSetChanged()
    }
}
