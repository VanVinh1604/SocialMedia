package com.example.socialmedia.project.Adapter

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.socialmedia.project.Fragment.FollowListTabFragment

class FollowViewPagerAdapter(
    fragment: Fragment,
    private val userId: String
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2 // Chúng ta có 2 tab

    override fun createFragment(position: Int): Fragment {
        // Tạo Fragment con mới
        val fragment = FollowListTabFragment()

        // Gửi dữ liệu (userId và loại tab) cho Fragment con
        fragment.arguments = Bundle().apply {
            putString("userId", userId)
            // Tab 0 là "followers", Tab 1 là "following"
            putString("listType", if (position == 0) "followers" else "following")
        }
        return fragment
    }
}
