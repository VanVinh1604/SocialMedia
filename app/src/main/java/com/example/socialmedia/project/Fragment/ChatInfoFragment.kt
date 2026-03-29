package com.example.socialmedia.project.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentChatInfoBinding
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.ViewModel.ChatViewModel
import com.google.android.material.imageview.ShapeableImageView

class ChatInfoFragment : Fragment() {

    private var _binding: FragmentChatInfoBinding? = null
    private val binding get() = _binding!!

    private var conversationId: String? = null
    private var otherUserName: String? = null
    private var otherUserAvatar: String? = null

    private val chatViewModel: ChatViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            conversationId = it.getString("conversationId")
            otherUserName = it.getString("otherUserName")
            otherUserAvatar = it.getString("otherUserAvatar")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = ChatInfoFragmentArgs.fromBundle(requireArguments())
        conversationId = args.conversationId
        otherUserName = args.userName
        otherUserAvatar = args.userAvatar

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Hiển thị 1-1 chat mặc định
        binding.tvName.text = otherUserName ?: "Người dùng"
        setupSingleAvatar(otherUserAvatar)

        conversationId?.let { convId ->
            chatViewModel.getParticipantsLive(convId).observe(viewLifecycleOwner) { participants ->
                updateParticipantsUI(participants)
            }
        }

        // Click listener cho action buttons
        // Nhấn nút thêm thành viên
        binding.AddMember.setOnClickListener {
            openAddMemberBottomSheet()
        }
        binding.layoutNotification.setOnClickListener { /* handle notification */ }
        binding.layoutSearch.setOnClickListener { /* handle search */ }

        binding.layoutLeaveGroup.setOnClickListener {
            conversationId?.let { chatId ->
                chatViewModel.leaveGroup(chatId)
            }
        }

        chatViewModel.leaveGroupResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Đã rời nhóm", Toast.LENGTH_SHORT).show()

                findNavController().popBackStack(
                    R.id.messageFragment,
                    false   // không xoá chính messageFragment
                )
            } else {
                Toast.makeText(requireContext(), "Rời nhóm thất bại", Toast.LENGTH_SHORT).show()
            }
        }

        


    }

    private fun updateParticipantsUI(participants: List<UserModel>) {
        if (participants.size > 2) {
            // Group chat
            binding.tvName.text = "Nhóm chat (${participants.size} thành viên)"
            binding.tvMemberCount.text = participants.size.toString()
            binding.tvMemberCount.visibility = View.VISIBLE
            binding.viewOnlineStatus.visibility = View.GONE
            setupGroupAvatars(participants)
            binding.layoutLeaveGroup.visibility =
                if (participants.size > 2) View.VISIBLE else View.GONE

        } else if (participants.size == 2) {
            // 1-1 chat: ưu tiên arguments, fallback mới lấy user khác
            val other = participants.firstOrNull { it.userId != chatViewModel.currentUserId }

            binding.tvName.text = otherUserName ?: other?.fullName ?: "Người dùng"
            setupSingleAvatar(otherUserAvatar ?: other?.profilePictureUrl)

            binding.tvMemberCount.visibility = View.GONE
            binding.viewOnlineStatus.visibility = View.VISIBLE
        }
    }


    private fun openAddMemberBottomSheet() {

        conversationId?.let { convId ->

            // Lấy participants mới nhất từ Firestore trước khi mở BottomSheet
            chatViewModel.getParticipantsLive(convId).observe(viewLifecycleOwner) { participants ->

                val currentMembers = participants.map { it.userId }

                // Lấy danh sách những user bạn đang follow
                chatViewModel.getFollowedUsers().observe(viewLifecycleOwner) { followedUsers ->

                    AddMemberBottomSheet(
                        followedUsers = followedUsers,
                        currentMembers = currentMembers
                    ) { selectedNewUserIds ->
                        if (selectedNewUserIds.isNotEmpty()) {
                            chatViewModel.addMembersToConversation(convId, selectedNewUserIds)
                        }
                    }.show(parentFragmentManager, "AddMembers")
                }
            }
        }
    }


    private fun setupSingleAvatar(avatarUrl: String?) {
        binding.ivAvatarSingle.visibility = View.VISIBLE
        binding.ivAvatarGroup.root.visibility = View.GONE
        Glide.with(this)
            .load(avatarUrl ?: R.drawable.image_avata_user)
            .circleCrop()
            .into(binding.ivAvatarSingle)
    }

    private fun setupGroupAvatars(participants: List<UserModel>) {
        binding.ivAvatarSingle.visibility = View.GONE
        binding.ivAvatarGroup.root.visibility = View.VISIBLE

        val avatar1 = binding.ivAvatarGroup.root.findViewById<ShapeableImageView>(R.id.avatar1)
        val avatar2 = binding.ivAvatarGroup.root.findViewById<ShapeableImageView>(R.id.avatar2)

        val firstAvatar = participants.getOrNull(0)?.profilePictureUrl ?: R.drawable.image_avata_user
        val secondAvatar = participants.getOrNull(1)?.profilePictureUrl ?: R.drawable.image_avata_user

        Glide.with(this).load(firstAvatar).circleCrop().into(avatar1)
        Glide.with(this).load(secondAvatar).circleCrop().into(avatar2)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
