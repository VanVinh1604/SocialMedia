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
import com.example.socialmedia.project.Domain.Model.StoryModel
import com.example.socialmedia.project.ViewModel.StoryViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

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
        observeNotificationBadge()


        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        storyViewModel.loadStories(currentUserId)
    }

    private fun setupRecycler() {
        binding.recyclerStory.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }



    private fun setupObservers() {
        storyViewModel.stories.observe(viewLifecycleOwner) { storyList ->
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@observe
            binding.recyclerStory.adapter = StoryAdapter(storyList, currentUserId)
        }


        storyViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { println("⚠️ Firebase error: $it") }
        }
    }

    private fun setupClicks() {
        binding.ivMessage.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_messageFragment)
        }

        binding.ivNotification.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_notificationFragment)
        }
    }

    private fun observeNotificationBadge() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().getReference("notifications")

        // Lắng nghe realtime thông báo mới của user
        database.orderByChild("userId").equalTo(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var hasNew = false
                    for (child in snapshot.children) {
                        val isRead = child.child("isRead").getValue(Boolean::class.java) ?: false
                        if (!isRead) {
                            hasNew = true
                            break
                        }
                    }
                    binding.badgeNotification.visibility = if (hasNew) View.VISIBLE else View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    // Không cần xử lý đặc biệt
                }
            })
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
