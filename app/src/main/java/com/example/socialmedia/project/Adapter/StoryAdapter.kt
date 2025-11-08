package com.example.socialmedia.project.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.project.Domain.Model.StoryModel
import com.example.socialmedia.project.Server.Firebase.FirebaseService
import com.google.android.material.imageview.ShapeableImageView
import de.hdodenhof.circleimageview.CircleImageView

class StoryAdapter(
    private val stories: List<StoryModel>,
    private val currentUserId: String,
    private val firebaseService: FirebaseService = FirebaseService(),
    private val onAddStoryClick: (() -> Unit)? = null,
    private val onStoryClick: ((StoryModel) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_ADD_STORY = 0
        private const val TYPE_STORY = 1
        private const val TYPE_SUGGEST_FRIEND = 2
    }

    override fun getItemViewType(position: Int): Int {
        val story = stories[position]
        return when {
            story.isAddStory -> TYPE_ADD_STORY
            story.isSuggestFriend -> TYPE_SUGGEST_FRIEND
            else -> TYPE_STORY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_ADD_STORY -> AddStoryViewHolder(inflater.inflate(R.layout.item_add_story, parent, false))
            TYPE_SUGGEST_FRIEND -> SuggestFriendViewHolder(inflater.inflate(R.layout.item_story_suggest_follow, parent, false))
            else -> StoryViewHolder(inflater.inflate(R.layout.item_story, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val story = stories[position]
        when (holder) {

            is AddStoryViewHolder -> {
                holder.btnAdd?.setImageResource(R.drawable.baseline_add_24)
            }

            is StoryViewHolder -> {
                holder.tvUserName?.text = story.userName

                // Load ảnh story nền
                Glide.with(holder.itemView.context)
                    .load(story.mediaUrl.takeIf { !it.isNullOrEmpty() }) // link từ Firebase
                    .placeholder(R.drawable.image_person) // ảnh mặc định nếu chưa có
                    .error(R.drawable.image_person)
                    .centerCrop()
                    .into(holder.imgStory!!)

                // Load avatar
                Glide.with(holder.itemView.context)
                    .load(story.userProfileImage.takeIf { !it.isNullOrEmpty() })
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .circleCrop()
                    .into(holder.imgAvatar!!)

                holder.itemView.setOnClickListener {
                    onStoryClick?.invoke(story)
                }
            }


            is SuggestFriendViewHolder -> bindSuggestFriend(holder, story)
        }
    }

    private fun bindSuggestFriend(holder: SuggestFriendViewHolder, story: StoryModel) {
        val context = holder.itemView.context
        val targetUserId = story.userId

        holder.tvUsername?.text = story.userName

        Glide.with(context)
            .load(story.userProfileImage)
            .placeholder(R.drawable.image_avata_user)
            .into(holder.imgAvatar!!)

        // ✅ Chỉ kiểm tra trạng thái follow 1 lần duy nhất
        firebaseService.isUserFollowing(currentUserId, targetUserId) { isFollowing ->
            updateFollowButton(holder.tvFollow, isFollowing)

            holder.tvFollow.setOnClickListener {
                holder.tvFollow.isEnabled = false // chống spam click

                if (isFollowing) {
                    // 👉 Unfollow
                    firebaseService.unfollowUser(
                        currentUserId = currentUserId,
                        targetUserId = targetUserId,
                        onSuccess = {
                            updateFollowButton(holder.tvFollow, false)
                            holder.tvFollow.isEnabled = true
                        },
                        onError = {
                            holder.tvFollow.text = "Erro"
                            holder.tvFollow.isEnabled = true
                        }
                    )
                } else {
                    // 👉 Follow
                    firebaseService.followUser(
                        currentUserId = currentUserId,
                        targetUserId = targetUserId,
                        onSuccess = {
                            updateFollowButton(holder.tvFollow, true)
                            holder.tvFollow.isEnabled = true
                        },
                        onError = {
                            holder.tvFollow.text = "Try Again"
                            holder.tvFollow.isEnabled = true
                        }
                    )
                }
            }
        }

        holder.btnClose?.setOnClickListener {
            // TODO: Ẩn gợi ý nếu cần
        }
    }

    // ✅ Hàm cập nhật giao diện nút Follow
    private fun updateFollowButton(tvFollow: TextView, isFollowing: Boolean) {
        if (isFollowing) {
            tvFollow.text = "UnFollow"
//            tvFollow.setBackgroundResource(R.drawable.c)
            tvFollow.setTextColor(tvFollow.context.getColor(android.R.color.black))
        } else {
            tvFollow.text = "Follow"
            tvFollow.setBackgroundResource(R.drawable.btn_gradient)
            tvFollow.setTextColor(tvFollow.context.getColor(android.R.color.white))
        }
    }

    override fun getItemCount(): Int = stories.size

    // ==================== ViewHolders ====================
    inner class AddStoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val btnAdd: ImageView? = view.findViewById(R.id.btnStory)
        init { btnAdd?.setOnClickListener { onAddStoryClick?.invoke() // gọi callback khi click
         }
        }
    }

    inner class StoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgStory: ShapeableImageView? = view.findViewById(R.id.imgStory)
        val imgAvatar: CircleImageView? = view.findViewById(R.id.imgAvatar)
        val tvUserName: TextView? = view.findViewById(R.id.tvUsername)
    }

    inner class SuggestFriendViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgAvatar: ShapeableImageView? = view.findViewById(R.id.imgAvatar)
        val tvUsername: TextView? = view.findViewById(R.id.tvUsername)
        val tvFollow: TextView = view.findViewById(R.id.tvFollow)
        val btnClose: ImageView? = view.findViewById(R.id.ivClose)
    }
}
