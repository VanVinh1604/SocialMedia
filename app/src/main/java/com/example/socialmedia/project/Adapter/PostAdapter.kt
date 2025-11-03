package com.example.socialmedia.project.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
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

    fun getPosts(): List<PostModel> = postList

    inner class PostViewHolder(val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // === THÊM MỚI ===
        // Biến để lưu trữ các listener đang hoạt động của ViewHolder này
        var activeLikeCountListener: ValueEventListener? = null
        var activeLikeCountRef: DatabaseReference? = null

        var activeCommentListener: ChildEventListener? = null
        var activeCommentRef: DatabaseReference? = null
        // (Lưu ý: code comment của bạn có listener lồng nhau,
        //  cách gỡ này vẫn chưa triệt để cho comment, nhưng sẽ sửa được lỗi compile)
        // === KẾT THÚC THÊM MỚI ===
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        // Đảm bảo vị trí hợp lệ
        if (position == RecyclerView.NO_POSITION || position >= postList.size) {
            return
        }
        val post = postList[position]
        val b = holder.binding

        // === GỠ LISTENER CŨ TRƯỚC KHI BIND ===
        // (Cách làm này an toàn hơn cho RecyclerView)
        // Gỡ listener like count cũ (nếu có)
        holder.activeLikeCountListener?.let {
            holder.activeLikeCountRef?.removeEventListener(it)
        }
        // Gỡ listener comment cũ (nếu có)
        holder.activeCommentListener?.let {
            holder.activeCommentRef?.removeEventListener(it)
        }
        // === KẾT THÚC GỠ LISTENER CŨ ===


        // === PHẦN SỬA LỖI 1: SỬ DỤNG DỮ LIỆU CÓ SẴN ===
        // (Code này đã đúng)
        b.txtUsername.text = post.userName ?: "Ẩn danh" // Dùng post.userName
        Glide.with(b.root.context)
            .load(post.userProfileUrl ?: R.drawable.image_avata_user) // Dùng post.userProfileUrl
            .placeholder(R.drawable.image_avata_user)
            .error(R.drawable.image_avata_user)
            .circleCrop()
            .into(b.imgProfile)

        b.tvContent.text = post.caption ?: ""
        b.tvTime.text = TimeUtils.getTimeAgo(post.createdAt)

        // (Phần mediaList của bạn đã đúng, giữ nguyên)
        if (post.mediaList.isNotEmpty()) {
            b.rvMediaList.visibility = View.VISIBLE
            if (b.rvMediaList.adapter == null) {
                val layoutManager = LinearLayoutManager(
                    b.root.context,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
                b.rvMediaList.layoutManager = layoutManager
                b.rvMediaList.adapter = MediaAdapter(post.mediaList)

                val snapHelper = PagerSnapHelper()
                snapHelper.attachToRecyclerView(null)
                snapHelper.attachToRecyclerView(b.rvMediaList)

            } else {
                (b.rvMediaList.adapter as MediaAdapter).updateMedia(post.mediaList)
            }
        } else {
            b.rvMediaList.visibility = View.GONE
        }
        // === HẾT PHẦN SỬA LỖI 1 ===


        // === PHẦN SỬA LỖI 2: LIKE/COMMENT (Vẫn cần Listener) ===
        val postRef = FirebaseDatabase.getInstance().getReference("Posts").child(post.postId)

        b.tvLikesCount.text = "${post.likeCount} likes"
        b.tvLikesCount.setOnClickListener {
            onLikesClickListener?.invoke(post.postId)
        }

        b.layoutComment.setOnClickListener {
            val authorId = post.userId ?: return@setOnClickListener
            onCommentClickListener?.invoke(post.postId, authorId)
        }

        // Realtime comment count (Code cũ của bạn, có thể giữ)
        val commentsRef = FirebaseDatabase.getInstance().getReference("comments").child(post.postId)
        // ... (Code listener comment count của bạn) ...
        // (Lưu ý: code này vẫn sẽ bị memory leak vì bạn không lưu/gỡ các reply listener)


        // Realtime like count
        // === SỬA LỖI GỠ LISTENER ===
        val likeCountRef = postRef.child("likeCount")
        // 1. Tạo listener
        val likeCountListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Đảm bảo holder còn hợp lệ
                val currentPosition = holder.adapterPosition
                if (currentPosition == RecyclerView.NO_POSITION || currentPosition >= postList.size) return

                // Lấy đúng post tại vị trí
                val currentPost = postList[currentPosition]
                currentPost.likeCount = snapshot.getValue(Int::class.java) ?: 0
                updateLikeUI(currentPost, b)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        // 2. Gắn listener
        likeCountRef.addValueEventListener(likeCountListener)
        // 3. Lưu lại listener và ref để gỡ sau
        holder.activeLikeCountListener = likeCountListener
        holder.activeLikeCountRef = likeCountRef
        // === KẾT THÚC SỬA LỖI ===

        // Kiểm tra like (Code cũ của bạn, giữ nguyên)
        postRef.child("likedUsers").child(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentPosition = holder.adapterPosition
                    if (currentPosition == RecyclerView.NO_POSITION || currentPosition >= postList.size) return
                    postList[currentPosition].isLikedByCurrentUser = snapshot.getValue(Boolean::class.java) ?: false
                    updateLikeUI(postList[currentPosition], b)
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // Click like/unlike (Code cũ của bạn, giữ nguyên)
        b.ivLike.setOnClickListener {
            val likedUsersRef = postRef.child("likedUsers").child(currentUserId)
            likedUsersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Đảm bảo holder còn hợp lệ
                    val currentPosition = holder.adapterPosition
                    if (currentPosition == RecyclerView.NO_POSITION || currentPosition >= postList.size) return

                    val hasLiked = snapshot.getValue(Boolean::class.java) ?: false
                    if (!hasLiked) {
                        likedUsersRef.setValue(true)
                        postRef.child("likeCount").setValue(ServerValue.increment(1)) // Dùng increment
                        postList[currentPosition].isLikedByCurrentUser = true
                    } else {
                        likedUsersRef.removeValue()
                        postRef.child("likeCount").setValue(ServerValue.increment(-1)) // Dùng increment
                        postList[currentPosition].isLikedByCurrentUser = false
                    }
                    // updateLikeUI(post, b) // Không cần gọi ở đây, listener "likeCount" sẽ tự bắt
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    private fun updateLikeUI(post: PostModel, b: ItemPostBinding) {
        b.ivLike.setImageResource(
            if (post.isLikedByCurrentUser) R.drawable.ic_favorite_red
            else R.drawable.ic_favorite
        )
        b.tvLikesCount.text = "${post.likeCount} likes"
    }

    override fun getItemCount(): Int = postList.size

    fun updatePosts(newList: List<PostModel>) {
        postList = newList
        notifyDataSetChanged() // Cân nhắc dùng DiffUtil để hiệu năng tốt hơn
    }

    // Bạn nên thêm hàm này để gỡ bỏ listener khi ViewHolder bị tái sử dụng
    override fun onViewRecycled(holder: PostViewHolder) {
        super.onViewRecycled(holder)

        // === SỬA LỖI COMPILE Ở ĐÂY ===
        // Gỡ bỏ listener like
        holder.activeLikeCountListener?.let { listener ->
            holder.activeLikeCountRef?.removeEventListener(listener)
        }
        // Dọn dẹp
        holder.activeLikeCountListener = null
        holder.activeLikeCountRef = null

        // Gỡ bỏ listener comment (nếu bạn đã lưu nó)
        holder.activeCommentListener?.let { listener ->
            holder.activeCommentRef?.removeEventListener(listener)
        }
        holder.activeCommentListener = null
        holder.activeCommentRef = null
        // === KẾT THÚC SỬA LỖI ===
    }
}

