package com.xiiann.ledsync.presentation.home

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiiann.ledsync.data.executor.IRootExecutor
import com.xiiann.ledsync.data.repository.HardwareRepository
import com.xiiann.ledsync.data.repository.PreferencesRepository
import com.xiiann.ledsync.domain.model.DeviceConfig
import com.xiiann.ledsync.domain.model.LH8nConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class DeviceInfoState(
    val model: String = "TECNO POVA 5 PRO 5G (LH8n)",
    val androidVersion: String = "Android ${Build.VERSION.RELEASE}",
    val kernelVersion: String = "",
    val isRooted: Boolean = false
)

data class LiveStatsState(
    val cpuPct: Float = 0.0f,
    val ramUsedMb: Int = 0,
    val ramTotalMb: Int = 0,
    val batteryLevel: Int = 0,
    val isCharging: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rootExecutor: IRootExecutor,
    private val hardwareRepository: HardwareRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _deviceInfo = MutableStateFlow(DeviceInfoState())
    val deviceInfo: StateFlow<DeviceInfoState> = _deviceInfo.asStateFlow()

    private val _liveStats = MutableStateFlow(LiveStatsState())
    val liveStats: StateFlow<LiveStatsState> = _liveStats.asStateFlow()

    val availableConfigs: List<DeviceConfig> = listOf(LH8nConfig())

    val selectedDeviceName = preferencesRepository.selectedDeviceConfig
    val tooltipShown = preferencesRepository.tooltipShown

    private var prevIdle = 0L
    private var prevTotal = 0L

    init {
        loadDeviceInfo()
        startStatsPolling()
    }

    private fun loadDeviceInfo() {
        viewModelScope.launch {
            val rooted = rootExecutor.isRooted()
            val kernel = rootExecutor.runSuOutput("uname -r")
            val release = Build.VERSION.RELEASE
            val label = when {
                release.contains("13") -> "Android 13 (Tiramisu)"
                release.contains("14") -> "Android 14 (Upside Down Cake)"
                release.contains("15") -> "Android 15 (Vanilla Ice Cream)"
                release.contains("16") -> "Android 16 (Baklava)"
                else -> "Android $release"
            }

            _deviceInfo.value = DeviceInfoState(
                model = hardwareRepository.getConfig().deviceName,
                androidVersion = label,
                kernelVersion = kernel,
                isRooted = rooted
            )
        }
    }

    private fun startStatsPolling() {
        viewModelScope.launch {
            while (true) {
                readStats()
                delay(2000L)
            }
        }
    }

    private suspend fun readStats() = withContext(Dispatchers.IO) {
        readBatteryInfo()
        val raw = rootExecutor.runSuOutput("cat /proc/stat; echo \"---\"; cat /proc/meminfo")
        if (raw.isNotBlank() && raw.contains("---")) {
            val parts = raw.split("---\n")
            if (parts.size >= 2) {
                parseCpu(parts[0])
                parseRam(parts[1])
            }
        }
    }

    private fun readBatteryInfo() {
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = context.registerReceiver(null, filter)
            if (batteryStatus != null) {
                val level: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val pct = if (level >= 0 && scale > 0) ((level * 100f) / scale.toFloat()).toInt() else 0

                val status: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                _liveStats.value = _liveStats.value.copy(
                    batteryLevel = pct,
                    isCharging = isCharging
                )
            }
        } catch (_: Exception) { }
    }

    private fun parseCpu(statRaw: String) {
        val line = statRaw.lineSequence().firstOrNull { it.startsWith("cpu ") } ?: return
        val nums = line.split("\\s+".toRegex()).drop(1).filter { it.isNotEmpty() }.mapNotNull { it.toLongOrNull() }
        if (nums.size < 4) return

        val idle = nums[3] + if (nums.size > 4) nums[4] else 0L
        val total = nums.sum()

        val dI = idle - prevIdle
        val dT = total - prevTotal
        prevIdle = idle
        prevTotal = total

        val pct = if (dT > 0) (1.0f - (dI.toFloat() / dT.toFloat())).coerceIn(0.0f, 1.0f) else 0.0f
        _liveStats.value = _liveStats.value.copy(cpuPct = pct)
    }

    private fun parseRam(meminfoRaw: String) {
        var total = 0
        var avail = 0
        for (line in meminfoRaw.lineSequence()) {
            val parts = line.split("\\s+".toRegex())
            if (parts.size < 2) continue
            val v = parts[1].toIntOrNull() ?: 0
            if (line.startsWith("MemTotal:")) total = v
            if (line.startsWith("MemAvailable:")) avail = v
        }
        if (total > 0) {
            _liveStats.value = _liveStats.value.copy(
                ramUsedMb = (total - avail) / 1024,
                ramTotalMb = total / 1024
            )
        }
    }

    fun setSelectedDevice(config: DeviceConfig) {
        viewModelScope.launch {
            hardwareRepository.setConfig(config)
            preferencesRepository.setSelectedDevice(config.deviceName)
            _deviceInfo.value = _deviceInfo.value.copy(model = config.deviceName)
        }
    }

    fun markTooltipShown() {
        viewModelScope.launch {
            preferencesRepository.setTooltipShown(true)
        }
    }
}
