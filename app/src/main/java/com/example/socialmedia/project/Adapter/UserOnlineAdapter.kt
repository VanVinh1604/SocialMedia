package com.example.socialmedia.project.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.ItemUserOnlineBinding
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.Helper.TimeUtils.getTimeAgo
import com.google.firebase.database.*

class UserOnlineAdapter(
    private val users: MutableList<UserModel>,
    private val onClick: (UserModel) -> Unit = {}
) : RecyclerView.Adapter<UserOnlineAdapter.UserViewHolder>() {

    inner class UserViewHolder(private val binding: ItemUserOnlineBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var userRef: DatabaseReference? = null
        private var listener: ValueEventListener? = null

        fun bind(user: UserModel) {
            binding.tvUserName.text = user.fullName.ifBlank { "Ẩn danh" }

            // Remove old listener nếu có
            userRef?.removeEventListener(listener ?: return)

            userRef = FirebaseDatabase.getInstance().getReference("InfoUser").child(user.userId)
            listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val profileUrl = snapshot.child("profilePictureUrl").getValue(String::class.java)
                    val isOnline = snapshot.child("isOnline").getValue(Boolean::class.java) ?: false
                    val lastLogin = snapshot.child("lastLogin").getValue(Long::class.java)

                    // Update avatar
                    Glide.with(binding.root.context)
                        .load(profileUrl ?: user.profilePictureUrl ?: R.drawable.image_avata_user)
                        .placeholder(R.drawable.image_avata_user)
                        .error(R.drawable.image_avata_user)
                        .circleCrop()
                        .into(binding.ivAvatar)

                    // Update status online/offline
                    if (isOnline) {
                        binding.statusIndicator.setBackgroundResource(R.drawable.circle_status_online)
                        binding.tvLastActive.text = "Đang hoạt động"
                    } else {
                        binding.statusIndicator.setBackgroundResource(R.drawable.circle_status_offline)
                        binding.tvLastActive.text =
                            if (lastLogin != null) getTimeAgo(lastLogin) else "Ngoại tuyến"
                    }

                    // Update internal model
                    val index = users.indexOfFirst { it.userId == user.userId }
                    if (index != -1) {
                        users[index] = users[index].copy(
                            isOnline = isOnline,
                            profilePictureUrl = profileUrl ?: users[index].profilePictureUrl
                        )
                        notifyItemChanged(index)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }

            userRef?.addValueEventListener(listener!!)

            binding.root.setOnClickListener { onClick(user) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserOnlineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    fun updateList(newList: List<UserModel>) {
        users.clear()
        users.addAll(newList)
        notifyDataSetChanged()
    }
}
