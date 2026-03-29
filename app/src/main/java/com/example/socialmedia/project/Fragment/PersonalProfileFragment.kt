package com.example.socialmedia.project.Fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentPersonalProfileBinding
import com.example.socialmedia.project.Adapter.ProfilePostAdapter
import com.example.socialmedia.project.Adapter.StoryHighlightAdapter
import com.example.socialmedia.project.Domain.Model.PostModel
import com.example.socialmedia.project.Domain.Model.ReelModel
import com.example.socialmedia.project.Domain.Model.StoryHighlightModel
import com.example.socialmedia.project.Domain.Model.UserModel
import com.example.socialmedia.project.ViewModel.ProfileViewModel
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth

class PersonalProfileFragment : Fragment() {

    private var _binding: FragmentPersonalProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by navGraphViewModels(R.id.nav_graph)

    private lateinit var postAdapter: ProfilePostAdapter
    private lateinit var highlightAdapter: StoryHighlightAdapter

    // 🆕 BIẾN MỚI: Tách list Posts (bài viết thường) và Reels
    private var allPosts: List<PostModel> = emptyList() // Bài viết thường (ảnh/video dài)
    private var allReels: List<PostModel> = emptyList() // Reels đã được convert sang PostModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPersonalProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPostGrid()
        setupHighlights()
        setupObservers()
        setupClickListeners()
        setupTabs()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadProfile(null)

        // --- [THÊM VÀO] Tải danh sách Story để kiểm tra vòng sáng ---
        viewModel.loadUserStories()
        // ----------------------------------------------------------
    }

    private fun setupHighlights() {
        highlightAdapter = StoryHighlightAdapter(emptyList()) { highlight ->
            if (highlight.id == "ADD_NEW") {
                try {
                    findNavController().navigate(R.id.action_personalProfileFragment_to_createHighlightFragment)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                val bundle = Bundle().apply {
                    putString("highlightId", highlight.id)
                }
                try {
                    findNavController().navigate(R.id.action_personalProfileFragment_to_highlightViewerFragment, bundle)
                } catch (e: Exception) {
                    Toast.makeText(context, "Lỗi điều hướng: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.rvStoryHighlights.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = highlightAdapter
        }
    }

    private fun setupPostGrid() {
        postAdapter = ProfilePostAdapter { post ->
            val postId = post.postId

            // LOGIC CLICK ĐÃ ĐƯỢC CẬP NHẬT: PHÂN LOẠI REEL VÀ ẢNH
            if (post.isReel) {
                // TRƯỜNG HỢP LÀ REELS -> Mở ReelsFragment
                val bundle = Bundle().apply {
                    // Dùng post.userId và post.postId (đã được mapping từ reelId)
                    putString("userId", post.userId)
                    putString("startReelId", post.postId)
                }

                try {
                    findNavController().navigate(R.id.reelsFragment, bundle)
                } catch (e: Exception) {
                    try {
                        findNavController().navigate(R.id.action_personalProfileFragment_to_reelsFragment, bundle)
                    } catch (e2: Exception) {
                        Toast.makeText(context, "Chưa cấu hình điều hướng Reels trong NavGraph", Toast.LENGTH_SHORT).show()
                    }
                }

            } else {
                // TRƯỜNG HỢP LÀ ẢNH/POST THƯỜNG -> Mở PostDetailFragment (Code cũ)
                val bundle = bundleOf(
                    "postId" to postId,
                    "userId" to post.userId
                )
                try {
                    findNavController().navigate(R.id.action_personalProfileFragment_to_postDetailFragment, bundle)
                } catch (e: Exception) {
                    Toast.makeText(context, "Lỗi NavGraph: " + e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.rvPhotos.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = postAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupObservers() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        viewModel.userProfile.observe(viewLifecycleOwner, Observer { user ->
            if (user != null && user.userId == currentUid) {
                updateUi(user)
            }
        })

        // 1. Lắng nghe Posts thường (Ảnh/Video dài)
        viewModel.userPosts.observe(viewLifecycleOwner, Observer { posts ->
            if (view == null) return@Observer
            allPosts = posts
            updateCountsAndFilter()
        })

        // 2. Lắng nghe Reels và CONVERT sang PostModel
        viewModel.userReels.observe(viewLifecycleOwner, Observer { reels ->
            // Nếu list null thì gán rỗng để tránh crash
            val safeReels = reels ?: emptyList()

            allReels = safeReels.map { reel ->
                PostModel(
                    postId = reel.reelId,
                    userId = reel.userId,
                    isReel = true,
                    thumbnail = reel.thumbnailUrl,
                    videoUrl = reel.videoUrl,
                    caption = reel.caption,
                    likeCount = reel.likeCount,
                    commentCount = reel.commentCount,
                    shareCount = reel.shareCount,
                    viewCount = reel.viewCount,
                    createdAt = reel.createdAt
                )
            }
            updateCountsAndFilter()
        })


        viewModel.userHighlights.observe(viewLifecycleOwner) { firebaseHighlights ->
            val displayList = ArrayList<StoryHighlightModel>()
            displayList.add(
                StoryHighlightModel(
                    id = "ADD_NEW",
                    name = "Mới",
                    coverUrl = ""
                )
            )
            displayList.addAll(firebaseHighlights)
            highlightAdapter.submitList(displayList)
            binding.rvStoryHighlights.visibility = View.VISIBLE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner, Observer { errorMsg ->
            if (!errorMsg.isNullOrEmpty()) {
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            }
        })

        // --- [THÊM VÀO] Lắng nghe Story của tôi để Bật/Tắt vòng sáng ---
        // --- [SỬA LẠI ĐOẠN NÀY] ---
        viewModel.myStories.observe(viewLifecycleOwner) { stories ->
            // 1. Lấy thời gian hiện tại
            val currentTime = System.currentTimeMillis()
            val twentyFourHoursInMillis = 24 * 60 * 60 * 1000L // 24 giờ đổi ra mili-giây

            // 2. Lọc danh sách: Chỉ lấy những story chưa quá 24h
            val activeStories = stories.filter { story ->
                val timeDiff = currentTime - story.createdAt
                timeDiff < twentyFourHoursInMillis
            }

            // 3. Kiểm tra danh sách ĐÃ LỌC
            if (activeStories.isNotEmpty()) {
                // Chỉ sáng đèn nếu còn story "sống" (chưa hết hạn)
                updateStoryRing(true)
            } else {
                // Tắt đèn nếu không có story nào hoặc toàn story cũ
                updateStoryRing(false)
            }
        }
        // -------------------------------------------------------------
        // -------------------------------------------------------------
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                updateCountsAndFilter() // Gọi hàm cập nhật chung
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateCountsAndFilter() {
        binding.tvPostCount.text = allPosts.size.toString()
        val selectedTabPosition = binding.tabLayout.selectedTabPosition

        val (listToShow, emptyMessageTitle, emptyMessageSub) = if (selectedTabPosition == 0) {
            Triple(allPosts, "Chưa có bài đăng", "Ảnh và video của bạn sẽ ở đây.")
        } else {
            Triple(allReels, "Chưa có Reels", "Reels của bạn sẽ xuất hiện ở đây.")
        }

        postAdapter.submitList(listToShow)

        if (listToShow.isNotEmpty()) {
            binding.rvPhotos.visibility = View.VISIBLE
            binding.llEmptyPhotos.visibility = View.GONE
        } else {
            binding.rvPhotos.visibility = View.GONE
            binding.llEmptyPhotos.visibility = View.VISIBLE
            binding.tvEmptyMessage.text = emptyMessageTitle
            binding.tvEmptySubMessage.text = emptyMessageSub
        }
    }

    private fun setupClickListeners() {
        binding.ivBackButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.ivMenuButton.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_personalProfileFragment_to_settingFragment)
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi NavGraph: ...to_settingFragment", Toast.LENGTH_LONG).show()
            }
        }

        binding.btnEditProfile.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_personalProfileFragment_to_editProfileNameFragment)
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi NavGraph: ...to_editProfileNameFragment", Toast.LENGTH_LONG).show()
            }
        }

        binding.btnShareProfile.setOnClickListener {
            val currentUser = viewModel.userProfile.value
            if (currentUser != null) {
                shareUserProfile(currentUser)
            } else {
                Toast.makeText(context, "Đang tải dữ liệu...", Toast.LENGTH_SHORT).show()
            }
        }

        binding.ivProfileImage.setOnClickListener {
            Toast.makeText(context, "Mở trình chọn ảnh", Toast.LENGTH_SHORT).show()
        }

        binding.llFollowersWrapper.setOnClickListener {
            val userId = viewModel.userProfile.value?.userId
            if (userId != null) {
                val bundle = Bundle().apply {
                    putString("userId", userId)
                    putString("listType", "followers")
                }
                try {
                    findNavController().navigate(R.id.action_personalProfileFragment_to_followListFragment, bundle)
                } catch (e: Exception) {
                    Toast.makeText(context, "Lỗi NavGraph: " + e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.llFollowingWrapper.setOnClickListener {
            val userId = viewModel.userProfile.value?.userId
            if (userId != null) {
                val bundle = Bundle().apply {
                    putString("userId", userId)
                    putString("listType", "following")
                }
                try {
                    findNavController().navigate(R.id.action_personalProfileFragment_to_followListFragment, bundle)
                } catch (e: Exception) {
                    Toast.makeText(context, "Lỗi NavGraph: " + e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun shareUserProfile(user: UserModel) {
        val profileLink = "https://my-social-app.com/profile/${user.userId}"
        val bioLine = if (user.bio.isNullOrEmpty()) "" else "\"${user.bio}\"\n\n"
        val shareText = """
            Check out ${user.fullName} on SocialApp!
            
            $bioLine
            See their profile: $profileLink
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Check out ${user.fullName}'s Profile")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Profile via"))
    }

    private fun updateUi(user: UserModel) {
        binding.tvUsername.text = user.fullName

        if (user.bio.isNullOrEmpty()) {
            binding.tvBio.visibility = View.GONE
        } else {
            binding.tvBio.visibility = View.VISIBLE
            binding.tvBio.text = user.bio
        }

        binding.tvFollowersCount.text = formatCount(user.followerCount)
        binding.tvFollowingCount.text = formatCount(user.followingCount)

        Glide.with(this)
            .load(user.headerPictureUrl)
            .centerCrop()
            .placeholder(R.drawable.image_backgroud)
            .error(R.drawable.image_backgroud)
            .into(binding.ivHeaderImage)

        Glide.with(this)
            .load(user.profilePictureUrl)
            .placeholder(R.drawable.image_avata_user)
            .error(R.drawable.image_avata_user)
            .circleCrop()
            .into(binding.ivProfileImage)

        // --- [ĐÃ XÓA] dòng val userHasStory = true gây lỗi luôn sáng ---
    }

    private fun formatCount(count: Int): String {
        return when {
            count >= 1_000_000 -> String.format("%.1fm", count / 1_000_000.0)
            count >= 100_000 -> "${count / 1_000}k"
            count >= 10_000 -> String.format("%.1fk", count / 1_000.0)
            count >= 1_000 -> "${count / 1_000}k"
            else -> count.toString()
        }
    }


    private fun updateStoryRing(hasStory: Boolean) {
        val container = binding.flProfileImageContainer // FrameLayout vừa tạo

        if (hasStory) {
            // Có Story: Hiện vòng Gradient
            container.setBackgroundResource(R.drawable.bg_story_ring)

            // Tùy chọn: Thêm padding để vòng sáng hiện ra (nếu bị mất padding khi set background)
            container.setPadding(8, 8, 8, 8) // 8px ~ 3dp
        } else {
            // Không có Story: Ẩn vòng (Set background null hoặc màu trắng/trong suốt)
            container.background = null

            // Reset padding về 0 để ảnh to ra bình thường hoặc giữ nguyên tùy thiết kế
            container.setPadding(0, 0, 0, 0)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvPhotos.adapter = null
        binding.rvStoryHighlights.adapter = null
        _binding = null
    }
}