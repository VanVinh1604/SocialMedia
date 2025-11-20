package com.example.socialmedia.project.Adapter

import android.R.attr.fragment
import android.content.Context
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.*
import com.example.socialmedia.project.Domain.Enum.MessageType
import com.example.socialmedia.project.Domain.Model.MessageModel
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.Fragment.ChatMessageBottomSheet
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private var messages: List<MessageModel>,
    private val currentUserId: String,
    private val userMap: Map<String, UserModel>,
    private val onMessageClick: ((message: MessageModel) -> Unit)? = null, // ✅ callback
    private val onReply: ((MessageModel) -> Unit)? = null,
    private val onEdit: ((MessageModel) -> Unit)? = null,
    private val onDelete: ((MessageModel) -> Unit)? = null

) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    private val TYPE_OUTGOING = 1
    private val TYPE_INCOMING = 2
    private val TYPE_AUDIO_OUTGOING = 3
    private val TYPE_AUDIO_INCOMING = 4
    private val TYPE_IMAGE_OUTGOING = 5
    private val TYPE_IMAGE_INCOMING = 6

    private val TYPE_STORY_OUTGOING = 7
    private val TYPE_STORY_INCOMING = 8

    private val TYPE_DELETED_OUTGOING = 100
    private val TYPE_DELETED_INCOMING = 101


    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingUrl: String? = null
    private var currentlyPlayingHolder: RecyclerView.ViewHolder? = null

    private fun getValidMessages() = messages.filter { (!it.content.isNullOrBlank() || it.mediaUrl != null) && !it.isDeleted }

    override fun getItemViewType(position: Int): Int {
        val msg = messages[position]

        // ===== 🎯 ƯU TIÊN XỬ LÝ MESSAGE ĐÃ XOÁ =====
        if (msg.isDeleted) {
            return if (msg.senderId == currentUserId)
                TYPE_DELETED_OUTGOING
            else
                TYPE_DELETED_INCOMING
        }

        // ===== story =====
        if (msg.messageType == MessageType.STORY_REPLY) {
            return if (msg.senderId == currentUserId) TYPE_STORY_OUTGOING
            else TYPE_STORY_INCOMING
        }

        // ===== audio =====
        if (msg.messageType == MessageType.VOICE) {
            return if (msg.senderId == currentUserId) TYPE_AUDIO_OUTGOING
            else TYPE_AUDIO_INCOMING
        }

        // ===== image =====
        if (msg.messageType == MessageType.IMAGE) {
            return if (msg.senderId == currentUserId) TYPE_IMAGE_OUTGOING
            else TYPE_IMAGE_INCOMING
        }

        // ===== text =====
        return if (msg.senderId == currentUserId) TYPE_OUTGOING
        else TYPE_INCOMING
    }


    inner class DeletedOutgoingHolder(private val binding: ItemChatDeletedOutgoingBinding)
        : RecyclerView.ViewHolder(binding.root){
        fun getContainer(): View = binding.root

        fun bind(msg: MessageModel, sender: UserModel) {

//        // Load avatar đúng người gửi
//        Glide.with(binding.root.context)
//            .load(sender.profilePictureUrl ?: msg.senderAvatar)
//            .placeholder(R.drawable.image_avata_user)
//            .error(R.drawable.image_avata_user)
//            .circleCrop()
//            .into(binding.ivAvatar)

        binding.tvDeleted.text = "Tin nhắn đã bị thu hồi"
    }}



    inner class DeletedIncomingHolder(private val binding: ItemChatDeletedIncomingBinding)
        : RecyclerView.ViewHolder(binding.root){
        fun getContainer(): View = binding.root


        fun bind(msg: MessageModel, sender: UserModel) {

            // Load avatar đúng người gửi
            Glide.with(binding.root.context)
                .load(sender.profilePictureUrl ?: msg.senderAvatar)
                .placeholder(R.drawable.image_avata_user)
                .error(R.drawable.image_avata_user)
                .circleCrop()
                .into(binding.ivAvatar)

            binding.tvDeleted.text = "Tin nhắn đã bị thu hồi"
        }
    }


    inner class OutgoingStoryViewHolder(
        private val binding: ItemChatStoryOutgoingBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun getContainer(): View = binding.root


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
        }
    }


    inner class IncomingStoryViewHolder(
        private val binding: ItemChatStoryIncomingBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun getContainer(): View = binding.root


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
        fun getContainer(): View = binding.root

        fun bind(item: MessageModel, sender: UserModel) {
            item.mediaUrl?.let {
                Glide.with(binding.root.context)
                    .load(it)
                    .placeholder(R.drawable.image_placeholder)
                    .error(R.drawable.background_header)
                    .into(binding.ivImage)
            }
            binding.tvTime.text = SimpleDateFormat("hh a", Locale.getDefault()).format(Date(item.createdAt))
        }
    }

    inner class IncomingImageViewHolder(private val binding: ItemChatImageIncomingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun getContainer(): View = binding.root

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
        fun getContainer(): View = binding.root

        fun bind(item: MessageModel, sender: UserModel) {
            binding.tvDuration.text = item.duration ?: "0:00"


            binding.ivPlay.setOnClickListener { item.mediaUrl?.let { url -> playAudio(url, this) } }
        }
        fun updatePlayButton(isPlaying: Boolean) {
            binding.ivPlay.setImageResource(if (isPlaying) R.drawable.ic_stop else R.drawable.ic_play)
        }
    }

    inner class IncomingAudioViewHolder(private val binding: ItemChatAudioIncomingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun getContainer(): View = binding.root

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


    // --- Trong OutgoingViewHolder ---
    inner class OutgoingViewHolder(private val binding: ItemChatMessageOutgoingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun getContainer(): View = binding.root


        fun bind(item: MessageModel, sender: UserModel) {
            // Hiển thị bình thường
            binding.tvMessage.text = item.content.trim()
            binding.tvTime.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(item.createdAt))
            binding.tvEdited.visibility = if (item.isEdited) View.VISIBLE else View.GONE

            bindReply(item, this)
            bindHistory(item)

            // Long click để reply
            itemView.setOnLongClickListener { onReply?.invoke(item); true }
        }

        private fun bindReply(message: MessageModel, holder: RecyclerView.ViewHolder) {
            val replyLayoutField = holder.itemView.findViewById<LinearLayout>(R.id.layoutReply)
            val replySenderField = holder.itemView.findViewById<TextView>(R.id.tvReplyUser)
            val replyContentField = holder.itemView.findViewById<TextView>(R.id.tvReplyContent)

            if (message.replyTo != null) {
                val repliedMsgIndex = messages.indexOfFirst { it.messageId == message.replyTo }
                val repliedMsg = messages.getOrNull(repliedMsgIndex)

                if (repliedMsg != null) {
                    replyLayoutField.visibility = View.VISIBLE
                    replySenderField.text = repliedMsg.senderName ?: "Người dùng"
                    replyContentField.text = when (repliedMsg.messageType) {
                        MessageType.TEXT -> repliedMsg.content
                        MessageType.IMAGE -> "[Hình ảnh]"
                        MessageType.VOICE -> "[Tin nhắn âm thanh]"
                        else -> "[Tin nhắn]"
                    }
                    replyLayoutField.setOnClickListener {
                        if (repliedMsgIndex != -1) {
//                            val recycler = holder.itemView.parent as? RecyclerView
//                            recycler?.scrollToPosition(repliedMsgIndex)
//
//                            // highlight tin nhắn
//                            recycler?.findViewHolderForAdapterPosition(repliedMsgIndex)?.let { vh ->
//                                highlightMessage(vh)
//                            }
                            val recycler = holder.itemView.parent as? RecyclerView
                            recycler?.let { rv ->
                                rv.post {
                                    rv.smoothScrollToPosition(repliedMsgIndex)
                                    rv.postDelayed({
                                        rv.findViewHolderForAdapterPosition(repliedMsgIndex)?.let { vh ->
                                            highlightMessage(vh)
                                        }
                                    }, 200) // delay cho scroll hoàn tất
                                }
                            }

                        }
                    }

                } else {
                    replyLayoutField.visibility = View.GONE
                }
            } else {
                replyLayoutField.visibility = View.GONE
            }
        }

        private fun bindHistory(item: MessageModel) {
            binding.layoutHistory.visibility = View.GONE
            binding.layoutHistory.removeAllViews()
            item.editHistory?.forEach { oldText ->
                val tv = TextView(binding.root.context).apply {
                    text = oldText
                    textSize = 13f
                    setTextColor(Color.parseColor("#555555"))
                    setPadding(12, 8, 12, 8)
                    background = ContextCompat.getDrawable(context, R.drawable.bg_edit_history)
                    maxWidth = 260
                }
                binding.layoutHistory.addView(tv)
            }
            binding.tvMessage.setOnClickListener {
                binding.layoutHistory.visibility = if (binding.layoutHistory.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
        }
    }

    // --- IncomingViewHolder tương tự ---
    inner class IncomingViewHolder(private val binding: ItemChatMessageIncomingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun getContainer(): View = binding.root


        fun bind(item: MessageModel, sender: UserModel) {
            binding.tvMessage.text = item.content.trim()
            binding.tvTime.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(item.createdAt))
            Glide.with(binding.root.context)
                .load(sender.profilePictureUrl ?: item.senderAvatar ?: R.drawable.image_avata_user)
                .circleCrop()
                .into(binding.ivAvatar)
            binding.tvEdited.visibility = if (item.isEdited) View.VISIBLE else View.GONE

            bindReply(item, this)
            bindHistory(item)

            itemView.setOnLongClickListener { onReply?.invoke(item); true }
        }

        private fun bindReply(message: MessageModel, holder: RecyclerView.ViewHolder) {
            val replyLayoutField = holder.itemView.findViewById<LinearLayout>(R.id.layoutReply)
            val replySenderField = holder.itemView.findViewById<TextView>(R.id.tvReplyUser)
            val replyContentField = holder.itemView.findViewById<TextView>(R.id.tvReplyContent)

            if (message.replyTo != null) {
                val repliedMsgIndex = messages.indexOfFirst { it.messageId == message.replyTo }
                val repliedMsg = messages.getOrNull(repliedMsgIndex)

                if (repliedMsg != null) {
                    replyLayoutField.visibility = View.VISIBLE
                    replySenderField.text = repliedMsg.senderName ?: "Người dùng"
                    replyContentField.text = when (repliedMsg.messageType) {
                        MessageType.TEXT -> repliedMsg.content
                        MessageType.IMAGE -> "[Hình ảnh]"
                        MessageType.VOICE -> "[Tin nhắn âm thanh]"
                        else -> "[Tin nhắn]"
                    }

                    // Khi bấm vào layoutReply -> scroll tới tin nhắn được reply và highlight
                    replyLayoutField.setOnClickListener {
                        if (repliedMsgIndex != -1) {

                            val recycler = holder.itemView.parent as? RecyclerView
                            recycler?.let { rv ->
                                rv.post {
                                    rv.smoothScrollToPosition(repliedMsgIndex)
                                    rv.postDelayed({
                                        rv.findViewHolderForAdapterPosition(repliedMsgIndex)?.let { vh ->
                                            highlightMessage(vh)
                                        }
                                    }, 200) // delay cho scroll hoàn tất
                                }
                            }

                        }
                    }
                } else {
                    replyLayoutField.visibility = View.GONE
                }
            } else {
                replyLayoutField.visibility = View.GONE
            }
        }


        private fun bindHistory(item: MessageModel) {
            binding.layoutHistory.visibility = View.GONE
            binding.layoutHistory.removeAllViews()
            item.editHistory?.forEach { oldText ->
                val tv = TextView(binding.root.context).apply {
                    text = oldText
                    textSize = 13f
                    setTextColor(Color.parseColor("#555555"))
                    setPadding(12, 8, 12, 8)
                    background = ContextCompat.getDrawable(context, R.drawable.bg_edit_history)
                    maxWidth = 260
                }
                binding.layoutHistory.addView(tv)
            }
            binding.tvMessage.setOnClickListener {
                binding.layoutHistory.visibility = if (binding.layoutHistory.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
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
            TYPE_DELETED_OUTGOING ->
                DeletedOutgoingHolder(ItemChatDeletedOutgoingBinding.inflate(inflater, parent, false))

            TYPE_DELETED_INCOMING ->
                DeletedIncomingHolder(ItemChatDeletedIncomingBinding.inflate(inflater, parent, false))
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
        val msg = messages[position]

        if (msg.isDeleted) {

            val sender = if (msg.senderId == currentUserId)
                UserModel(currentUserId, "Bạn", null)
            else
                userMap[msg.senderId] ?: UserModel(msg.senderId, "Người dùng", null)

            when (holder) {
                is DeletedOutgoingHolder -> holder.bind(msg,sender)
                is DeletedIncomingHolder -> holder.bind(msg, sender)
            }

            // Tắt long click cho message đã xoá
            holder.itemView.setOnLongClickListener { true }
            return
        }

        if (msg.replyTo != null) {
            val repliedMsg = messages.find { it.messageId == msg.replyTo }

            if (repliedMsg != null) {
                // Kiểm tra holder có replyLayout không
                val replyLayoutField = holder.itemView.findViewById<LinearLayout>(R.id.llReplyPreview)
                val replySenderField = holder.itemView.findViewById<TextView>(R.id.tvReplySender)
                val replyContentField = holder.itemView.findViewById<TextView>(R.id.tvReplyContent)

                replyLayoutField?.visibility = View.VISIBLE
                replySenderField?.text = repliedMsg.senderName ?: "Người dùng"
                replyContentField?.text = when (repliedMsg.messageType) {
                    MessageType.TEXT -> repliedMsg.content
                    MessageType.IMAGE -> "[Hình ảnh]"
                    else -> "[Tin nhắn]"
                }
            } else {
                holder.itemView.findViewById<LinearLayout>(R.id.llReplyPreview)?.visibility = View.GONE
            }
        } else {
            holder.itemView.findViewById<LinearLayout>(R.id.llReplyPreview)?.visibility = View.GONE
        }

        // Bind bình thường các loại message
        val sender = if (msg.senderId == currentUserId) {
            UserModel(currentUserId, "Bạn", null)
        } else {
            userMap[msg.senderId] ?: UserModel(msg.senderId, "Người dùng", null)
        }

        when (holder) {
            is OutgoingViewHolder -> holder.bind(msg, sender)
            is IncomingViewHolder -> holder.bind(msg, sender)
            is OutgoingAudioViewHolder -> holder.bind(msg, sender)
            is IncomingAudioViewHolder -> holder.bind(msg, sender)
            is OutgoingImageViewHolder -> holder.bind(msg, sender)
            is IncomingImageViewHolder -> holder.bind(msg, sender)
            is OutgoingStoryViewHolder -> holder.bind(msg, sender)
            is IncomingStoryViewHolder -> holder.bind(msg, sender)
        }

        holder.itemView.setOnLongClickListener {
            val bottomSheet = ChatMessageBottomSheet(
                message = msg,
                onReply = { onReply?.invoke(it) },
                onEdit = { onEdit?.invoke(it) }, // ✅ chỉ gọi callback fragment
                onDelete = { onDelete?.invoke(it) }
            )
            bottomSheet.show(
                (holder.itemView.context as FragmentActivity).supportFragmentManager,
                "ChatMessageOptions"
            )
            true
        }

    }

    private fun highlightMessage(holder: RecyclerView.ViewHolder) {
        // Lấy container của holder
        val container: View = when (holder) {
            is OutgoingViewHolder -> holder.getContainer()
            is IncomingViewHolder -> holder.getContainer()
            is OutgoingImageViewHolder -> holder.getContainer()
            is IncomingImageViewHolder -> holder.getContainer()
            is OutgoingAudioViewHolder -> holder.getContainer()
            is IncomingAudioViewHolder -> holder.getContainer()
            is OutgoingStoryViewHolder -> holder.getContainer()
            is IncomingStoryViewHolder -> holder.getContainer()
            is DeletedOutgoingHolder -> holder.getContainer()
            is DeletedIncomingHolder -> holder.getContainer()
            else -> holder.itemView
        }

        // Set viền hoặc background highlight
        container.setBackgroundResource(R.drawable.bg_highlight_message)

        // Sau 1.5s bỏ highlight
        container.postDelayed({
            container.setBackgroundColor(Color.TRANSPARENT)
        }, 1500)
    }

    override fun getItemCount(): Int = messages.size
    fun updateMessages(newMessages: List<MessageModel>) {
        messages = newMessages.map { it.copy() } // tạo copy để tránh tham chiếu cũ
        notifyDataSetChanged()
    }

    fun releasePlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
        currentlyPlayingUrl = null
        currentlyPlayingHolder = null
    }

}
