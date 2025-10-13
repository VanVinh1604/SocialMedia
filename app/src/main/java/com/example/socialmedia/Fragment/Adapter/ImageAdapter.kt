package com.example.socialmedia.Fragment.Adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmedia.R

class ImageAdapter(
    private var images: MutableList<Uri>,
    private val onImageClick: (Uri) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    private val selectedImages = mutableSetOf<Uri>()

    inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.image_item)
        val checkOverlay: View = view.findViewById(R.id.check_overlay)
        val checkNumber: TextView = view.findViewById(R.id.check_number)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_grid, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUri = images[position]
        holder.imageView.setImageURI(imageUri)

        val isSelected = selectedImages.contains(imageUri)
        holder.checkOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.checkNumber.visibility = if (isSelected) View.VISIBLE else View.GONE

        if (isSelected) {
            val index = selectedImages.indexOf(imageUri) + 1
            holder.checkNumber.text = index.toString()
        }

        holder.itemView.setOnClickListener {
            if (selectedImages.contains(imageUri)) {
                selectedImages.remove(imageUri)
            } else {
                selectedImages.add(imageUri)
            }
            notifyItemChanged(position)
            onImageClick(imageUri)
        }
    }

    override fun getItemCount() = images.size

    fun updateImages(newImages: List<Uri>) {
        images.clear()
        images.addAll(newImages)
        notifyDataSetChanged()
    }

    fun getSelectedImages(): List<Uri> = selectedImages.toList()

    fun clearSelection() {
        selectedImages.clear()
        notifyDataSetChanged()
    }
}
