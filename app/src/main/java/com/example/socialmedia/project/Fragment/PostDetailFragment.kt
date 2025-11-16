package com.example.socialmedia.project.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat // <-- Thêm import
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.socialmedia.R
// Xóa import navArgs, chúng ta sẽ lấy postId từ arguments
// import androidx.navigation.fragment.navArgs
// import com.example.socialmedia.project.Adapter.CommentAdapter // <-- KHÔNG DÙNG NỮA
import com.example.socialmedia.project.Adapter.MediaViewPagerAdapter
import com.example.socialmedia.databinding.FragmentPostDetailBinding
import com.example.socialmedia.project.ViewModel.PostDetailViewModel
import com.example.socialmedia.project.Domain.Model.PostModel
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostDetailFragment : Fragment() {

    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: PostDetailViewModel
    private var postId: String? = null
    private var postAuthorId: String? = null
    private var currentUserId: String? = null

    private lateinit var mediaAdapter: MediaViewPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPostDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Lấy postId từ arguments (cách chuẩn)
        postId = arguments?.getString("postId")
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (postId == null || currentUserId == null) {
            Toast.makeText(context, "Lỗi: Không tìm thấy bài đăng hoặc người dùng", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        viewModel = ViewModelProvider(this).get(PostDetailViewModel::class.java)

        setupUI()
        setupClickListeners() // <-- ĐÃ CẬP NHẬT
        setupObservers() // <-- ĐÃ CẬP NHẬT

        viewModel.loadPostDetails(postId!!)
    }

    private fun setupUI() {
        mediaAdapter = MediaViewPagerAdapter()
        binding.viewPagerMedia.adapter = mediaAdapter

        // === XÓA LOGIC RecyclerView BÌNH LUẬN ===
        // binding.rvComments.layoutManager = LinearLayoutManager(context)
        // Chúng ta sẽ ẩn nó đi và thêm một nút "Xem bình luận"
        binding.rvComments.visibility = View.GONE // Ẩn RecyclerView

        // Thêm một TextView để hiển thị "Xem tất cả bình luận" (nếu bạn muốn)
        // (Trong layout của bạn không có, chúng ta sẽ dùng nút comment)
    }

    private fun setupClickListeners() {
        binding.ivToolbarBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // === TÁI SỬ DỤNG LOGIC VIEWMODEL ===
        binding.btnLike.setOnClickListener {
            viewModel.toggleLike(postId!!)
        }

        binding.btnBookmark.setOnClickListener {
            viewModel.toggleBookmark(postId!!)
        }

        // === ĐÂY LÀ PHẦN TÁI SỬ DỤNG QUAN TRỌNG NHẤT ===
        binding.btnComment.setOnClickListener {
            openComments()
        }

        // Cũng mở comment khi nhấn vào EditText
        binding.etAddComment.setOnClickListener {
            openComments()
        }
    }

    // Hàm tái sử dụng CommentBottomSheetFragment
    private fun openComments() {
        if (postAuthorId == null) {
            Toast.makeText(context, "Đang tải...", Toast.LENGTH_SHORT).show()
            return
        }

        // TÁI SỬ DỤNG HOÀN TOÀN BottomSheet của bạn
        val commentSheet = CommentBottomSheetFragment(
            postId = postId!!,
            currentUserId = currentUserId!!,
            postAuthorId = postAuthorId!!
        )
        commentSheet.show(parentFragmentManager, "CommentBottomSheet")
    }


    private fun setupObservers() {
        // 1. Lắng nghe thông tin bài post
        viewModel.post.observe(viewLifecycleOwner) { post ->
            if (post != null) {
                postAuthorId = post.userId // Lấy ID tác giả
                updatePostUI(post)
            }
        }

        // === XÓA OBSERVER CHO COMMENTS ===

        // 2. Lắng nghe trạng thái Like (Tái sử dụng)
        viewModel.isLikedByCurrentUser.observe(viewLifecycleOwner) { isLiked ->
            val icon = if (isLiked) R.drawable.ic_favorite_red else R.drawable.ic_favorite
            binding.btnLike.setImageResource(icon)
        }

        // 3. Lắng nghe số lượng Like (Tái sử dụng)
        viewModel.likeCount.observe(viewLifecycleOwner) { count ->
            binding.tvLikeCount.text = "$count lượt thích"
        }

        // 4. Lắng nghe trạng thái Bookmark (Tái sử dụng)
        viewModel.isBookmarked.observe(viewLifecycleOwner) { isBookmarked ->
            if (isBookmarked) {
                // Nếu đã lưu: Dùng icon outline và TÔ MÀU (ví dụ màu đen)
                binding.btnBookmark.setImageResource(R.drawable.ic_bookmark_outline)
                binding.btnBookmark.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.black))
            } else {
                // Nếu chưa lưu: Dùng icon outline và BỎ MÀU
                binding.btnBookmark.setImageResource(R.drawable.ic_bookmark_outline)
                binding.btnBookmark.clearColorFilter()
            }
        }

        // 5. Lắng nghe avatar của người dùng hiện tại (để điền vào ô comment)
        FirebaseDatabase.getInstance().getReference("InfoUser").child(currentUserId!!)
            .child("profilePictureUrl").get().addOnSuccessListener {
                Glide.with(this)
                    .load(it.value.toString())
                    .placeholder(R.drawable.image_avata_user)
                    .circleCrop()
                    .into(binding.ivMyAvatar)
            }

        // Lắng nghe lỗi
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updatePostUI(post: PostModel) {
        // Cập nhật Header
        Glide.with(this)
            .load(post.userProfileUrl)
            .placeholder(R.drawable.image_avata_user)
            .circleCrop()
            .into(binding.ivUserAvatar)
        binding.tvUsername.text = post.userName

        // Cập nhật MediaAdapter bằng .submitList()
        mediaAdapter.submitList(post.mediaList)

        // Cập nhật dấu chấm
        if (post.mediaList.size > 1) {
            binding.tabIndicator.isVisible = true
            TabLayoutMediator(binding.tabIndicator, binding.viewPagerMedia) { tab, position -> }.attach()
        } else {
            binding.tabIndicator.isVisible = false
        }

        // Cập nhật Caption và Timestamp
        binding.tvCaption.isVisible = !post.caption.isNullOrEmpty()
        binding.tvCaption.text = post.caption
        binding.tvTimestamp.text = formatTimestamp(post.createdAt)
    }

    private fun formatTimestamp(timestamp: Long): String {
        try {
            val sdf = SimpleDateFormat("dd 'tháng' MM, yyyy 'lúc' HH:mm", Locale("vi", "VN"))
            val netDate = Date(timestamp)
            return sdf.format(netDate)
        } catch (e: Exception) {
            return "Vừa xong"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}