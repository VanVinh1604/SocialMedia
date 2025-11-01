// ChatFragment.kt
package com.example.socialmedia.project.Fragment

import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentChatBinding
import com.example.socialmedia.project.Adapter.ChatAdapter
import com.example.socialmedia.project.Domain.Enum.MessageType
import com.example.socialmedia.project.Domain.Model.MessageModel
import com.example.socialmedia.project.ViewModel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ChatFragment : Fragment() {

    private lateinit var binding: FragmentChatBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var viewModel: ChatViewModel
    private val messages = mutableListOf<MessageModel>()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private var conversationId: String? = null
    private var otherUserName: String? = null
    private var otherUserAvatar: String? = null

    private var isLoadingMore = false
    private var mediaRecorder: MediaRecorder? = null
    private var audioFilePath: String? = null
    private var isRecording = false
    private var recordingStartTime: Long = 0L
    private var recordingTimer: Runnable? = null
    private val handler = Handler()

    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
    private var permissionToRecordAccepted = false
    private val permissions = arrayOf(android.Manifest.permission.RECORD_AUDIO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions(permissions, REQUEST_RECORD_AUDIO_PERMISSION)

        arguments?.let {
            conversationId = it.getString("conversationId")
            otherUserName = it.getString("userName")
            otherUserAvatar = it.getString("userAvatar")
        }

        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentChatBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hiển thị thông tin người dùng
        binding.tvUserName.text = otherUserName ?: "Người dùng"
        Glide.with(requireContext())
            .load(otherUserAvatar ?: R.drawable.image_avata_user)
            .circleCrop()
            .into(binding.ivAvatar)

        binding.ivBack.setOnClickListener { parentFragmentManager.popBackStack() }

        // Khởi tạo adapter trước để tránh warning RecyclerView
        chatAdapter = ChatAdapter(messages, currentUserId, mapOf())
        val layoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
        binding.rvChat.layoutManager = layoutManager
        binding.rvChat.adapter = chatAdapter

        // Nếu conversationId null thì chỉ log, không crash
        if (conversationId.isNullOrEmpty()) {
            Log.e("ChatFragment", "conversationId null! Không load được chat")
        } else {
            viewModel.messages.observe(viewLifecycleOwner) { newMessages ->
                if (newMessages.isEmpty()) return@observe
                val oldSize = messages.size
                messages.clear()
                messages.addAll(newMessages.sortedBy { it.createdAt })
                chatAdapter.updateMessages(messages)
                binding.rvChat.post {
                    val lastVisible = layoutManager.findLastCompletelyVisibleItemPosition()
                    if (lastVisible == oldSize - 1 || oldSize == 0)
                        binding.rvChat.smoothScrollToPosition(messages.size - 1)
                }
            }

            viewModel.loadLatestMessages(conversationId, 20) {
                viewModel.listenNewMessages(conversationId)
            }

            binding.rvChat.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                override fun onScrolled(rv: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                    val firstVisible = layoutManager.findFirstVisibleItemPosition()
                    if (firstVisible == 0 && messages.isNotEmpty() && !isLoadingMore) {
                        isLoadingMore = true
                        val oldestTimestamp = messages.first().createdAt
                        viewModel.loadMoreMessages(conversationId, oldestTimestamp) { _ ->
                            isLoadingMore = false
                        }
                    }
                }
            })
        }

        // Gửi tin nhắn
        binding.ivSend.setOnClickListener { sendMessage() }
        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendMessage(); true } else false
        }

        // Ghi âm
        binding.ivMic.setOnClickListener {
            if (!isRecording) startRecording() else { stopRecording(); stopTimer() }
        }
    }

    // --- Các hàm ghi âm ---
    private fun startRecording() {
        if (!permissionToRecordAccepted) { requestPermissions(permissions, REQUEST_RECORD_AUDIO_PERMISSION); return }
        audioFilePath = requireContext().externalCacheDir?.absolutePath + "/voice_${System.currentTimeMillis()}.mp3"
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(audioFilePath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            prepare()
            start()
        }
        isRecording = true
        binding.ivMic.setImageResource(R.drawable.ic_stop)
        recordingStartTime = System.currentTimeMillis()
        startTimer()
    }

    private fun stopRecording() {
        mediaRecorder?.apply { stop(); release() }
        mediaRecorder = null
        isRecording = false
        binding.ivMic.setImageResource(R.drawable.ic_mic)
        audioFilePath?.let { uploadVoice(it) }
    }

    private fun startTimer() {
        recordingTimer = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - recordingStartTime
                val seconds = (elapsed / 1000 % 60).toInt()
                val minutes = (elapsed / 1000 / 60).toInt()
                binding.tvRecordingTime.text = String.format("%02d:%02d", minutes, seconds)
                handler.postDelayed(this, 500)
            }
        }
        binding.tvRecordingTime.visibility = View.VISIBLE
        handler.post(recordingTimer!!)
    }

    private fun stopTimer() {
        recordingTimer?.let { handler.removeCallbacks(it) }
        binding.tvRecordingTime.visibility = View.GONE
    }

    private fun uploadVoice(filePath: String) {
        val file = Uri.fromFile(File(filePath))
        val storageRef = FirebaseStorage.getInstance().reference
            .child("voices/${conversationId}_${System.currentTimeMillis()}.mp3")

        storageRef.putFile(file)
            .addOnSuccessListener { storageRef.downloadUrl.addOnSuccessListener { uri -> sendVoiceMessage(uri.toString()) } }
            .addOnFailureListener { Log.e("ChatFragment", "Upload voice failed", it) }
    }

    private fun sendVoiceMessage(audioUrl: String) {
        val msgId = java.util.UUID.randomUUID().toString()
        val message = MessageModel(
            messageId = msgId,
            conversationId = conversationId!!,
            senderId = currentUserId,
            senderName = "Bạn",
            mediaUrl = audioUrl,
            messageType = MessageType.VOICE,
            createdAt = System.currentTimeMillis()
        )
        viewModel.sendMessage(conversationId, message) { Log.d("ChatFragment", "Voice sent: $it") }
    }


    private fun sendMessage() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isEmpty() || conversationId.isNullOrEmpty()) return

        val msgId = java.util.UUID.randomUUID().toString()
        val message = MessageModel(
            messageId = msgId,
            conversationId = conversationId!!,
            senderId = currentUserId,
            senderName = "Bạn",
            content = text,
            createdAt = System.currentTimeMillis()
        )

        viewModel.sendMessage(conversationId, message) {
            if (it) binding.etMessage.setText("")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.removeListener()
    }

}
