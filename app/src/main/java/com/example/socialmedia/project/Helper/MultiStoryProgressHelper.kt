package com.example.socialmedia.project.Helper

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.ProgressBar
import com.example.socialmedia.R

class MultiStoryProgressHelper(
    private val container: LinearLayout,
    private val duration: Long = 5000L,
    private val onFinishSegment: (() -> Unit)? = null,
    private val onFinishAll: (() -> Unit)? = null
) {

    private val handler = Handler(Looper.getMainLooper())
    private val segmentViews = mutableListOf<View>()
    private var currentIndex = 0
    private var isRunning = false
    private var startTime: Long = 0
    private var elapsedBeforePause: Long = 0

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return

            val view = segmentViews.getOrNull(currentIndex) ?: return
            val elapsed = System.currentTimeMillis() - startTime + elapsedBeforePause
            val fraction = (elapsed.toFloat() / duration).coerceIn(0f, 1f)

            if (segmentViews.size == 1 && view is ProgressBar) {
                view.progress = (view.max * fraction).toInt()
            } else {
                val lp = view.layoutParams as LinearLayout.LayoutParams
                lp.weight = fraction
                view.layoutParams = lp
                view.setBackgroundColor(Color.WHITE)
            }

            if (fraction >= 1f) {
                elapsedBeforePause = 0
                currentIndex++
                onFinishSegment?.invoke()
                if (currentIndex >= segmentViews.size) {
                    isRunning = false
                    onFinishAll?.invoke()
                } else {
                    startTime = System.currentTimeMillis()
                    handler.postDelayed(this, 16)
                }
            } else {
                handler.postDelayed(this, 16)
            }
        }
    }

    fun setup(count: Int) {
        container.removeAllViews()
        segmentViews.clear()

        if (count <= 0) return

        for (i in 0 until count) {
            val v = View(container.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f  // chia đều width
                ).apply {
                    if (i != count - 1) marginEnd = 4 // khoảng cách giữa các segment
                }
                setBackgroundColor(Color.parseColor("#666666")) // màu nền
            }
            container.addView(v)
            segmentViews.add(v)
        }
    }


    fun start() {
        if (segmentViews.isEmpty()) return

        container.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                container.viewTreeObserver.removeOnGlobalLayoutListener(this)
                startTime = System.currentTimeMillis()
                isRunning = true
                handler.post(updateRunnable)
            }
        })
    }

    fun pause() {
        if (!isRunning) return
        isRunning = false
        elapsedBeforePause += System.currentTimeMillis() - startTime
        handler.removeCallbacks(updateRunnable)
    }

    fun resume() {
        if (isRunning) return
        isRunning = true
        startTime = System.currentTimeMillis()
        handler.post(updateRunnable)
    }

    fun reset() {
        handler.removeCallbacksAndMessages(null)
        isRunning = false
        currentIndex = 0
        elapsedBeforePause = 0

        segmentViews.forEach {
            if (it is ProgressBar) {
                it.progress = 0
            } else {
                val lp = it.layoutParams as LinearLayout.LayoutParams
                lp.weight = 0f
                it.layoutParams = lp
                it.setBackgroundColor(Color.parseColor("#80FFFFFF"))
            }
        }
    }

    private fun updateSegmentUI(fraction: Float) {
        segmentViews.forEachIndexed { index, view ->
            when {
                index < currentIndex -> view.setBackgroundColor(Color.WHITE)  // segment đã hoàn thành
                index == currentIndex -> {
                    // segment hiện tại fill theo fraction
                    view.setBackgroundColor(Color.WHITE)
                    // nếu muốn fill từng pixel mượt thì có thể dùng view con hoặc progress drawable
                }
                else -> view.setBackgroundColor(Color.parseColor("#666666")) // segment chưa đến
            }
        }
    }

    fun goTo(index: Int) {
        if (index !in segmentViews.indices) return
        handler.removeCallbacks(updateRunnable)
        currentIndex = index
        elapsedBeforePause = 0
        startTime = System.currentTimeMillis()
        updateSegmentUI(0f)  // vẽ segment hiện tại ngay
        if (isRunning) handler.post(updateRunnable)
    }

}
