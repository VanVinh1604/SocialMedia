package com.example.socialmedia.project.Adapter

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmedia.R

class StoryProgressAdapter(private val count: Int, private val duration: Long) :
    RecyclerView.Adapter<StoryProgressAdapter.ProgressViewHolder>() {

    private var currentIndex = 0
    private val handler = Handler(Looper.getMainLooper())
    private var isPaused = false

    private var onFinishSegment: (() -> Unit)? = null
    private var onFinishAll: (() -> Unit)? = null

    private val progressBars = mutableListOf<ProgressBar>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgressViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_story_progress, parent, false) as ProgressBar
        return ProgressViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProgressViewHolder, position: Int) {
        progressBars.add(holder.progressBar)
    }

    override fun getItemCount() = count

    fun start() {
        if (progressBars.isEmpty()) return
        animateProgress(currentIndex)
    }

    private fun animateProgress(index: Int) {
        if (index >= progressBars.size) {
            onFinishAll?.invoke()
            return
        }

        val bar = progressBars[index]
        bar.progress = 0
        val step = 100
        val totalSteps = (duration / step).toInt()

        var progressValue = 0
        handler.post(object : Runnable {
            override fun run() {
                if (isPaused) {
                    handler.postDelayed(this, step.toLong())
                    return
                }
                progressValue++
                bar.progress = (progressValue * (10000 / totalSteps)).coerceAtMost(10000)

                if (progressValue < totalSteps) {
                    handler.postDelayed(this, step.toLong())
                } else {
                    onFinishSegment?.invoke()
                    currentIndex++
                    animateProgress(currentIndex)
                }
            }
        })
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
    }
    fun stop() {
        handler.removeCallbacksAndMessages(null)
        currentIndex = 0
        isPaused = false
        progressBars.forEach { bar ->
            bar.progress = 0
        }
    }


    fun goTo(position: Int) {
        // Reset các bar trước đó full, sau đó chạy lại từ bar mới
        progressBars.forEachIndexed { i, bar ->
            bar.progress = when {
                i < position -> 10000
                i == position -> 0
                else -> 0
            }
        }
        currentIndex = position
        start()
    }

    fun setCallbacks(onFinishSegment: () -> Unit, onFinishAll: () -> Unit) {
        this.onFinishSegment = onFinishSegment
        this.onFinishAll = onFinishAll
    }

    inner class ProgressViewHolder(val progressBar: ProgressBar) :
        RecyclerView.ViewHolder(progressBar)
}
