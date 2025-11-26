package com.example.socialmedia.project.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentSimpleListBinding // Đảm bảo binding đúng layout trên
import com.example.socialmedia.project.Adapter.ProfilePostAdapter
import com.example.socialmedia.project.Domain.Model.PostModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SavedFragment : Fragment() {

    private lateinit var binding: FragmentSimpleListBinding
    private lateinit var postAdapter: ProfilePostAdapter
    private val savedPostsList = ArrayList<PostModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSimpleListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        loadSavedPosts()
    }

    private fun setupUI() {
        binding.tvTitle.text = "Saved Posts"
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }

        // Tái sử dụng ProfilePostAdapter để hiển thị Grid ảnh
        postAdapter = ProfilePostAdapter { post ->
            // Logic click vào ảnh để xem chi tiết (giống ProfileFragment)
            val bundle = bundleOf(
                "postId" to post.postId,
                "userId" to (post.userId ?: "")
            )
            findNavController().navigate(R.id.action_savedFragment_to_postDetailFragment, bundle)
        }

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(context, 3) // Hiển thị 3 cột
            adapter = postAdapter
        }
    }

    private fun loadSavedPosts() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().reference

        // 1. Lấy danh sách ID bài viết từ node "bookmarks"
        database.child("bookmarks").child(currentUid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                savedPostsList.clear()
                val totalBookmarks = snapshot.childrenCount.toInt()
                var loadedCount = 0

                if (totalBookmarks == 0) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    postAdapter.submitList(emptyList())
                    return
                } else {
                    binding.tvEmpty.visibility = View.GONE
                }

                // 2. Với mỗi ID, tải chi tiết bài viết từ node "posts"
                for (snap in snapshot.children) {
                    val postId = snap.key ?: continue

                    database.child("posts").child(postId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(postSnap: DataSnapshot) {
                            val post = postSnap.getValue(PostModel::class.java)
                            if (post != null) {
                                post.postId = postSnap.key ?: ""
                                savedPostsList.add(post)
                            }

                            loadedCount++
                            // Khi đã tải xong hết các bài
                            if (loadedCount >= totalBookmarks) {
                                // Sắp xếp bài mới lưu lên đầu (nếu cần logic sắp xếp)
                                savedPostsList.reverse()
                                postAdapter.submitList(ArrayList(savedPostsList))
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Lỗi tải bookmark", Toast.LENGTH_SHORT).show()
            }
        })
    }
}