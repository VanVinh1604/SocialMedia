package com.example.socialmedia.project.Adapter

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmedia.R

class StoryProgressAdapter(
    private val segmentDurations: List<Long> // mỗi item có duration riêng
) : RecyclerView.Adapter<StoryProgressAdapter.ProgressViewHolder>() {

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
        if (!progressBars.contains(holder.progressBar)) {
            progressBars.add(holder.progressBar)
        }
        holder.progressBar.progress = 0
    }

    override fun getItemCount() = segmentDurations.size

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
        val duration = segmentDurations.getOrNull(index) ?: 5000L
        bar.progress = 0

        val step = 50L // update mỗi 50ms
        val totalSteps = (duration / step).toInt().coerceAtLeast(1)
        var progressValue = 0

        handler.post(object : Runnable {
            override fun run() {
                if (isPaused) {
                    handler.postDelayed(this, step)
                    return
                }
                progressValue++
                val progress = (progressValue * (10000 / totalSteps)).coerceAtMost(10000)
                bar.progress = progress

                if (progressValue < totalSteps) {
                    handler.postDelayed(this, step)
                } else {
                    onFinishSegment?.invoke()
                    currentIndex++
                    animateProgress(currentIndex)
                }
            }
        })
    }

    fun reset() {
        handler.removeCallbacksAndMessages(null)
        currentIndex = 0
        progressBars.forEach { it.progress = 0 }
        isPaused = false
    }


    inner class ProgressViewHolder(val progressBar: ProgressBar) :
        RecyclerView.ViewHolder(progressBar)
}
