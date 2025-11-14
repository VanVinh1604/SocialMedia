package com.example.socialmedia.project.Fragment

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.VideoView
import android.widget.Toast
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentStoryViewerBinding
import com.example.socialmedia.project.Adapter.StoryViewerAdapter
import com.example.socialmedia.project.Adapter.StoryViewerBottomSheetAdapter
import com.example.socialmedia.project.Domain.Enum.MediaType
import com.example.socialmedia.project.Domain.Model.StoryModel
import com.example.socialmedia.project.Domain.Model.StoryViewerItem
import com.example.socialmedia.project.Helper.MultiStoryProgressHelper
import com.example.socialmedia.project.Server.Firebase.FirebaseService
import com.example.socialmedia.project.ViewModel.StoryViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.database.*

class StoryViewerFragment : Fragment() {

    private lateinit var binding: FragmentStoryViewerBinding
    private val storyViewModel: StoryViewModel by viewModels()

    private val firebaseService = FirebaseService()

    private var stories: List<StoryModel> = emptyList()
    private var usersWithStories: List<String> = emptyList()
    private var currentUserIndex = 0
    private var currentStoryIndex = 0
    private var progressHelper: MultiStoryProgressHelper? = null

    // Lưu progress và vị trí hiện tại của từng user
    private val userStoryPositions = mutableMapOf<String, Int>()
    private val userSegmentProgressMap = mutableMapOf<String, MutableMap<Int, Float>>()

    private lateinit var gestureDetector: GestureDetectorCompat
    private var currentVideoView: VideoView? = null
    private var currentViewListener: ValueEventListener? = null
    private var currentStoryIdListening: String? = null
    private var storyAdapter: StoryViewerAdapter? = null

    private fun getCurrentUserId(): String = usersWithStories.getOrNull(currentUserIndex) ?: ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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
        setupUIListeners()

        storyViewModel.stories.observe(viewLifecycleOwner) { loadedStories ->
            if (!isAdded) return@observe
            if (loadedStories.isEmpty()) {
                Toast.makeText(requireContext(), "Không có story!", Toast.LENGTH_SHORT).show()
                return@observe
            }
            displayStories(loadedStories)
        }

        loadStoriesForUser(userId)
    }

    private fun setupUIListeners() {
        binding.btnClose.setOnClickListener { safeExitStory() }
        binding.btnMore.setOnClickListener {
            showMoreOptionsBottomSheet()
        }

        binding.etReplyMessage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                progressHelper?.pause()
                currentVideoView?.pause()
            } else {
                progressHelper?.resume()
                getCurrentVideoViewIfNeeded()?.start()
            }
        }

        binding.btnSendReply.setOnClickListener {
            val msg = binding.etReplyMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                Toast.makeText(requireContext(), "Đã gửi: $msg", Toast.LENGTH_SHORT).show()
                binding.etReplyMessage.text.clear()
                binding.etReplyMessage.clearFocus()
            } else Toast.makeText(requireContext(), "❤️", Toast.LENGTH_SHORT).show()
        }

        binding.tvViewerInfo.setOnClickListener {
            val story = stories.getOrNull(currentStoryIndex) ?: return@setOnClickListener
            showStoryViewsBottomSheet(story)
        }
        binding.btnLikeStory.setOnClickListener {
            val story = stories.getOrNull(currentStoryIndex) ?: return@setOnClickListener
            val currentUserId = firebaseService.getCurrentUserId() ?: return@setOnClickListener

            firebaseService.toggleStoryLike(story.storyId, currentUserId) { liked ->
                // cập nhật UI nút Like
                binding.btnLikeStory.isSelected = liked

                // cập nhật userLikes map
                story.userLikes[currentUserId] = liked

                // --- Animation ❤️ ---
                if (liked) showHeartAnimation()
            }
        }


    }

    private fun setupProgressHelper() {
        val durations = stories.map { story ->
            if (story.mediaType == MediaType.VIDEO) story.duration.takeIf { it > 0 }?.toLong() ?: 5000L else 5000L
        }

        val uid = getCurrentUserId()
        val savedIndex = userStoryPositions[uid] ?: 0
        val savedProgress = userSegmentProgressMap[uid]

        progressHelper = MultiStoryProgressHelper(
            container = binding.storyProgressContainer,
            segmentDurations = durations,
            onFinishSegment = { navigateToNextStoryOrUser() },
            onFinishAll = { navigateToNextUser() },
            onProgressUpdate = { index, progressMap ->
                userSegmentProgressMap[uid] = progressMap.toMutableMap()
                userStoryPositions[uid] = index
            }
        ).apply {
            setup()
            restoreProgress(savedIndex, savedProgress)
            start()
        }
    }

    private fun loadStoriesForUser(userId: String) {
        storyViewModel.loadUserStories(userId)
    }

    private fun displayStories(loadedStories: List<StoryModel>) {
        resetForNewUser()

        stories = loadedStories
        val uid = getCurrentUserId()
        updateBottomLayoutForUser(uid)
        if (uid == firebaseService.getCurrentUserId()) observeStoryViewCountForUser(stories)

        currentStoryIndex = userStoryPositions[uid] ?: 0

        storyAdapter = StoryViewerAdapter(
            stories,
            onVideoReady = { videoView, position, _ ->
                if (position == currentStoryIndex) {
                    currentVideoView = videoView
                    val progress = userSegmentProgressMap[uid]?.get(currentStoryIndex) ?: 0f
                    if (stories[currentStoryIndex].mediaType == MediaType.VIDEO && progress > 0f) {
                        videoView?.seekTo((stories[currentStoryIndex].duration * progress).toInt())
                        videoView?.start()
                    }
                }
            },
            onItemClick = { story, _ -> showStoryViewsBottomSheet(story) },
            onVideoCompleted = { navigateToNextStoryOrUser() }
        )

        binding.storyViewPager.adapter = storyAdapter
        binding.storyViewPager.setCurrentItem(currentStoryIndex, false)
        updateTopUserInfo(currentStoryIndex)

        setupProgressHelper()

        binding.storyViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentStoryIndex = position
                val story = stories[position]
                updateBottomLayoutForUser(story.userId)
            }
        })
    }

    private fun getCurrentVideoViewIfNeeded(): VideoView? {
        if (currentVideoView != null) return currentVideoView

        val currentStory = stories.getOrNull(currentStoryIndex)
        if (currentStory?.mediaType == MediaType.VIDEO) {
            val recycler = binding.storyViewPager.getChildAt(0) as? RecyclerView
            val viewHolder = recycler?.findViewHolderForAdapterPosition(currentStoryIndex)
            val videoView = viewHolder?.itemView?.findViewById<VideoView>(R.id.videoStory)
            currentVideoView = videoView
            return videoView
        }
        return null
    }

    private fun observeStoryViewCountForUser(userStories: List<StoryModel>) {
        val currentUserId = firebaseService.getCurrentUserId() ?: return
        userStories.forEachIndexed { index, story ->
            firebaseService.listenStoryViewCount(story.storyId) { count ->
                story.viewCount = count
                if (story.userId == currentUserId && index == currentStoryIndex) {
                    binding.tvViewerInfo.text = "🌟Tin của bạn $count người xem"
                }
            }
        }
    }

    private fun setupViewPager() {
        binding.storyViewPager.isUserInputEnabled = false
        binding.storyViewPager.setPageTransformer { page, position ->
            val scale = 0.85f.coerceAtLeast(1 - kotlin.math.abs(position))
            val alpha = 0.5f.coerceAtLeast(1 - kotlin.math.abs(position))
            page.scaleX = scale
            page.scaleY = scale
            page.alpha = alpha
            page.translationX = -position * page.width
        }
        binding.storyViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentVideoView = null
                getCurrentVideoViewIfNeeded()
            }
        })
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
                getCurrentVideoViewIfNeeded()?.pause()
            }
        })

        binding.root.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                progressHelper?.resume()
                getCurrentVideoViewIfNeeded()?.start()
            }
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun animateStoryChange(toNext: Boolean, onAnimationEnd: () -> Unit) {
        currentVideoView?.pause()
        currentVideoView = null
        binding.storyViewPager.getChildAt(0)?.animate()?.alpha(0f)?.setDuration(150)?.withEndAction {
            onAnimationEnd()
            binding.storyViewPager.getChildAt(0)?.alpha = 1f
            getCurrentVideoViewIfNeeded()
        }?.start()
    }

    private fun animateUserChange(toNext: Boolean, onMidAnimation: () -> Unit) {
        val root = binding.root
        val duration = 400L
        val distance = 6000 * resources.displayMetrics.density
        root.cameraDistance = distance

        root.animate()
            .rotationY(if (toNext) -90f else 90f)
            .setDuration(duration / 2)
            .withEndAction {
                onMidAnimation()
                root.rotationY = if (toNext) 90f else -90f
                root.animate().rotationY(0f).setDuration(duration / 2).start()
            }.start()
    }

    private fun navigateToNextStoryOrUser() {
        if (stories.isEmpty()) return
        if (currentStoryIndex < stories.size - 1) {
            markStoryAsViewed(stories[currentStoryIndex])
            val nextIndex = currentStoryIndex + 1
            animateStoryChange(true) {
                currentStoryIndex = nextIndex
                binding.storyViewPager.setCurrentItem(currentStoryIndex, false)
                updateTopUserInfo(currentStoryIndex)
                progressHelper?.goTo(currentStoryIndex)
            }
        } else markStoryAsViewed(stories.last()).also { navigateToNextUser() }
    }

    private fun navigateToPreviousStoryOrUser() {
        if (stories.isEmpty()) return
        if (currentStoryIndex > 0) {
            val prevIndex = currentStoryIndex - 1
            animateStoryChange(false) {
                currentStoryIndex = prevIndex
                binding.storyViewPager.setCurrentItem(currentStoryIndex, false)
                updateTopUserInfo(currentStoryIndex)
                progressHelper?.goTo(currentStoryIndex, true, false)
            }
        } else navigateToPreviousUser()
    }

    private fun navigateToNextUser() {
        markAllCurrentUserStoriesAsViewed()
        if (currentUserIndex >= usersWithStories.size - 1) { safeExitStory(); return }
        val nextIndex = currentUserIndex + 1
        animateUserChange(true) {
            currentUserIndex = nextIndex
            resetForNewUser()
            loadStoriesForUser(usersWithStories[nextIndex])
        }
    }

    private fun navigateToPreviousUser() {
        markAllCurrentUserStoriesAsViewed()
        if (currentUserIndex <= 0) return
        val prevIndex = currentUserIndex - 1
        animateUserChange(false) {
            currentUserIndex = prevIndex
            resetForNewUser()
            loadStoriesForUser(usersWithStories[prevIndex])
        }
    }

    private fun resetForNewUser() {
        currentVideoView?.pause()
        currentVideoView = null
        progressHelper?.reset()
        progressHelper = null
        storyAdapter = null
        stories = emptyList()
        currentStoryIndex = 0
        binding.storyViewPager.adapter = null
    }

    private fun markAllCurrentUserStoriesAsViewed() {
        val loggedInUserId = firebaseService.getCurrentUserId() ?: return
        stories.forEach { firebaseService.markStoryAsViewed(it.storyId, loggedInUserId) }
    }

    private fun markStoryAsViewed(story: StoryModel) {
        val currentUser = firebaseService.getCurrentUserId() ?: return
        if (story.userId == currentUser) return
        story.isViewed = true
        story.userStoryViews[story.storyId] = true
        firebaseService.markStoryAsViewed(story.storyId, currentUser)
    }

    private fun updateBottomLayoutForUser(userId: String) {
        val loggedInUserId = firebaseService.getCurrentUserId() ?: return
        val currentStory = stories.getOrNull(currentStoryIndex) ?: return

        if (userId == loggedInUserId) {
            binding.tvViewerInfo.visibility = View.VISIBLE
            binding.etReplyMessage.visibility = View.GONE
            binding.btnSendReply.visibility = View.GONE

            currentStoryIdListening?.let { oldId ->
                currentViewListener?.let { listener ->
                    FirebaseDatabase.getInstance().getReference("stories")
                        .child(oldId).child("views").removeEventListener(listener)
                }
            }

            val storyId = currentStory.storyId
            val ref = FirebaseDatabase.getInstance().getReference("stories").child(storyId).child("views")
            currentStoryIdListening = storyId

            currentViewListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    binding.tvViewerInfo.text = "🌟Tin của bạn ${snapshot.childrenCount} người xem"
                }
                override fun onCancelled(error: DatabaseError) {}
            }

            ref.addValueEventListener(currentViewListener!!)
        } else {
            binding.tvViewerInfo.visibility = View.GONE
            binding.etReplyMessage.visibility = View.VISIBLE
            binding.btnSendReply.visibility = View.VISIBLE
        }
    }

    private fun updateTopUserInfo(index: Int) {
        if (!isAdded || stories.isEmpty() || index !in stories.indices) return
        val story = stories[index]
        val timeAgo = getTimeAgo(story.createdAt)
        binding.tvUserName.text = "${story.userName} · $timeAgo"
        binding.imgUserAvatar.post {
            Glide.with(this)
                .load(story.userProfileImage)
                .placeholder(R.drawable.default_avatar)
                .error(R.drawable.default_avatar)
                .circleCrop()
                .into(binding.imgUserAvatar)
        }
    }

    private fun showStoryViewsBottomSheet(story: StoryModel) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.bottom_sheet_story_views, null)

        val rvStoryViews: RecyclerView = view.findViewById(R.id.rvStoryViews)
        rvStoryViews.layoutManager = LinearLayoutManager(requireContext())
        val adapter = StoryViewerBottomSheetAdapter(emptyList())
        rvStoryViews.adapter = adapter
        binding.btnLikeStory.visibility = View.GONE


        // --- Realtime update ---
        storyViewModel.observeStoryViewsAndLikesRealtime(story) { viewers ->
            adapter.apply {
                this.viewers = viewers  // cần đổi viewers thành var trong adapter
                notifyDataSetChanged()
            }
        }

        bottomSheet.setContentView(view)
        bottomSheet.show()
    }


    private fun showMoreOptionsBottomSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.bottom_sheet_more_options, null)

        val tvDelete: TextView = view.findViewById(R.id.tvDelete)
        val tvArchive: TextView = view.findViewById(R.id.tvArchive)
        val tvCancel: TextView = view.findViewById(R.id.tvCancel)

        tvDelete.setOnClickListener {
            val story = stories.getOrNull(currentStoryIndex) ?: return@setOnClickListener
            firebaseService.deleteStory(story.storyId)  // cần implement hàm deleteStory trong FirebaseService
            Toast.makeText(requireContext(), "Đã xóa story", Toast.LENGTH_SHORT).show()
            bottomSheet.dismiss()
            navigateToNextStoryOrUser()  // tự động chuyển sang story tiếp theo
        }

        tvArchive.setOnClickListener {
            val story = stories.getOrNull(currentStoryIndex) ?: return@setOnClickListener
            firebaseService.archiveStory(story.storyId) // cần implement archiveStory trong FirebaseService
            Toast.makeText(requireContext(), "Đã lưu trữ story", Toast.LENGTH_SHORT).show()
            bottomSheet.dismiss()
        }

        tvCancel.setOnClickListener {
            bottomSheet.dismiss()
        }

        bottomSheet.setContentView(view)
        bottomSheet.show()
    }


    private fun togglePauseResume() {
        progressHelper?.let { helper ->
            if (helper.isRunning) {
                helper.pause()
                getCurrentVideoViewIfNeeded()?.pause()
                Log.d("StoryViewer", "⏸️ Paused")
            } else {
                helper.resume()
                val video = getCurrentVideoViewIfNeeded()
                if (video != null && stories.getOrNull(currentStoryIndex)?.mediaType == MediaType.VIDEO) video.start()
            }
        }
    }

    private fun showHeartAnimation() {
        val heart = TextView(requireContext()).apply {
            text = "❤️"
            textSize = 36f
            alpha = 0f
            x = (binding.root.width / 2 - 50).toFloat()  // canh giữa
            y = (binding.root.height / 2 - 50).toFloat()
        }

        binding.root.addView(heart)

        // Animation: pop + lên trên + mờ dần
        heart.animate()
            .alpha(1f)
            .scaleX(1.5f)
            .scaleY(1.5f)
            .setDuration(200)
            .withEndAction {
                heart.animate()
                    .translationYBy(-200f)   // lên trên 200px
                    .alpha(0f)               // mờ dần
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(500)
                    .withEndAction { binding.root.removeView(heart) }
                    .start()
            }
            .start()
    }


    private fun getTimeAgo(postTime: Long): String {
        val diff = System.currentTimeMillis() - postTime
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        return when {
            seconds < 60 -> "vừa xong"
            minutes < 60 -> "${minutes} phút trước"
            hours < 24 -> "${hours} giờ trước"
            else -> "${days} ngày trước"
        }
    }

    private fun safeExitStory() {
        progressHelper?.reset()
        progressHelper = null
        currentVideoView = null
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    override fun onPause() {
        super.onPause()
        progressHelper?.pause()
        getCurrentVideoViewIfNeeded()?.pause()
    }

    override fun onResume() {
        super.onResume()
        progressHelper?.resume()
        getCurrentVideoViewIfNeeded()?.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressHelper?.reset()
        progressHelper = null
        currentVideoView = null
        storyAdapter = null
    }
}
