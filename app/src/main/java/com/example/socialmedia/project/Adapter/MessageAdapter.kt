package com.example.socialmedia.project.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmedia.databinding.ItemMessageBinding
import com.example.socialmedia.project.Domain.UserTestMess

data class MessageItem(
    val name: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false
)

class MessageAdapter(
    private val messageList: List<MessageItem>,
    private val onClick: ((MessageItem) -> Unit)? = null
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MessageItem) {
            binding.tvName.text = item.name
            binding.tvLastMessage.text = item.lastMessage
            binding.tvTime.text = item.time

            if (item.unreadCount > 0) {
                binding.badgeUnread.text = item.unreadCount.toString()
                binding.badgeUnread.visibility = android.view.View.VISIBLE
            } else {
                binding.badgeUnread.visibility = android.view.View.GONE
            }

            // Optionally change avatar background for online status
            // binding.ivAvatar.background = if (item.isOnline) ... else ...

            binding.root.setOnClickListener {
                onClick?.invoke(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messageList[position])
    }

    override fun getItemCount(): Int = messageList.size
}
