package com.example.socialmedia.project.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.ItemMessageBinding
import com.example.socialmedia.project.Domain.Model.MessageItem
import com.google.firebase.database.*

class MessageAdapter(
    private var messageList: List<MessageItem>,
    private val onClick: (MessageItem) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(private val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MessageItem) {
            binding.tvName.text = item.name
            binding.tvLastMessage.text = item.lastMessage
            binding.tvTime.text = item.time

            Glide.with(binding.root.context)
                .load(item.userProfileImage ?: R.drawable.image_avata_user)
                .circleCrop()
                .into(binding.ivAvatar)

            if (item.unreadCount > 0) {
                binding.badgeUnread.visibility = View.VISIBLE
                binding.badgeUnread.text = item.unreadCount.toString()
            } else {
                binding.badgeUnread.visibility = View.GONE
            }

            // Click listener: gửi chính xác MessageItem ra ngoài
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        MessageViewHolder(ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) =
        holder.bind(messageList[position])

    override fun getItemCount() = messageList.size

    fun updateList(newList: List<MessageItem>) {
        messageList = newList
        notifyDataSetChanged()
    }
}

