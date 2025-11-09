package com.example.socialmedia.project.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentStoryViewerBinding
import com.example.socialmedia.project.Adapter.StoryProgressAdapter
import com.example.socialmedia.project.Adapter.StoryViewerAdapter
import com.example.socialmedia.project.Domain.Model.StoryModel
import com.example.socialmedia.project.Helper.MultiStoryProgressHelper
import com.example.socialmedia.project.ViewModel.StoryViewModel

class StoryViewerFragment : Fragment() {

    private lateinit var binding: FragmentStoryViewerBinding
    private val storyViewModel: StoryViewModel by viewModels()

    private var currentStoryIndex = 0
    private var progressHelper: MultiStoryProgressHelper? = null

    private var stories: List<StoryModel> = emptyList()
    private var progressAdapter: StoryProgressAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentStoryViewerBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val userId = arguments?.getString("userId") ?: return

        storyViewModel.loadUserStories(userId)
        storyViewModel.stories.observe(viewLifecycleOwner) { loadedStories ->
            Log.d("StoryViewer", "Observe triggered: ${loadedStories.size} items")

            if (loadedStories.isEmpty()) return@observe

            stories = loadedStories.filter { it.userId == userId }
            Log.d("StoryViewer", "Filtered ${stories.size} stories for userId=$userId")

            if (stories.isEmpty()) {
                Toast.makeText(requireContext(), "Không có story nào!", Toast.LENGTH_SHORT).show()
                return@observe
            }

            binding.storyViewPager.adapter = StoryViewerAdapter(stories)
            updateTopUserInfo(currentStoryIndex)
            setupProgressBar(stories.size)
        }




// Close button
        binding.btnClose.setOnClickListener {
            safeExitStory()
        }

        // Button more
        binding.btnMore.setOnClickListener {
            Toast.makeText(requireContext(), "More clicked", Toast.LENGTH_SHORT).show()
        }

        // Pause/resume khi người dùng giữ hoặc thả tay
        binding.storyViewPager.getChildAt(0).setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> progressHelper?.pause() // pause ngay
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> progressHelper?.resume() // resume khi thả
            }
            false
        }

// Khi focus vào reply box thì pause
        binding.etReplyMessage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) progressHelper?.pause() else progressHelper?.resume()
        }


        // Nút gửi tin nhắn
        binding.btnSendReply.setOnClickListener {
            if (binding.etReplyMessage.hasFocus()) {
                val msg = binding.etReplyMessage.text.toString().trim()
                if (msg.isNotEmpty()) {
                    Toast.makeText(requireContext(), "Đã gửi: $msg", Toast.LENGTH_SHORT).show()
                    binding.etReplyMessage.text.clear()
                    binding.etReplyMessage.clearFocus()
                }
            } else {
                Toast.makeText(requireContext(), "❤️", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Hàm thoát an toàn
    private fun safeExitStory() {
        progressHelper?.reset()
        progressHelper = null
        if (isAdded && !isRemoving) {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupProgressBar(storyCount: Int) {
        progressHelper = MultiStoryProgressHelper(
            container = binding.storyProgressContainer,
            duration = 5000L,
            onFinishSegment = {
                if (currentStoryIndex < stories.size - 1) {
                    currentStoryIndex++
                    binding.storyViewPager.currentItem = currentStoryIndex
                    updateTopUserInfo(currentStoryIndex)
                }
            },
            onFinishAll = {
                safeExitStory()
            }
        )

        progressHelper?.setup(storyCount)
        progressHelper?.start()

        binding.storyViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentStoryIndex = position
                updateTopUserInfo(position)
                progressHelper?.goTo(position)
            }
        })
    }


    private fun updateTopUserInfo(index: Int) {
        if (stories.isEmpty() || index !in stories.indices) return
        val story = stories[index]
        binding.tvUserName.text = story.userName

        val imageView = binding.imgUserAvatar
        if (imageView.width > 0 && imageView.height > 0) {
            Glide.with(this)
                .load(story.userProfileImage)
                .placeholder(R.drawable.default_avatar)
                .circleCrop()
                .into(imageView)
        } else {
            // fallback: override size để tránh crash
            Glide.with(this)
                .load(story.userProfileImage)
                .placeholder(R.drawable.default_avatar)
                .circleCrop()
                .override(100, 100)
                .into(imageView)
        }
    }

    override fun onPause() {
        super.onPause()
        progressAdapter?.pause()
    }

    override fun onResume() {
        super.onResume()
        progressAdapter?.resume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressHelper?.reset()
        progressHelper = null
    }

}
