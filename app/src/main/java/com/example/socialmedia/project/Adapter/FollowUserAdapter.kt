package com.example.socialmedia.project.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.ItemFollowUserBinding
import com.example.socialmedia.project.Domain.Model.UserModel

class FollowUserAdapter(
    private val currentUserId: String, // <-- THAM SỐ BỊ THIẾU LÀ ĐÂY
    private val onUserClick: (UserModel) -> Unit,
    private val onFollowClick: (UserModel, isFollowing: Boolean) -> Unit
) : ListAdapter<UserModel, FollowUserAdapter.FollowViewHolder>(UserDiffCallback()) {

    // Biến (RẤT QUAN TRỌNG) giữ danh sách ID của user HIỆN TẠI
    private var myFollowingIds: Set<String> = emptySet()

    /**
     * Fragment sẽ gọi hàm này để truyền danh sách ID
     * những người user hiện tại đang follow.
     */
    fun updateMyFollowingIds(followingIds: Set<String>) {
        myFollowingIds = followingIds
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowViewHolder {
        val binding = ItemFollowUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FollowViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FollowViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user)
    }

    inner class FollowViewHolder(private val binding: ItemFollowUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: UserModel) {
            // Bind thông tin cơ bản
            binding.tvFullName.text = user.fullName
            binding.tvUsername.text = user.bio ?: user.email

            Glide.with(binding.root.context)
                .load(user.profilePictureUrl)
                .placeholder(R.drawable.image_avata_user)
                .error(R.drawable.image_avata_user)
                .circleCrop()
                .into(binding.ivAvatar)

            // === LOGIC HIỂN THỊ NÚT BẤM (ĐÃ SỬA) ===

            // 1. Nếu người trong danh sách là TÔI (user hiện tại)
            if (user.userId == currentUserId) {
                binding.btnFollow.visibility = View.GONE // Ẩn nút
            }
            else {
                binding.btnFollow.visibility = View.VISIBLE // Hiện nút

                // 2. Kiểm tra xem TÔI có đang follow người NÀY không
                val isFollowing = myFollowingIds.contains(user.userId)

                val context = binding.root.context
                if (isFollowing) {
                    // 2a. ĐÃ FOLLOW: Hiển thị "Unfollow" (Màu xám)
                    binding.btnFollow.text = "Unfollow"
                    binding.btnFollow.backgroundTintList =
                        ContextCompat.getColorStateList(context, android.R.color.darker_gray)
                } else {
                    // 2b. CHƯA FOLLOW: Hiển thị "Follow" (Màu xanh)
                    binding.btnFollow.text = "Follow"
                    binding.btnFollow.backgroundTintList =
                        ContextCompat.getColorStateList(context, android.R.color.holo_blue_dark)
                }
            }
            // === KẾT THÚC LOGIC NÚT BẤM ===

            // Bắt sự kiện click
            binding.root.setOnClickListener {
                onUserClick(user)
            }
            binding.btnFollow.setOnClickListener {
                // (Chỉ gọi click nếu nút không bị ẩn)
                if (binding.btnFollow.visibility == View.VISIBLE) {
                    val isFollowing = myFollowingIds.contains(user.userId)
                    onFollowClick(user, isFollowing)
                }
            }
        }
    }

    // DiffUtil để ListAdapter hoạt động hiệu quả
    class UserDiffCallback : DiffUtil.ItemCallback<UserModel>() {
        override fun areItemsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem == newItem
        }
    }
}

