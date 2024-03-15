package com.rgbstudios.alte.data.model

data class Peep(
    val timeStamp: String = "",
    val peepUri: String = "",
    val caption: String = "",
    val viewList: List<String> = emptyList()
) {
    // Default (no-argument) constructor for Firebase deserialization
    constructor() : this("", "", "", emptyList())
}