package com.example.socialmedia.project.Fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.BottomsheetCommentsBinding
import com.example.socialmedia.project.Adapter.CommentAdapter
import com.example.socialmedia.project.Domain.Model.CommentModel
import com.example.socialmedia.project.ViewModel.CommentViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.database.*

class CommentBottomSheetFragment(
    private val postId: String,
    private val currentUserId: String,
    private val postAuthorId: String
) : BottomSheetDialogFragment() {

    private var _binding: BottomsheetCommentsBinding? = null
    private val binding get() = _binding!!
    private lateinit var commentViewModel: CommentViewModel

    // ✅ Khi reply vào comment con, vẫn ghi nhận parentCommentId là comment cha gốc
    private var replyingToRootComment: CommentModel? = null
    private var replyingToUserName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        commentViewModel = ViewModelProvider(this)[CommentViewModel::class.java]

        binding.rvComments.layoutManager = LinearLayoutManager(requireContext())

        // Mở bàn phím khi nhấn vào EditText
        binding.etComment.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) showKeyboard(v)
        }

        // Lắng nghe danh sách comment
        commentViewModel.comments.observe(viewLifecycleOwner) { comments ->
            binding.rvComments.adapter =
                CommentAdapter(comments, currentUserId, postAuthorId) { selectedComment ->
                    // ✅ Nếu là comment cha thì set luôn
                    if (selectedComment.parentCommentId == null) {
                        replyingToRootComment = selectedComment
                    } else {
                        // ✅ Nếu là comment con, lấy comment cha gốc từ ViewModel (hoặc firebase)
                        val rootComment = comments.find { it.commentId == selectedComment.parentCommentId }
                        replyingToRootComment = rootComment ?: selectedComment
                    }

                    getUserName(selectedComment.userId) { userName ->
                        replyingToUserName = userName
                        binding.etComment.hint = "Trả lời @$userName..."
                        binding.etComment.setText("@$userName ")
                        binding.etComment.requestFocus()
                        showKeyboard(binding.etComment)
                    }
                }
        }

        commentViewModel.listenComments(postId)

        // Load avatar người dùng hiện tại
        FirebaseDatabase.getInstance().getReference("InfoUser").child(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val profileUrl = snapshot.child("profilePictureUrl").getValue(String::class.java)
                    Glide.with(requireContext())
                        .load(profileUrl ?: R.drawable.image_avata_user)
                        .placeholder(R.drawable.image_avata_user)
                        .circleCrop()
                        .into(binding.imgUserAvatar)
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        // Xử lý gửi comment / reply
        binding.ivSendComment.setOnClickListener {
            val content = binding.etComment.text.toString().trim()
            if (content.isEmpty()) return@setOnClickListener

            replyingToRootComment?.let { parent ->
                // ✅ Gửi reply, đảm bảo luôn nằm trong replies của comment cha
                commentViewModel.addReply(
                    postId = postId,
                    parentCommentId = parent.commentId,
                    userId = currentUserId,
                    content = content
                )
                replyingToRootComment = null
                replyingToUserName = null
                binding.etComment.hint = "Viết bình luận..."
            } ?: run {
                // ✅ Gửi comment gốc
                commentViewModel.addComment(
                    postId = postId,
                    userId = currentUserId,
                    postOwnerId = postAuthorId,
                    content = content
                )
            }

            binding.etComment.text.clear()
            hideKeyboard(binding.etComment)
        }

        // Nút sticker (demo)
        binding.ivSticker.setOnClickListener {
            showStickerPicker(binding.ivSticker)
        }
    }

    // Lấy tên người dùng
    private fun getUserName(userId: String?, callback: (String) -> Unit) {
        if (userId == null) return
        FirebaseDatabase.getInstance().getReference("InfoUser").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("fullName").getValue(String::class.java) ?: "Người dùng"
                    callback(name)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun showKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    @SuppressLint("ServiceCast")
    private fun hideKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showStickerPicker(anchorView: View) {
        val inflater = LayoutInflater.from(requireContext())
        val popupView = inflater.inflate(R.layout.layout_sticker_picker, null)
        val popupWindow = PopupWindow(
            popupView,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            true
        )

        val stickers = listOf("😊", "😂", "😍", "👍", "🔥", "🥰", "😎", "😢", "😡", "🎉", "❤️", "👏")

        val gridView = popupView.findViewById<GridView>(R.id.gridStickers)
        gridView.adapter = object : BaseAdapter() {
            override fun getCount() = stickers.size
            override fun getItem(position: Int) = stickers[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val textView = TextView(requireContext())
                textView.text = stickers[position]
                textView.textSize = 28f
                textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
                textView.setPadding(8, 8, 8, 8)
                return textView
            }
        }

        gridView.setOnItemClickListener { _, _, position, _ ->
            val sticker = stickers[position]
            val current = binding.etComment.text.toString()
            binding.etComment.setText("$current$sticker")
            binding.etComment.setSelection(binding.etComment.text.length)
            popupWindow.dismiss()
            showKeyboard(binding.etComment)
        }

        popupWindow.elevation = 10f
        popupWindow.showAsDropDown(anchorView, -50, -400)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
