package com.example.socialmedia.project.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.project.Domain.Model.UserModel
import com.google.android.material.imageview.ShapeableImageView

class SelectUserAdapter(
    private var userList: List<UserModel>,
    private val onUserSelected: (UserModel, Boolean) -> Unit
) : RecyclerView.Adapter<SelectUserAdapter.UserVH>() {

    private val selectedIds = mutableSetOf<String>()

    inner class UserVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatar = itemView.findViewById<ShapeableImageView>(R.id.imgAvatar)
        val name = itemView.findViewById<TextView>(R.id.tvName)
        val radio = itemView.findViewById<RadioButton>(R.id.radioSelect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_select, parent, false)
        return UserVH(view)
    }

    override fun onBindViewHolder(holder: UserVH, position: Int) {
        val user = userList[position]

        holder.name.text = user.fullName
        Glide.with(holder.itemView).load(user.profilePictureUrl).into(holder.avatar)

        holder.radio.isChecked = selectedIds.contains(user.userId)

        holder.itemView.setOnClickListener {
            toggleUser(user, holder)
        }
        holder.radio.setOnClickListener {
            toggleUser(user, holder)
        }
    }

    private fun toggleUser(user: UserModel, holder: UserVH) {
        val isSelected: Boolean

        if (selectedIds.contains(user.userId)) {
            selectedIds.remove(user.userId)
            holder.radio.isChecked = false
            isSelected = false
        } else {
            selectedIds.add(user.userId)
            holder.radio.isChecked = true
            isSelected = true
        }

        onUserSelected(user, isSelected)
    }

    override fun getItemCount() = userList.size
}
