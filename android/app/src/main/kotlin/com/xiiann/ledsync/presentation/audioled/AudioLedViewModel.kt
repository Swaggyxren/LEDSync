package com.xiiann.ledsync.presentation.audioled

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiiann.ledsync.data.repository.HardwareRepository
import com.xiiann.ledsync.data.repository.PreferencesRepository
import com.xiiann.ledsync.domain.model.AudioGain
import com.xiiann.ledsync.domain.model.AudioLedMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioLedViewModel @Inject constructor(
    private val hardwareRepository: HardwareRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val isEnabled: StateFlow<Boolean> = preferencesRepository.audioLedEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isDynamic: StateFlow<Boolean> = preferencesRepository.audioLedDynamic
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val gainLevel: StateFlow<Int> = preferencesRepository.audioLedGain
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AudioGain.DEFAULT_LEVEL)

    fun toggleEnable(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAudioLedEnabled(enabled)
            if (enabled) {
                val dynamic = isDynamic.value
                hardwareRepository.setAudioReactiveMode(
                    mode = if (dynamic) AudioLedMode.DYNAMIC else AudioLedMode.STATIC,
                    gainLevel = gainLevel.value
                )
            } else {
                hardwareRepository.turnOffAudioReactive()
            }
        }
    }

    fun toggleMode(dynamic: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAudioLedDynamic(dynamic)
            if (isEnabled.value) {
                hardwareRepository.setAudioReactiveMode(
                    mode = if (dynamic) AudioLedMode.DYNAMIC else AudioLedMode.STATIC,
                    gainLevel = gainLevel.value
                )
            }
        }
    }

    /** Called on slider release (onValueChangeFinished), not per drag tick --
     *  avoids flooding the root shell with a write per pixel of drag. */
    fun setGain(level: Int) {
        viewModelScope.launch {
            preferencesRepository.setAudioLedGain(level)
            if (isEnabled.value) {
                hardwareRepository.setAudioGain(level)
            }
        }
    }
}
