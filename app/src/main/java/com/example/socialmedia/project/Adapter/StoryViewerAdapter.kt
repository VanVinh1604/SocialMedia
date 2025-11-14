package com.example.socialmedia.project.Adapter

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.socialmedia.R
import com.example.socialmedia.project.Domain.Enum.MediaType
import com.example.socialmedia.project.Domain.Model.StoryModel

class StoryViewerAdapter(
    private val stories: List<StoryModel>,
    private val onVideoReady: ((videoView: VideoView?, position: Int, duration: Long) -> Unit)? = null,
    private val onVideoCompleted: (() -> Unit)? = null,
    private val onItemClick: ((story: StoryModel, position: Int) -> Unit)? = null // ✅ thêm

) : RecyclerView.Adapter<StoryViewerAdapter.StoryViewHolder>() {

    private val videoPositions = mutableMapOf<Int, Int>()

    inner class StoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgStory: ImageView = view.findViewById(R.id.imgStory)
        val videoStory: VideoView = view.findViewById(R.id.videoStory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_story_viewpager, parent, false)

        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val story = stories[position]

        if (story.mediaType == MediaType.VIDEO) {
            bindVideo(holder, story, position)
        } else {
            bindImage(holder, story)
        }
    }

    private fun bindVideo(holder: StoryViewHolder, story: StoryModel, position: Int) {
        holder.imgStory.visibility = View.GONE
        holder.videoStory.visibility = View.VISIBLE

        try {
            val uri = Uri.parse(story.mediaUrl)
            holder.videoStory.setVideoURI(uri)

            holder.videoStory.setOnPreparedListener { mp ->
                mp.isLooping = false
                mp.setVolume(1f, 1f)

                val actualDuration =
                    if (story.duration > 0) story.duration.toLong()
                    else mp.duration.toLong()

                Log.d("StoryViewerAdapter", "🎬 Video ready ($actualDuration ms) at pos=$position")

                // Restore vị trí video nếu trước đó đã pause
                val lastPos = videoPositions[position] ?: 0
                holder.videoStory.seekTo(lastPos)
                holder.videoStory.start()

                onVideoReady?.invoke(holder.videoStory, position, actualDuration)
            }

            holder.videoStory.setOnCompletionListener {
                Log.d("StoryViewerAdapter", "✅ Video completed at pos=$position")
                onVideoCompleted?.invoke()
            }

            holder.videoStory.setOnErrorListener { _, what, extra ->
                Log.e("StoryViewerAdapter", "❌ Video error: what=$what, extra=$extra")
                holder.videoStory.visibility = View.GONE
                holder.imgStory.visibility = View.VISIBLE
                Glide.with(holder.itemView.context)
                    .load(story.thumbnailUrl ?: R.drawable.story_placeholder)
                    .transition(DrawableTransitionOptions.withCrossFade(300))
                    .into(holder.imgStory)
                true
            }

        } catch (e: Exception) {
            Log.e("StoryViewerAdapter", "❌ Exception: ${e.message}")
            holder.videoStory.visibility = View.GONE
            holder.imgStory.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(story.thumbnailUrl ?: R.drawable.story_placeholder)
                .into(holder.imgStory)
        }
    }

    private fun bindImage(holder: StoryViewHolder, story: StoryModel) {
        holder.videoStory.visibility = View.GONE
        holder.imgStory.visibility = View.VISIBLE

        Glide.with(holder.itemView.context)
            .load(story.mediaUrl)
            .placeholder(R.drawable.story_placeholder)
            .error(R.drawable.story_placeholder)
            .transition(DrawableTransitionOptions.withCrossFade(300))
            .into(holder.imgStory)
    }

    override fun onViewRecycled(holder: StoryViewHolder) {
        super.onViewRecycled(holder)
        // Lưu lại vị trí video hiện tại trước khi recycle
        holder.adapterPosition.takeIf { it in stories.indices }?.let { pos ->
            videoPositions[pos] = holder.videoStory.currentPosition
        }
        holder.videoStory.stopPlayback()
        holder.videoStory.setOnPreparedListener(null)
        holder.videoStory.setOnCompletionListener(null)
    }

    override fun onViewDetachedFromWindow(holder: StoryViewHolder) {
        super.onViewDetachedFromWindow(holder)
        // Chỉ stop video khi view bị detach hoàn toàn
        holder.adapterPosition.takeIf { it in stories.indices }?.let { pos ->
            videoPositions[pos] = holder.videoStory.currentPosition
        }
        holder.videoStory.pause() // giữ frame hiện tại, không stopPlayback
    }

    override fun getItemCount(): Int = stories.size
}
