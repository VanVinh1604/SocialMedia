package com.example.socialmedia.project.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.project.Domain.Enum.NotificationType
import com.example.socialmedia.project.Domain.Model.NotificationModel
import com.example.socialmedia.project.Helper.TimeUtils
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val items: MutableList<NotificationItem> = mutableListOf()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is NotificationItem.Header -> TYPE_HEADER
            is NotificationItem.NotificationData -> TYPE_NOTIFICATION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(inflater.inflate(R.layout.item_header_notification, parent, false))
            else -> NotificationViewHolder(inflater.inflate(R.layout.item_notification, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is NotificationItem.Header -> {
                (holder as HeaderViewHolder).tvHeader.text = item.title
            }

            is NotificationItem.NotificationData -> {
                val n = item.data
                val vh = holder as NotificationViewHolder

                val actorName = n.actorName ?: "Người dùng"
                val message = when (n.notificationType) {
                    NotificationType.LIKE -> "$actorName đã thích bài viết của bạn"
                    NotificationType.COMMENT -> "$actorName đã bình luận về bài viết của bạn"
                    NotificationType.FOLLOW -> "$actorName đã bắt đầu theo dõi bạn"
                    NotificationType.FOLLOW_REQUEST -> "$actorName đã gửi yêu cầu theo dõi bạn"
                    NotificationType.MENTION -> "$actorName đã nhắc đến bạn trong một bài viết"
                    NotificationType.TAG -> "$actorName đã gắn thẻ bạn trong một bài viết"
                    NotificationType.COMMENT_REPLY -> "$actorName đã trả lời bình luận của bạn"
                    NotificationType.STORY_REPLY -> "$actorName đã phản hồi story của bạn"
                    NotificationType.POST_SHARE -> "$actorName đã chia sẻ bài viết của bạn"
                }

// 💬 Làm đậm tên người dùng
                val spannable = android.text.SpannableString(message)
                spannable.setSpan(
                    android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                    0,
                    actorName.length,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                vh.tvMessage.text = spannable

                vh.tvTime.text = TimeUtils.getTimeAgo(n.createdAt)

                Glide.with(vh.itemView.context)
                    .load(n.actorAvatar.takeIf { !it.isNullOrEmpty() })
                    .placeholder(R.drawable.image_avata_user) // ảnh mặc định khi chưa có avatar
                    .error(R.drawable.image_avata_user)       // ảnh fallback nếu lỗi
                    .circleCrop()
                    .into(vh.imgUser)

            }
        }
    }

    override fun getItemCount(): Int = items.size

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHeader: TextView = view.findViewById(R.id.tvHeader)
    }

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgUser: ImageView = view.findViewById(R.id.imgUser)
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }
    fun updateListGrouped(notifications: List<NotificationModel>) {
        val grouped = mutableListOf<NotificationItem>()

        // Nhóm theo "ngày" nhưng hiển thị bằng tiêu đề thân thiện
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val map = notifications.groupBy { sdf.format(Date(it.createdAt)) }

        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000

        map.toSortedMap(compareByDescending { it }).forEach { (dateKey, list) ->
            val sampleDate = Date(list.first().createdAt)
            val diffDays = (now - sampleDate.time) / oneDay

            val title = when {
                diffDays < 1 -> "Hôm nay"
                diffDays < 2 -> "Hôm qua"
                diffDays < 7 -> "$diffDays ngày trước"
                else -> {
                    val sdfDisplay = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    sdfDisplay.format(sampleDate)
                }
            }

            grouped.add(NotificationItem.Header(title))
            grouped.addAll(list.map { NotificationItem.NotificationData(it) })
        }

        items.clear()
        items.addAll(grouped)
        notifyDataSetChanged()
    }

}
