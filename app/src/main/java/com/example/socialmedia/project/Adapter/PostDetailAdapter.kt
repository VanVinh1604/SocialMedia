package com.example.socialmedia.project.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.ItemPostDetailFullBinding
import com.example.socialmedia.project.Domain.Model.PostModel
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostDetailAdapter(
    private val context: Context,
    private var posts: List<PostModel>,
    private val onBackClick: () -> Unit,
    private val onCommentClick: (PostModel) -> Unit,
    private val onLikeClick: (String, Boolean) -> Unit, // postId, currentStatus
    private val onBookmarkClick: (String, Boolean) -> Unit
) : RecyclerView.Adapter<PostDetailAdapter.PostViewHolder>() {

    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid

    inner class PostViewHolder(val binding: ItemPostDetailFullBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: PostModel) {
            // 1. Set thông tin cơ bản
            binding.tvUsername.text = post.userName
            binding.tvCaption.text = post.caption
            binding.tvCaption.isVisible = !post.caption.isNullOrEmpty()
            binding.tvTimestamp.text = formatTimestamp(post.createdAt)

            Glide.with(context)
                .load(post.userProfileUrl)
                .placeholder(R.drawable.image_avata_user)
                .circleCrop()
                .into(binding.ivUserAvatar)

            // 2. Setup Media ViewPager (Ảnh bên trong bài viết)
            val mediaAdapter = MediaViewPagerAdapter() // Dùng lại adapter cũ của bạn
            binding.viewPagerMedia.adapter = mediaAdapter
            mediaAdapter.submitList(post.mediaList)

            if (post.mediaList.size > 1) {
                binding.tabIndicator.isVisible = true
                TabLayoutMediator(binding.tabIndicator, binding.viewPagerMedia) { _, _ -> }.attach()
            } else {
                binding.tabIndicator.isVisible = false
            }

            // 3. Check trạng thái Like/Bookmark Realtime cho TỪNG item
            checkLikeStatus(post.postId, binding)
            checkBookmarkStatus(post.postId, binding)

            // 4. Click Events
            binding.ivToolbarBack.setOnClickListener { onBackClick() }

            binding.btnComment.setOnClickListener { onCommentClick(post) }

            // Lưu ý: Logic like click sẽ được xử lý trong checkLikeStatus để lấy trạng thái mới nhất
            // Nhưng để đơn giản ta gán tạm ở đây
        }

        private fun checkLikeStatus(postId: String, binding: ItemPostDetailFullBinding) {
            val likesRef = FirebaseDatabase.getInstance().reference.child("post_likes").child(postId)
            likesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!binding.root.isAttachedToWindow) return

                    val count = snapshot.childrenCount
                    val isLiked = snapshot.hasChild(currentUid ?: "")

                    binding.tvLikeCount.text = "$count lượt thích"
                    val icon = if (isLiked) R.drawable.ic_favorite_red else R.drawable.ic_favorite
                    binding.btnLike.setImageResource(icon)

                    binding.btnLike.setOnClickListener {
                        onLikeClick(postId, isLiked)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }

        private fun checkBookmarkStatus(postId: String, binding: ItemPostDetailFullBinding) {
            if (currentUid == null) return
            val bookmarkRef = FirebaseDatabase.getInstance().reference
                .child("bookmarks").child(currentUid).child(postId)

            bookmarkRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!binding.root.isAttachedToWindow) return

                    val isBookmarked = snapshot.exists()
                    if (isBookmarked) {
                        binding.btnBookmark.setImageResource(R.drawable.ic_bookmark_outline)
                        binding.btnBookmark.setColorFilter(ContextCompat.getColor(context, android.R.color.black))
                    } else {
                        binding.btnBookmark.setImageResource(R.drawable.ic_bookmark_outline)
                        binding.btnBookmark.clearColorFilter()
                    }

                    binding.btnBookmark.setOnClickListener {
                        onBookmarkClick(postId, isBookmarked)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostDetailFullBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount(): Int = posts.size

    fun updateData(newPosts: List<PostModel>) {
        this.posts = newPosts
        notifyDataSetChanged()
    }

    private fun formatTimestamp(timestamp: Long): String {
        return try {
            val sdf = SimpleDateFormat("dd 'tháng' MM, yyyy 'lúc' HH:mm", Locale("vi", "VN"))
            sdf.format(Date(timestamp))
        } catch (e: Exception) { "Vừa xong" }
    }
}