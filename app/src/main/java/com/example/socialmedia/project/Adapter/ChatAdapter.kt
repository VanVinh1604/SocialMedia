package com.example.socialmedia.project.Adapter

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.*
import com.example.socialmedia.project.Domain.Enum.MessageType
import com.example.socialmedia.project.Domain.Model.MessageModel
import com.example.socialmedia.project.Domain.Model.UserModel
import com.google.firebase.auth.FirebaseAuth
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
    private val TYPE_IMAGE_OUTGOING = 5
    private val TYPE_IMAGE_INCOMING = 6

    private val TYPE_STORY_OUTGOING = 7
    private val TYPE_STORY_INCOMING = 8


    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingUrl: String? = null
    private var currentlyPlayingHolder: RecyclerView.ViewHolder? = null

    private fun getValidMessages() = messages.filter { (!it.content.isNullOrBlank() || it.mediaUrl != null) && !it.isDeleted }

    override fun getItemViewType(position: Int): Int {
        val msg = getValidMessages()[position]

        // Xử lý Story Reply
        if (msg.messageType == MessageType.STORY_REPLY) {
            return if (msg.senderId == currentUserId) TYPE_STORY_OUTGOING
            else TYPE_STORY_INCOMING
        }

        return when {
            msg.senderId == currentUserId && msg.messageType == MessageType.VOICE -> TYPE_AUDIO_OUTGOING
            msg.senderId != currentUserId && msg.messageType == MessageType.VOICE -> TYPE_AUDIO_INCOMING
            msg.senderId == currentUserId && msg.messageType == MessageType.IMAGE -> TYPE_IMAGE_OUTGOING
            msg.senderId != currentUserId && msg.messageType == MessageType.IMAGE -> TYPE_IMAGE_INCOMING
            msg.senderId == currentUserId -> TYPE_OUTGOING
            else -> TYPE_INCOMING
        }
    }

    inner class OutgoingStoryViewHolder(
        private val binding: ItemChatStoryOutgoingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: MessageModel, currentUser: UserModel) {
            val story = message.story

            // Nội dung hiển thị
            binding.tvMessage.text = message.content.ifBlank { story?.textOverlay?.text ?: "" }

            // Thời gian gửi
            binding.tvTime.text = SimpleDateFormat("hh:mm a", Locale.getDefault())
                .format(Date(message.createdAt))

            // Ảnh story
            Glide.with(binding.root.context)
                .load(message.storyThumbnail ?: story?.thumbnailUrl ?: R.drawable.image_placeholder)
                .placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_placeholder)
                .into(binding.ivImage)

            // Avatar người gửi (nếu muốn hiển thị)
            Glide.with(binding.root.context)
                .load(currentUser.profilePictureUrl ?: R.drawable.image_avata_user)
                .circleCrop()
                .placeholder(R.drawable.image_avata_user)
                .into(binding.ivAvatar)
        }
    }


    inner class IncomingStoryViewHolder(
        private val binding: ItemChatStoryIncomingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: MessageModel, sender: UserModel) {
            val story = message.story

            // Nội dung hiển thị
            binding.tvMessage.text = message.content.ifBlank { story?.textOverlay?.text ?: "" }

            // Thời gian gửi
            binding.tvTime.text = SimpleDateFormat("hh:mm a", Locale.getDefault())
                .format(Date(message.createdAt))

            // Ảnh story
            Glide.with(binding.root.context)
                .load(message.storyThumbnail ?: story?.thumbnailUrl ?: R.drawable.image_placeholder)
                .placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_placeholder)
                .into(binding.ivImage)

            // Avatar người gửi
            Glide.with(binding.root.context)
                .load(sender.profilePictureUrl ?: message.senderAvatar ?: R.drawable.image_avata_user)
                .circleCrop()
                .placeholder(R.drawable.image_avata_user)
                .into(binding.ivAvatar)
        }
    }


    // --- ViewHolder cho ảnh ---
    inner class OutgoingImageViewHolder(private val binding: ItemChatImageOutgoingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MessageModel, sender: UserModel) {
            item.mediaUrl?.let {
                Glide.with(binding.root.context)
                    .load(it)
                    .placeholder(R.drawable.image_placeholder)
                    .error(R.drawable.background_header)
                    .into(binding.ivImage)
            }
            binding.tvTime.text = SimpleDateFormat("hh a", Locale.getDefault()).format(Date(item.createdAt))

            Glide.with(binding.root.context)
                .load(sender.profilePictureUrl ?: item.senderAvatar)
                .placeholder(R.drawable.image_avata_user)
                .error(R.drawable.image_avata_user)
                .circleCrop()
                .into(binding.ivAvatar)
        }
    }

    inner class IncomingImageViewHolder(private val binding: ItemChatImageIncomingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MessageModel, sender: UserModel) {
            item.mediaUrl?.let {
                Glide.with(binding.root.context)
                    .load(it)
                    .placeholder(R.drawable.image_placeholder)
                    .error(R.drawable.background_header)
                    .into(binding.ivImage)
            }
            binding.tvTime.text = SimpleDateFormat("hh a", Locale.getDefault()).format(Date(item.createdAt))
            Glide.with(binding.root.context)
                .load(sender.profilePictureUrl ?: item.senderAvatar)
                .placeholder(R.drawable.image_avata_user)
                .error(R.drawable.image_avata_user)
                .circleCrop()
                .into(binding.ivAvatar)
        }
    }

    // --- ViewHolder cho audio ---
    inner class OutgoingAudioViewHolder(private val binding: ItemChatAudioOutgoingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MessageModel, sender: UserModel) {
            binding.tvDuration.text = item.duration ?: "0:00"

            Glide.with(binding.root.context)
                .load(sender.profilePictureUrl ?: item.senderAvatar)
                .placeholder(R.drawable.image_avata_user)
                .error(R.drawable.image_avata_user)
                .circleCrop()
                .into(binding.ivAvatar)

            binding.ivPlay.setOnClickListener { item.mediaUrl?.let { url -> playAudio(url, this) } }
        }
        fun updatePlayButton(isPlaying: Boolean) {
            binding.ivPlay.setImageResource(if (isPlaying) R.drawable.ic_stop else R.drawable.ic_play)
        }
    }

    inner class IncomingAudioViewHolder(private val binding: ItemChatAudioIncomingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MessageModel, sender: UserModel) {
            binding.tvDuration.text = item.duration ?: "0:00"

            Glide.with(binding.root.context)
                .load(sender.profilePictureUrl ?: item.senderAvatar)
                .placeholder(R.drawable.image_avata_user)
                .error(R.drawable.image_avata_user)
                .circleCrop()
                .into(binding.ivAvatar)

            binding.ivPlay.setOnClickListener { item.mediaUrl?.let { url -> playAudio(url, this) } }
        }
        fun updatePlayButton(isPlaying: Boolean) {
            binding.ivPlay.setImageResource(if (isPlaying) R.drawable.ic_stop else R.drawable.ic_play)
        }
    }

    // --- ViewHolder cho text ---
    inner class OutgoingViewHolder(private val binding: ItemChatMessageOutgoingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MessageModel, sender: UserModel) {
            binding.tvMessage.text = item.content.trim()
            binding.tvTime.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(item.createdAt))
            Glide.with(binding.root.context)
                .load(sender.profilePictureUrl ?: item.senderAvatar ?: R.drawable.image_avata_user)
                .placeholder(R.drawable.image_avata_user)
                .error(R.drawable.image_avata_user)
                .circleCrop()
                .into(binding.ivAvatar)
        }
    }

    inner class IncomingViewHolder(private val binding: ItemChatMessageIncomingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val defaultAvatar = R.drawable.image_avata_user
        fun bind(item: MessageModel, sender: UserModel) {
            binding.tvMessage.text = item.content.trim()
            binding.tvTime.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(item.createdAt))
            Glide.with(binding.root.context)
                .load(sender.profilePictureUrl ?: item.senderAvatar ?: defaultAvatar)
                .placeholder(defaultAvatar)
                .error(defaultAvatar)
                .circleCrop()
                .into(binding.ivAvatar)
        }
    }

    // --- MediaPlayer ---
    private fun playAudio(url: String, holder: RecyclerView.ViewHolder) {
        Log.d("ChatAdapter", "Attempting to play: $url")
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(url)
                setOnPreparedListener {
                    start()
                    updateHolderPlayButton(holder, true)
                }
                setOnCompletionListener {
                    currentlyPlayingUrl = null
                    updateHolderPlayButton(holder, false)
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("ChatAdapter", "MediaPlayer error: what=$what, extra=$extra")
                    currentlyPlayingUrl = null
                    updateHolderPlayButton(holder, false)
                    true
                }
                prepareAsync()
            } catch (e: Exception) {
                Log.e("ChatAdapter", "Error setting up MediaPlayer", e)
            }
        }
    }

    private fun updateHolderPlayButton(holder: RecyclerView.ViewHolder, isPlaying: Boolean) {
        when (holder) {
            is IncomingAudioViewHolder -> holder.updatePlayButton(isPlaying)
            is OutgoingAudioViewHolder -> holder.updatePlayButton(isPlaying)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_STORY_OUTGOING -> OutgoingStoryViewHolder(ItemChatStoryOutgoingBinding.inflate(inflater, parent, false))
            TYPE_STORY_INCOMING -> IncomingStoryViewHolder(ItemChatStoryIncomingBinding.inflate(inflater, parent, false))
            TYPE_OUTGOING -> OutgoingViewHolder(ItemChatMessageOutgoingBinding.inflate(inflater, parent, false))
            TYPE_INCOMING -> IncomingViewHolder(ItemChatMessageIncomingBinding.inflate(inflater, parent, false))
            TYPE_AUDIO_OUTGOING -> OutgoingAudioViewHolder(ItemChatAudioOutgoingBinding.inflate(inflater, parent, false))
            TYPE_AUDIO_INCOMING -> IncomingAudioViewHolder(ItemChatAudioIncomingBinding.inflate(inflater, parent, false))
            TYPE_IMAGE_OUTGOING -> OutgoingImageViewHolder(ItemChatImageOutgoingBinding.inflate(inflater, parent, false))
            TYPE_IMAGE_INCOMING -> IncomingImageViewHolder(ItemChatImageIncomingBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getValidMessages()[position]
        val sender = if (message.senderId == currentUserId) {
            UserModel(
                userId = currentUserId,
                fullName = "Bạn",
                profilePictureUrl = FirebaseAuth.getInstance().currentUser?.photoUrl?.toString()
            )
        } else {
            userMap[message.senderId] ?: UserModel(userId = message.senderId, fullName = "Người dùng", profilePictureUrl = null)
        }


        when (holder) {
            is OutgoingStoryViewHolder -> holder.bind(message, sender)
            is IncomingStoryViewHolder -> holder.bind(message, sender)
            is OutgoingViewHolder -> holder.bind(message, sender)
            is IncomingViewHolder -> holder.bind(message, sender)
            is OutgoingAudioViewHolder -> holder.bind(message, sender)
            is IncomingAudioViewHolder -> holder.bind(message, sender)
            is OutgoingImageViewHolder -> holder.bind(message, sender)
            is IncomingImageViewHolder -> holder.bind(message, sender)
        }
    }

    override fun getItemCount(): Int = getValidMessages().size

    fun updateMessages(newMessages: List<MessageModel>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    fun releasePlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
        currentlyPlayingUrl = null
        currentlyPlayingHolder = null
    }

}
