package com.xiiann.ledsync.presentation.audioled

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiiann.ledsync.data.repository.HardwareRepository
import com.xiiann.ledsync.data.repository.PreferencesRepository
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

    val isDynamic: StateFlow<Boolean> = preferencesRepository.audioLedDynamic
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleMode(dynamic: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAudioLedDynamic(dynamic)
            hardwareRepository.setAudioReactiveMode(
                if (dynamic) AudioLedMode.DYNAMIC else AudioLedMode.STATIC
            )
        }
    }
}
