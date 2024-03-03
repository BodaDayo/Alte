package com.rgbstudios.alte

import android.app.Application

class AlteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        private var instance: AlteApplication? = null

        fun applicationContext() : AlteApplication {
            return instance as AlteApplication
        }
    }
}