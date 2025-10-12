package com.example.socialmedia.project.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmedia.R
import com.example.socialmedia.project.Adapter.PostAdapter
import com.example.socialmedia.project.Domain.StoryTest
import com.example.socialmedia.project.Adapter.StoryAdapter
import com.example.socialmedia.project.Domain.PostModel

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerStory)
        val recyclerPost = view.findViewById<RecyclerView>(R.id.recyclerPost)


        val storyList = listOf(
            StoryTest(0, R.drawable.baseline_add_24, R.drawable.bg_story_rounded, true),
            StoryTest(1, R.drawable.image_backgroud, R.drawable.image_person),
            StoryTest(2, R.drawable.image_avata_user, R.drawable.image_backgroud),
            StoryTest(3, R.drawable.image_person, R.drawable.image_avata_user),
            StoryTest(4, R.drawable.image_backgroud, R.drawable.image_backgroud)
        )

        val samplePosts = List(5) {
            PostModel(
                postId = "$it",
                userId = "User$it",
                content = "This is a sample post #$it",
                likeCount = 10 + it,
                shareCount = 2 + it
            )
        }

        recyclerPost.layoutManager = LinearLayoutManager(requireContext())
        recyclerPost.adapter = PostAdapter(samplePosts)


        recyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = StoryAdapter(storyList)

        return view
    }
}
