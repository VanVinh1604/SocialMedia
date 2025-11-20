package com.example.socialmedia.project.Fragment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.MainActivity
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentChatBinding
import com.example.socialmedia.project.Adapter.ChatAdapter
import com.example.socialmedia.project.Domain.Enum.MessageType
import com.example.socialmedia.project.Domain.Model.MessageModel
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.Utils.ChatCloudinaryHelper
import com.example.socialmedia.project.ViewModel.ChatViewModel
import com.example.socialmedia.project.ViewModel.SharedUserViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.zegocloud.uikit.service.defines.ZegoUIKitUser
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.*

class ChatFragment : Fragment() {

    companion object {
        private const val TAG = "ChatFragment"
    }

    private lateinit var binding: FragmentChatBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var viewModel: ChatViewModel
    private val messages = mutableListOf<MessageModel>()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val sharedUserViewModel: SharedUserViewModel by activityViewModels()

    private var conversationId: String? = null
    private var otherUserId: String? = null
    private var otherUserName: String? = null
    private var otherUserAvatar: String? = null

    // ✅ Cache thông tin user hiện tại
    private var currentUserName: String = "Bạn"
    private var currentUserAvatar: String? = null

    private var isLoadingMore = false
    private var mediaRecorder: MediaRecorder? = null
    private var audioFilePath: String? = null
    private var isRecording = false

    private var editingMessage: MessageModel? = null

    private lateinit var llEditPreview: LinearLayout
    private lateinit var tvEditingMessage: TextView
    private lateinit var ivCancelEdit: ImageView

    private lateinit var llReplyPreview: LinearLayout
    private lateinit var tvReplySender: TextView
    private lateinit var tvReplyContent: TextView
    private lateinit var ivCancelReply: ImageView

    private var replyingMessage: MessageModel? = null


    private var recordingStartTime: Long = 0L
    private var recordingTimer: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())
    val userMap = mutableMapOf<String, UserModel>()

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { sendImageMessage(it) }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.all { it }
        if (granted) startRecording()
        else Toast.makeText(requireContext(), "Ứng dụng cần quyền Micro để ghi âm", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            conversationId = it.getString("conversationId")
            otherUserId = it.getString("userId")
            otherUserName = it.getString("userName")
            otherUserAvatar = it.getString("userAvatar")
        }
        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]

        // ✅ Load thông tin user hiện tại ngay khi khởi tạo
        loadCurrentUserInfo()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ) = FragmentChatBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        chatAdapter = ChatAdapter(
            messages,
            currentUserId,
            userMap,
            onReply = { msg ->
                showReplyPreview(msg)
            },
            onEdit = { msg ->
                onEditMessageSelected(msg) // ✅ gọi hàm show input edit
            },
            onDelete = { msg ->
                conversationId?.let { convId ->
                    viewModel.deleteMessage(convId, msg.messageId) { success ->
                        if (!success) Toast.makeText(requireContext(), "Xoá tin nhắn thất bại", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        val layoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
        binding.rvChat.layoutManager = layoutManager
        binding.rvChat.adapter = chatAdapter

// Observe LiveData
        viewModel.messages.observe(viewLifecycleOwner) { newMessages ->
            messages.clear()
            messages.addAll(newMessages.sortedBy { it.createdAt })
            chatAdapter.updateMessages(messages)
            binding.rvChat.post {
                binding.rvChat.scrollToPosition(messages.size - 1)
            }
        }

        binding.rvChat.adapter = chatAdapter

        llEditPreview = binding.llEditingMessage
        tvEditingMessage = binding.tvEditingContent
        ivCancelEdit = binding.ivCancelEditing

        ivCancelEdit.setOnClickListener {
            cancelEditingMessage()
        }

        llReplyPreview = binding.llReplyPreview
        tvReplySender = binding.tvReplySender
        tvReplyContent = binding.tvReplyContent
        ivCancelReply = binding.ivCancelReply

        ivCancelReply.setOnClickListener {
            cancelReplyMessage()
        }


        initMessages()
        initSendMessage()
        initRecordingButtons()
        setupZegoCallButtonsWithDelay()
        conversationId?.let { convId ->
            viewModel.markMessagesAsRead(convId, currentUserId)
        }

    }

    private fun showReplyPreview(message: MessageModel) {
        replyingMessage = message

        llReplyPreview.visibility = View.VISIBLE
        tvReplySender.text = if (message.senderId == currentUserId) "Bạn" else message.senderName
        tvReplyContent.text = if (message.messageType == MessageType.TEXT)
            message.content
        else
            "[Hình ảnh]"

        binding.etMessage.requestFocus()
    }


    // ✅ Hàm load thông tin user hiện tại từ Firebase
    private fun loadCurrentUserInfo() {
        if (currentUserId.isEmpty()) return

        val userRef = FirebaseDatabase.getInstance()
            .getReference("InfoUser")
            .child(currentUserId)

        userRef.get().addOnSuccessListener { snapshot ->
            currentUserName = snapshot.child("fullName").getValue(String::class.java) ?: "Bạn"
            currentUserAvatar = snapshot.child("profilePictureUrl").getValue(String::class.java)

            Log.d(TAG, "✅ Loaded current user: $currentUserName, avatar: $currentUserAvatar")
        }.addOnFailureListener { e ->
            Log.e(TAG, "❌ Failed to load current user info", e)
        }
    }

    private fun setupUI() {
        binding.tvUserName.text = otherUserName ?: "Người dùng"
        Glide.with(requireContext())
            .load(otherUserAvatar ?: R.drawable.image_avata_user)
            .circleCrop()
            .into(binding.ivAvatar)

        binding.ivBack.setOnClickListener { parentFragmentManager.popBackStack() }

//        chatAdapter = ChatAdapter(messages, currentUserId, mapOf())
        val layoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
        binding.rvChat.layoutManager = layoutManager
//        binding.rvChat.adapter = chatAdapter

        binding.ivFile.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Listen online status
        if (!otherUserId.isNullOrEmpty()) {
            val userRef = FirebaseDatabase.getInstance()
                .getReference("InfoUser").child(otherUserId!!)

            userRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val isOnline = snapshot.child("isOnline").getValue(Boolean::class.java) ?: false
                    val lastLogin = snapshot.child("lastLogin").getValue(Long::class.java)

                    if (isOnline) {
                        binding.tvUserStatus.text = "Đang hoạt động"
                        binding.tvUserStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                    } else {
                        binding.tvUserStatus.text = if (lastLogin != null)
                            com.example.socialmedia.project.Helper.TimeUtils.getTimeAgo(lastLogin)
                        else "Ngoại tuyến"
                        binding.tvUserStatus.setTextColor(android.graphics.Color.parseColor("#888888"))
                    }
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })
        }

    }
    private fun cancelEditingMessage() {
        editingMessage = null
        llEditPreview.visibility = View.GONE
        binding.etMessage.setText("")
    }

    private fun cancelReplyMessage() {
        replyingMessage = null
        llReplyPreview.visibility = View.GONE
    }


    private fun onEditMessageSelected(message: MessageModel) {
        editingMessage = message
        tvEditingMessage.text = message.content
        llEditPreview.visibility = View.VISIBLE
        binding.etMessage.setText(message.content)
        binding.etMessage.requestFocus()
    }


    private fun sendImageMessage(uri: Uri) {
        if (conversationId.isNullOrEmpty()) return

        binding.etMessage.setText("Đang upload ảnh...")
        binding.ivSend.isEnabled = false

        lifecycleScope.launch {
            val uploadedUrl = ChatCloudinaryHelper.uploadImage(uri, requireContext())
            binding.ivSend.isEnabled = true

            if (uploadedUrl.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Upload ảnh thất bại", Toast.LENGTH_SHORT).show()
                binding.etMessage.setText("")
                return@launch
            }

            val message = MessageModel(
                messageId = UUID.randomUUID().toString(),
                conversationId = conversationId!!,
                senderId = currentUserId,
                senderName = currentUserName,
                senderAvatar = currentUserAvatar,
                mediaUrl = uploadedUrl,
                messageType = MessageType.IMAGE,
                createdAt = System.currentTimeMillis()
            )

            val participants = listOf(currentUserId, otherUserId ?: "")

            viewModel.sendMessage(conversationId!!, participants, message) { success ->
                if (success) {
                    binding.etMessage.setText("")
                    Toast.makeText(requireContext(), "Gửi ảnh thành công", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Gửi ảnh thất bại", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initMessages() {
        if (conversationId.isNullOrEmpty()) return
        val layoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
        binding.rvChat.layoutManager = layoutManager

        // ✅ Observe LiveData
        viewModel.messages.observe(viewLifecycleOwner) { newMessages ->
            if (newMessages.isEmpty()) return@observe

            Log.d("ChatFragment", "📩 Received ${newMessages.size} messages from LiveData")
            newMessages.forEachIndexed { index, msg ->
                Log.d("ChatFragment", "  [$index] ${msg.messageId.take(8)}: isDeleted=${msg.isDeleted}, content=${msg.content.take(20)}")
                Log.d("ChatFragment", "  [$index] editHistory=${msg.editHistory}")

            }

            val oldSize = messages.size
            messages.clear()
            messages.addAll(newMessages.sortedBy { it.createdAt })

            // Fetch user info
            messages.forEach { msg ->
                if (!userMap.containsKey(msg.senderId)) {
                    fetchUserInfo(msg.senderId) { user ->
                        user?.let {
                            userMap[msg.senderId] = it
                            chatAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }

            // ✅ Update adapter
            chatAdapter.updateMessages(messages)

            // Auto scroll
            binding.rvChat.post {
                val lastVisible = layoutManager.findLastCompletelyVisibleItemPosition()
                if (lastVisible == oldSize - 1 || oldSize == 0)
                    binding.rvChat.smoothScrollToPosition(messages.size - 1)
            }
        }

        // ✅ Load initial messages (sẽ tự động listen changes)
        viewModel.loadLatestMessages(conversationId!!, 20) {}

        // Scroll listener cho load more
        binding.rvChat.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                if (firstVisible == 0 && messages.isNotEmpty() && !isLoadingMore) {
                    isLoadingMore = true
                    val oldestTimestamp = messages.first().createdAt
                    viewModel.loadMoreMessages(conversationId!!, oldestTimestamp) { _ ->
                        isLoadingMore = false
                    }
                }
            }
        })


    }

    private fun fetchUserInfo(userId: String, callback: (UserModel?) -> Unit) {
        val userRef = FirebaseDatabase.getInstance()
            .getReference("InfoUser").child(userId)
        userRef.get().addOnSuccessListener { snapshot ->
            val name = snapshot.child("fullName").getValue(String::class.java) ?: "Người dùng"
            val avatar = snapshot.child("profilePictureUrl").getValue(String::class.java)
            callback(UserModel(userId = userId, fullName = name, profilePictureUrl = avatar))
        }.addOnFailureListener {
            callback(null)
        }
    }

    private fun initSendMessage() {
        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendTextMessage(); true } else false
        }
        binding.ivSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            binding.ivSend.setOnClickListener {
                val text = binding.etMessage.text.toString().trim()
                if (text.isEmpty()) return@setOnClickListener

                if (editingMessage != null) {
                    val msg = editingMessage!!
                    val updatedMessage = msg.copy(
                        content = text,
                        isEdited = true,
                        editedAt = System.currentTimeMillis(),
                        editHistory = (msg.editHistory.toMutableList().apply { add(msg.content) })
                    )
                    conversationId?.let { convId ->
                        viewModel.editMessage(convId, updatedMessage)
                    }
                    cancelEditingMessage()
                } else {
                    sendTextMessage()
                }
            }
        }
    }

    private fun sendTextMessage() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isEmpty() || conversationId.isNullOrEmpty()) return
        val replyToId = replyingMessage?.messageId

        val message = MessageModel(
            messageId = UUID.randomUUID().toString(),
            conversationId = conversationId!!,
            senderId = currentUserId,
            senderName = currentUserName,
            senderAvatar = currentUserAvatar,
            content = text,
            replyTo = replyToId, // thêm trường replyTo trong MessageModel

            createdAt = System.currentTimeMillis()
        )
        val participants = listOf(currentUserId, otherUserId ?: "")

        viewModel.sendMessage(conversationId!!, participants, message) { success ->
            if (success) {
                binding.etMessage.setText("")
                cancelReplyMessage()
            }
        }
    }

    private fun initRecordingButtons() {
        binding.ivMic.setOnClickListener { checkPermissionsAndRecord() }
        binding.ivCancelRecording.setOnClickListener { cancelRecording() }
        binding.ivSendRecording.setOnClickListener { stopRecordingAndSend() }
    }

    private fun checkPermissionsAndRecord() {
        val micPermission = Manifest.permission.RECORD_AUDIO
        val readAudioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        val requiredPermissions = arrayOf(micPermission, readAudioPermission)

        val hasPermission = requiredPermissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

        if (!hasPermission) {
            AlertDialog.Builder(requireContext())
                .setTitle("Cấp quyền Micro")
                .setMessage("Ứng dụng cần quyền Micro để ghi âm tin nhắn thoại. Bạn có muốn cấp quyền không?")
                .setPositiveButton("Có") { _, _ -> requestPermissionLauncher.launch(requiredPermissions) }
                .setNegativeButton("Không", null)
                .show()
        } else startRecording()
    }

    private fun startRecording() {
        try {
            val context = requireContext()
            if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
                Toast.makeText(context, "Thiết bị không có microphone", Toast.LENGTH_SHORT).show()
                return
            }
            audioFilePath = "${context.externalCacheDir?.absolutePath}/voice_${System.currentTimeMillis()}.m4a"
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFilePath)
                prepare()
                start()
            }
            isRecording = true
            recordingStartTime = System.currentTimeMillis()
            binding.inputBar.visibility = View.GONE
            binding.recordingBar.visibility = View.VISIBLE
            startTimer()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Không thể ghi âm: ${e.message}", Toast.LENGTH_SHORT).show()
            resetRecordingUI()
        }
    }

    private fun stopRecordingAndSend() {
        if (!isRecording) return
        try {
            mediaRecorder?.apply { stop(); release() }
            mediaRecorder = null
            isRecording = false
            stopTimer()
            val file = File(audioFilePath ?: return)
            if (file.length() < 5000) { file.delete(); resetRecordingUI(); return }
            binding.recordingBar.visibility = View.GONE
            binding.inputBar.visibility = View.VISIBLE
            sendVoiceMessage(file.absolutePath, getAudioDuration(requireContext(), file.absolutePath))
        } catch (e: Exception) { resetRecordingUI() }
    }

    private fun cancelRecording() {
        try {
            mediaRecorder?.apply { stop(); release() }
            mediaRecorder = null
            isRecording = false
            stopTimer()
            audioFilePath?.let { File(it).delete() }
            resetRecordingUI()
        } catch (e: Exception) { resetRecordingUI() }
    }

    private fun resetRecordingUI() {
        binding.recordingBar.visibility = View.GONE
        binding.inputBar.visibility = View.VISIBLE
        binding.tvRecordingTime.text = "00:00"
    }

    private fun startTimer() {
        recordingTimer = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - recordingStartTime
                val minutes = (elapsed / 1000 / 60).toInt()
                val seconds = (elapsed / 1000 % 60).toInt()
                binding.tvRecordingTime.text = "%02d:%02d".format(minutes, seconds)
                handler.postDelayed(this, 500)
            }
        }
        handler.post(recordingTimer!!)
    }

    private fun stopTimer() { recordingTimer?.let { handler.removeCallbacks(it) } }

    private fun sendVoiceMessage(filePath: String, duration: String) {
        val file = File(filePath)
        if (!file.exists() || conversationId.isNullOrEmpty()) return
        binding.tvRecordingTime.text = "Uploading..."
        lifecycleScope.launch {
            val uploadedUrl = ChatCloudinaryHelper.uploadVoiceMessage(file)
            if (!uploadedUrl.isNullOrEmpty()) {
                val message = MessageModel(
                    messageId = UUID.randomUUID().toString(),
                    conversationId = conversationId!!,
                    senderId = currentUserId,
                    senderName = currentUserName,
                    senderAvatar = currentUserAvatar,
                    mediaUrl = uploadedUrl,
                    messageType = MessageType.VOICE,
                    duration = duration,
                    createdAt = System.currentTimeMillis()
                )
                val participants = listOf(currentUserId, otherUserId ?: "")

                viewModel.sendMessage(conversationId!!, participants, message){ success ->
                    if (success) { file.delete(); resetRecordingUI() } else resetRecordingUI()
                }
            } else resetRecordingUI()
        }
    }

    private fun getAudioDuration(context: Context, path: String): String {
        return try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(path)
            val durationMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            val minutes = (durationMs / 1000) / 60
            val seconds = (durationMs / 1000) % 60
            "%d:%02d".format(minutes, seconds)
        } catch (e: Exception) { "0:00" }
    }

    private fun setupZegoCallButtonsWithDelay() {
        handler.postDelayed({
            if (MainActivity.isZegoInitialized) {
                setupZegoCallButtons()
            } else {
                Log.e(TAG, "❌ Zego chưa initialized")
                Toast.makeText(requireContext(), "Dịch vụ gọi chưa sẵn sàng", Toast.LENGTH_SHORT).show()
            }
        }, 1000)
    }

    private fun setupZegoCallButtons() {
        if (otherUserId.isNullOrEmpty()) {
            Log.e(TAG, "❌ otherUserId is null/empty")
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        try {
            val inviteeName = otherUserName ?: "Người dùng"
            val invitee = listOf(ZegoUIKitUser(otherUserId!!, inviteeName))

            Log.d(TAG, "✅ Setting up call buttons for: $inviteeName (${otherUserId})")

            binding.btnVoiceCall.apply {
                visibility = View.VISIBLE
                setIsVideoCall(false)
                resourceID = "zego_uikit_call"
                setInvitees(invitee)
            }

            binding.btnVideoCall.apply {
                visibility = View.VISIBLE
                setIsVideoCall(true)
                resourceID = "zego_uikit_call"
                setInvitees(invitee)
            }

            Log.d(TAG, "✅ Call buttons ready for $inviteeName")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setup buttons: ${e.message}", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (isRecording) cancelRecording()
        chatAdapter.releasePlayer()
        viewModel.removeListener()
    }
}