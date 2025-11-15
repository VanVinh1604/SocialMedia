package com.example.socialmedia.project.Fragment // (Kiểm tra lại package của bạn)

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.navGraphViewModels
import com.example.socialmedia.R
import com.example.socialmedia.project.ViewModel.ProfileViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ProfileOptionsBottomSheet : BottomSheetDialogFragment() {

    // Lấy ViewModel được chia sẻ từ NavGraph
    private val viewModel: ProfileViewModel by navGraphViewModels(R.id.nav_graph)

    private var targetUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            targetUserId = it.getString(ARG_USER_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_profile_options, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val optionBlock = view.findViewById<TextView>(R.id.option_block)
        val optionReport = view.findViewById<TextView>(R.id.option_report)
        val optionShare = view.findViewById<TextView>(R.id.option_share)
        val optionCancel = view.findViewById<TextView>(R.id.option_cancel)

        // === LOGIC MỚI: KIỂM TRA TRẠNG THÁI CHẶN ===

        // 1. Yêu cầu ViewModel kiểm tra
        targetUserId?.let {
            viewModel.checkBlockStatus(it)
        }

        // 2. Lắng nghe kết quả và cập nhật UI
        viewModel.isTargetUserBlocked.observe(viewLifecycleOwner) { isBlocked ->
            if (isBlocked) {
                // Đã chặn -> Hiển thị "Bỏ chặn"
                optionBlock.text = "Bỏ chặn"
                // (Bạn có thể đổi màu text sang màu bình thường)
                optionBlock.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black)) // Ví dụ: màu đen

                optionBlock.setOnClickListener {
                    targetUserId?.let {
                        Log.d("BottomSheet", "Click Bỏ chặn User ID: $it")
                        viewModel.unblockUser(it) // Gọi hàm BỎ CHẶN
                    }
                    dismiss()
                }
            } else {
                // Chưa chặn -> Hiển thị "Chặn"
                optionBlock.text = "Chặn"
                // (Bạn có thể đổi màu text sang màu đỏ)
                optionBlock.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)) // Ví dụ: màu đỏ

                optionBlock.setOnClickListener {
                    targetUserId?.let {
                        Log.d("BottomSheet", "Click Chặn User ID: $it")
                        viewModel.blockUser(it) // Gọi hàm CHẶN
                    }
                    dismiss()
                }
            }
        }
        // ============================================

        // Logic Báo cáo (giữ nguyên)
        optionReport.setOnClickListener {
            targetUserId?.let {
                Log.d("BottomSheet", "Click Báo cáo User ID: $it")
                viewModel.reportUser(it)
                Toast.makeText(context, "Đã gửi báo cáo", Toast.LENGTH_SHORT).show()
            }
            dismiss()
        }

        // Logic Chia sẻ (giữ nguyên)
        optionShare.setOnClickListener {
            targetUserId?.let {
                Log.d("BottomSheet", "Click Chia sẻ User ID: $it")
                shareProfileLink(it)
            }
            dismiss()
        }

        optionCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun shareProfileLink(userId: String) {
        val context = context ?: return
        val profileLink = "https://my-social-app.com/profile/$userId"
        val shareText = "Xem trang cá nhân này trên SocialApp!\n$profileLink"

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Chia sẻ trang cá nhân")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ qua"))
    }

    companion object {
        private const val ARG_USER_ID = "user_id"
        fun newInstance(userId: String): ProfileOptionsBottomSheet {
            val fragment = ProfileOptionsBottomSheet()
            val args = Bundle()
            args.putString(ARG_USER_ID, userId)
            fragment.arguments = args
            return fragment
        }
    }
}