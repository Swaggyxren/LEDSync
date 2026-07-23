package com.xiiann.ledsync.presentation.lab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiiann.ledsync.data.repository.HardwareRepository
import com.xiiann.ledsync.data.repository.LogEntry
import com.xiiann.ledsync.domain.model.DeviceConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LedLabViewModel @Inject constructor(
    private val hardwareRepository: HardwareRepository
) : ViewModel() {

    val isReady: StateFlow<Boolean> = hardwareRepository.isReady
    val actionLogs: StateFlow<List<LogEntry>> = hardwareRepository.actionLogs

    fun getDeviceConfig(): DeviceConfig = hardwareRepository.getConfig()

    fun sendEffect(name: String, hex: String) {
        viewModelScope.launch {
            hardwareRepository.sendRawHex(hex, "Effect active: $name")
        }
    }

    fun emergencyKillAndRevive() {
        viewModelScope.launch {
            hardwareRepository.emergencyKillAndRevive()
        }
    }

    fun initHardware() {
        viewModelScope.launch {
            hardwareRepository.initializeHardware(force = true)
        }
    }
}
