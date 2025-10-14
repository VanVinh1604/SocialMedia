package com.example.socialmedia.project.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.socialmedia.R
import com.example.socialmedia.project.Domain.Model.StoryModel
import com.google.android.material.imageview.ShapeableImageView
import de.hdodenhof.circleimageview.CircleImageView

class StoryAdapter(private val stories: List<StoryModel>,) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_ADD_STORY = 0
        private const val TYPE_STORY = 1
        private const val TYPE_SUGGEST_FRIEND = 2
    }

    override fun getItemViewType(position: Int): Int {
        val story = stories[position]
        return when {
            story.isAddStory -> TYPE_ADD_STORY
            story.isSuggestFriend -> TYPE_SUGGEST_FRIEND
            else -> TYPE_STORY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_ADD_STORY -> AddStoryViewHolder(
                inflater.inflate(R.layout.item_add_story, parent, false)
            )
            TYPE_SUGGEST_FRIEND -> SuggestFriendViewHolder(
                inflater.inflate(R.layout.item_story_suggest_follow, parent, false)
            )
            else -> StoryViewHolder(
                inflater.inflate(R.layout.item_story, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val story = stories[position]
        when (holder) {
            is AddStoryViewHolder -> {
                holder.btnAdd?.setImageResource(R.drawable.baseline_add_24)
            }

            is StoryViewHolder -> {
                holder.tvUserName?.text = story.userName
                Glide.with(holder.itemView.context)
                    .load(story.userProfileImage)
                    .placeholder(R.drawable.image_avata_user)
                    .into(holder.imgAvatar!!)

                Glide.with(holder.itemView.context)
                    .load(story.mediaUrl)
                    .placeholder(R.drawable.bg_story_rounded)
//                    .into(holder.imgBackground!!)
            }

            is SuggestFriendViewHolder -> {
                holder.tvUsername?.text = story.userName
                Glide.with(holder.itemView.context)
                    .load(story.userProfileImage)
                    .placeholder(R.drawable.image_avata_user)
                    .skipMemoryCache(true)       // bỏ cache memory
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // bỏ cache đĩa
                    .into(holder.imgAvatar!!)

                holder.tvFollow?.setOnClickListener {
                    holder.tvFollow.text = "Đã theo dõi"
                }
            }
        }
    }

    override fun getItemCount(): Int = stories.size

    // ViewHolders
    inner class AddStoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val btnAdd: ImageView? = view.findViewById(R.id.btnStory)
    }

    inner class StoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgAvatar: CircleImageView? = view.findViewById(R.id.imgAvatar)
//        val imgBackground: ImageView? = view.findViewById(R.id.im)
        val tvUserName: TextView? = view.findViewById(R.id.tvUsername)
    }

    inner class SuggestFriendViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgAvatar: ShapeableImageView? = view.findViewById(R.id.imgAvatar)
        val tvUsername: TextView? = view.findViewById(R.id.tvUsername)
        val tvFollow: TextView? = view.findViewById(R.id.tvFollow)
        val btnClose: ImageView? = view.findViewById(R.id.ivClose)
    }
}
