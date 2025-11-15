package com.example.socialmedia.project.Adapter // (Hoặc package Adapter của bạn)

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.ItemBlockedUserBinding
import com.example.socialmedia.project.Domain.Model.UserModel

class BlockedUserAdapter(
    private val onUnblockClicked: (String) -> Unit
) : ListAdapter<UserModel, BlockedUserAdapter.BlockedUserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockedUserViewHolder {
        val binding = ItemBlockedUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BlockedUserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BlockedUserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BlockedUserViewHolder(private val binding: ItemBlockedUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: UserModel) {
            binding.tvUsername.text = user.fullName

            Glide.with(binding.root.context)
                .load(user.profilePictureUrl)
                .placeholder(R.drawable.image_avata_user) // Ảnh placeholder
                .circleCrop()
                .into(binding.ivProfileImage)

            binding.btnUnblock.setOnClickListener {
                onUnblockClicked(user.userId)
            }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<UserModel>() {
        override fun areItemsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem == newItem
        }
    }
}