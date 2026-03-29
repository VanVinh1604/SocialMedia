package com.example.socialmedia.project.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentCreateHighlightBinding
import com.example.socialmedia.project.Adapter.SelectStoryAdapter
import com.example.socialmedia.project.ViewModel.ProfileViewModel

class CreateHighlightFragment : Fragment() {

    private var _binding: FragmentCreateHighlightBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by navGraphViewModels(R.id.nav_graph)
    private lateinit var adapter: SelectStoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateHighlightBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        loadData()
    }

    private fun setupUI() {
        binding.tvCancel.setOnClickListener { findNavController().popBackStack() }

        binding.tvDone.setOnClickListener {
            val name = binding.etHighlightName.text.toString().trim()
            val selectedIds = adapter.selectedStoryIds
            val coverUrl = adapter.firstSelectedImageUrl

            if (name.isEmpty()) {
                Toast.makeText(context, "Hãy đặt tên cho tin nổi bật", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedIds.isEmpty()) {
                Toast.makeText(context, "Vui lòng chọn ít nhất 1 story", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.createHighlight(name, coverUrl, selectedIds)
            Toast.makeText(context, "Đã tạo thành công!", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }

        // Khởi tạo RecyclerView rỗng trước để tránh lỗi layout
        adapter = SelectStoryAdapter(emptyList()) { count ->
            binding.tvDone.isEnabled = count > 0
            binding.tvDone.alpha = if (count > 0) 1.0f else 0.5f
        }
        binding.rvSelectStories.layoutManager = GridLayoutManager(context, 3)
        binding.rvSelectStories.adapter = adapter
    }

    private fun loadData() {
        // Gọi hàm tải dữ liệu
        viewModel.loadUserStories()

        // Lắng nghe kết quả
        viewModel.myStories.observe(viewLifecycleOwner) { stories ->
            Log.d("CreateHighlight", "Nhận được ${stories.size} story từ ViewModel")

            if (stories.isNullOrEmpty()) {
                // Nếu vẫn rỗng sau khi tải -> Người dùng thực sự chưa có story hoặc lỗi path
                Toast.makeText(context, "Không tìm thấy story nào.", Toast.LENGTH_SHORT).show()
            } else {
                // Nếu có dữ liệu -> Cập nhật adapter
                adapter = SelectStoryAdapter(stories) { count ->
                    binding.tvDone.isEnabled = count > 0
                    binding.tvDone.alpha = if (count > 0) 1.0f else 0.5f
                }
                binding.rvSelectStories.adapter = adapter
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}