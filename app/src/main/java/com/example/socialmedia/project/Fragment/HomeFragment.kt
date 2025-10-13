package com.example.socialmedia.project.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentHomeBinding
import com.example.socialmedia.project.Adapter.StoryAdapter
import com.example.socialmedia.project.ViewModel.StoryViewModel
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val storyViewModel: StoryViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        setupObservers()
        setupClicks()

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        storyViewModel.loadStories(currentUserId)
    }

    private fun setupRecycler() {
        binding.recyclerStory.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }

    private fun setupObservers() {
        storyViewModel.stories.observe(viewLifecycleOwner) { storyList ->
            binding.recyclerStory.adapter = StoryAdapter(storyList)
        }

        storyViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { println("⚠️ Firebase error: $it") }
        }
    }

    private fun setupClicks() {
        binding.ivMessage.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_messageFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
