package com.example.socialmedia.project.Fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.project.Helper.Constants
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallConfig
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallFragment

class CallFragment : Fragment() {


    private var otherUserId: String? = null
    private var otherUserName: String? = null
    private var otherUserAvatar: String? = null
    private var isVideoCall: Boolean = true

    private lateinit var tvCallUserName: TextView
    private lateinit var ivCallAvatar: ImageView
    private lateinit var ivCallBack: ImageView
    private lateinit var tvCallTimer: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var startTime = 0L
    private var timerRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            otherUserId = it.getString(ARG_OTHER_USER_ID)
            otherUserName = it.getString(ARG_OTHER_USER_NAME)
            otherUserAvatar = it.getString(ARG_OTHER_USER_AVATAR)
            isVideoCall = it.getBoolean(ARG_IS_VIDEO_CALL, true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_call, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvCallUserName = view.findViewById(R.id.tvCallUserName)
        ivCallAvatar = view.findViewById(R.id.ivCallAvatar)
        ivCallBack = view.findViewById(R.id.ivCallBack)
        tvCallTimer = view.findViewById(R.id.tvCallTimer)

        tvCallUserName.text = otherUserName ?: "Người dùng"
        Glide.with(requireContext())
            .load(otherUserAvatar ?: R.drawable.image_avata_user)
            .circleCrop()
            .into(ivCallAvatar)

        ivCallBack.setOnClickListener { parentFragmentManager.popBackStack() }

        initZegoCallFragment()
        startCallTimer()
    }

    private fun initZegoCallFragment() {
        val localUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val callId = "conversation_${otherUserId}"

        val callConfig = if (isVideoCall) {
            ZegoUIKitPrebuiltCallConfig.oneOnOneVideoCall().apply {
                turnOnCameraWhenJoining = true
                turnOnMicrophoneWhenJoining = true
                useSpeakerWhenJoining = true
            }
        } else {
            ZegoUIKitPrebuiltCallConfig.oneOnOneVoiceCall().apply {
                turnOnCameraWhenJoining = false
                turnOnMicrophoneWhenJoining = true
                useSpeakerWhenJoining = true
            }
        }

        val localUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val localName = localUser?.displayName ?: "Bạn"

        val callFragment = ZegoUIKitPrebuiltCallFragment.newInstance(
            Constants.APP_ID.toLong(),
            Constants.APP_SIGN,
            localUserId,
            localName,
            callId,
            callConfig
        )


        childFragmentManager.beginTransaction()
            .replace(R.id.videoContainer, callFragment)
            .commit()
    }

    private fun startCallTimer() {
        startTime = System.currentTimeMillis()
        timerRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                val minutes = (elapsed / 1000 / 60).toInt()
                val seconds = (elapsed / 1000 % 60).toInt()
                tvCallTimer.text = "%02d:%02d".format(minutes, seconds)
                handler.postDelayed(this, 500)
            }
        }
        handler.post(timerRunnable!!)
    }

    private fun stopCallTimer() {
        timerRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopCallTimer()
    }

    companion object {
        private const val ARG_OTHER_USER_ID = "otherUserId"
        private const val ARG_OTHER_USER_NAME = "otherUserName"
        private const val ARG_OTHER_USER_AVATAR = "otherUserAvatar"
        private const val ARG_IS_VIDEO_CALL = "isVideoCall"

        @JvmStatic
        fun newInstance(
            otherUserId: String,
            otherUserName: String,
            otherUserAvatar: String?,
            isVideoCall: Boolean
        ) = CallFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_OTHER_USER_ID, otherUserId)
                putString(ARG_OTHER_USER_NAME, otherUserName)
                putString(ARG_OTHER_USER_AVATAR, otherUserAvatar)
                putBoolean(ARG_IS_VIDEO_CALL, isVideoCall)
            }
        }
    }


}
