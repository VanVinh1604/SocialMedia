package com.example.socialmedia.project.Adapter

import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.ItemMessageBinding
import com.example.socialmedia.project.Domain.Model.ConversationModel
import com.example.socialmedia.project.ViewModel.ConversationViewModel

class MessageAdapter(
    private var conversationList: List<ConversationModel>,
    private val currentUserId: String,
    private val conversationViewModel: ConversationViewModel,
    private val onClick: (ConversationModel) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(private val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(conv: ConversationModel) {
            binding.tvName.text = conv.name ?: "Người dùng"

            Glide.with(binding.root.context)
                .load(conv.photoUrl ?: R.drawable.image_avata_user)
                .circleCrop()
                .placeholder(R.drawable.image_avata_user)
                .error(R.drawable.image_avata_user)
                .into(binding.ivAvatar)

            // Tạo preview tin nhắn
            val preview = buildPreviewText(conv)
            binding.tvLastMessage.text = preview
            binding.tvTime.text = getRelativeTime(conv.lastMessageAt)

            // --- Tính unread ---
            val unread = conv.unreadCount[currentUserId] ?: 0
            if (unread > 0) {
                binding.badgeUnread.visibility = View.VISIBLE
                binding.badgeUnread.text = if (unread > 99) "99+" else unread.toString()

                // 🔸 Làm in đậm khi có tin chưa đọc
                binding.tvName.setTypeface(null, Typeface.BOLD)
                binding.tvLastMessage.setTypeface(null, Typeface.BOLD)
            } else {
                binding.badgeUnread.visibility = View.GONE
                binding.tvName.setTypeface(null, Typeface.NORMAL)
                binding.tvLastMessage.setTypeface(null, Typeface.NORMAL)
            }

            // Click vào sẽ đánh dấu đọc
            binding.root.setOnClickListener {
                conversationViewModel.markConversationAsRead(conv.conversationId, currentUserId)
                onClick(conv)
            }
        }

        private fun buildPreviewText(conv: ConversationModel): String {
            val preview = conv.lastMessagePreview ?: ""
            val senderId = conv.lastMessageSenderId ?: ""
            return when {
                preview.isEmpty() -> "Bắt đầu cuộc trò chuyện"
                senderId == currentUserId -> "Bạn: $preview"
                else -> preview
            }
        }

        private fun getRelativeTime(timestamp: Long?): String {
            if (timestamp == null) return ""
            val diff = System.currentTimeMillis() - timestamp
            val minutes = diff / 60000
            val hours = minutes / 60
            val days = hours / 24
            return when {
                minutes < 1 -> "Vừa xong"
                minutes < 60 -> "${minutes}m"
                hours < 24 -> "${hours}h"
                days < 7 -> "${days}d"
                else -> "${days / 7}w"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        MessageViewHolder(ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) =
        holder.bind(conversationList[position])

    override fun getItemCount() = conversationList.size

    fun updateList(newList: List<ConversationModel>) {
        conversationList = newList.sortedByDescending { it.lastMessageAt ?: 0 }
        notifyDataSetChanged()
    }

    // ✅ Hàm đếm tổng tin chưa đọc
    fun getTotalUnreadCount(): Long {
        return conversationList.sumOf { it.unreadCount[currentUserId] ?: 0 }
    }
}
