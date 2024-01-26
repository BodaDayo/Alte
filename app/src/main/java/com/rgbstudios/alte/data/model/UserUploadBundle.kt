package com.rgbstudios.alte.data.model

import android.graphics.Bitmap

data class UserUploadBundle(
    val uid: String,
    val name: String?,
    val username: String,
    val gender: String?,
    val about: String?,
    val dob: String?,
    val avatar: Bitmap?,
    val status: String,
)