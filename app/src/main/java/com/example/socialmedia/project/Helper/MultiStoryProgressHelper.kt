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
    private var pausedFraction = 0f
    private var segmentWidth = 0

    fun setup(count: Int) {
        container.removeAllViews()
        segmentViews.clear()

        if (count <= 1) {
            // 1 story -> full width ProgressBar
            val pb = ProgressBar(container.context, null, android.R.attr.progressBarStyleHorizontal).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                max = 10000
                progress = 0
                progressDrawable = container.context.getDrawable(R.drawable.story_progress_drawable)
            }
            container.addView(pb)
            segmentViews.add(pb)
        } else {
            // N stories -> nhiều segment nhỏ
            for (i in 0 until count) {
                val v = View(container.context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                        if (i != count - 1) marginEnd = 4
                    }
                    setBackgroundColor(Color.parseColor("#666666"))
                }
                container.addView(v)
                segmentViews.add(v)
            }
        }
    }

    fun start() {
        if (segmentViews.isEmpty()) return

        container.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                container.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (segmentViews.size > 1) {
                    segmentWidth = container.width / segmentViews.size
                }
                currentIndex = 0
                pausedFraction = 0f
                isRunning = true
                startSegment()
            }
        })
    }

    private fun startSegment() {
        if (currentIndex >= segmentViews.size) {
            isRunning = false
            onFinishAll?.invoke()
            return
        }

        val view = segmentViews[currentIndex]
        startTime = System.currentTimeMillis()

        handler.post(object : Runnable {
            override fun run() {
                if (!isRunning) return
                val elapsed = System.currentTimeMillis() - startTime
                val fraction = ((elapsed.toFloat() / duration) + pausedFraction).coerceIn(0f, 1f)

                if (segmentViews.size == 1 && view is ProgressBar) {
                    view.progress = (view.max * fraction).toInt()
                } else {
                    // Fill bằng weight
                    val lp = view.layoutParams as LinearLayout.LayoutParams
                    lp.weight = fraction
                    view.layoutParams = lp
                    view.setBackgroundColor(Color.WHITE)
                }

                if (fraction >= 1f) {
                    pausedFraction = 0f
                    onFinishSegment?.invoke()
                    currentIndex++
                    startSegment()
                } else {
                    handler.postDelayed(this, 16)
                }
            }
        })
    }


    fun pause() {
        if (!isRunning) return
        isRunning = false
        val view = segmentViews.getOrNull(currentIndex) ?: return
        pausedFraction = if (segmentViews.size == 1 && view is ProgressBar) {
            view.progress.toFloat() / view.max
        } else {
            view.width.toFloat() / segmentWidth
        }
    }

    fun resume() {
        if (isRunning) return
        isRunning = true
        startTime = System.currentTimeMillis()
        startSegment()
    }

    fun reset() {
        handler.removeCallbacksAndMessages(null)
        isRunning = false
        currentIndex = 0
        pausedFraction = 0f

        segmentViews.forEach {
            if (it is ProgressBar) {
                it.progress = 0
            } else {
                val lp = it.layoutParams as LinearLayout.LayoutParams
                lp.weight = 0f
                it.layoutParams = lp
                it.setBackgroundColor(Color.parseColor("#80FFFFFF")) // nền chưa fill
            }
        }

    }
}
