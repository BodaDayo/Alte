package com.rgbstudios.alte.data.model

data class CurrentUserDetails(
    val about: String?,
    val avatarUri: String?,
    val dob: String?,
    val exile: List<String>?,
    val folks: List<String>?,
    val gender: String?,
    val invites: List<String>?,
    val location: String?,
    val name: String,
    val typing: String,
    val requests: List<String>?,
    val starredMessages: List<Pair<String?, Chat?>>,
    val status: String,
    val uid: String,
    val username: String,
)
