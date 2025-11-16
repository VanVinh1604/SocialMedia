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
import com.google.firebase.database.*

@UnstableApi
class ReelsFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: ReelsAdapter
    private val reelsList = mutableListOf<ReelModel>()
    private val database = FirebaseDatabase.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reels, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ẩn bottom navigation và xóa padding khi vào ReelsFragment
        hideBottomNavigation()
        removeNavHostPadding()

        viewPager = view.findViewById(R.id.viewPagerReels)
        progressBar = view.findViewById(R.id.progressBar)

        setupAdapter()
        setupViewPager()
        loadReels()
    }

    private fun hideBottomNavigation() {
        activity?.findViewById<View>(R.id.container)?.visibility = View.GONE
    }

    private fun showBottomNavigation() {
        activity?.findViewById<View>(R.id.container)?.visibility = View.VISIBLE
    }

    private fun removeNavHostPadding() {
        activity?.findViewById<View>(R.id.navHostFragment)?.setPadding(0, 0, 0, 0)
    }

    private fun restoreNavHostPadding() {
        activity?.findViewById<View>(R.id.navHostFragment)?.setPadding(0, 0, 0, 80.dpToPx())
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
    // Thay thế onCommentClick trong setupAdapter() của ReelsFragment.kt

    private fun setupAdapter() {
        adapter = ReelsAdapter(
            reels = reelsList,
            onLikeClick = { reel, position ->
                Log.d("ReelsFragment", "Like clicked: ${reel.reelId}")
            },
            onCommentClick = { reel, position ->
                // Mở CommentsBottomSheet
                val commentsSheet = CommentsBottomSheet.newInstance(reel.reelId)
                commentsSheet.show(childFragmentManager, "CommentsBottomSheet")
            },
            onShareClick = { reel, position ->
                Toast.makeText(requireContext(), "Share: ${reel.reelId}", Toast.LENGTH_SHORT).show()
                // TODO: Mở Share Dialog
            },
            onFollowClick = { reel, position ->
                Toast.makeText(requireContext(), "Following ${reel.userId}", Toast.LENGTH_SHORT).show()
                // TODO: Thêm logic follow user
            },
            onProfileClick = { reel, position ->
                Toast.makeText(requireContext(), "Profile: ${reel.userId}", Toast.LENGTH_SHORT).show()
                // TODO: Mở Profile Activity/Fragment
            }
        )
    }

    private fun setupViewPager() {
        viewPager.adapter = adapter
        viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL

        // Tự động phát video khi chuyển trang
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                // Pause video trước đó
                val previousPosition = position - 1
                if (previousPosition >= 0) {
                    val previousHolder = getViewHolderAtPosition(previousPosition)
                    previousHolder?.let { adapter.pauseCurrentVideo(it) }
                }

                // Play video hiện tại
                val currentHolder = getViewHolderAtPosition(position)
                currentHolder?.let { adapter.playVideo(position, it) }
            }
        })
    }

    private fun getViewHolderAtPosition(position: Int): ReelsAdapter.ReelViewHolder? {
        val recyclerView = viewPager.getChildAt(0) as? RecyclerView
        return recyclerView?.findViewHolderForAdapterPosition(position) as? ReelsAdapter.ReelViewHolder
    }

    private fun loadReels() {
        progressBar.visibility = View.VISIBLE

        database.reference.child("Reels")
            .orderByChild("createdAt")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    reelsList.clear()

                    for (reelSnapshot in snapshot.children) {
                        try {
                            val reel = reelSnapshot.getValue(ReelModel::class.java)
                            if (reel != null) {
                                reelsList.add(reel)
                            }
                        } catch (e: Exception) {
                            Log.e("ReelsFragment", "Error parsing reel: ${e.message}")
                        }
                    }

                    // Đảo ngược để hiển thị video mới nhất trước
                    reelsList.reverse()

                    progressBar.visibility = View.GONE

                    if (reelsList.isEmpty()) {
                        Toast.makeText(requireContext(), "No reels available", Toast.LENGTH_SHORT).show()
                    } else {
                        adapter.notifyDataSetChanged()

                        // Tự động phát video đầu tiên
                        viewPager.post {
                            val firstHolder = getViewHolderAtPosition(0)
                            firstHolder?.let { adapter.playVideo(0, it) }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "Failed to load reels: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("ReelsFragment", "Database error: ${error.message}")
                }
            })
    }

    override fun onPause() {
        super.onPause()
        // Pause video khi fragment bị pause
        val currentPosition = viewPager.currentItem
        val currentHolder = getViewHolderAtPosition(currentPosition)
        currentHolder?.let { adapter.pauseCurrentVideo(it) }
    }

    override fun onResume() {
        super.onResume()
        // Resume video khi fragment được resume
        val currentPosition = viewPager.currentItem
        val currentHolder = getViewHolderAtPosition(currentPosition)
        currentHolder?.let { adapter.playVideo(currentPosition, it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Release tất cả players
        viewPager.adapter = null
    }
}