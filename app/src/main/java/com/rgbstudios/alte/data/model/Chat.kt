package com.rgbstudios.alte.data.model

data class Chat(
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timeStamp: String = "",
    val recipientTimestamp: String = "",
    val imageUri: String = "",
    val editTimeStamp: String = "",
    val isDelivered: Boolean = false,
    val isRead: Boolean = false,
) {
    // Default (no-argument) constructor for Firebase deserialization
    constructor() : this("", "", "", "", "", "", "",false, false)
}