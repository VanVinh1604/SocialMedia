package com.example.socialmedia.project.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.ItemProfilePostBinding // (Layout chúng ta sẽ tạo ở bước 2)
import com.example.socialmedia.project.Domain.Model.PostModel

/**
 * Adapter này hiển thị một lưới ảnh (grid) trên trang cá nhân.
 * Nó chỉ hiển thị ảnh thumbnail đầu tiên của mỗi bài đăng.
 */
class ProfilePostAdapter(
    // Một lambda function để xử lý khi người dùng nhấn vào 1 ảnh
    private val onPostClick: (PostModel) -> Unit
) : ListAdapter<PostModel, ProfilePostAdapter.PostViewHolder>(PostDiffCallback()) {

    /**
     * Tạo ViewHolder mới
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemProfilePostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding)
    }

    /**
     * Gắn dữ liệu (bind) vào ViewHolder
     */
    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        holder.bind(post)
    }

    /**
     * Lớp ViewHolder nội
     */
    inner class PostViewHolder(private val binding: ItemProfilePostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: PostModel) {
            // Lấy ảnh media đầu tiên (nếu có) để làm thumbnail
            val thumbnailUrl = if (post.mediaList.isNotEmpty()) {
                post.mediaList[0].mediaUrl
            } else {
                null // Hoặc một ảnh placeholder nếu bài đăng không có ảnh
            }

            // Dùng Glide để tải ảnh
            Glide.with(binding.root.context)
                .load(thumbnailUrl)
                .placeholder(R.drawable.image_backgroud) // Ảnh placeholder
                .error(R.drawable.image_backgroud)       // Ảnh khi lỗi
                .centerCrop() // Cắt ảnh cho vừa ô vuông
                .into(binding.ivPostThumbnail)

            // Bắt sự kiện click vào cả ô ảnh
            binding.root.setOnClickListener {
                onPostClick(post)
            }
        }
    }

    /**
     * DiffUtil giúp RecyclerView cập nhật danh sách một cách hiệu quả
     */
    class PostDiffCallback : DiffUtil.ItemCallback<PostModel>() {
        override fun areItemsTheSame(oldItem: PostModel, newItem: PostModel): Boolean {
            // So sánh ID để biết 2 item có phải là MỘT hay không
            return oldItem.postId == newItem.postId
        }

        override fun areContentsTheSame(oldItem: PostModel, newItem: PostModel): Boolean {
            // So sánh nội dung để biết item có THAY ĐỔI hay không
            return oldItem == newItem // Data class tự động so sánh các trường
        }
    }
}

