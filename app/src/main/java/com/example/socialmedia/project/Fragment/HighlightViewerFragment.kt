package com.example.socialmedia.project.Fragment

import android.app.AlertDialog
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.FragmentHighlightViewerBinding
import com.example.socialmedia.project.Domain.Model.StoryModel
import com.example.socialmedia.project.Helper.MultiStoryProgressHelper
import com.example.socialmedia.project.ViewModel.ProfileViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID

class HighlightViewerFragment : Fragment() {

    private var _binding: FragmentHighlightViewerBinding? = null
    private val binding get() = _binding!!
    private val profileViewModel: ProfileViewModel by navGraphViewModels(R.id.nav_graph)
    private var stories: List<StoryModel> = emptyList()
    private var currentStoryIndex = 0
    private var progressHelper: MultiStoryProgressHelper? = null
    private lateinit var gestureDetector: GestureDetectorCompat

    // Launcher chọn ảnh
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uploadAndAddToHighlight(uri)
        } else {
            progressHelper?.resume()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHighlightViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toggleFullScreenMode(true)

        val highlightId = arguments?.getString("highlightId")
        val highlightOwnerId = arguments?.getString("userId")

        setupViewPager()
        setupGestureDetector()

        binding.btnClose.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.layoutShare.setOnClickListener {
            shareCurrentStory()
        }

        // --- LOGIC ADMIN (NÚT 3 CHẤM) ---
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserId != null && highlightOwnerId != null && currentUserId == highlightOwnerId) {
            binding.btnMore.visibility = View.VISIBLE
            binding.btnMore.setOnClickListener {
                if (highlightId != null) {
                    showOwnerOptions(highlightId)
                } else {
                    Toast.makeText(context, "Lỗi: Highlight ID bị Null", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            binding.btnMore.visibility = View.GONE
        }

        // Tải dữ liệu
        binding.progressBar.visibility = View.VISIBLE
        if (highlightId != null) {
            profileViewModel.loadStoriesForHighlight(highlightId, highlightOwnerId)
        } else {
            Toast.makeText(requireContext(), "Không tìm thấy Highlight", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }

        profileViewModel.currentViewingStories.observe(viewLifecycleOwner) { loadedStories ->
            binding.progressBar.visibility = View.GONE
            if (!loadedStories.isNullOrEmpty()) {
                displayStories(loadedStories)
            } else {
                if (stories.isEmpty()) {
                    Toast.makeText(requireContext(), "Highlight này trống", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
        }
    }

    // =====================================================================
    // XỬ LÝ HIỂN THỊ & VIDEO
    // =====================================================================

    private fun displayStories(loadedStories: List<StoryModel>) {
        stories = loadedStories
        val adapter = SimpleStoryAdapter(stories)
        binding.storyViewPager.adapter = adapter

        if (currentStoryIndex >= stories.size) {
            currentStoryIndex = (stories.size - 1).coerceAtLeast(0)
        }

        binding.storyViewPager.post {
            binding.storyViewPager.setCurrentItem(currentStoryIndex, false)
        }

        if (stories.isNotEmpty()) updateUserInfo(stories[currentStoryIndex])

        setupProgressHelper()
    }

    private fun setupProgressHelper() {
        progressHelper?.reset()

        // Tạo danh sách thời gian: Ảnh = 5s, Video = 30s (Dự phòng)
        val durations = stories.map { story ->
            if (story.type == "video" || story.mediaUrl.contains(".mp4")) 30000L else 5000L
        }

        progressHelper = MultiStoryProgressHelper(
            container = binding.storyProgressContainer,
            segmentDurations = durations,
            onFinishSegment = {
                if (currentStoryIndex < stories.size - 1) {
                    binding.storyViewPager.setCurrentItem(currentStoryIndex + 1, true)
                } else {
                    findNavController().popBackStack()
                }
            },
            onFinishAll = { findNavController().popBackStack() }
        )

        progressHelper?.setup()
        progressHelper?.goTo(currentStoryIndex)

        if (binding.progressBar.visibility == View.GONE) {
            progressHelper?.start()
        }
    }

    private fun setupViewPager() {
        binding.storyViewPager.isUserInputEnabled = false
        binding.storyViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentStoryIndex = position
                progressHelper?.goTo(position)
                if (stories.isNotEmpty() && position < stories.size) {
                    updateUserInfo(stories[position])
                    progressHelper?.pause()
                }
            }
        })
    }

    // --- ADAPTER XỬ LÝ VIDEO CÓ TIẾNG ---
    inner class SimpleStoryAdapter(private val list: List<StoryModel>) : RecyclerView.Adapter<SimpleStoryAdapter.Holder>() {
        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val img: ImageView = view.findViewById(R.id.ivStoryImage)
            val vid: VideoView = view.findViewById(R.id.vvStoryVideo)
            val pb: ProgressBar = view.findViewById(R.id.pbLoading)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_story_viewer_simple, parent, false)
            view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = list[position]
            holder.pb.visibility = View.VISIBLE

            holder.img.visibility = View.GONE
            holder.vid.visibility = View.GONE
            holder.vid.stopPlayback()

            val isVideo = item.type == "video" || item.mediaUrl.contains(".mp4")

            if (isVideo) {
                holder.vid.visibility = View.VISIBLE
                holder.vid.setVideoURI(Uri.parse(item.mediaUrl))

                holder.vid.setOnPreparedListener { mp ->
                    holder.pb.visibility = View.GONE

                    // === [FIX QUAN TRỌNG] CẤU HÌNH ÂM THANH ===
                    try {
                        val audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                            .build()
                        mp.setAudioAttributes(audioAttributes)

                        // Bật max volume (1.0f = 100%)
                        mp.setVolume(1f, 1f)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    mp.start()

                    if (position == currentStoryIndex) {
                        progressHelper?.resume()
                    }
                }

                holder.vid.setOnCompletionListener {
                    if (position == currentStoryIndex) {
                        if (currentStoryIndex < list.size - 1) {
                            binding.storyViewPager.setCurrentItem(currentStoryIndex + 1, true)
                        } else {
                            findNavController().popBackStack()
                        }
                    }
                }

                holder.vid.setOnErrorListener { _, _, _ ->
                    holder.pb.visibility = View.GONE
                    false
                }

            } else {
                holder.img.visibility = View.VISIBLE
                Glide.with(holder.itemView.context)
                    .load(item.mediaUrl)
                    .placeholder(android.R.color.black)
                    .into(holder.img)

                Handler(Looper.getMainLooper()).postDelayed({
                    holder.pb.visibility = View.GONE
                    if (position == currentStoryIndex) {
                        progressHelper?.resume()
                    }
                }, 500)
            }
        }

        override fun getItemCount() = list.size
    }

    // =====================================================================
    // CÁC HÀM XỬ LÝ ADMIN
    // =====================================================================

    private fun showOwnerOptions(highlightId: String) {
        progressHelper?.pause()
        val bottomSheet = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_highlight_options, null)
        bottomSheet.setContentView(view)
        var actionSelected = false

        view.findViewById<View>(R.id.option_add_story)?.setOnClickListener {
            actionSelected = true; bottomSheet.dismiss(); pickImageLauncher.launch("image/*")
        }
        view.findViewById<View>(R.id.option_rename)?.setOnClickListener {
            actionSelected = true; bottomSheet.dismiss(); showRenameDialog(highlightId)
        }
        view.findViewById<View>(R.id.option_remove_current)?.setOnClickListener {
            actionSelected = true; bottomSheet.dismiss(); removeCurrentStory(highlightId)
        }
        view.findViewById<View>(R.id.option_delete_highlight)?.setOnClickListener {
            actionSelected = true; bottomSheet.dismiss(); confirmDeleteHighlight(highlightId)
        }
        bottomSheet.setOnDismissListener { if (!actionSelected) progressHelper?.resume() }
        bottomSheet.show()
    }

    private fun showRenameDialog(highlightId: String) {
        progressHelper?.pause()
        val input = EditText(requireContext())
        input.hint = "Nhập tên mới..."
        AlertDialog.Builder(requireContext()).setTitle("Đổi tên").setView(input).setCancelable(false)
            .setPositiveButton("Lưu") { _, _ ->
                profileViewModel.updateHighlightName(highlightId, input.text.toString())
                Toast.makeText(context, "Đã cập nhật tên", Toast.LENGTH_SHORT).show()
                progressHelper?.resume()
            }
            .setNegativeButton("Hủy") { d, _ -> d.dismiss(); progressHelper?.resume() }.show()
    }

    private fun removeCurrentStory(highlightId: String) {
        if (stories.isEmpty()) return
        AlertDialog.Builder(requireContext()).setTitle("Gỡ ảnh/video?").setMessage("Bạn chắc chắn muốn gỡ?")
            .setCancelable(false)
            .setPositiveButton("Gỡ") { _, _ ->
                binding.progressBar.visibility = View.VISIBLE
                profileViewModel.removeStoryFromHighlight(highlightId, stories[currentStoryIndex].storyId)
            }
            .setNegativeButton("Hủy") { _, _ -> progressHelper?.resume() }.show()
    }

    private fun confirmDeleteHighlight(highlightId: String) {
        AlertDialog.Builder(requireContext()).setTitle("Xóa Highlight?").setMessage("Hành động này không thể hoàn tác.")
            .setCancelable(false)
            .setPositiveButton("Xóa") { _, _ ->
                profileViewModel.deleteHighlight(highlightId); findNavController().popBackStack()
            }
            .setNegativeButton("Hủy") { _, _ -> progressHelper?.resume() }.show()
    }

    private fun uploadAndAddToHighlight(imageUri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        progressHelper?.pause()
        Toast.makeText(context, "Đang tải lên...", Toast.LENGTH_SHORT).show()

        profileViewModel.uploadStoryImage(imageUri,
            onSuccess = { imageUrl ->
                val highlightId = arguments?.getString("highlightId")
                val userId = arguments?.getString("userId")
                if (highlightId != null && userId != null) {
                    val newStory = StoryModel(
                        storyId = UUID.randomUUID().toString(),
                        userId = userId,
                        mediaUrl = imageUrl,
                        createdAt = System.currentTimeMillis(),
                        type = "image",
                        viewCount = 0
                    )
                    profileViewModel.addStoryToHighlight(highlightId, newStory)
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Đã thêm thành công! Đang tải lại...", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { msg ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Lỗi: $msg", Toast.LENGTH_LONG).show()
                progressHelper?.resume()
            }
        )
    }

    // =====================================================================
    // HÀM HỖ TRỢ KHÁC
    // =====================================================================

    private fun setupGestureDetector() {
        val width = resources.displayMetrics.widthPixels
        gestureDetector = GestureDetectorCompat(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent) = true
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (e.x < width * 0.3) {
                    if (currentStoryIndex > 0) binding.storyViewPager.setCurrentItem(currentStoryIndex - 1, true)
                    else progressHelper?.goTo(0)
                } else {
                    if (currentStoryIndex < stories.size - 1) binding.storyViewPager.setCurrentItem(currentStoryIndex + 1, true)
                    else findNavController().popBackStack()
                }
                return true
            }
            override fun onLongPress(e: MotionEvent) { progressHelper?.pause() }
        })
        binding.touchView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) progressHelper?.resume()
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun toggleFullScreenMode(enable: Boolean) {
        val act = activity ?: return
        val bottomContainer = act.findViewById<View>(R.id.container)
        val navHost = act.findViewById<View>(R.id.navHostFragment)
        if (enable) {
            bottomContainer?.visibility = View.GONE
            navHost?.setPadding(0, 0, 0, 0)
        } else {
            bottomContainer?.visibility = View.VISIBLE
            val density = resources.displayMetrics.density
            val paddingBottomPx = (80 * density).toInt()
            navHost?.setPadding(0, 0, 0, paddingBottomPx)
        }
    }

    private fun shareCurrentStory() {
        if (stories.isEmpty()) return
        val currentStory = stories[currentStoryIndex]
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Chia sẻ Highlight")
            putExtra(Intent.EXTRA_TEXT, "Xem story này: ${currentStory.mediaUrl}")
        }
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ qua"))
    }

    private fun updateUserInfo(story: StoryModel) {
        binding.tvUserName.text = story.userName ?: "User"
        binding.tvTimeAgo.text = "· ${getTimeAgo(story.createdAt)}"
        binding.tvViewCount.text = "${story.viewCount} người xem"
        Glide.with(this).load(story.userProfileImage).placeholder(R.drawable.default_avatar).into(binding.imgUserAvatar)
    }

    private fun getTimeAgo(time: Long): String {
        val diff = System.currentTimeMillis() - time
        val minutes = diff / 60000
        val hours = minutes / 60
        val days = hours / 24
        return when {
            minutes < 1 -> "vừa xong"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}h"
            else -> "${days}d"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        toggleFullScreenMode(false)
        progressHelper?.reset()
        _binding = null
    }
}