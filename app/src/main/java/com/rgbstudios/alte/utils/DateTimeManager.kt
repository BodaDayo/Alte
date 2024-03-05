package com.rgbstudios.alte.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DateTimeManager {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val photoDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun getPhotoTimeStamp(): String {
        val currentTime = Calendar.getInstance().time
        return photoDateFormat.format(currentTime)
    }

    fun getCurrentTimeFormatted(): String {
        // Get the current date and time
        val currentTime = Calendar.getInstance().time

        // Format the date and time
        return dateFormat.format(currentTime)
    }

    fun getStatusTimeStamp(): String {
        // Get the current date and time
        val currentTime = Calendar.getInstance().time
        val format =SimpleDateFormat("yyyy-MM-dd HH:mm a", Locale.getDefault())
        // Format the date and time
        return format.format(currentTime)
    }
}