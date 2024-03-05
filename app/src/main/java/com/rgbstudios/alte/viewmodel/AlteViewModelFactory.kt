package com.rgbstudios.alte.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rgbstudios.alte.AlteApplication
import com.rgbstudios.alte.data.repository.AlteRepository

class AlteViewModelFactory(private val application: AlteApplication, private val alteRepository: AlteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlteViewModel(application, alteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
