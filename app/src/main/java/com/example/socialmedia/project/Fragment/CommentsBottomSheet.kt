package com.example.socialmedia.project.Fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.project.Adapter.CommentsAdapter
import com.example.socialmedia.project.Domain.Model.CommentsModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView
import java.util.UUID

class CommentsBottomSheet : BottomSheetDialogFragment() {

    private lateinit var rvComments: RecyclerView
    private lateinit var etComment: EditText
    private lateinit var ivSendComment: ImageView
    private lateinit var ivSticker: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var imgUserAvatar: CircleImageView

    private lateinit var adapter: CommentsAdapter
    private val commentsList = mutableListOf<CommentsModel>()
    private val database = FirebaseDatabase.getInstance()

    private var reelId: String = ""
    private var reelOwnerId: String = "" // ✅ Thêm biến này
    private var replyToComment: CommentsModel? = null

    companion object {
        private const val ARG_REEL_ID = "reel_id"
        private const val ARG_REEL_OWNER_ID = "reel_owner_id"
        private const val TAG = "CommentsBottomSheet"

        // ✅ Overload để hỗ trợ cả 2 cách gọi (backward compatibility)
        fun newInstance(reelId: String): CommentsBottomSheet {
            return newInstance(reelId, "")
        }

        fun newInstance(reelId: String, reelOwnerId: String): CommentsBottomSheet {
            return CommentsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_REEL_ID, reelId)
                    putString(ARG_REEL_OWNER_ID, reelOwnerId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reelId = arguments?.getString(ARG_REEL_ID) ?: ""
        reelOwnerId = arguments?.getString(ARG_REEL_OWNER_ID) ?: "" // ✅ Lấy reelOwnerId
        Log.d(TAG, "CommentsBottomSheet created with reelId: $reelId, ownerId: $reelOwnerId")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottomsheet_comments, container, false)

        // Initialize views
        rvComments = view.findViewById(R.id.rvComments)
        etComment = view.findViewById(R.id.etComment)
        ivSendComment = view.findViewById(R.id.ivSendComment)
        ivSticker = view.findViewById(R.id.ivSticker)
        tvTitle = view.findViewById(R.id.tvTitle)
        imgUserAvatar = view.findViewById(R.id.imgUserAvatar)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupInputField()
        loadComments()
        loadUserAvatar()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        adapter = CommentsAdapter(
            comments = commentsList,
            reelOwnerId = reelOwnerId, // ✅ Truyền reelOwnerId vào adapter
            onReplyClick = { comment ->
                replyToComment = comment

                // Get username for hint
                database.reference.child("InfoUser").child(comment.userId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val fullName = snapshot.child("fullName").getValue(String::class.java) ?: "Người dùng"
                            etComment.hint = "Trả lời @$fullName..."
                            etComment.requestFocus()

                            // Show keyboard
                            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                                    as android.view.inputmethod.InputMethodManager
                            imm.showSoftInput(etComment, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
            },
            onLikeClick = { comment ->
                // Like handled in adapter
                Log.d(TAG, "Comment liked: ${comment.commentId}")
            }
        )

        rvComments.layoutManager = LinearLayoutManager(requireContext())
        rvComments.adapter = adapter
    }

    private fun setupInputField() {
        etComment.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                ivSendComment.isEnabled = !s.isNullOrBlank()
                ivSendComment.alpha = if (s.isNullOrBlank()) 0.3f else 1f
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupClickListeners() {
        ivSendComment.setOnClickListener {
            postComment()
        }

        ivSticker.setOnClickListener {
            Toast.makeText(requireContext(), "Sticker feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserAvatar() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        database.reference.child("InfoUser").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val url = snapshot.child("profilePictureUrl").getValue(String::class.java)
                    if (!url.isNullOrEmpty()) {
                        Glide.with(this@CommentsBottomSheet)
                            .load(url)
                            .placeholder(R.drawable.image_avata_user)
                            .into(imgUserAvatar)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadComments() {
        Log.d(TAG, "Loading comments for reelId: $reelId")

        database.reference.child("Comments")
            .orderByChild("commentableId")
            .equalTo(reelId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d(TAG, "Comments snapshot received, children count: ${snapshot.childrenCount}")
                    commentsList.clear()

                    for (commentSnapshot in snapshot.children) {
                        val comment = commentSnapshot.getValue(CommentsModel::class.java)
                        Log.d(TAG, "Comment found: ${comment?.content}, parentId: ${comment?.parentCommentId}")

                        // Only add root comments (no parent)
                        if (comment != null && comment.parentCommentId == null) {
                            commentsList.add(comment)
                        }
                    }

                    Log.d(TAG, "Total root comments: ${commentsList.size}")

                    // Sort by newest first
                    commentsList.sortByDescending { it.createdAt }

                    adapter.notifyDataSetChanged()
                    updateCommentsCount()

                    // Scroll to top when new comment added
                    if (commentsList.isNotEmpty()) {
                        rvComments.smoothScrollToPosition(0)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to load comments", error.toException())
                    Toast.makeText(requireContext(), "Không thể tải bình luận", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun postComment() {
        val content = etComment.text.toString().trim()
        if (content.isEmpty()) return

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để bình luận", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable send button during posting
        ivSendComment.isEnabled = false
        ivSendComment.alpha = 0.3f

        val comment = CommentsModel(
            commentId = UUID.randomUUID().toString(),
            userId = uid,
            commentableType = "REEL",
            commentableId = reelId,
            parentCommentId = replyToComment?.commentId,
            content = content,
            createdAt = System.currentTimeMillis(),
            likeCount = 0,
            replyCount = 0
        )

        Log.d(TAG, "Posting comment: ${comment.commentId}, parentId: ${comment.parentCommentId}")

        // Save to Firebase
        database.reference.child("Comments").child(comment.commentId)
            .setValue(comment)
            .addOnSuccessListener {
                Log.d(TAG, "Comment posted successfully")

                // Update comment count in Reel
                database.reference.child("Reels").child(reelId)
                    .child("commentCount")
                    .setValue(ServerValue.increment(1))

                // If reply, update reply count
                replyToComment?.let { parent ->
                    database.reference.child("Comments").child(parent.commentId)
                        .child("replyCount")
                        .setValue(ServerValue.increment(1))
                }

                // Clear input
                etComment.text.clear()
                replyToComment = null
                etComment.hint = "Viết bình luận..."

                // Hide keyboard
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                        as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(etComment.windowToken, 0)

                Toast.makeText(requireContext(), "Đã đăng bình luận!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to post comment", e)
                Toast.makeText(requireContext(), "Không thể đăng bình luận", Toast.LENGTH_SHORT).show()
                ivSendComment.isEnabled = true
                ivSendComment.alpha = 1f
            }
    }

    private fun updateCommentsCount() {
        val count = commentsList.size
        tvTitle.text = if (count == 0) {
            "Bình luận"
        } else {
            "Bình luận ($count)"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Hide keyboard when closing
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }
}