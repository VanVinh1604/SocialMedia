import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.socialmedia.databinding.BottomsheetLikedUsersBinding

import com.example.socialmedia.project.ViewModel.LikeViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class LikedUsersBottomSheetFragment(private val postId: String) : BottomSheetDialogFragment() {

    private var _binding: BottomsheetLikedUsersBinding? = null
    private val binding get() = _binding!!
    private lateinit var likeViewModel: LikeViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetLikedUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        likeViewModel = ViewModelProvider(this)[LikeViewModel::class.java]

        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())

        // Observe danh sách user
        likeViewModel.likedUsers.observe(viewLifecycleOwner) { users ->
            binding.rvUsers.adapter = UserLikedAdapter(users)
        }

        likeViewModel.loadLikedUsers(postId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
