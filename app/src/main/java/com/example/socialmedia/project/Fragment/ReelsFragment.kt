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

        hideBottomNavigation()
        removeNavHostPadding()

        viewPager = view.findViewById(R.id.viewPagerReels)
        progressBar = view.findViewById(R.id.progressBar)

        recommendationEngine = RecommendationEngine(database)

        setupAdapter()
        setupViewPager()
        view.findViewById<View>(R.id.btnDebug)?.setOnClickListener {
            recommendationEngine.getHashtagScoresDebug { scores ->
                Log.d("ReelsFragment", "HASHTAG SCORES: $scores")
            }
        }

        // Dùng loadAllReels() để test, sau này đổi thành loadRecommendedReels()
        loadAllReels()

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
                Log.d(TAG, "LIKE clicked: ${reel.reelId}, hashtags: ${reel.hashtags}")
                recommendationEngine.recordInteraction(
                    reelId = reel.reelId,
                    interactionType = "LIKE",

                )
            },
            onCommentClick = { reel, _ ->
                Log.d(TAG, "COMMENT clicked: ${reel.reelId}")
                recommendationEngine.recordInteraction(
                    reelId = reel.reelId,
                    interactionType = "COMMENT",

                )

                val commentsSheet = CommentsBottomSheet.newInstance(reel.reelId, reel.userId)
                commentsSheet.show(childFragmentManager, "CommentsBottomSheet")
            },
            onShareClick = { reel, _ ->
                Log.d(TAG, "SHARE clicked: ${reel.reelId}")
                recommendationEngine.recordInteraction(
                    reelId = reel.reelId,
                    interactionType = "SHARE",

                )
                Toast.makeText(requireContext(), "Share: ${reel.reelId}", Toast.LENGTH_SHORT).show()
            },
            onFollowClick = { reel, _ ->
                Toast.makeText(requireContext(), "Following ${reel.userId}", Toast.LENGTH_SHORT).show()
            },
            onProfileClick = { reel, _ ->
                Toast.makeText(requireContext(), "Profile: ${reel.userId}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setupViewPager() {
        viewPager.adapter = adapter
        viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                if (position < reelsList.size) {
                    val currentReel = reelsList[position]
                    Log.d(TAG, "VIEW: ${currentReel.reelId}, hashtags: ${currentReel.hashtags}")

                    recommendationEngine.recordInteraction(
                        reelId = currentReel.reelId,
                        interactionType = "VIEW",

                    )
                }

                val previousPosition = position - 1
                if (previousPosition >= 0) {
                    getViewHolderAtPosition(previousPosition)?.let { adapter.pauseCurrentVideo(it) }
                }
                getViewHolderAtPosition(position)?.let { adapter.playVideo(position, it) }
            }



        }) // ← Đóng callback
    } // ← Đóng hàm setupViewPager()

    private fun getViewHolderAtPosition(position: Int): ReelsAdapter.ReelViewHolder? {
        val recyclerView = viewPager.getChildAt(0) as? RecyclerView
        return recyclerView?.findViewHolderForAdapterPosition(position) as? ReelsAdapter.ReelViewHolder
    }

    private fun loadRecommendedReels() {
        progressBar.visibility = View.VISIBLE
        recommendationEngine.getRecommendedReels { recommendedReels ->
            reelsList.clear()
            if (recommendedReels.isEmpty()) {
                loadAllReels()
            } else {
                reelsList.addAll(recommendedReels)
                progressBar.visibility = View.GONE
                adapter.notifyDataSetChanged()
                viewPager.post { getViewHolderAtPosition(0)?.let { adapter.playVideo(0, it) } }
            }
        }
    }

    private fun loadAllReels() {
        progressBar.visibility = View.VISIBLE
        database.reference.child("Reels")
            .orderByChild("createdAt")
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
                        viewPager.post { getViewHolderAtPosition(0)?.let { adapter.playVideo(0, it) } }
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
        getViewHolderAtPosition(viewPager.currentItem)?.let { adapter.pauseCurrentVideo(it) }
    }

    override fun onResume() {
        super.onResume()
        getViewHolderAtPosition(viewPager.currentItem)?.let { adapter.playVideo(viewPager.currentItem, it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewPager.adapter = null
    }
}