package com.example.socialmedia.project.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.socialmedia.databinding.FragmentFollowListBinding
import com.example.socialmedia.project.Adapter.FollowViewPagerAdapter
import com.google.android.material.tabs.TabLayoutMediator

/**
 * Fragment này (Fragment Cha) chứa TabLayout và ViewPager2
 * Nó sẽ thay thế code FollowListFragment cũ của bạn
 */
class FollowListFragment : Fragment() {

    private var _binding: FragmentFollowListBinding? = null
    private val binding get() = _binding!!

    // Không cần ViewModel hay Adapter (RecyclerView) ở đây nữa
    // Chúng sẽ được chuyển vào Fragment con (FollowListTabFragment)

    private var userId: String = ""
    private var listType: String = "" // "followers" hoặc "following"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Lấy arguments từ PersonalProfileFragment (gửi qua Bundle)
        arguments?.let {
            userId = it.getString("userId") ?: ""
            listType = it.getString("listType") ?: "followers"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFollowListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Kiểm tra nếu không có userId, quay lại
        if (userId.isEmpty()) {
            Log.e("FollowListFragment", "UserId bị rỗng, không thể tải.")
            Toast.makeText(context, "Lỗi: Không tìm thấy ID người dùng", Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
            return
        }

        setupUI()
    }

    private fun setupUI() {
        // 1. Cài đặt Toolbar
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        // TODO: Lấy tên user (huy_neeewwd) từ ViewModel (hoặc Bundle)
        // và cập nhật binding.toolbar.title
        // Tạm thời để trống
        binding.toolbar.title = "..."

        // 2. Cài đặt ViewPager
        // (FollowViewPagerAdapter là 1 trong 3 file MỚI bạn cần tạo)
        val viewPagerAdapter = FollowViewPagerAdapter(this, userId)
        binding.viewPager.adapter = viewPagerAdapter

        // 3. Liên kết TabLayout với ViewPager
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            // TODO: Lấy số lượng thực tế từ ViewModel
            when (position) {
                0 -> tab.text = "Người theo dõi" // (Followers)
                1 -> tab.text = "Đang theo dõi" // (Following)
            }
        }.attach()

        // 4. [QUAN TRỌNG] Tự động chọn đúng tab
        // Nếu người dùng bấm "Following" (gửi "following"), mở tab 1
        if (listType == "following") {
            binding.viewPager.setCurrentItem(1, false)
        }
        // Nếu không, mặc định là tab 0 (Followers)

        // TODO: Cài đặt logic cho thanh tìm kiếm
        // binding.searchView.setOnQueryTextListener(...)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Gỡ adapter của ViewPager
        binding.viewPager.adapter = null
        _binding = null
    }
}

