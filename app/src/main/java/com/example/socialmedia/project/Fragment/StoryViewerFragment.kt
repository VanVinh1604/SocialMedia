package com.example.socialmedia.project.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentStoryViewerBinding
import com.example.socialmedia.project.Adapter.StoryViewerAdapter
import com.example.socialmedia.project.ViewModel.StoryViewModel
import com.example.socialmedia.project.Domain.Model.StoryModel
import com.example.socialmedia.project.Helper.MultiStoryProgressHelper

class StoryViewerFragment : Fragment() {

    private lateinit var binding: FragmentStoryViewerBinding
    private val storyViewModel: StoryViewModel by viewModels()
    private var storyProgressHelper: MultiStoryProgressHelper? = null
    private var currentStoryIndex = 0
    private var stories: List<StoryModel> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentStoryViewerBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val userId = arguments?.getString("userId") ?: return

        storyViewModel.loadUserStories(userId)
        storyViewModel.stories.observe(viewLifecycleOwner) { loadedStories ->
            if (loadedStories.isNotEmpty()) {
                stories = loadedStories
                binding.storyViewPager.adapter = StoryViewerAdapter(stories)
                updateTopUserInfo(currentStoryIndex)
                startStoryProgress()

                binding.storyViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        currentStoryIndex = position
                        updateTopUserInfo(position)
                        storyProgressHelper?.reset()
                        startStoryProgress()
                    }
                })
            }
        }

        binding.btnClose.setOnClickListener {
            storyProgressHelper?.reset()
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnMore.setOnClickListener {
            Toast.makeText(requireContext(), "More clicked", Toast.LENGTH_SHORT).show()
        }

        binding.btnSendReply.setOnClickListener {
            val msg = binding.etReplyMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                Toast.makeText(requireContext(), "Đã gửi: $msg", Toast.LENGTH_SHORT).show()
                binding.etReplyMessage.text.clear()
            }
        }
    }

    private fun updateTopUserInfo(index: Int) {
        if (stories.isEmpty() || index !in stories.indices) return
        val story = stories[index]
        binding.tvUserName.text = story.userName
        Glide.with(this)
            .load(story.userProfileImage)
            .placeholder(R.drawable.default_avatar)
            .circleCrop()
            .into(binding.imgUserAvatar)
    }

    private fun startStoryProgress() {
        if (stories.isEmpty() || currentStoryIndex !in stories.indices) return

        storyProgressHelper = MultiStoryProgressHelper(
            container = binding.progressContainer,
            duration = 5000L,
            onFinishSegment = {
                currentStoryIndex++
                if (currentStoryIndex < stories.size) {
                    binding.storyViewPager.currentItem = currentStoryIndex
                }
            },
            onFinishAll = {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        )

        storyProgressHelper?.setup(stories.size)
        storyProgressHelper?.start()
    }

    override fun onPause() {
        super.onPause()
        storyProgressHelper?.pause()
    }

    override fun onResume() {
        super.onResume()
        storyProgressHelper?.resume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        storyProgressHelper?.reset()
    }
}
