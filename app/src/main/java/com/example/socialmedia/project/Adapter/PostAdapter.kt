package com.example.socialmedia.project.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
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
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]
        val b = holder.binding

        // Nội dung bài viết
        b.tvContent.text = post.caption ?: ""
        b.tvLikesCount.text = "${post.likeCount} likes"
        b.tvTime.text = TimeUtils.getTimeAgo(post.createdAt)

        b.tvLikesCount.setOnClickListener {
            onLikesClickListener?.invoke(post.postId)
        }

        b.layoutComment.setOnClickListener {
            val authorId = post.userId ?: return@setOnClickListener
            onCommentClickListener?.invoke(post.postId, authorId)
        }


        val commentsRef = FirebaseDatabase.getInstance().getReference("comments").child(post.postId)

        var totalComments = 0 // lưu tổng số comment + reply

// Listener cho comment cha
        commentsRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                totalComments++ // comment cha mới
                // cộng số reply nếu đã có sẵn
                totalComments += snapshot.child("replies").childrenCount.toInt()
                b.tvCommentCount.text = "$totalComments comments"

                // listener cho reply của comment này
                val repliesRef = snapshot.ref.child("replies")
                repliesRef.addChildEventListener(object : ChildEventListener {
                    override fun onChildAdded(snap: DataSnapshot, previousChildName: String?) {
                        totalComments++
                        b.tvCommentCount.text = "$totalComments comments"
                    }

                    override fun onChildRemoved(snap: DataSnapshot) {
                        totalComments--
                        b.tvCommentCount.text = "$totalComments comments"
                    }

                    override fun onChildChanged(snap: DataSnapshot, previousChildName: String?) {}
                    override fun onChildMoved(snap: DataSnapshot, previousChildName: String?) {}
                    override fun onCancelled(error: DatabaseError) {}
                })
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // trừ comment cha + số reply của nó
                totalComments -= 1 + snapshot.child("replies").childrenCount.toInt()
                b.tvCommentCount.text = "$totalComments comments"
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })

        if (post.mediaList.isNotEmpty()) {
            b.rvMediaList.visibility = View.VISIBLE

            // Gắn LayoutManager chỉ 1 lần
            if (b.rvMediaList.layoutManager == null) {
                b.rvMediaList.layoutManager = LinearLayoutManager(
                    b.root.context,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
            }

            // Gắn SnapHelper chỉ nếu chưa có
            if (b.rvMediaList.onFlingListener == null) {
                val snapHelper = PagerSnapHelper()
                snapHelper.attachToRecyclerView(b.rvMediaList)
            }

            // Set adapter (cập nhật hoặc tạo mới)
            if (b.rvMediaList.adapter == null) {
                b.rvMediaList.adapter = MediaAdapter(post.mediaList)
            } else {
                (b.rvMediaList.adapter as MediaAdapter).updateMedia(post.mediaList)
            }

            // Tạo dots
            b.dotsContainer.removeAllViews()
            for (i in post.mediaList.indices) {
                val dot = ImageView(b.root.context)
                dot.setImageResource(if (i == 0) R.drawable.dot_selected else R.drawable.dot_unselected)
                val params = LinearLayout.LayoutParams(16, 16)
                params.setMargins(4, 0, 4, 0)
                dot.layoutParams = params
                b.dotsContainer.addView(dot)
            }

            // Lắng nghe scroll — chỉ gắn 1 lần duy nhất
            if (b.rvMediaList.getTag(R.id.rvMediaList) == null) {
                b.rvMediaList.setTag(R.id.rvMediaList, true) // đánh dấu đã gắn listener
                b.rvMediaList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                            val snapHelper = PagerSnapHelper()
                            val snapView = snapHelper.findSnapView(layoutManager)
                            val pos = snapView?.let { layoutManager.getPosition(it) } ?: 0

                            for (i in 0 until post.mediaList.size) {
                                val dot = b.dotsContainer.getChildAt(i) as ImageView
                                dot.setImageResource(
                                    if (i == pos) R.drawable.dot_selected else R.drawable.dot_unselected
                                )
                            }
                        }
                    }
                })
            }
        } else {
            b.rvMediaList.visibility = View.GONE
        }

        // Load thông tin user
        val userId = post.userId ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("InfoUser").child(userId)
        userRef.keepSynced(true)
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userName = snapshot.child("fullName").getValue(String::class.java) ?: "Ẩn danh"
                val profileUrl = snapshot.child("profilePictureUrl").getValue(String::class.java)

                b.txtUsername.text = userName

                Glide.with(b.root.context)
                    .load(profileUrl ?: R.drawable.image_avata_user)
                    .placeholder(R.drawable.image_avata_user)
                    .error(R.drawable.image_avata_user)
                    .circleCrop()
                    .into(b.imgProfile)
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        // Realtime like count
        val postRef = FirebaseDatabase.getInstance().getReference("Posts").child(post.postId)
        postRef.child("likeCount").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                post.likeCount = snapshot.getValue(Int::class.java) ?: 0
                updateLikeUI(post, b)
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // Kiểm tra like
        postRef.child("likedUsers").child(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    post.isLikedByCurrentUser = snapshot.getValue(Boolean::class.java) ?: false
                    updateLikeUI(post, b)
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // Click like/unlike
        b.ivLike.setOnClickListener {
            val likedUsersRef = postRef.child("likedUsers").child(currentUserId)
            likedUsersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val hasLiked = snapshot.getValue(Boolean::class.java) ?: false
                    if (!hasLiked) {
                        // 🔹 Thả tym
                        likedUsersRef.setValue(true)
                        postRef.child("likeCount").setValue(post.likeCount + 1)
                        post.isLikedByCurrentUser = true

                        // ✅ Gửi thông báo cho chủ bài viết
                        val notifRepo = com.example.socialmedia.project.data.repository.NotificationRepository()
                        val postOwnerId = post.userId ?: return
                        notifRepo.sendLikeNotification(currentUserId, postOwnerId, post.postId)
                    } else {
                        // 🔹 Bỏ tym
                        likedUsersRef.removeValue()
                        postRef.child("likeCount").setValue(post.likeCount - 1)
                        post.isLikedByCurrentUser = false
                    }
                    updateLikeUI(post, b)
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
        notifyDataSetChanged()
    }
}
