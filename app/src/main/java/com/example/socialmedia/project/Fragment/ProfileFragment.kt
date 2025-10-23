package com.example.socialmedia.project.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmedia.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView

class ProfileFragment : Fragment() {

    // View components
    private lateinit var ivBackButton: ImageView
    private lateinit var ivMenuButton: ImageView
    private lateinit var ivHeaderImage: ImageView
    private lateinit var ivProfileImage: ShapeableImageView

    private lateinit var tvUsername: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvPostCount: TextView
    private lateinit var tvFollowingCount: TextView
    private lateinit var tvLikeCount: TextView

    private lateinit var btnFollow: MaterialButton
    private lateinit var btnShareProfile: MaterialButton

    private lateinit var rvPhotos: RecyclerView

    // Data
    private var isFollowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        initViews(view)
        setupListeners()
        loadUserData()
        setupPhotoGrid()

        return view
    }

    private fun initViews(view: View) {
        // Header
        ivBackButton = view.findViewById(R.id.ivBackButton)
        ivMenuButton = view.findViewById(R.id.ivMenuButton)
        ivHeaderImage = view.findViewById(R.id.ivHeaderImage)
        ivProfileImage = view.findViewById(R.id.ivProfileImage)

        // User info
        tvUsername = view.findViewById(R.id.tvUsername)
        tvBio = view.findViewById(R.id.tvBio)

        // Stats
        tvPostCount = view.findViewById(R.id.tvPostCount)
        tvFollowingCount = view.findViewById(R.id.tvFollowingCount)
        tvLikeCount = view.findViewById(R.id.tvLikeCount)

        // Buttons
        btnFollow = view.findViewById(R.id.btnFollow)
        btnShareProfile = view.findViewById(R.id.btnShareProfile)

        // RecyclerView
        rvPhotos = view.findViewById(R.id.rvPhotos)
    }

    private fun setupListeners() {
        // Back button
        ivBackButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        // Menu button
        ivMenuButton.setOnClickListener {
            showMenu()
        }

        // Follow button
        btnFollow.setOnClickListener {
            toggleFollow()
        }

        // Share button
        btnShareProfile.setOnClickListener {
            shareProfile()
        }

        // Profile image click
        ivProfileImage.setOnClickListener {
            showProfileImageFullScreen()
        }
    }

    private fun loadUserData() {
        // Load user data - Replace with actual data from your backend/database
        tvUsername.text = "Van Vinh"
        tvBio.text = "Photography enthusiast 📸 | Traveler ✈️"
        tvPostCount.text = "10"
        tvFollowingCount.text = "15k"
        tvLikeCount.text = "190k"
    }

    private fun setupPhotoGrid() {
        rvPhotos.layoutManager = GridLayoutManager(requireContext(), 3)

        // TODO: Set up your photo adapter
        // val photoAdapter = PhotoAdapter(photoList)
        // rvPhotos.adapter = photoAdapter
    }

    private fun toggleFollow() {
        isFollowing = !isFollowing

        if (isFollowing) {
            btnFollow.text = "Following"
            btnFollow.setBackgroundColor(resources.getColor(android.R.color.transparent))
            btnFollow.setTextColor(resources.getColor(R.color.purple_main))
            btnFollow.strokeColor = resources.getColorStateList(R.color.purple_main)
            btnFollow.strokeWidth = 4

            Toast.makeText(requireContext(), "Followed!", Toast.LENGTH_SHORT).show()

            // Update following count
            val currentCount = tvFollowingCount.text.toString().replace("k", "").toFloatOrNull() ?: 0f
            tvFollowingCount.text = "${String.format("%.1f", currentCount + 0.1)}k"
        } else {
            btnFollow.text = "Follow"
            btnFollow.setBackgroundColor(resources.getColor(R.color.purple_main))
            btnFollow.setTextColor(resources.getColor(android.R.color.white))
            btnFollow.strokeWidth = 0

            Toast.makeText(requireContext(), "Unfollowed!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareProfile() {
        // Implement share functionality
        Toast.makeText(requireContext(), "Share profile", Toast.LENGTH_SHORT).show()

        // Example share intent:
        /*
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Check out ${tvUsername.text}'s profile!")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Share profile via"))
        */
    }

    private fun showMenu() {
        // Show menu options
        Toast.makeText(requireContext(), "Menu clicked", Toast.LENGTH_SHORT).show()

        // TODO: Implement popup menu or bottom sheet
        /*
        val popup = PopupMenu(requireContext(), ivMenuButton)
        popup.menuInflater.inflate(R.menu.profile_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_block -> {
                    // Block user
                    true
                }
                R.id.menu_report -> {
                    // Report user
                    true
                }
                else -> false
            }
        }
        popup.show()
        */
    }

    private fun showProfileImageFullScreen() {
        // Show profile image in full screen
        Toast.makeText(requireContext(), "View profile image", Toast.LENGTH_SHORT).show()

        // TODO: Implement full screen image viewer
    }

    companion object {
        private const val ARG_USER_ID = "user_id"

        @JvmStatic
        fun newInstance(userId: String) =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USER_ID, userId)
                }
            }
    }
}