package com.example.socialmedia.project.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.ItemChatAudioIncomingBinding
import com.example.socialmedia.databinding.ItemChatAudioOutgoingBinding
import com.example.socialmedia.databinding.ItemChatMessageIncomingBinding
import com.example.socialmedia.databinding.ItemChatMessageOutgoingBinding
import com.example.socialmedia.project.Domain.Enum.MessageType
import com.example.socialmedia.project.Domain.Model.MessageModel
import com.example.socialmedia.project.Domain.Model.UserModel
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private var messages: List<MessageModel>,
    private val currentUserId: String,
    private val userMap: Map<String, UserModel>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_OUTGOING = 1
    private val TYPE_INCOMING = 2
    private val TYPE_AUDIO_OUTGOING = 3
    private val TYPE_AUDIO_INCOMING = 4


    // ✅ Hàm lọc động – chỉ lấy tin nhắn có nội dung hợp lệ
    private fun getValidMessages(): List<MessageModel> {
        return messages.filter {
            (!it.content.isNullOrBlank() || it.mediaUrl != null) && !it.isDeleted
        }
    }

    override fun getItemViewType(position: Int): Int {
        val validMessages = getValidMessages()
        val msg = validMessages[position]

        return when {
            msg.senderId == currentUserId && msg.messageType == MessageType.VOICE -> TYPE_AUDIO_OUTGOING
            msg.senderId != currentUserId && msg.messageType == MessageType.VOICE -> TYPE_AUDIO_INCOMING
            msg.senderId == currentUserId -> TYPE_OUTGOING
            else -> TYPE_INCOMING
        }
    }

    inner class OutgoingAudioViewHolder(private val binding: ItemChatAudioOutgoingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MessageModel) {
            binding.tvDuration.text = "0:10" // hoặc lấy duration từ metadata audio
            binding.ivPlay.setOnClickListener {
                item.mediaUrl?.let { playAudio(it) }
            }
        }
    }

    inner class IncomingAudioViewHolder(private val binding: ItemChatAudioIncomingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MessageModel) {
            binding.tvDuration.text = "0:10"
            val avatarUrl = userMap[item.senderId]?.profilePictureUrl ?: item.senderAvatar
            Glide.with(binding.root.context)
                .load(avatarUrl)
                .circleCrop()
                .into(binding.ivAvatar)
            binding.ivPlay.setOnClickListener {
                item.mediaUrl?.let { playAudio(it) }
            }
        }
    }

    inner class OutgoingViewHolder(private val binding: ItemChatMessageOutgoingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MessageModel) {
            binding.tvMessage.text = item.content.trim()
            binding.tvTime.text = SimpleDateFormat("hh:mm a", Locale.getDefault())
                .format(Date(item.createdAt))
        }
    }

    inner class IncomingViewHolder(private val binding: ItemChatMessageIncomingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val defaultAvatar = R.drawable.image_avata_user

        fun bind(item: MessageModel) {
            binding.tvMessage.text = item.content.trim()
            binding.tvTime.text = SimpleDateFormat("hh:mm a", Locale.getDefault())
                .format(Date(item.createdAt))

            val avatarUrl = userMap[item.senderId]?.profilePictureUrl ?: item.senderAvatar
            Glide.with(binding.root.context)
                .load(avatarUrl)
                .placeholder(defaultAvatar)
                .error(defaultAvatar)
                .circleCrop()
                .into(binding.ivAvatar)
        }
    }

    private fun playAudio(url: String?) {
        if (url.isNullOrEmpty()) return

        // Dùng MediaPlayer đơn giản
        val mediaPlayer = android.media.MediaPlayer()
        try {
            mediaPlayer.setDataSource(url)
            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when(viewType) {
            TYPE_OUTGOING -> OutgoingViewHolder(ItemChatMessageOutgoingBinding.inflate(inflater, parent, false))
            TYPE_INCOMING -> IncomingViewHolder(ItemChatMessageIncomingBinding.inflate(inflater, parent, false))
            TYPE_AUDIO_OUTGOING -> OutgoingAudioViewHolder(ItemChatAudioOutgoingBinding.inflate(inflater, parent, false))
            TYPE_AUDIO_INCOMING -> IncomingAudioViewHolder(ItemChatAudioIncomingBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Unknown type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getValidMessages()[position]
        when(holder) {
            is OutgoingViewHolder -> holder.bind(message)
            is IncomingViewHolder -> holder.bind(message)
            is OutgoingAudioViewHolder -> holder.bind(message)
            is IncomingAudioViewHolder -> holder.bind(message)
        }
    }


    override fun getItemCount(): Int = getValidMessages().size

    // ✅ Hàm cập nhật dữ liệu khi Firebase load mới
    fun updateMessages(newMessages: List<MessageModel>) {
        this.messages = newMessages
        notifyDataSetChanged()
    }
}
