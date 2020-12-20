package com.hitg.routeme.presentation.ui.maps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MapsViewModelFactory(
    private val mapsApiKey: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(String::class.java)
            .newInstance(mapsApiKey)
    }
}