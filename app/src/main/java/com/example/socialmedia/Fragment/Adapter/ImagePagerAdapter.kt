package com.example.socialmedia.Fragment.Fragment

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmedia.R

class ImagePagerAdapter(
    private var images: MutableList<Uri>
) : RecyclerView.Adapter<ImagePagerAdapter.PagerViewHolder>() {

    inner class PagerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.image_pager_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_pager, parent, false)
        return PagerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
        holder.imageView.setImageURI(images[position])
    }

    override fun getItemCount() = images.size

    fun updateImages(newImages: List<Uri>) {
        images.clear()
        images.addAll(newImages)
        notifyDataSetChanged()
    }
}