package com.rgbstudios.alte.data.model

data class UserDetails(
    val about: String?,
    val avatarUri: String?,
    val dob: String?,
    val gender: String?,
    val location: String?,
    val name: String,
    val typing: String,
    val status: String,
    val uid: String,
    val username: String
)
