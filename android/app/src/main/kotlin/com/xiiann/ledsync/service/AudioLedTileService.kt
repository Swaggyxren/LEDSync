package com.xiiann.ledsync.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.xiiann.ledsync.data.repository.HardwareRepository
import com.xiiann.ledsync.data.repository.PreferencesRepository
import com.xiiann.ledsync.domain.model.AudioLedMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AudioLedTileService : TileService() {

    @Inject
    lateinit var hardwareRepository: HardwareRepository

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        serviceScope.launch {
            val currentlyEnabled = preferencesRepository.audioLedEnabled.first()
            val newState = !currentlyEnabled

            preferencesRepository.setAudioLedEnabled(newState)
            if (newState) {
                val isDynamic = preferencesRepository.audioLedDynamic.first()
                hardwareRepository.setAudioReactiveMode(
                    if (isDynamic) AudioLedMode.DYNAMIC else AudioLedMode.STATIC
                )
            } else {
                hardwareRepository.turnOffAudioReactive()
            }
            updateTileState()
        }
    }

    private fun updateTileState() {
        serviceScope.launch {
            val qsTile = qsTile ?: return@launch
            val enabled = preferencesRepository.audioLedEnabled.first()
            qsTile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            qsTile.label = "Audio LED"
            qsTile.subtitle = if (enabled) "Active" else "Off"
            qsTile.updateTile()
        }
    }
}
