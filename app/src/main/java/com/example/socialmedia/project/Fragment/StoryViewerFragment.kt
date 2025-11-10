package com.example.socialmedia.project.Fragment

import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentStoryViewerBinding
import com.example.socialmedia.project.Adapter.StoryViewerAdapter
import com.example.socialmedia.project.Domain.Model.StoryModel
import com.example.socialmedia.project.Helper.MultiStoryProgressHelper
import com.example.socialmedia.project.ViewModel.StoryViewModel

class StoryViewerFragment : Fragment() {

    private lateinit var binding: FragmentStoryViewerBinding
    private val storyViewModel: StoryViewModel by viewModels()

    private var stories: List<StoryModel> = emptyList()
    private var usersWithStories: List<String> = emptyList()
    private var currentUserIndex = 0
    private var currentStoryIndex = 0
    private var progressHelper: MultiStoryProgressHelper? = null

    // Lưu progress và vị trí hiện tại của từng user
    private val userStoryPositions = mutableMapOf<String, Int>()
    private val userSegmentProgressMap = mutableMapOf<String, MutableMap<Int, Float>>()

    private lateinit var gestureDetector: GestureDetectorCompat

    override fun onCreateView(
        inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentStoryViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val userId = arguments?.getString("userId") ?: return
        val allUsersWithStories = arguments?.getStringArrayList("usersList") ?: arrayListOf()
        usersWithStories = allUsersWithStories
        currentUserIndex = usersWithStories.indexOf(userId).takeIf { it >= 0 } ?: 0

        setupViewPager()
        setupGestureDetector()
        loadStoriesForUser(userId)

        binding.btnClose.setOnClickListener { safeExitStory() }
        binding.btnMore.setOnClickListener {
            Toast.makeText(requireContext(), "More clicked", Toast.LENGTH_SHORT).show()
        }

        binding.etReplyMessage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) progressHelper?.pause() else progressHelper?.resume()
        }

        binding.btnSendReply.setOnClickListener {
            val msg = binding.etReplyMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                Toast.makeText(requireContext(), "Đã gửi: $msg", Toast.LENGTH_SHORT).show()
                binding.etReplyMessage.text.clear()
                binding.etReplyMessage.clearFocus()
            } else {
                Toast.makeText(requireContext(), "❤️", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupViewPager() {
        // Disable swipe người dùng vì ta dùng gesture riêng
        binding.storyViewPager.isUserInputEnabled = false

        // PageTransformer tạo hiệu ứng lật trang kiểu Instagram
        binding.storyViewPager.setPageTransformer { page, position ->
            val scale = 0.85f.coerceAtLeast(1 - kotlin.math.abs(position))
            val alpha = 0.5f.coerceAtLeast(1 - kotlin.math.abs(position))
            page.scaleX = scale
            page.scaleY = scale
            page.alpha = alpha
            page.translationX = -position * page.width
        }
    }

    private fun setupGestureDetector() {
        val screenWidth = resources.displayMetrics.widthPixels
        val leftZone = screenWidth * 0.3f
        val rightZone = screenWidth * 0.7f

        gestureDetector = GestureDetectorCompat(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent) = true
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val x = e.x
                when {
                    x < leftZone -> navigateToPreviousStoryOrUser()
                    x > rightZone -> navigateToNextStoryOrUser()
                    else -> togglePauseResume()
                }
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                progressHelper?.pause()
            }
        })

        binding.root.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                progressHelper?.resume()
            }
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun loadStoriesForUser(userId: String) {
        storyViewModel.loadUserStories(userId)
        storyViewModel.stories.observe(viewLifecycleOwner) { loadedStories ->
            if (!isAdded) return@observe
            if (loadedStories.isEmpty()) {
                Toast.makeText(requireContext(), "Không có story!", Toast.LENGTH_SHORT).show()
                return@observe
            }

            stories = loadedStories
            binding.storyViewPager.adapter = StoryViewerAdapter(stories)

            // Load vị trí trước đó của user
            currentStoryIndex = userStoryPositions[userId] ?: 0
            currentStoryIndex = currentStoryIndex.coerceIn(0, stories.size - 1)
            binding.storyViewPager.setCurrentItem(currentStoryIndex, false)
            updateTopUserInfo(currentStoryIndex)

            if (progressHelper == null) {
                progressHelper = MultiStoryProgressHelper(
                    container = binding.storyProgressContainer,
                    duration = 5000L,
                    onFinishSegment = { navigateToNextStoryOrUser() },
                    onFinishAll = { navigateToNextUser() },
                    onProgressUpdate = { index, progressMap ->
                        userSegmentProgressMap[userId] = progressMap.toMutableMap()
                        userStoryPositions[userId] = index
                    }
                )
            }

            progressHelper?.setup(stories.size)

            val savedProgress = userSegmentProgressMap[userId]
            if (savedProgress != null) {
                progressHelper?.restoreProgress(currentStoryIndex, savedProgress)
            } else {
                progressHelper?.goTo(currentStoryIndex, resumeImmediately = true, resetCurrentSegment = true)
            }

            progressHelper?.start()
        }
    }

    private fun navigateToNextStoryOrUser() {
        if (currentStoryIndex < stories.size - 1) {
            currentStoryIndex++
            binding.storyViewPager.setCurrentItem(currentStoryIndex, true)
            updateTopUserInfo(currentStoryIndex)
            progressHelper?.goTo(currentStoryIndex)
        } else {
            navigateToNextUser()
        }
    }

    private fun navigateToPreviousStoryOrUser() {
        if (currentStoryIndex > 0) {
            currentStoryIndex--
            binding.storyViewPager.setCurrentItem(currentStoryIndex, true)
            updateTopUserInfo(currentStoryIndex)
            progressHelper?.goTo(currentStoryIndex, resumeImmediately = true, resetCurrentSegment = false)
        } else {
            navigateToPreviousUser()
        }
    }

    private fun navigateToNextUser() {
        if (currentUserIndex >= usersWithStories.size - 1) {
            safeExitStory()
            return
        }
        val nextIndex = currentUserIndex + 1 // chưa đổi currentUserIndex
        animateUserChange(toNext = true) {
            // Load stories cho user kế tiếp
            loadStoriesForUser(usersWithStories[nextIndex])
            // Sau khi container đã swap xong thì mới update index
            currentUserIndex = nextIndex
        }
    }


    private fun navigateToPreviousUser() {
        if (currentUserIndex <= 0) return
        val prevIndex = currentUserIndex - 1
        animateUserChange(toNext = false) {
            loadStoriesForUser(usersWithStories[prevIndex])
            currentUserIndex = prevIndex
        }
    }



    private fun animateUserChange(toNext: Boolean, onMidAnimation: () -> Unit) {
        val root = binding.root
        // Pivot giữa để xoay như khối
        root.pivotX = root.width / 2f
        root.pivotY = root.height / 2f
        root.cameraDistance = 8000 * resources.displayMetrics.density

        val startRotation = 0f
        val midRotation = if (toNext) 90f else -90f
        val endRotation = 0f

        // Bước 1: xoay 90 độ (khối nghiêng)
        root.animate()
            .rotationY(midRotation)
            .setDuration(250)
            .withEndAction {
                // Thực hiện load dữ liệu user mới ở giữa animation
                onMidAnimation()
                // Đặt rotation ngược để xoay về 0
                root.rotationY = if (toNext) -90f else 90f
                // Bước 2: xoay trở về vị trí chính
                root.animate()
                    .rotationY(endRotation)
                    .setDuration(250)
                    .start()
            }
            .start()
    }


    private fun safeExitStory() {
        progressHelper?.reset()
        progressHelper = null
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    private fun updateTopUserInfo(index: Int) {
        if (!isAdded || stories.isEmpty() || index !in stories.indices) return
        val story = stories[index]
        binding.tvUserName.text = story.userName
        Glide.with(this)
            .load(story.userProfileImage)
            .placeholder(R.drawable.default_avatar)
            .circleCrop()
            .into(binding.imgUserAvatar)
    }

    private fun togglePauseResume() {
        progressHelper?.let { if (it.isRunning) it.pause() else it.resume() }
    }

    override fun onPause() {
        super.onPause()
        progressHelper?.pause()
    }

    override fun onResume() {
        super.onResume()
        progressHelper?.resume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressHelper?.reset()
        progressHelper = null
    }
}
