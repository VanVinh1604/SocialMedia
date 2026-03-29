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
    private val segmentDurations: List<Long>, // Duration riêng cho từng video
    private val onFinishSegment: (() -> Unit)? = null,
    private val onFinishAll: (() -> Unit)? = null,
    private val onProgressUpdate: ((currentIndex: Int, progress: Map<Int, Float>) -> Unit)? = null
) {

    private val handler = Handler(Looper.getMainLooper())
    private val segmentForegrounds = mutableListOf<View>()
    private var currentIndex = 0

    var isRunning = false
        private set

    private var startTime: Long = 0
    private var elapsedBeforePause: Long = 0
    private val segmentProgressMap = mutableMapOf<Int, Float>()

    fun setup() {
        container.removeAllViews()
        segmentForegrounds.clear()
        segmentProgressMap.clear()
        if (segmentDurations.isEmpty()) return

        for (i in segmentDurations.indices) {
            val frame = FrameLayout(container.context).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                    .apply { if (i != segmentDurations.lastIndex) marginEnd = 8 }
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
                // Segment đã xem hoàn toàn hoặc có progress = 1f → full
                savedProgress?.get(i)?.let { it >= 1f } == true -> 1f
                // Segment hiện tại → lấy progress lưu nếu có, nếu không full luôn
                i == currentIndex -> savedProgress?.get(i) ?: 1f
                // Segment chưa xem → 0
                else -> 0f
            }
            fg.layoutParams = (fg.layoutParams as FrameLayout.LayoutParams).apply { width = (parentWidth * p).toInt() }
            fg.requestLayout()
            segmentProgressMap[i] = p
        }
        this.currentIndex = currentIndex
        elapsedBeforePause = savedProgress?.get(currentIndex)?.times(segmentDurations.getOrElse(currentIndex) { 5000L })?.toLong() ?: 0
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
        elapsedBeforePause = if (resetCurrentSegment) 0 else (segmentProgressMap[index] ?: 0f * segmentDurations[index]).toLong()
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
                Log.d("MultiStoryProgressHelper", "Runnable stopped, view not attached")
                return
            }

            val currentDuration = segmentDurations.getOrElse(currentIndex) { 5000L }
            val elapsed = System.currentTimeMillis() - startTime + elapsedBeforePause
            val fraction = (elapsed.toFloat() / currentDuration).coerceIn(0f, 1f)
            val parentWidth = parent.width
            fg.layoutParams = (fg.layoutParams as FrameLayout.LayoutParams).apply { width = (parentWidth * fraction).toInt() }
            fg.requestLayout()
            segmentProgressMap[currentIndex] = fraction

            Log.d("MultiStoryProgressHelper", "Segment $currentIndex: fraction=$fraction, elapsed=$elapsed/${currentDuration}")

            onProgressUpdate?.invoke(currentIndex, segmentProgressMap)

            if (fraction >= 1f) {
                elapsedBeforePause = 0
                currentIndex++
                try { onFinishSegment?.invoke() } catch (e: Exception) {}
                if (currentIndex >= segmentForegrounds.size) {
                    isRunning = false
                    try { onFinishAll?.invoke() } catch (e: Exception) {}
                    Log.d("MultiStoryProgressHelper", "All segments finished")
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
