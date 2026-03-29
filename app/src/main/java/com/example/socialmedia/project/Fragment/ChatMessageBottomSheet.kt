package com.example.socialmedia.project.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.example.socialmedia.R
import com.example.socialmedia.project.Domain.Model.MessageModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ChatMessageBottomSheet(
    private val message: MessageModel,
    private val onReply: (MessageModel) -> Unit,
    private val onEdit: (MessageModel) -> Unit,
    private val onDelete: (MessageModel) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottomsheet_chat_options, container, false)

        val tvReplyLayout = view.findViewById<LinearLayout>(R.id.tvReply)
        val tvEditLayout = view.findViewById<LinearLayout>(R.id.tvEdit)
        val tvDeleteLayout = view.findViewById<LinearLayout>(R.id.tvDelete)

        // Lấy userId hiện tại
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().uid

        // ❌ Không phải tin nhắn của user → ẩn Edit + Delete
        if (message.senderId != currentUserId) {
            tvEditLayout.visibility = View.GONE
            tvDeleteLayout.visibility = View.GONE
        }

        // Reply
        tvReplyLayout.setOnClickListener {
            onReply(message)
            dismiss()
        }

        // Edit
        tvEditLayout.setOnClickListener {
            onEdit(message)
            dismiss()
        }

        // Delete
        tvDeleteLayout.setOnClickListener {
            onDelete(message)
            dismiss()
        }

        return view
    }
}
