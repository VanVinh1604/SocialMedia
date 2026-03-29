package com.example.socialmedia.project.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.project.Domain.Model.NotificationModel
import com.example.socialmedia.project.Helper.TimeUtils
import com.example.socialmedia.project.Server.Firebase.FirebaseService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class NotificationAdapter(
    private val items: MutableList<NotificationModel> = mutableListOf()
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {


    private val firebaseService = FirebaseService()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    inner class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgUser: ImageView = view.findViewById(R.id.imgUser)
        val imgPost: ImageView = view.findViewById(R.id.imgPost)
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val btnMore: ImageView = view.findViewById(R.id.btnMore)

        val btnFollow: LinearLayout = view.findViewById(R.id.btnFollow)
        val tvFollowText: TextView = view.findViewById(R.id.tvFollowText)


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val n = items[position]

        val actorName = n.actorName.ifEmpty { "Người dùng" }
        holder.itemView.findViewById<TextView>(R.id.tvActorName).text = actorName

        // Nội dung thông báo
        val message = when (n.notificationType.name) {
            "LIKE" -> "đã thích bài viết của bạn"
            "COMMENT" -> "đã bình luận về bài viết của bạn"
            "FOLLOW" -> "đã bắt đầu theo dõi bạn"
            "FOLLOW_REQUEST" -> "đã gửi yêu cầu theo dõi bạn"
            "MENTION" -> "đã nhắc đến bạn trong một bài viết"
            "TAG" -> "đã gắn thẻ bạn trong một bài viết"
            "COMMENT_REPLY" -> "đã trả lời bình luận của bạn"
            "STORY_REPLY" -> "đã phản hồi story của bạn"
            "POST_SHARE" -> "đã chia sẻ bài viết của bạn"
            else -> "có hành động mới"
        }
        holder.tvMessage.text = message

        // Thời gian hiển thị
        holder.tvTime.text = TimeUtils.getTimeAgo(n.createdAt)

        // Avatar người gửi
        Glide.with(holder.itemView.context)
            .load(n.actorAvatar)
            .placeholder(R.drawable.image_avata_user)
            .error(R.drawable.image_avata_user)
            .circleCrop()
            .into(holder.imgUser)

        // Thumbnail bài post (ảnh đầu tiên)
        if ((n.notificationType.name == "LIKE" ||
                    n.notificationType.name == "COMMENT" ||
                    n.notificationType.name == "MENTION") && !n.targetId.isNullOrEmpty()) {

            holder.imgPost.visibility = View.VISIBLE

            val postRef = FirebaseDatabase.getInstance()
                .getReference("posts")
                .child(n.targetId)
                .child("mediaList")

            postRef.limitToFirst(1).get().addOnSuccessListener { snapshot ->
                val firstUrl = snapshot.children.firstOrNull()
                    ?.child("mediaUrl")
                    ?.getValue(String::class.java)
                if (!firstUrl.isNullOrEmpty()) {
                    Glide.with(holder.imgPost.context)
                        .load(firstUrl)
                        .placeholder(R.drawable.background_header)
                        .centerCrop()
                        .into(holder.imgPost)
                }
            }.addOnFailureListener {
                holder.imgPost.setImageResource(R.drawable.background_header)
            }

        } else {
            holder.imgPost.visibility = View.GONE
        }

        if (n.notificationType.name == "FOLLOW") {
            holder.btnMore.visibility = View.GONE
            holder.imgPost.visibility = View.GONE
            holder.btnFollow.visibility = View.VISIBLE

            if (currentUserId != null && n.actorId != null) {
                firebaseService.isUserFollowing(currentUserId, n.actorId!!) { isFollowing ->
                    updateFollowButtonUI(holder.btnFollow, holder.tvFollowText, isFollowing)
                }

                holder.btnFollow.setOnClickListener {
                    firebaseService.isUserFollowing(currentUserId, n.actorId!!) { isFollowing ->
                        if (isFollowing) {
                            firebaseService.unfollowUser(currentUserId, n.actorId!!, {
                                updateFollowButtonUI(holder.btnFollow, holder.tvFollowText, false)
                            }, {})
                        } else {
                            firebaseService.followUser(currentUserId, n.actorId!!, {
                                updateFollowButtonUI(holder.btnFollow, holder.tvFollowText, true)
                            }, {})
                        }
                    }
                }
            }
        } else {
            // --- Các loại thông báo khác ---
            holder.btnMore.visibility = View.VISIBLE
            holder.btnFollow.visibility = View.GONE
            holder.imgPost.visibility = View.VISIBLE
        }

        // Sự kiện click "More"
        holder.btnMore.setOnClickListener {
            // Gợi ý: hiển thị PopupMenu để block / xoá / báo cáo...
            // val popup = PopupMenu(holder.itemView.context, holder.btnMore)
            // popup.menuInflater.inflate(R.menu.notification_more_menu, popup.menu)
            // popup.show()
        }
    }


    override fun getItemCount(): Int = items.size

    fun updateList(list: List<NotificationModel>) {
        items.clear()
        items.addAll(list.sortedByDescending { it.createdAt })
        notifyDataSetChanged()
    }

    private fun updateFollowButtonUI(layout: LinearLayout, textView: TextView, isFollowing: Boolean) {
        if (isFollowing) {
            layout.setBackgroundResource(R.drawable.bg_following_button)
            textView.text = "Following"
            textView.setTextColor(layout.context.getColor(R.color.black))
        } else {
            layout.setBackgroundResource(R.drawable.bg_follow_button)
            textView.text = "Follow"
            textView.setTextColor(layout.context.getColor(android.R.color.white))
        }
    }


    // TODO: Cập nhật hàm này để lấy thumbnail bài viết từ Firebase
    private fun loadPostThumbnail(postId: String, imageView: ImageView) {
        // Ví dụ: FirebaseDatabase.getInstance().getReference("posts").child(postId).child("thumbnailUrl")...
        // Glide.with(imageView.context).load(url).into(imageView)
    }
}
