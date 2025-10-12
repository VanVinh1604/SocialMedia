package com.example.socialmedia.project.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentHomeBinding
import com.example.socialmedia.project.Adapter.PostAdapter
import com.example.socialmedia.project.Adapter.StoryAdapter
import com.example.socialmedia.project.Domain.PostModel
import com.example.socialmedia.project.Domain.StoryTest

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupStoryRecyclerView()
        setupPostRecyclerView()
        setupClickListeners()
    }

    private fun setupStoryRecyclerView() {
        val storyList = listOf(
            StoryTest(0, R.drawable.baseline_add_24, R.drawable.bg_story_rounded, true),
            StoryTest(1, R.drawable.image_backgroud, R.drawable.image_person),
            StoryTest(2, R.drawable.image_avata_user, R.drawable.image_backgroud),
            StoryTest(3, R.drawable.image_person, R.drawable.image_avata_user),
            StoryTest(4, R.drawable.image_backgroud, R.drawable.image_backgroud)
        )

        binding.recyclerStory.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = StoryAdapter(storyList)
        }
    }

    private fun setupPostRecyclerView() {
        val samplePosts = List(5) {
            PostModel(
                postId = "$it",
                userId = "User$it",
                content = "This is a sample post #$it",
                likeCount = 10 + it,
                shareCount = 2 + it
            )
        }

        binding.recyclerPost.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = PostAdapter(samplePosts)
        }
    }

    private fun setupClickListeners() {
        // Navigate to MessageFragment bằng Navigation Component
        binding.ivMessage.setOnClickListener {
            // DÙNG action để animation hoạt động
            findNavController().navigate(R.id.action_homeFragment_to_messageFragment)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}