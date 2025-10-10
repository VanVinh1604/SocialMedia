package com.example.socialmedia.Fragment.Helper

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.widget.TextView

object TextGradientUtils {
    fun applyGradient(textView: TextView, startColor: String, endColor: String, angle: Float = 45f) {
        textView.post {
            val width = textView.measuredWidth.toFloat()
            val height = textView.textSize
            val shader = LinearGradient(
                0f, 0f, width, height,
                intArrayOf(Color.parseColor(startColor), Color.parseColor(endColor)),
                null, Shader.TileMode.CLAMP
            )
            textView.paint.shader = shader
            textView.invalidate()
        }
    }
}