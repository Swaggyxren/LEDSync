package com.xiiann.ledsync.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiiann.ledsync.data.repository.HardwareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val hardwareRepository: HardwareRepository
) : ViewModel() {

    val isReady: StateFlow<Boolean> = hardwareRepository.isReady

    init {
        viewModelScope.launch {
            hardwareRepository.initializeHardware()
        }
    }

    fun retryRoot() {
        viewModelScope.launch {
            hardwareRepository.initializeHardware(force = true)
        }
    }
}
