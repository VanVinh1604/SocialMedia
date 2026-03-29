package com.example.socialmedia.project.Adapter

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmedia.R
import com.example.socialmedia.databinding.ItemSearchHistoryBinding
import com.example.socialmedia.databinding.ItemSearchResultBinding
import com.example.socialmedia.project.Domain.Model.UserModel
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth

// Adapter cho kết quả tìm kiếm
class SearchAdapter(
    private val onUserClick: (UserModel) -> Unit
) : ListAdapter<UserModel, SearchAdapter.SearchViewHolder>(SearchDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val binding = ItemSearchResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SearchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SearchViewHolder(
        private val binding: ItemSearchResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: UserModel) {
            // ✅ FIX: Lấy chính xác fullName từ UserModel
            val displayName = when {
                // Ưu tiên 1: fullName từ Firebase
                user.fullName.isNotBlank() && user.fullName != " " -> user.fullName.trim()

                // Ưu tiên 2: firstName + lastName
                user.firstName.isNotBlank() || user.lastName.isNotBlank() -> {
                    "${user.firstName} ${user.lastName}".trim()
                }

                // Ưu tiên 3: Phần đầu của email (trước @)
                user.email.isNotBlank() -> user.email.substringBefore("@")

                // Mặc định: "Người dùng"
                else -> "Người dùng"
            }

            binding.textViewUserName.text = displayName

            // Bio hoặc email làm dòng phụ
            binding.textViewUserBio.text = when {
                !user.bio.isNullOrBlank() -> user.bio
                user.email.isNotBlank() -> user.email
                else -> "Chưa có thông tin"
            }

            // Load avatar với Glide
            Glide.with(binding.root.context)
                .load(user.profilePictureUrl)
                .placeholder(R.drawable.image_avata_user)
                .error(R.drawable.image_avata_user)
                .circleCrop()
                .into(binding.imageViewAvatar)

            binding.root.setOnClickListener {
                onUserClick(user)
                navigateToProfile(user.userId)
            }
        }

        private fun navigateToProfile(targetUserId: String) {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

            if (currentUserId == null) {
                Toast.makeText(binding.root.context, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show()
                return
            }

            try {
                val navController = Navigation.findNavController(binding.root)

                if (targetUserId == currentUserId) {
                    navController.navigate(R.id.personalProfileFragment)
                    Log.d("SearchAdapter", "Navigating to PersonalProfileFragment")
                } else {
                    val bundle = Bundle().apply {
                        putString("userId", targetUserId)
                    }
                    navController.navigate(R.id.action_searchFragment_to_profileFragment, bundle)
                    Log.d("SearchAdapter", "Navigating to ProfileFragment with userId: $targetUserId")
                }
            } catch (e: Exception) {
                Log.e("SearchAdapter", "Navigation error: ${e.message}", e)
                Toast.makeText(binding.root.context, "Không thể mở trang cá nhân", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private class SearchDiffCallback : DiffUtil.ItemCallback<UserModel>() {
        override fun areItemsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem.userId == newItem.userId &&
                    oldItem.fullName == newItem.fullName &&
                    oldItem.firstName == newItem.firstName &&
                    oldItem.lastName == newItem.lastName &&
                    oldItem.profilePictureUrl == newItem.profilePictureUrl &&
                    oldItem.bio == newItem.bio
        }
    }
}

// Adapter cho lịch sử tìm kiếm
class SearchHistoryAdapter(
    private val onUserClick: (UserModel) -> Unit,
    private val onRemoveClick: (UserModel) -> Unit
) : ListAdapter<UserModel, SearchHistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemSearchHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HistoryViewHolder(
        private val binding: ItemSearchHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: UserModel) {
            // ✅ FIX: Lấy chính xác fullName từ UserModel
            val displayName = when {
                // Ưu tiên 1: fullName từ Firebase
                user.fullName.isNotBlank() && user.fullName != " " -> user.fullName.trim()

                // Ưu tiên 2: firstName + lastName
                user.firstName.isNotBlank() || user.lastName.isNotBlank() -> {
                    "${user.firstName} ${user.lastName}".trim()
                }

                // Ưu tiên 3: Phần đầu của email (trước @)
                user.email.isNotBlank() -> user.email.substringBefore("@")

                // Mặc định: "Người dùng"
                else -> "Người dùng"
            }

            binding.textViewUserName.text = displayName

            // Load avatar với Glide
            Glide.with(binding.root.context)
                .load(user.profilePictureUrl)
                .placeholder(R.drawable.image_avata_user)
                .error(R.drawable.image_avata_user)
                .circleCrop()
                .into(binding.imageViewAvatar)

            binding.root.setOnClickListener {
                onUserClick(user)
                navigateToProfile(user.userId)
            }

            binding.btnRemove.setOnClickListener {
                onRemoveClick(user)
            }
        }

        private fun navigateToProfile(targetUserId: String) {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

            if (currentUserId == null) {
                Toast.makeText(binding.root.context, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show()
                return
            }

            try {
                val navController = Navigation.findNavController(binding.root)

                if (targetUserId == currentUserId) {
                    navController.navigate(R.id.personalProfileFragment)
                    Log.d("SearchHistoryAdapter", "Navigating to PersonalProfileFragment")
                } else {
                    val bundle = Bundle().apply {
                        putString("userId", targetUserId)
                    }
                    navController.navigate(R.id.action_searchFragment_to_profileFragment, bundle)
                    Log.d("SearchHistoryAdapter", "Navigating to ProfileFragment with userId: $targetUserId")
                }
            } catch (e: Exception) {
                Log.e("SearchHistoryAdapter", "Navigation error: ${e.message}", e)
                Toast.makeText(binding.root.context, "Không thể mở trang cá nhân", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private class HistoryDiffCallback : DiffUtil.ItemCallback<UserModel>() {
        override fun areItemsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem.userId == newItem.userId &&
                    oldItem.fullName == newItem.fullName &&
                    oldItem.firstName == newItem.firstName &&
                    oldItem.lastName == newItem.lastName &&
                    oldItem.profilePictureUrl == newItem.profilePictureUrl
        }
    }
}