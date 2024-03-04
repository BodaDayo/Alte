package com.rgbstudios.alte.utils

import com.rgbstudios.alte.R

class AvatarManager {
    val defaultAvatarsList = listOf(
        R.drawable.asset1,
        R.drawable.asset2,
        R.drawable.asset3,
        R.drawable.asset4,
        R.drawable.asset5,
        R.drawable.asset6,
        R.drawable.asset7,
        R.drawable.asset8,
        R.drawable.asset9,
        R.drawable.asset10,
        R.drawable.asset11,
        R.drawable.asset12,
    )
    fun getDefaultAvatar(): Int {
        return defaultAvatarsList.random()
    }

    fun getOnboardingImages(): List<Int> {
        return listOf(
            R.drawable.onboarding1,
            R.drawable.onboarding2,
            R.drawable.onboarding3,
        )
    }
}