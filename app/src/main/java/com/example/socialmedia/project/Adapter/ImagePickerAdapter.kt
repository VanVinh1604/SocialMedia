package com.example.socialmedia.project.Adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R

class ImagePickerAdapter(
    private val images: List<Uri>,
    private val selectedImages: MutableList<Uri>,
    private val maxSelection: Int = 10,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<ImagePickerAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val overlay: View = itemView.findViewById(R.id.overlay)

        val orderNumber: TextView = itemView.findViewById(R.id.orderNumber)
        val iconSelected: ImageView = itemView.findViewById(R.id.iconSelected)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_picker, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUri = images[position]
        val isSelected = selectedImages.contains(imageUri)
        val selectionIndex = selectedImages.indexOf(imageUri)

        // Load image with Glide
        Glide.with(holder.itemView.context)
            .load(imageUri)
            .centerCrop()
            .into(holder.imageView)

        // Update selection state


        // Show/hide overlay
        if (isSelected) {
            holder.overlay.visibility = View.VISIBLE
            holder.overlay.alpha = 0.6f
        } else {
            holder.overlay.visibility = View.GONE
            holder.overlay.alpha = 0f
        }

        // Show order number if selected
        if (isSelected && selectionIndex >= 0) {
            holder.orderNumber.visibility = View.VISIBLE
            holder.orderNumber.text = (selectionIndex + 1).toString()
        } else {
            holder.orderNumber.visibility = View.GONE
        }

        // Click listener for the entire item
        holder.itemView.setOnClickListener {
            handleImageSelection(holder, imageUri, position)
        }

        // Prevent checkbox from being clickable independently

    }

    private fun handleImageSelection(holder: ImageViewHolder, imageUri: Uri, position: Int) {
        val isCurrentlySelected = selectedImages.contains(imageUri)

        if (isCurrentlySelected) {
            // Deselect image
            selectedImages.remove(imageUri)
            // Notify all items to update order numbers
            notifyDataSetChanged()
        } else {
            // Select image if limit not reached
            if (selectedImages.size < maxSelection) {
                selectedImages.add(imageUri)
                notifyItemChanged(position)
                // Update other items to show correct order numbers
                notifyItemRangeChanged(0, itemCount)
            } else {
                // Show toast when limit reached
                Toast.makeText(
                    holder.itemView.context,
                    "Chỉ có thể chọn tối đa $maxSelection ảnh",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Notify parent about selection change
        onSelectionChanged(selectedImages.size)
    }

    override fun getItemCount(): Int = images.size

    // Helper method to get selected images in order
    fun getSelectedImages(): List<Uri> = selectedImages.toList()

    // Helper method to clear all selections
    fun clearSelections() {
        selectedImages.clear()
        notifyDataSetChanged()
        onSelectionChanged(0)
    }
}