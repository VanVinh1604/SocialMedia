package com.example.socialmedia.project.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.socialmedia.databinding.BottomsheetCommentsBinding
import com.example.socialmedia.project.ViewModel.CommentViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.socialmedia.project.Adapter.CommentAdapter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.example.socialmedia.R
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class CommentBottomSheetFragment(private val postId: String, private val currentUserId: String,    private val postAuthorId: String ) : BottomSheetDialogFragment() {

    private var _binding: BottomsheetCommentsBinding? = null
    private val binding get() = _binding!!
    private lateinit var commentViewModel: CommentViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        commentViewModel = ViewModelProvider(this)[CommentViewModel::class.java]

        binding.rvComments.layoutManager = LinearLayoutManager(requireContext())

        // Load danh sách comment
        commentViewModel.comments.observe(viewLifecycleOwner) { comments ->
            binding.rvComments.adapter = CommentAdapter(comments, currentUserId, postAuthorId)
        }
        commentViewModel.loadComments(postId)

        // 🔹 Load avatar user hiện tại
        val userRef = FirebaseDatabase.getInstance().getReference("InfoUser").child(currentUserId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val profileUrl = snapshot.child("profilePictureUrl").getValue(String::class.java)
                Glide.with(requireContext())
                    .load(profileUrl ?: R.drawable.image_avata_user)
                    .placeholder(R.drawable.image_avata_user)
                    .circleCrop()
                    .into(binding.imgUserAvatar)
            }

            override fun onCancelled(error: DatabaseError) {
                // Có thể log lỗi ở đây
            }
        })


        // 🔹 Gửi comment
        binding.ivSendComment.setOnClickListener {
            val content = binding.etComment.text.toString().trim()
            if(content.isNotEmpty()) {
                commentViewModel.addComment(postId, currentUserId, content)
                binding.etComment.text.clear()
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
