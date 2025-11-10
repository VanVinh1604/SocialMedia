package com.example.socialmedia.project.Helper

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.LinearLayout

class MultiStoryProgressHelper(
    private val container: LinearLayout,
    private val duration: Long = 5000L,
    private val onFinishSegment: (() -> Unit)? = null,
    private val onFinishAll: (() -> Unit)? = null,
    private val onProgressUpdate: ((currentIndex: Int, progress: Map<Int, Float>) -> Unit)? = null // callback lưu progress
) {

    private val handler = Handler(Looper.getMainLooper())
    private val segmentForegrounds = mutableListOf<View>()
    private var currentIndex = 0

    var isRunning = false
        private set

    private var startTime: Long = 0
    private var elapsedBeforePause: Long = 0
    private val segmentProgressMap = mutableMapOf<Int, Float>()

    fun setup(count: Int) {
        container.removeAllViews()
        segmentForegrounds.clear()
        segmentProgressMap.clear()
        if (count <= 0) return

        for (i in 0 until count) {
            val frame = FrameLayout(container.context).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                    .apply { if (i != count - 1) marginEnd = 8 }
            }

            val bg = View(container.context).apply {
                setBackgroundColor(Color.parseColor("#666666"))
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }

            val fg = View(container.context).apply {
                setBackgroundColor(Color.WHITE)
                layoutParams = FrameLayout.LayoutParams(0, FrameLayout.LayoutParams.MATCH_PARENT)
            }

            frame.addView(bg)
            frame.addView(fg)
            container.addView(frame)
            segmentForegrounds.add(fg)
            segmentProgressMap[i] = 0f
        }
    }

    fun restoreProgress(currentIndex: Int, savedProgress: Map<Int, Float>?) {
        segmentForegrounds.forEachIndexed { i, fg ->
            val parentWidth = (fg.parent as? View)?.width ?: 0
            val p = when {
                i < currentIndex -> 1f
                i == currentIndex -> savedProgress?.get(i) ?: 0f
                else -> 0f
            }
            fg.layoutParams = (fg.layoutParams as FrameLayout.LayoutParams).apply { width = (parentWidth * p).toInt() }
            fg.requestLayout()
            segmentProgressMap[i] = p
        }
        this.currentIndex = currentIndex
        elapsedBeforePause = 0
    }

    fun start() {
        if (segmentForegrounds.isEmpty()) return
        container.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                container.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (container.windowToken == null) return
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
        if (container.windowToken == null) return
        isRunning = true
        startTime = System.currentTimeMillis()
        handler.post(updateRunnable)
    }

    fun reset() {
        handler.removeCallbacksAndMessages(null)
        isRunning = false
        currentIndex = 0
        elapsedBeforePause = 0
        segmentProgressMap.clear()
        segmentForegrounds.forEach { fg ->
            fg.layoutParams = (fg.layoutParams as FrameLayout.LayoutParams).apply { width = 0 }
            fg.requestLayout()
        }
    }



    fun goTo(index: Int, resumeImmediately: Boolean = true, resetCurrentSegment: Boolean = true) {
        if (index !in segmentForegrounds.indices) return
        handler.removeCallbacks(updateRunnable)
        currentIndex = index
        elapsedBeforePause = if (resetCurrentSegment) 0 else (segmentProgressMap[index] ?: 0f * duration).toLong()
        startTime = System.currentTimeMillis()

        segmentForegrounds.forEachIndexed { i, fg ->
            val parentWidth = (fg.parent as? View)?.width ?: 0
            val p = when {
                i < index -> 1f
                i == index -> if (resetCurrentSegment) 0f else segmentProgressMap[i] ?: 0f
                else -> 0f
            }
            fg.layoutParams = (fg.layoutParams as FrameLayout.LayoutParams).apply { width = (parentWidth * p).toInt() }
            fg.requestLayout()
            segmentProgressMap[i] = p
        }

        if (resumeImmediately) {
            isRunning = true
            handler.post(updateRunnable)
        }
    }

    // ---- PRIVATE RUNNABLE ----

    private val updateRunnable = object : Runnable {
        override fun run() {
            val fg = segmentForegrounds.getOrNull(currentIndex) ?: return
            val parent = fg.parent as? View ?: return
            if (parent.windowToken == null || !parent.isAttachedToWindow) {
                isRunning = false
                handler.removeCallbacks(this)
                return
            }

            val elapsed = System.currentTimeMillis() - startTime + elapsedBeforePause
            val fraction = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
            val parentWidth = (fg.parent as View).width
            fg.layoutParams = (fg.layoutParams as FrameLayout.LayoutParams).apply { width = (parentWidth * fraction).toInt() }
            fg.requestLayout()
            segmentProgressMap[currentIndex] = fraction

            // Tự động lưu progress
            onProgressUpdate?.invoke(currentIndex, segmentProgressMap)

            if (fraction >= 1f) {
                elapsedBeforePause = 0
                currentIndex++
                try { onFinishSegment?.invoke() } catch (e: Exception) {}
                if (currentIndex >= segmentForegrounds.size) {
                    isRunning = false
                    try { onFinishAll?.invoke() } catch (e: Exception) {}
                } else {
                    startTime = System.currentTimeMillis()
                    handler.postDelayed(this, 16)
                }
            } else {
                handler.postDelayed(this, 16)
            }
        }
    }
}

