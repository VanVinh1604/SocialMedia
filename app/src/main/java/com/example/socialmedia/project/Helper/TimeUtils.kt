package com.example.socialmedia.project.Helper

import android.text.format.DateUtils

object TimeUtils {
    fun getTimeAgo(time: Long): String {
        val now = System.currentTimeMillis()
        return DateUtils.getRelativeTimeSpanString(
            time,
            now,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }
}
