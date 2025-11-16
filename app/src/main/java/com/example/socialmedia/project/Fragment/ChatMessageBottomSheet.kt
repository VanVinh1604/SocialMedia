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

    private val onReply: (MessageModel) -> Unit,  // thêm callback Reply
    private val onEdit: (MessageModel) -> Unit,
    private val onDelete: (MessageModel) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottomsheet_chat_options, container, false)

        val tvReplyLayout = view.findViewById<LinearLayout>(R.id.tvReply)
        tvReplyLayout.setOnClickListener {
            onReply(message)
            dismiss()
        }

        val tvEditLayout = view.findViewById<LinearLayout>(R.id.tvEdit)
        tvEditLayout.setOnClickListener {
            onEdit(message)
            dismiss()
        }

        val tvDeleteLayout = view.findViewById<LinearLayout>(R.id.tvDelete)
        tvDeleteLayout.setOnClickListener {
            onDelete(message)
            dismiss()
        }


        return view
    }
}
