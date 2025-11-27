package com.example.socialmedia.project.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.socialmedia.R
import com.example.socialmedia.project.Adapter.ReelsAdapter
import com.example.socialmedia.project.Domain.Model.ReelModel
import com.example.socialmedia.project.Utils.RecommendationEngine
import com.google.firebase.database.*

@UnstableApi
class ReelsFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: ReelsAdapter
    private val reelsList = mutableListOf<ReelModel>()
    private val database = FirebaseDatabase.getInstance()
    private lateinit var recommendationEngine: RecommendationEngine

    private var videoStartTime = 0L

    // 🔄 BIẾN QUẢN LÝ LOAD MORE
    private var isLoadingMore = false
    private var hasMoreReels = true
    private val BATCH_SIZE = 10
    private val LOAD_MORE_THRESHOLD = 5

    // 🆕 BIẾN MỚI: QUẢN LÝ CHẾ ĐỘ XEM PROFILE
    private var isProfileMode = false
    private var targetUserId: String? = null
    private var startReelId: String? = null

    private val TAG = "ReelsFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reels, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. NHẬN DỮ LIỆU TỪ BUNDLE (NẾU CÓ)
        arguments?.let {
            targetUserId = it.getString("userId")
            startReelId = it.getString("startReelId")
        }

        // Nếu có userId -> Đang xem từ Profile
        if (targetUserId != null) {
            isProfileMode = true
        }

        hideBottomNavigation()
        removeNavHostPadding()

        viewPager = view.findViewById(R.id.viewPagerReels)
        progressBar = view.findViewById(R.id.progressBar)

        // Ẩn nút debug nếu đang xem profile
        view.findViewById<View>(R.id.btnDebug)?.visibility = if (isProfileMode) View.GONE else View.VISIBLE

        recommendationEngine = RecommendationEngine(database)

        setupAdapter()
        setupViewPager()

        // 🐛 NÚT DEBUG (Chỉ hoạt động khi xem chế độ đề xuất)
        view.findViewById<View>(R.id.btnDebug)?.setOnClickListener {
            recommendationEngine.getHashtagScoresDebug { scores ->
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "📊 HASHTAG SCORES DEBUG:")
                scores.entries
                    .sortedByDescending { it.value }
                    .forEachIndexed { index, (tag, score) ->
                        Log.d(TAG, "   ${index + 1}. #$tag = $score điểm")
                    }
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Toast.makeText(requireContext(), "Check Logcat!", Toast.LENGTH_SHORT).show()
            }
        }

        // 🎯 QUYẾT ĐỊNH LOAD DỮ LIỆU
        if (isProfileMode) {
            loadUserReels(targetUserId!!)
        } else {
            loadMoreRecommendedReels()
        }
    }

    private fun hideBottomNavigation() {
        activity?.findViewById<View>(R.id.container)?.visibility = View.GONE
    }

    private fun removeNavHostPadding() {
        activity?.findViewById<View>(R.id.navHostFragment)?.setPadding(0, 0, 0, 0)
    }

    private fun setupAdapter() {
        adapter = ReelsAdapter(
            reels = reelsList,

            onLikeClick = { reel, _ ->
                Log.d(TAG, "❤️ LIKE: ${reel.reelId.take(8)}")
                recommendationEngine.recordInteraction(
                    reelId = reel.reelId,
                    interactionType = "LIKE"
                )
            },

            onCommentClick = { reel, _ ->
                Log.d(TAG, "💬 COMMENT: ${reel.reelId.take(8)}")
                recommendationEngine.recordInteraction(
                    reelId = reel.reelId,
                    interactionType = "COMMENT"
                )

                val commentsSheet = CommentsBottomSheet.newInstance(reel.reelId, reel.userId)
                commentsSheet.show(childFragmentManager, "CommentsBottomSheet")
            },

            onShareClick = { reel, _ ->
                Log.d(TAG, "📤 SHARE: ${reel.reelId.take(8)}")
                recommendationEngine.recordInteraction(
                    reelId = reel.reelId,
                    interactionType = "SHARE"
                )
                Toast.makeText(requireContext(), "Share: ${reel.reelId}", Toast.LENGTH_SHORT).show()
            },

            onFollowClick = { reel, _ ->
                Toast.makeText(requireContext(), "Following ${reel.userId}", Toast.LENGTH_SHORT).show()
            },

            onProfileClick = { reel, _ ->
                // Nếu đang ở trang profile của người đó rồi thì không cần click nữa
                if (!isProfileMode) {
                    Toast.makeText(requireContext(), "Profile: ${reel.userId}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun setupViewPager() {
        viewPager.adapter = adapter
        viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                // ⏱️ XỬ LÝ VIDEO TRƯỚC ĐÓ
                if (videoStartTime > 0 && position > 0 && position - 1 < reelsList.size) {
                    val watchDuration = System.currentTimeMillis() - videoStartTime
                    val previousReel = reelsList[position - 1]

                    Log.d(TAG, "⏱️ Watched ${previousReel.reelId.take(8)} for ${watchDuration}ms")

                    // Chỉ ghi nhận tương tác nếu KHÔNG PHẢI chế độ xem profile cá nhân (để tránh làm lệch thuật toán)
                    if (!isProfileMode) {
                        if (watchDuration < 2000) {
                            Log.d(TAG, "🚫 SKIPPED")
                            recommendationEngine.recordInteraction(
                                reelId = previousReel.reelId,
                                interactionType = "SKIP"
                            )
                        } else {
                            recommendationEngine.recordInteraction(
                                reelId = previousReel.reelId,
                                interactionType = "WATCH_TIME",
                                duration = watchDuration
                            )
                        }
                    }
                }

                // 🎬 VIDEO HIỆN TẠI
                if (position < reelsList.size) {
                    val currentReel = reelsList[position]
                    Log.d(TAG, "👁️ VIEW: ${currentReel.reelId.take(8)} (${position + 1}/${reelsList.size})")

                    if (!isProfileMode) {
                        recommendationEngine.recordInteraction(
                            reelId = currentReel.reelId,
                            interactionType = "VIEW"
                        )
                    }

                    videoStartTime = System.currentTimeMillis()
                }

                // 🔄 KIỂM TRA VÀ LOAD THÊM
                checkAndLoadMore(position)

                // 🎥 PLAY/PAUSE
                val previousPosition = position - 1
                if (previousPosition >= 0) {
                    getViewHolderAtPosition(previousPosition)?.let { adapter.pauseCurrentVideo(it) }
                }
                getViewHolderAtPosition(position)?.let { adapter.playVideo(position, it) }
            }
        })
    }

    private fun getViewHolderAtPosition(position: Int): ReelsAdapter.ReelViewHolder? {
        val recyclerView = viewPager.getChildAt(0) as? RecyclerView
        return recyclerView?.findViewHolderForAdapterPosition(position) as? ReelsAdapter.ReelViewHolder
    }

    // 🔄 KIỂM TRA VÀ LOAD THÊM VIDEO
    private fun checkAndLoadMore(currentPosition: Int) {
        // Nếu đang ở chế độ Profile thì KHÔNG load thêm video đề xuất
        if (isProfileMode) return

        if (isLoadingMore || !hasMoreReels) return

        val remainingReels = reelsList.size - currentPosition - 1
        Log.d(TAG, "📊 Position: $currentPosition, Total: ${reelsList.size}, Remaining: $remainingReels")

        // Nếu còn <= 5 video → load thêm
        if (remainingReels <= LOAD_MORE_THRESHOLD) {
            Log.d(TAG, "🔄 Triggering load more...")
            loadMoreRecommendedReels()
        }
    }

    // 📦 LOAD BATCH VIDEO MỚI (CHO CHẾ ĐỘ ĐỀ XUẤT)
    private fun loadMoreRecommendedReels() {
        if (isLoadingMore) {
            Log.d(TAG, "⚠️ Already loading, skipping...")
            return
        }

        isLoadingMore = true
        progressBar.visibility = View.VISIBLE

        Log.d(TAG, "📦 Loading batch of $BATCH_SIZE reels...")

        recommendationEngine.getRecommendedReelsBatch(BATCH_SIZE) { newReels ->
            isLoadingMore = false
            progressBar.visibility = View.GONE

            if (newReels.isEmpty()) {
                Log.w(TAG, "⚠️ No more reels available")
                hasMoreReels = false
                // Chỉ thông báo nếu list đang trống
                if (reelsList.isEmpty()) {
                    Toast.makeText(requireContext(), "Đã hết video mới!", Toast.LENGTH_SHORT).show()
                }
                return@getRecommendedReelsBatch
            }

            val startPosition = reelsList.size
            reelsList.addAll(newReels)

            Log.d(TAG, "✅ Added ${newReels.size} reels (Total: ${reelsList.size})")

            adapter.notifyItemRangeInserted(startPosition, newReels.size)

            // Nếu là lần đầu load → play video đầu
            if (startPosition == 0 && reelsList.isNotEmpty()) {
                viewPager.post {
                    getViewHolderAtPosition(0)?.let { adapter.playVideo(0, it) }
                    videoStartTime = System.currentTimeMillis()
                }
            }
        }
    }

    // 🆕 HÀM MỚI: LOAD REELS CỦA MỘT USER CỤ THỂ
    private fun loadUserReels(userId: String) {
        progressBar.visibility = View.VISIBLE

        database.reference.child("Reels")
            .orderByChild("userId")
            .equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    reelsList.clear()
                    for (child in snapshot.children) {
                        val reel = child.getValue(ReelModel::class.java)
                        reel?.let { reelsList.add(it) }
                    }

                    // Sắp xếp video mới nhất lên đầu
                    reelsList.sortByDescending { it.createdAt }

                    progressBar.visibility = View.GONE
                    adapter.notifyDataSetChanged()

                    // SCROLL ĐẾN VIDEO ĐƯỢC CLICK TỪ PROFILE
                    if (reelsList.isNotEmpty()) {
                        var startIndex = 0
                        if (startReelId != null) {
                            val foundIndex = reelsList.indexOfFirst { it.reelId == startReelId }
                            if (foundIndex != -1) {
                                startIndex = foundIndex
                            }
                        }

                        // Scroll ngay lập tức (false = no smooth scroll) để user thấy ngay video đó
                        viewPager.setCurrentItem(startIndex, false)

                        // Play video đó
                        viewPager.post {
                            getViewHolderAtPosition(startIndex)?.let { adapter.playVideo(startIndex, it) }
                            videoStartTime = System.currentTimeMillis()
                        }
                    } else {
                        Toast.makeText(context, "User này chưa có Reels", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Lỗi tải Reels: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onPause() {
        super.onPause()

        if (videoStartTime > 0 && viewPager.currentItem < reelsList.size) {
            val watchDuration = System.currentTimeMillis() - videoStartTime
            val currentReel = reelsList[viewPager.currentItem]

            // Chỉ ghi nhận nếu không phải đang xem profile
            if (!isProfileMode && watchDuration > 2000) {
                recommendationEngine.recordInteraction(
                    reelId = currentReel.reelId,
                    interactionType = "WATCH_TIME",
                    duration = watchDuration
                )
            }
        }

        getViewHolderAtPosition(viewPager.currentItem)?.let { adapter.pauseCurrentVideo(it) }
    }

    override fun onResume() {
        super.onResume()
        // Đảm bảo video tiếp tục chạy khi quay lại
        getViewHolderAtPosition(viewPager.currentItem)?.let {
            adapter.playVideo(viewPager.currentItem, it)
        }
        videoStartTime = System.currentTimeMillis()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        videoStartTime = 0
        viewPager.adapter = null
    }
}