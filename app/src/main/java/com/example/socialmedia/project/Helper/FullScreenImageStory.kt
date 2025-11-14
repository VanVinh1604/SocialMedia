package com.example.socialmedia.project.Helper

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

/**
 * 🔹 ImageView full màn hình, luôn crop để fill parent
 */
class FullScreenImageStory @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var videoWidth = 0
    private var videoHeight = 0

    fun setVideoSize(width: Int, height: Int) {
        videoWidth = width
        videoHeight = height
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (videoWidth == 0 || videoHeight == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        val parentWidth = MeasureSpec.getSize(widthMeasureSpec)
        val parentHeight = MeasureSpec.getSize(heightMeasureSpec)

        val videoRatio = videoWidth.toFloat() / videoHeight
        val parentRatio = parentWidth.toFloat() / parentHeight

        val finalWidth: Int
        val finalHeight: Int

        // 🔹 Center Crop Logic: Luôn phủ kín màn hình
        if (videoRatio > parentRatio) {
            // Video rộng hơn → Scale theo chiều cao, crop hai bên
            finalHeight = parentHeight
            finalWidth = (parentHeight * videoRatio).toInt()
        } else {
            // Video cao hơn → Scale theo chiều rộng, crop trên dưới
            finalWidth = parentWidth
            finalHeight = (parentWidth / videoRatio).toInt()
        }

        setMeasuredDimension(finalWidth, finalHeight)
    }

    override fun layout(l: Int, t: Int, r: Int, b: Int) {
        val parentView = parent as? android.view.View
        val parentWidth = parentView?.width ?: (r - l)
        val parentHeight = parentView?.height ?: (b - t)

        // 🔹 Căn giữa video
        val left = (parentWidth - measuredWidth) / 2
        val top = (parentHeight - measuredHeight) / 2
        val right = left + measuredWidth
        val bottom = top + measuredHeight

        super.layout(left, top, right, bottom)
    }
}
