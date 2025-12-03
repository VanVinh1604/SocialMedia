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

        // ✅ ẨN HOÀN TOÀN UI CỦA MAINACTIVITY
        hideSystemUI()
        hideBottomNavigation()
        removeNavHostPadding()

        viewPager = view.findViewById(R.id.viewPagerReels)
        progressBar = view.findViewById(R.id.progressBar)

        recommendationEngine = RecommendationEngine(database)

        setupAdapter()
        setupViewPager()

   /*     // 🐛 NÚT DEBUG
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
        }*/

        // 🎯 LOAD BATCH ĐẦU TIÊN
        loadMoreRecommendedReels()
    }

    // ✅ ẨN HOÀN TOÀN SYSTEM UI (STATUS BAR + NAVIGATION BAR)
    private fun hideSystemUI() {
        @Suppress("DEPRECATION")
        activity?.window?.decorView?.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
    }

    // ✅ KHÔI PHỤC LẠI SYSTEM UI
    private fun showSystemUI() {
        @Suppress("DEPRECATION")
        activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    // ✅ ẨN BOTTOM NAVIGATION + FAB
    private fun hideBottomNavigation() {
        activity?.findViewById<View>(R.id.container)?.visibility = View.GONE
    }

    // ✅ HIỆN LẠI BOTTOM NAVIGATION + FAB
    private fun showBottomNavigation() {
        activity?.findViewById<View>(R.id.container)?.visibility = View.VISIBLE
    }

    // ✅ XÓA PADDING BOTTOM CỦA NAVHOSTFRAGMENT
    private fun removeNavHostPadding() {
        activity?.findViewById<View>(R.id.navHostFragment)?.apply {
            setPadding(0, 0, 0, 0)
        }
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
                Log.d(TAG, "👥 FOLLOW: ${reel.userId}")
                Toast.makeText(requireContext(), "Following ${reel.userId}", Toast.LENGTH_SHORT).show()
            },

            onProfileClick = { reel, _ ->
                Log.d(TAG, "👤 PROFILE CLICK: ${reel.userId}")
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
                if (videoStartTime > 0 && position > 0) {
                    val watchDuration = System.currentTimeMillis() - videoStartTime
                    val previousReel = reelsList[position - 1]

                    Log.d(TAG, "⏱️ Watched ${previousReel.reelId.take(8)} for ${watchDuration}ms")

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

                // 🎬 VIDEO HIỆN TẠI
                if (position < reelsList.size) {
                    val currentReel = reelsList[position]
                    Log.d(TAG, "👁️ VIEW: ${currentReel.reelId.take(8)} (${position + 1}/${reelsList.size})")

                    recommendationEngine.recordInteraction(
                        reelId = currentReel.reelId,
                        interactionType = "VIEW"
                    )

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
        if (isLoadingMore || !hasMoreReels) return

        val remainingReels = reelsList.size - currentPosition - 1

        Log.d(TAG, "📊 Position: $currentPosition, Total: ${reelsList.size}, Remaining: $remainingReels")

        if (remainingReels <= LOAD_MORE_THRESHOLD) {
            Log.d(TAG, "🔄 Triggering load more...")
            loadMoreRecommendedReels()
        }
    }

    // 📦 LOAD BATCH VIDEO MỚI
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
                Toast.makeText(requireContext(), "Đã hết video mới!", Toast.LENGTH_SHORT).show()
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

    // 📋 LOAD TẤT CẢ (BACKUP - KHI KHÔNG CÓ ĐỀ XUẤT)
    private fun loadAllReels() {
        progressBar.visibility = View.VISIBLE

        database.reference.child("Reels")
            .orderByChild("createdAt")
            .limitToLast(50)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    reelsList.clear()
                    for (child in snapshot.children) {
                        val reel = child.getValue(ReelModel::class.java)
                        reel?.let { reelsList.add(it) }
                    }
                    reelsList.reverse()
                    progressBar.visibility = View.GONE
                    adapter.notifyDataSetChanged()

                    if (reelsList.isNotEmpty()) {
                        viewPager.post {
                            getViewHolderAtPosition(0)?.let { adapter.playVideo(0, it) }
                            videoStartTime = System.currentTimeMillis()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Load failed: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onPause() {
        super.onPause()

        if (videoStartTime > 0 && viewPager.currentItem < reelsList.size) {
            val watchDuration = System.currentTimeMillis() - videoStartTime
            val currentReel = reelsList[viewPager.currentItem]

            if (watchDuration > 2000) {
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

        // ✅ ẨN LẠI UI KHI QUAY LẠI REELSFRAGMENT
        hideSystemUI()
        hideBottomNavigation()
        removeNavHostPadding()

        // Play video hiện tại
        getViewHolderAtPosition(viewPager.currentItem)?.let {
            adapter.playVideo(viewPager.currentItem, it)
        }
        videoStartTime = System.currentTimeMillis()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // ✅ KHÔI PHỤC LẠI UI KHI RỜI KHỎI REELSFRAGMENT
        showSystemUI()
        showBottomNavigation()


        videoStartTime = 0
        viewPager.adapter = null
    }
}