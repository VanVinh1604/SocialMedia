package com.example.socialmedia.project.Helper

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.LinearLayout

class MultiStoryProgressHelper(
    private val container: LinearLayout,
    private val duration: Long = 5000L,
    private val onFinishSegment: (() -> Unit)? = null,
    private val onFinishAll: (() -> Unit)? = null
) {

    private val handler = Handler(Looper.getMainLooper())
    private val segmentForegrounds = mutableListOf<View>()
    private var currentIndex = 0
    private var isRunning = false
    private var startTime: Long = 0
    private var elapsedBeforePause: Long = 0

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            val fg = segmentForegrounds.getOrNull(currentIndex) ?: return

            val elapsed = System.currentTimeMillis() - startTime + elapsedBeforePause
            val fraction = (elapsed.toFloat() / duration).coerceIn(0f, 1f)

            // Tính chiều rộng theo % của segment gốc
            val parentWidth = (fg.parent as View).width
            val newWidth = (parentWidth * fraction).toInt()
            fg.layoutParams = (fg.layoutParams as FrameLayout.LayoutParams).apply {
                width = newWidth
            }
            fg.requestLayout()

            if (fraction >= 1f) {
                elapsedBeforePause = 0
                currentIndex++
                onFinishSegment?.invoke()
                if (currentIndex >= segmentForegrounds.size) {
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
        segmentForegrounds.clear()
        if (count <= 0) return

        for (i in 0 until count) {
            val frame = FrameLayout(container.context).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                    .apply { if (i != count - 1) marginEnd = 8 }
            }

            val bg = View(container.context).apply {
                setBackgroundColor(Color.parseColor("#666666"))
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }

            val fg = View(container.context).apply {
                setBackgroundColor(Color.WHITE)
                layoutParams = FrameLayout.LayoutParams(0, FrameLayout.LayoutParams.MATCH_PARENT)
            }

            frame.addView(bg)
            frame.addView(fg)
            container.addView(frame)
            segmentForegrounds.add(fg)
        }
    }

    fun start() {
        if (segmentForegrounds.isEmpty()) return
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
        segmentForegrounds.forEach { fg ->
            fg.layoutParams = (fg.layoutParams as FrameLayout.LayoutParams).apply {
                width = 0
            }
            fg.requestLayout()
        }
    }

    fun goTo(index: Int) {
        if (index !in segmentForegrounds.indices) return
        handler.removeCallbacks(updateRunnable)
        currentIndex = index
        elapsedBeforePause = 0
        startTime = System.currentTimeMillis()

        // Cập nhật UI: các thanh trước đầy, thanh hiện tại reset
        segmentForegrounds.forEachIndexed { i, fg ->
            val parent = fg.parent as FrameLayout
            val fullWidth = parent.width
            fg.layoutParams = (fg.layoutParams as FrameLayout.LayoutParams).apply {
                width = if (i < index) fullWidth else 0
            }
            fg.requestLayout()
        }

        if (isRunning) handler.post(updateRunnable)
    }
}
