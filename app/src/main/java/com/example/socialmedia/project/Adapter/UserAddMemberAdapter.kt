package com.example.socialmedia.project.Adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.project.Domain.Model.UserModel
import com.google.android.material.imageview.ShapeableImageView

class UserAddMemberAdapter(
    private val allUsers: List<UserModel>,
    private val currentMembers: List<String>,   // userId đã trong nhóm
    private val onSelectedChanged: (List<String>) -> Unit
) : RecyclerView.Adapter<UserAddMemberAdapter.UserViewHolder>() {

    private var filteredList = allUsers.toMutableList()
    private val selectedToAdd = mutableSetOf<String>() // user mới cần add

    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgAvatar = view.findViewById<ShapeableImageView>(R.id.imgAvatar)
        val tvName = view.findViewById<TextView>(R.id.tvName)
        val checkBox = view.findViewById<CheckBox>(R.id.cbSelect)
        val tvJoined = view.findViewById<TextView>(R.id.tvJoined) // Tag "Đã tham gia"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_add_member, parent, false)
        return UserViewHolder(view)
    }

    override fun getItemCount() = filteredList.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = filteredList[position]

        // Avatar
        Glide.with(holder.itemView.context)
            .load(user.profilePictureUrl)
            .placeholder(R.drawable.image_avata_user)
            .into(holder.imgAvatar)

        holder.tvName.text = user.fullName

        val isAlreadyMember = currentMembers.contains(user.userId)

        if (isAlreadyMember) {

            holder.checkBox.isChecked = true
            holder.checkBox.isEnabled = false

            holder.tvJoined.visibility = View.VISIBLE

            // Làm mờ dòng
            holder.itemView.alpha = 0.5f

            // Khóa click
            holder.itemView.isClickable = false

            Log.e("DEBUG", "User: ${user.userId} - inGroup: $isAlreadyMember")


        } else {
            holder.tvJoined.visibility = View.GONE
            holder.itemView.alpha = 1f
            holder.checkBox.isEnabled = true

            holder.checkBox.isChecked = selectedToAdd.contains(user.userId)

            holder.itemView.setOnClickListener {
                toggleSelection(user.userId, holder)
            }

            holder.checkBox.setOnClickListener {
                toggleSelection(user.userId, holder)
            }
        }
    }

    private fun toggleSelection(userId: String, holder: UserViewHolder) {
        if (selectedToAdd.contains(userId)) {
            selectedToAdd.remove(userId)
            holder.checkBox.isChecked = false
        } else {
            selectedToAdd.add(userId)
            holder.checkBox.isChecked = true
        }
        onSelectedChanged(selectedToAdd.toList())
    }

    fun filter(query: String) {
        filteredList = if (query.isBlank()) {
            allUsers.toMutableList()
        } else {
            allUsers.filter {
                it.fullName.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }
}


