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
import com.example.socialmedia.databinding.FragmentSimpleListBinding
import com.example.socialmedia.project.Adapter.ProfilePostAdapter
import com.example.socialmedia.project.Domain.Model.PostModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HistoryFragment : Fragment() {

    private var _binding: FragmentSimpleListBinding? = null
    private val binding get() = _binding!!

    private lateinit var postAdapter: ProfilePostAdapter
    private val historyPostsList = ArrayList<PostModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSimpleListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        loadLikedHistory()
    }

    private fun setupUI() {
        binding.tvTitle.text = "Bài viết đã thích"
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Tái sử dụng ProfilePostAdapter để hiển thị dạng lưới (Grid 3 cột)
        postAdapter = ProfilePostAdapter { post ->
            val bundle = bundleOf(
                "postId" to post.postId,
                "userId" to (post.userId ?: "")
            )
            // Chuyển sang xem chi tiết bài viết
            try {
// Sử dụng ID của màn hình đích thay vì ID của action
                findNavController().navigate(R.id.postDetailFragment, bundle)                // Lưu ý: Nếu id action trong nav_graph khác, hãy đổi lại cho khớp.
                // Tốt nhất dùng ID đích: R.id.postDetailFragment (nếu có action toàn cục hoặc xử lý try catch)
            } catch (e: Exception) {
                // Fallback nếu chưa define action cụ thể từ history -> detail
                try {
                    findNavController().navigate(R.id.postDetailFragment, bundle)
                } catch (e2: Exception) {
                    Toast.makeText(context, "Lỗi điều hướng", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = postAdapter
        }
    }

    private fun loadLikedHistory() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().reference

        binding.tvEmpty.text = "Đang tải..."
        binding.tvEmpty.visibility = View.VISIBLE

        // 1. Truy cập vào node "user_likes" của người dùng hiện tại
        database.child("user_likes").child(currentUid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                historyPostsList.clear()
                val totalLikes = snapshot.childrenCount.toInt()
                var loadedCount = 0

                if (totalLikes == 0) {
                    binding.tvEmpty.text = "Bạn chưa thích bài viết nào"
                    binding.tvEmpty.visibility = View.VISIBLE
                    postAdapter.submitList(emptyList())
                    return
                } else {
                    binding.tvEmpty.visibility = View.GONE
                }

                // 2. Lấy danh sách ID bài viết
                // Lưu ý: snapshot.children sẽ trả về danh sách ID bài viết dạng Key
                // Để hiển thị mới nhất lên đầu, ta có thể đảo ngược list key trước khi load
                val likedPostIds = snapshot.children.mapNotNull { it.key }.reversed()

                for (postId in likedPostIds) {
                    // 3. Tải chi tiết từng bài viết từ node "posts"
                    database.child("posts").child(postId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(postSnap: DataSnapshot) {
                            val post = postSnap.getValue(PostModel::class.java)
                            if (post != null) {
                                post.postId = postSnap.key ?: ""
                                historyPostsList.add(post)
                            }

                            loadedCount++
                            // Khi đã tải xong hết
                            if (loadedCount >= totalLikes) {
                                // Sắp xếp lại danh sách theo thứ tự ID đã lấy ban đầu (để đúng thứ tự thời gian like)
                                // (Logic đơn giản: Cứ hiển thị danh sách đã tải được)
                                postAdapter.submitList(ArrayList(historyPostsList))
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.tvEmpty.text = "Lỗi tải dữ liệu"
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}