package com.rgbstudios.alte.data.model

data class CurrentUserDetails(
    val about: String?,
    val avatarUri: String?,
    val dob: String?,
    val folks: List<String>?,
    val gender: String?,
    val invites: List<String>?,
    val name: String?,
    val requests: List<String>?,
    val status: String,
    val uid: String,
    val username: String
)
