package com.example.socialmedia.project.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmedia.R
import com.example.socialmedia.databinding.ItemSearchHistoryBinding
import com.example.socialmedia.databinding.ItemSearchResultBinding
import com.example.socialmedia.project.Domain.Model.UserModel
import com.bumptech.glide.Glide

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
            // Tạo fullName từ firstName và lastName
            val fullName = "${user.firstName} ${user.lastName}".trim()
            binding.textViewUserName.text = fullName.ifEmpty { user.email }
            binding.textViewUserBio.text = user.bio ?: user.email

            // Load avatar với Glide
            Glide.with(binding.root.context)
                .load(user.profilePictureUrl)
                .placeholder(R.drawable.image_avata_user)
                .error(R.drawable.image_avata_user)
                .circleCrop()
                .into(binding.imageViewAvatar)

            binding.root.setOnClickListener {
                onUserClick(user)
            }
        }
    }

    private class SearchDiffCallback : DiffUtil.ItemCallback<UserModel>() {
        override fun areItemsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem.userId == newItem.userId &&
                    oldItem.firstName == newItem.firstName &&
                    oldItem.lastName == newItem.lastName &&
                    oldItem.profilePictureUrl == newItem.profilePictureUrl
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
            // Tạo fullName từ firstName và lastName
            val fullName = "${user.firstName} ${user.lastName}".trim()
            binding.textViewUserName.text = fullName.ifEmpty { user.email }

            // Load avatar với Glide
            Glide.with(binding.root.context)
                .load(user.profilePictureUrl)
                .placeholder(R.drawable.image_avata_user)
                .error(R.drawable.image_avata_user)
                .circleCrop()
                .into(binding.imageViewAvatar)

            binding.root.setOnClickListener {
                onUserClick(user)
            }

            binding.btnRemove.setOnClickListener {
                onRemoveClick(user)
            }
        }
    }

    private class HistoryDiffCallback : DiffUtil.ItemCallback<UserModel>() {
        override fun areItemsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem.userId == newItem.userId &&
                    oldItem.firstName == newItem.firstName &&
                    oldItem.lastName == newItem.lastName
        }
    }
}