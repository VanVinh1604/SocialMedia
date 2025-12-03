
package com.example.socialmedia.project.Adapter

import androidx.appcompat.app.AppCompatActivity
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import com.example.socialmedia.project.Fragment.CommentsBottomSheet
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.project.Domain.Model.ReelModel
import com.example.socialmedia.R
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.common.util.UnstableApi
import androidx.navigation.Navigation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.UUID

@UnstableApi
class ReelsAdapter(
    private val reels: List<ReelModel>,
    private val onLikeClick: (ReelModel, Int) -> Unit,
    private val onCommentClick: (ReelModel, Int) -> Unit,
    private val onShareClick: (ReelModel, Int) -> Unit,
    private val onFollowClick: (ReelModel, Int) -> Unit,
    private val onProfileClick: (ReelModel, Int) -> Unit
) : RecyclerView.Adapter<ReelsAdapter.ReelViewHolder>() {

    private var currentPlayingPosition = -1
    private val database = FirebaseDatabase.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reel, parent, false)
        return ReelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReelViewHolder, position: Int) {
        holder.bind(reels[position], position)
    }

    override fun getItemCount() = reels.size

    fun playVideo(position: Int, holder: ReelViewHolder) {
        if (currentPlayingPosition != position) {
            holder.playVideo()
            currentPlayingPosition = position
        }
    }

    fun pauseCurrentVideo(holder: ReelViewHolder) {
        holder.pauseVideo()
    }

    inner class ReelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val playerView: PlayerView = itemView.findViewById(R.id.playerView)
        private val ivHeartFly: ImageView = itemView.findViewById(R.id.ivHeartFly)

        private val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        private val tvCaption: TextView = itemView.findViewById(R.id.tvCaption)
        private val tvHashtag: TextView = itemView.findViewById(R.id.tvHashtag)
        private val tvMusicInfo: TextView = itemView.findViewById(R.id.tvMusicInfo)
        private val tvLikeCount: TextView = itemView.findViewById(R.id.tvLikeCount)
        private val tvCommentCount: TextView = itemView.findViewById(R.id.tvCommentCount)
        private val tvShareCount: TextView = itemView.findViewById(R.id.tvShareCount)

        private val ivLike: ImageView = itemView.findViewById(R.id.ivLike)
        private val ivProfilePic: ImageView = itemView.findViewById(R.id.ivProfilePic)
        private val cvProfilePic: CardView = itemView.findViewById(R.id.cvProfilePic)
        private val btnFollow: ImageView = itemView.findViewById(R.id.btnFollow)

        private val ivPlayPause: ImageView = itemView.findViewById(R.id.ivPlayPause)
        private val ivSeekBackward: ImageView = itemView.findViewById(R.id.ivSeekBackward)
        private val ivSeekForward: ImageView = itemView.findViewById(R.id.ivSeekForward)

        private var player: ExoPlayer? = null
        private var isLiked = false
        private var lastTapTime = 0L
        private val hideControllerRunnable = Runnable { hideController() }

        @SuppressLint("ClickableViewAccessibility")
        fun bind(reel: ReelModel, position: Int) {
            tvUsername.text = "@loading..."
            tvCaption.text = reel.caption ?: ""

            // ✅ Hiển thị hashtags
            tvHashtag.text = if (reel.hashtags.isNotEmpty()) {
                reel.hashtags.joinToString(" ") { "#$it" }
            } else {
                "#Reels"
            }

            tvMusicInfo.text = "Original Sound - loading..."

            tvLikeCount.text = formatCount(reel.likeCount)
            tvCommentCount.text = formatCount(reel.commentCount)
            tvShareCount.text = formatCount(reel.shareCount)

            Glide.with(itemView.context)
                .load(R.drawable.ic_profile)
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .into(ivProfilePic)

            loadUserInfo(reel.userId)
            checkIfUserLiked(reel)
            setupFollowButton(reel)
            initializePlayer(reel.videoUrl)

            // ✅ FIX: Like button - GỌI CALLBACK
            itemView.findViewById<View>(R.id.layoutLike).setOnClickListener {
                toggleLike(reel, position)
                onLikeClick(reel, position)
            }

            // ✅ FIX: Comment button - GỌI CALLBACK
            itemView.findViewById<View>(R.id.layoutComment).setOnClickListener {
                onCommentClick(reel, position)

                val fragment = CommentsBottomSheet.newInstance(reel.reelId, reel.userId)
                (itemView.context as? androidx.fragment.app.FragmentActivity)?.let { activity ->
                    fragment.show(activity.supportFragmentManager, "CommentsBottomSheet")
                }
            }

            // ✅ FIX: Share button - GỌI CALLBACK
            itemView.findViewById<View>(R.id.layoutShare).setOnClickListener {
                onShareClick(reel, position)
            }

            // ✅ THAY ĐỔI: Click vào avatar
            cvProfilePic.setOnClickListener {
                navigateToProfile(reel.userId)
            }

            // ✅ THAY ĐỔI: Click vào username
            tvUsername.setOnClickListener {
                navigateToProfile(reel.userId)
            }

            itemView.findViewById<ImageView>(R.id.ivBack).setOnClickListener {
                (itemView.context as? AppCompatActivity)?.onBackPressed()
            }

            // ✅ FIX: Double tap - GỌI CALLBACK
            itemView.setOnClickListener {
                val now = System.currentTimeMillis()
                if (now - lastTapTime < 300) {
                    toggleLike(reel, position)
                    showHeartAnimation()
                    onLikeClick(reel, position)
                } else {
                    togglePlayPause()
                    toggleController()
                }
                lastTapTime = now
            }

            ivSeekBackward.setOnClickListener { seekBackward() }
            ivSeekForward.setOnClickListener { seekForward() }

            val gestureDetector = GestureDetector(itemView.context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    val width = itemView.width
                    if (e.x < width / 2) {
                        seekBackward()
                        showReplayAnimation(true)
                    } else {
                        seekForward()
                        showReplayAnimation(false)
                    }
                    return true
                }
            })
            itemView.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
        }

        // ✅ HÀM MỚI: Điều hướng đến profile
        private fun navigateToProfile(targetUserId: String) {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

            if (currentUserId == null) {
                Toast.makeText(itemView.context, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show()
                return
            }

            val activity = itemView.context as? androidx.fragment.app.FragmentActivity
            if (activity == null) {
                Log.e("ReelsAdapter", "Activity is not FragmentActivity")
                return
            }

            try {
                val navController = Navigation.findNavController(activity, R.id.navHostFragment)

                if (targetUserId == currentUserId) {
                    // ✅ Chuyển đến PersonalProfileFragment (trang của chính mình)
                    navController.navigate(R.id.personalProfileFragment)
                    Log.d("ReelsAdapter", "Navigating to PersonalProfileFragment")
                } else {
                    // ✅ Chuyển đến ProfileFragment (trang của người khác)
                    val bundle = Bundle().apply {
                        putString("userId", targetUserId)
                    }
                    navController.navigate(R.id.action_reelsFragment_to_profileFragment, bundle)
                    Log.d("ReelsAdapter", "Navigating to ProfileFragment with userId: $targetUserId")
                }
            } catch (e: Exception) {
                Log.e("ReelsAdapter", "Navigation error: ${e.message}", e)
                Toast.makeText(itemView.context, "Không thể mở trang cá nhân", Toast.LENGTH_SHORT).show()
            }
        }

        private fun setupFollowButton(reel: ReelModel) {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId == null) {
                btnFollow.visibility = View.GONE
                return
            }

            val reelOwnerId = reel.userId
            if (currentUserId == reelOwnerId) {
                btnFollow.visibility = View.GONE
                return
            }

            database.reference.child("Follow").child(currentUserId)
                .child("following").child(reelOwnerId)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val isFollowing = snapshot.getValue(Boolean::class.java) ?: false
                        btnFollow.visibility = if (isFollowing) View.GONE else View.VISIBLE
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("ReelsAdapter", "Error checking follow status", error.toException())
                    }
                })

            btnFollow.setOnClickListener {
                followUser(currentUserId, reelOwnerId)
                onFollowClick(reel, bindingAdapterPosition)
            }
        }

        private fun followUser(currentUserId: String, targetUserId: String) {
            database.reference.child("Follow").child(currentUserId)
                .child("following").child(targetUserId).setValue(true)
                .addOnSuccessListener {
                    database.reference.child("Follow").child(targetUserId)
                        .child("followers").child(currentUserId).setValue(true)

                    database.reference.child("InfoUser").child(currentUserId)
                        .child("followingCount")
                        .setValue(ServerValue.increment(1))

                    database.reference.child("InfoUser").child(targetUserId)
                        .child("followerCount")
                        .setValue(ServerValue.increment(1))

                    val followId = UUID.randomUUID().toString()
                    val followModel = mapOf(
                        "followId" to followId,
                        "followerId" to currentUserId,
                        "followingId" to targetUserId,
                        "status" to "ACCEPTED",
                        "createdAt" to System.currentTimeMillis()
                    )
                    database.reference.child("Follows").child(followId).setValue(followModel)

                    Toast.makeText(itemView.context, "Đã theo dõi", Toast.LENGTH_SHORT).show()
                    btnFollow.visibility = View.GONE
                }
                .addOnFailureListener { e ->
                    Log.e("ReelsAdapter", "Failed to follow user", e)
                    Toast.makeText(itemView.context, "Không thể theo dõi", Toast.LENGTH_SHORT).show()
                }
        }

        private fun showReplayAnimation(isBackward: Boolean) {
            val replayIcon = ImageView(itemView.context).apply {
                setImageResource(if (isBackward) R.drawable.ic_replay_10 else R.drawable.ic_forward_10)
                scaleX = 0f
                scaleY = 0f
                alpha = 1f
            }
            (itemView as ViewGroup).addView(replayIcon)
            replayIcon.x = if (isBackward) 100f else itemView.width - 300f
            replayIcon.y = itemView.height / 2f - 100f

            replayIcon.animate()
                .scaleX(1.5f).scaleY(1.5f).setDuration(400)
                .withEndAction {
                    replayIcon.animate().scaleX(0f).scaleY(0f).alpha(0f).setDuration(300)
                        .withEndAction { (itemView as ViewGroup).removeView(replayIcon) }
                        .start()
                }.start()
        }

        private fun toggleController() {
            if (ivPlayPause.visibility == View.VISIBLE) {
                hideController()
            } else {
                showController()
                itemView.removeCallbacks(hideControllerRunnable)
                itemView.postDelayed(hideControllerRunnable, 3000)
            }
        }

        private fun showController() {
            ivPlayPause.visibility = View.VISIBLE
            ivSeekBackward.visibility = View.VISIBLE
            ivSeekForward.visibility = View.VISIBLE
            ivPlayPause.alpha = 1f
            ivSeekBackward.alpha = 0.8f
            ivSeekForward.alpha = 0.8f
        }

        private fun hideController() {
            ivPlayPause.animate().alpha(0f).setDuration(300).withEndAction { ivPlayPause.visibility = View.GONE }
            ivSeekBackward.animate().alpha(0f).setDuration(300).withEndAction { ivSeekBackward.visibility = View.GONE }
            ivSeekForward.animate().alpha(0f).setDuration(300).withEndAction { ivSeekForward.visibility = View.GONE }
        }

        private fun togglePlayPause() {
            player?.let {
                if (it.isPlaying) it.pause() else it.play()
                ivPlayPause.setImageResource(if (it.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_reels)
                ivPlayPause.visibility = View.VISIBLE
                ivPlayPause.postDelayed({ if (!ivPlayPause.isPressed) ivPlayPause.visibility = View.GONE }, 800)
            }
        }

        private fun seekForward() = player?.seekTo((player!!.currentPosition + 10000).coerceAtMost(player!!.duration - 1000))
        private fun seekBackward() = player?.seekTo((player!!.currentPosition - 10000).coerceAtLeast(0L))

        private fun showHeartAnimation() {
            ivHeartFly.visibility = View.VISIBLE
            ivHeartFly.scaleX = 0f
            ivHeartFly.scaleY = 0f
            ivHeartFly.alpha = 1f
            ivHeartFly.animate()
                .scaleX(1.3f).scaleY(1.3f).setDuration(300)
                .withEndAction {
                    ivHeartFly.animate()
                        .scaleX(0f).scaleY(0f).alpha(0f).setDuration(400)
                        .withEndAction { ivHeartFly.visibility = View.GONE }
                        .start()
                }.start()
        }

        private fun checkIfUserLiked(reel: ReelModel) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            database.reference.child("Reels").child(reel.reelId).child("likedBy").child(uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        isLiked = snapshot.exists()
                        updateLikeUI()
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        private fun toggleLike(reel: ReelModel, position: Int) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Toast.makeText(itemView.context, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show()
                return
            }

            val reelRef = database.reference.child("Reels").child(reel.reelId)
            val likedByRef = reelRef.child("likedBy").child(uid)

            if (isLiked) {
                likedByRef.removeValue()
                reelRef.child("likeCount").setValue(ServerValue.increment(-1))
                reel.likeCount = maxOf(0, reel.likeCount - 1)
            } else {
                likedByRef.setValue(true)
                reelRef.child("likeCount").setValue(ServerValue.increment(1))
                reel.likeCount += 1
            }

            isLiked = !isLiked
            updateLikeUI()
            animateLikeButton()
            tvLikeCount.text = formatCount(reel.likeCount)
        }

        private fun updateLikeUI() {
            if (isLiked) {
                ivLike.setImageResource(R.drawable.ic_heart_filled)
            } else {
                ivLike.setImageResource(R.drawable.ic_heart)
            }
        }

        private fun animateLikeButton() {
            ivLike.animate().scaleX(1.3f).scaleY(1.3f).setDuration(150)
                .withEndAction { ivLike.animate().scaleX(1f).scaleY(1f).setDuration(150).start() }
                .start()
        }

        private fun loadUserInfo(userId: String) {
            database.reference.child("InfoUser").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val name = snapshot.child("fullName").getValue(String::class.java)
                                ?: snapshot.child("firstName").getValue(String::class.java) ?: "User"
                            tvUsername.text = "@$name"
                            tvMusicInfo.text = "Original Sound - $name"

                            val url = snapshot.child("profilePictureUrl").getValue(String::class.java)
                            if (!url.isNullOrEmpty()) {
                                Glide.with(itemView.context).load(url).circleCrop().into(ivProfilePic)
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        private fun initializePlayer(videoUrl: String) {
            player = ExoPlayer.Builder(itemView.context).build().apply {
                setMediaItem(MediaItem.fromUri(videoUrl))
                repeatMode = Player.REPEAT_MODE_ONE
                prepare()
            }
            playerView.player = player
            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT

            player?.addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    val ratio = videoSize.width.toFloat() / videoSize.height.toFloat()
                    playerView.resizeMode = if (ratio < 1.0f) AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    else AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            })
        }

        fun playVideo() {
            player?.playWhenReady = true
        }

        fun pauseVideo() {
            player?.playWhenReady = false
        }

        private fun formatCount(count: Int): String = when {
            count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fk", count / 1_000.0)
            else -> count.toString()
        }

        fun releasePlayer() {
            player?.release()
            player = null
            itemView.removeCallbacks(hideControllerRunnable)
        }
    }

    override fun onViewRecycled(holder: ReelViewHolder) {
        holder.releasePlayer()
        super.onViewRecycled(holder)
    }
}