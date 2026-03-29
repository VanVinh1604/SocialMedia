package com.example.socialmedia.project.Helper


import android.content.Context
import android.util.AttributeSet
import android.widget.VideoView

/**
 * 🔹 VideoView bắt buộc phủ full màn hình
 *    - Luôn scale để fill cả width và height
 *    - Crop phần thừa nếu tỷ lệ không khớp
 */
class FullScreenVideoStory @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VideoView(context, attrs, defStyleAttr) {

    private var videoWidth = 0
    private var videoHeight = 0

    fun setVideoSize(width: Int, height: Int) {
        videoWidth = width
        videoHeight = height
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val parentWidth = MeasureSpec.getSize(widthMeasureSpec)
        val parentHeight = MeasureSpec.getSize(heightMeasureSpec)

        if (videoWidth == 0 || videoHeight == 0) {
            // Fallback: chiếm full màn hình
            setMeasuredDimension(parentWidth, parentHeight)
            return
        }

        val videoRatio = videoWidth.toFloat() / videoHeight
        val parentRatio = parentWidth.toFloat() / parentHeight

        val finalWidth: Int
        val finalHeight: Int

        // 🔹 FORCE FILL: Luôn scale để phủ kín cả 2 chiều
        if (videoRatio > parentRatio) {
            // Video rộng → Scale theo HEIGHT (crop left/right)
            finalHeight = parentHeight
            finalWidth = (parentHeight * videoRatio).toInt()
        } else {
            // Video cao → Scale theo WIDTH (crop top/bottom)
            finalWidth = parentWidth
            finalHeight = (parentWidth / videoRatio).toInt()
        }

        // 🔹 Đảm bảo không bao giờ nhỏ hơn parent
        val minWidth = finalWidth.coerceAtLeast(parentWidth)
        val minHeight = finalHeight.coerceAtLeast(parentHeight)

        setMeasuredDimension(minWidth, minHeight)
    }

    override fun layout(l: Int, t: Int, r: Int, b: Int) {
        val parentView = parent as? android.view.View
        val parentWidth = parentView?.width ?: (r - l)
        val parentHeight = parentView?.height ?: (b - t)

        // 🔹 Căn giữa phần crop
        val left = (parentWidth - measuredWidth) / 2
        val top = (parentHeight - measuredHeight) / 2
        val right = left + measuredWidth
        val bottom = top + measuredHeight

        super.layout(left, top, right, bottom)
    }
}