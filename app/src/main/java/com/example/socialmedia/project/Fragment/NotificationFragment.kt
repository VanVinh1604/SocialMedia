package com.example.socialmedia.project.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.socialmedia.databinding.FragmentNotificationBinding
import com.example.socialmedia.project.Adapter.NotificationAdapter
import com.example.socialmedia.project.Helper.TextGradientUtils
import com.example.socialmedia.project.ViewModel.NotificationViewModel
import com.example.socialmedia.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class NotificationFragment : Fragment() {

    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NotificationViewModel by viewModels()
    private val adapter = NotificationAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerNotification.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerNotification.adapter = adapter

        // Quan sát dữ liệu từ ViewModel
        viewModel.notifications.observe(viewLifecycleOwner) { list ->
            adapter.updateListGrouped(list)
        }

        // Gradient tiêu đề
        TextGradientUtils.applyGradient(binding.tvTitle, "#FF6FB1", "#9B59B6")

        // Gọi hàm tải dữ liệu realtime
        viewModel.loadNotifications()

        binding.ivBack.setOnClickListener {
            findNavController().navigate(R.id.action_notificationFragment_to_homeFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        markAllNotificationsAsRead()
    }

    private fun markAllNotificationsAsRead() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().getReference("notifications")

        database.orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        child.ref.child("isRead").setValue(true)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
