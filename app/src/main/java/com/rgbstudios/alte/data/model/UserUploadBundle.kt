package com.rgbstudios.alte.data.model

import android.graphics.Bitmap

data class UserUploadBundle(
    val about: String?,
    val avatar: Bitmap?,
    val dob: String?,
    val gender: String?,
    val location: String?,
    val name: String,
    val status: String,
    val uid: String,
    val username: String,
)