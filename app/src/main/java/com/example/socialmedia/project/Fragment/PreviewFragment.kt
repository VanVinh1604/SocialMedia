package com.example.socialmedia.Fragment.Fragment

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.socialmedia.R

class PreviewFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_preview, container, false)

        val selectedImages = arguments?.getParcelableArrayList<Uri>("selectedImages")
        Log.d("PreviewFragment", "📷 Nhận ${selectedImages?.size ?: 0} ảnh để xem trước")

        val previewImage = view.findViewById<ImageView>(R.id.previewImage)
        if (!selectedImages.isNullOrEmpty()) {
            previewImage.setImageURI(selectedImages[0])
        }

        return view
    }
}
