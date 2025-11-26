package com.example.socialmedia.project.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.project.Adapter.MediaViewPagerAdapter
import com.example.socialmedia.databinding.FragmentPostDetailBinding
import com.example.socialmedia.project.ViewModel.PostDetailViewModel
import com.example.socialmedia.project.Domain.Model.PostModel
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
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
    ): View {
        _binding = FragmentPostDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postId = arguments?.getString("postId")
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (postId == null || currentUserId == null) {
            Toast.makeText(context, "Lỗi: Không tìm thấy bài đăng hoặc người dùng", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        viewModel = ViewModelProvider(this)[PostDetailViewModel::class.java]

        setupUI()
        setupClickListeners()
        setupObservers()

        viewModel.loadPostDetails(postId!!)
    }

    private fun setupUI() {
        mediaAdapter = MediaViewPagerAdapter()
        binding.viewPagerMedia.adapter = mediaAdapter

        // Ẩn RecyclerView comments vì sẽ xem trong BottomSheet
        binding.rvComments.visibility = View.GONE
    }

    private fun setupClickListeners() {
        binding.ivToolbarBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnLike.setOnClickListener {
            viewModel.toggleLike(postId!!)
        }

        binding.btnBookmark.setOnClickListener {
            viewModel.toggleBookmark(postId!!)
        }

        // Chỉ mở comment khi bấm vào icon comment trên thanh action bar
        binding.btnComment.setOnClickListener {
            openComments()
        }

        // ĐÃ XÓA: binding.etAddComment.setOnClickListener
    }

    private fun openComments() {
        if (postAuthorId == null) {
            Toast.makeText(context, "Đang tải...", Toast.LENGTH_SHORT).show()
            return
        }

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
                postAuthorId = post.userId
                updatePostUI(post)
            }
        }

        // 2. Lắng nghe trạng thái Like
        viewModel.isLikedByCurrentUser.observe(viewLifecycleOwner) { isLiked ->
            val icon = if (isLiked) R.drawable.ic_favorite_red else R.drawable.ic_favorite
            binding.btnLike.setImageResource(icon)
        }

        // 3. Lắng nghe số lượng Like
        viewModel.likeCount.observe(viewLifecycleOwner) { count ->
            binding.tvLikeCount.text = "$count lượt thích"
        }

        // 4. Lắng nghe trạng thái Bookmark
        viewModel.isBookmarked.observe(viewLifecycleOwner) { isBookmarked ->
            if (isBookmarked) {
                binding.btnBookmark.setImageResource(R.drawable.ic_bookmark_outline)
                binding.btnBookmark.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.black))
            } else {
                binding.btnBookmark.setImageResource(R.drawable.ic_bookmark_outline)
                binding.btnBookmark.clearColorFilter()
            }
        }

        // ĐÃ XÓA: Code load avatar user hiện tại vào ivMyAvatar (vì view đã bị xóa)

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

        // Cập nhật Media
        mediaAdapter.submitList(post.mediaList)

        // Cập nhật Indicator
        if (post.mediaList.size > 1) {
            binding.tabIndicator.isVisible = true
            TabLayoutMediator(binding.tabIndicator, binding.viewPagerMedia) { _, _ -> }.attach()
        } else {
            binding.tabIndicator.isVisible = false
        }

        // Cập nhật Caption và Timestamp
        binding.tvCaption.isVisible = !post.caption.isNullOrEmpty()
        binding.tvCaption.text = post.caption
        binding.tvTimestamp.text = formatTimestamp(post.createdAt)
    }

    private fun formatTimestamp(timestamp: Long): String {
        return try {
            val sdf = SimpleDateFormat("dd 'tháng' MM, yyyy 'lúc' HH:mm", Locale("vi", "VN"))
            val netDate = Date(timestamp)
            sdf.format(netDate)
        } catch (e: Exception) {
            "Vừa xong"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}