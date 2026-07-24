package com.xiiann.ledsync.presentation.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiiann.ledsync.data.repository.BatteryConfig
import com.xiiann.ledsync.data.repository.HardwareRepository
import com.xiiann.ledsync.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

data class LiveBatteryState(
    val level: Int = 0,
    val isCharging: Boolean = false
)

@HiltViewModel
class BatteryConfigViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val hardwareRepository: HardwareRepository
) : ViewModel() {

    private val _workingConfig = MutableStateFlow(BatteryConfig())
    val workingConfig: StateFlow<BatteryConfig> = _workingConfig.asStateFlow()

    private val _savedConfig = MutableStateFlow(BatteryConfig())
    val savedConfig: StateFlow<BatteryConfig> = _savedConfig.asStateFlow()

    private val _isDirty = MutableStateFlow(false)
    val isDirty: StateFlow<Boolean> = _isDirty.asStateFlow()

    private val _liveBattery = MutableStateFlow(LiveBatteryState())
    val liveBattery: StateFlow<LiveBatteryState> = _liveBattery.asStateFlow()

    init {
        loadConfig()
        readBattery()
    }

    private fun loadConfig() {
        viewModelScope.launch {
            val saved = preferencesRepository.batteryConfig.first()
            _savedConfig.value = saved
            _workingConfig.value = saved
        }
    }

    fun readBattery() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = context.registerReceiver(null, filter)
        if (intent != null) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 0
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            _liveBattery.value = LiveBatteryState(level = pct, isCharging = charging)
        }
    }

    fun updateLowThreshold(newValue: Int) {
        val current = _workingConfig.value
        val validLow = ((newValue / 2f).roundToInt() * 2).coerceIn(20, 30)
        var validCrit = current.criticalThreshold
        if (validCrit > validLow) {
            validCrit = validLow
        }
        _workingConfig.value = current.copy(lowThreshold = validLow, criticalThreshold = validCrit)
        _isDirty.value = _workingConfig.value != _savedConfig.value
    }

    fun updateCriticalThreshold(newValue: Int) {
        val current = _workingConfig.value
        val validCrit = ((newValue / 2f).roundToInt() * 2).coerceIn(4, 20)
        var validLow = current.lowThreshold
        if (validCrit > validLow) {
            validLow = validCrit
        }
        _workingConfig.value = current.copy(criticalThreshold = validCrit, lowThreshold = validLow)
        _isDirty.value = _workingConfig.value != _savedConfig.value
    }

    fun updateFullThreshold(newValue: Int) {
        val current = _workingConfig.value
        val validFull = ((newValue / 5f).roundToInt() * 5).coerceIn(70, 100)
        _workingConfig.value = current.copy(fullThreshold = validFull)
        _isDirty.value = _workingConfig.value != _savedConfig.value
    }

    fun updateLowEffect(effectName: String?) {
        val current = _workingConfig.value
        _workingConfig.value = current.copy(lowEffectName = effectName ?: "Rise")
        _isDirty.value = _workingConfig.value != _savedConfig.value
    }

    fun updateCriticalEffect(effectName: String?) {
        val current = _workingConfig.value
        _workingConfig.value = current.copy(criticalEffectName = effectName ?: "Lightning")
        _isDirty.value = _workingConfig.value != _savedConfig.value
    }

    fun updateFullEffect(effectName: String?) {
        val current = _workingConfig.value
        _workingConfig.value = current.copy(fullEffectName = effectName ?: "Pureness")
        _isDirty.value = _workingConfig.value != _savedConfig.value
    }

    fun saveConfig() {
        viewModelScope.launch {
            preferencesRepository.saveBatteryConfig(_workingConfig.value)
            _savedConfig.value = _workingConfig.value
            _isDirty.value = false
        }
    }

    fun getDeviceConfig() = hardwareRepository.getConfig()
}
