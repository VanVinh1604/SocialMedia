package com.example.socialmedia.project.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.socialmedia.databinding.FragmentPostDetailBinding
import com.example.socialmedia.project.Adapter.PostDetailAdapter
import com.example.socialmedia.project.Domain.Model.PostModel
import com.example.socialmedia.project.ViewModel.PostDetailViewModel
import com.google.firebase.auth.FirebaseAuth

class PostDetailFragment : Fragment() {

    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: PostDetailViewModel
    private lateinit var detailAdapter: PostDetailAdapter

    // Biến nhận từ Bundle
    private var startPostId: String? = null
    private var postAuthorId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Nhận dữ liệu truyền vào
        startPostId = arguments?.getString("postId")
        postAuthorId = arguments?.getString("userId")

        if (startPostId == null || postAuthorId == null) {
            Toast.makeText(context, "Lỗi: Thiếu thông tin bài viết", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        viewModel = ViewModelProvider(this)[PostDetailViewModel::class.java]

        setupViewPager()
        setupObservers()

        // 2. Bắt đầu tải dữ liệu
        // Dùng !! ở đây an toàn vì ta đã check null ở if bên trên rồi
        viewModel.loadUserPosts(postAuthorId!!, startPostId!!)
    }

    private fun setupViewPager() {
        detailAdapter = PostDetailAdapter(
            context = requireContext(),
            posts = emptyList(),
            onBackClick = {
                findNavController().popBackStack()
            },
            onCommentClick = { post ->
                openComments(post)
            },
            onLikeClick = { postId, isLiked ->
                viewModel.toggleLike(postId, isLiked)
            },
            onBookmarkClick = { postId, isBookmarked ->
                viewModel.toggleBookmark(postId, isBookmarked)
            }
        )

        binding.viewPagerPosts.apply {
            adapter = detailAdapter
            orientation = ViewPager2.ORIENTATION_VERTICAL
            offscreenPageLimit = 1
        }
    }

    private fun setupObservers() {
        viewModel.postList.observe(viewLifecycleOwner) { posts ->
            if (posts.isNotEmpty()) {
                detailAdapter.updateData(posts)
            }
        }

        viewModel.initialPosition.observe(viewLifecycleOwner) { position ->
            if (position != null && position >= 0) {
                binding.viewPagerPosts.setCurrentItem(position, false)
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openComments(post: PostModel) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // === ĐÃ SỬA LỖI Ở ĐÂY ===
        // PostModel có thể trả về null, nên cần dùng ?: "" để lấy giá trị mặc định nếu null
        val commentSheet = CommentBottomSheetFragment(
            postId = post.postId ?: "",       // Sửa tại đây
            currentUserId = currentUid,
            postAuthorId = post.userId ?: ""  // Sửa tại đây
        )
        commentSheet.show(parentFragmentManager, "CommentBottomSheet")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}