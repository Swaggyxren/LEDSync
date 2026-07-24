package com.xiiann.ledsync.data.repository

import com.xiiann.ledsync.data.executor.IRootExecutor
import com.xiiann.ledsync.domain.model.AudioLedMode
import com.xiiann.ledsync.domain.model.DeviceConfig
import com.xiiann.ledsync.domain.model.LH8nConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class LogLevel { INFO, SUCCESS, WARNING, ERROR }

data class LogEntry(
    val timestamp: String,
    val message: String,
    val level: LogLevel
)

@Singleton
class HardwareRepository @Inject constructor(
    private val rootExecutor: IRootExecutor
) {
    private var activeConfig: DeviceConfig = LH8nConfig()
    private var isLightActive = false

    var masterEnabled: Boolean = true

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _actionLogs = MutableStateFlow<List<LogEntry>>(emptyList())
    val actionLogs: StateFlow<List<LogEntry>> = _actionLogs.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun setConfig(config: DeviceConfig) {
        activeConfig = config
        isLightActive = false
    }

    fun getConfig(): DeviceConfig = activeConfig

    fun log(msg: String, level: LogLevel = LogLevel.INFO) {
        val entry = LogEntry(
            timestamp = dateFormat.format(Date()),
            message = msg,
            level = level
        )
        val current = _actionLogs.value.toMutableList()
        current.add(entry)
        if (current.size > 500) current.removeAt(0)
        _actionLogs.value = current
    }

    fun clearLogs() {
        _actionLogs.value = emptyList()
    }

    suspend fun initializeHardware(force: Boolean = true): Boolean {
        log("[${dateFormat.format(Date())}] Initializing hardware components...")
        val rooted = rootExecutor.isRooted()
        if (!rooted) {
            log("[${dateFormat.format(Date())}] CRITICAL: No Root Access.", LogLevel.ERROR)
            _isReady.value = false
            return false
        }
        val ok = ensureLedEnabled(force = force)
        if (ok) {
            log("[${dateFormat.format(Date())}] Hardware initialized via sysfs", LogLevel.SUCCESS)
            log("[${dateFormat.format(Date())}] LED Controller: Active", LogLevel.SUCCESS)
            log("[${dateFormat.format(Date())}] System Ready. Awaiting effect selection.", LogLevel.SUCCESS)
            _isReady.value = true
        } else {
            log("[${dateFormat.format(Date())}] Hardware init failed", LogLevel.ERROR)
            _isReady.value = false
        }
        return ok
    }

    suspend fun ensureLedEnabled(force: Boolean = false): Boolean {
        if (isLightActive && !force) return true
        val cfg = activeConfig
        val cmd = "echo 1 > ${cfg.awPath}/hwen; " +
                "echo c > ${cfg.awPath}/imax 2>/dev/null || true; " +
                "echo 255 > ${cfg.awPath}/brightness; " +
                "echo none > ${cfg.awPath}/trigger 2>/dev/null || true; " +
                "echo -n '00 00 00 00 00 00' > ${cfg.lbCmd}"
        val ok = rootExecutor.runSu(cmd)
        if (ok) {
            isLightActive = true
        }
        return ok
    }

    suspend fun setAudioReactiveMode(mode: AudioLedMode): Boolean {
        ensureLedEnabled()
        val cfg = activeConfig
        val cmd = "echo -n '00 00 00 00 00 00' > ${cfg.lbCmd}; " +
                "echo -n '${mode.hex}' > ${cfg.lbCmd}"
        val ok = rootExecutor.runSu(cmd)
        log("[${dateFormat.format(Date())}] AUDIO_MODE[${mode.label}] -> ok=$ok hex='${mode.hex}'", if (ok) LogLevel.SUCCESS else LogLevel.ERROR)
        return ok
    }

    suspend fun sendRawHex(hex: String, tag: String = "RAW"): Boolean {
        if (!masterEnabled) return false
        ensureLedEnabled()
        val cfg = activeConfig
        val ok = rootExecutor.runSu("echo -n '$hex' > ${cfg.lbCmd}")
        log("[${dateFormat.format(Date())}] $tag -> ok=$ok hex='$hex'", if (ok) LogLevel.SUCCESS else LogLevel.ERROR)
        return ok
    }

    suspend fun fireEffect(hex: String, tag: String = "FIRE"): Boolean {
        if (!masterEnabled) return false
        ensureLedEnabled()
        val cfg = activeConfig
        val ok = rootExecutor.runSu("echo -n '$hex' > ${cfg.lbCmd}")
        log("[${dateFormat.format(Date())}] $tag -> ok=$ok hex='$hex'", if (ok) LogLevel.SUCCESS else LogLevel.ERROR)
        return ok
    }

    suspend fun fireStop(hex: String = activeConfig.turnOffHex, tag: String = "STOP"): Boolean {
        val cfg = activeConfig
        val ok = rootExecutor.runSu("echo -n '$hex' > ${cfg.lbCmd}")
        log("[${dateFormat.format(Date())}] $tag -> ok=$ok hex='$hex'", if (ok) LogLevel.INFO else LogLevel.ERROR)
        return ok
    }

    suspend fun turnOffAll(): Boolean {
        val cfg = activeConfig
        val ok = rootExecutor.runSu("echo -n '${cfg.turnOffHex}' > ${cfg.lbCmd}")
        isLightActive = false
        log("[${dateFormat.format(Date())}] Soft turn-off executed", LogLevel.INFO)
        return ok
    }

    suspend fun emergencyKillAndRevive(offTimeMs: Long = 250L): Boolean {
        log("[${dateFormat.format(Date())}] Emergency Stop — killing LED service…", LogLevel.WARNING)
        _isReady.value = false
        val cfg = activeConfig
        val killCmd = "echo -n '${cfg.turnOffHex}' > ${cfg.lbCmd}; " +
                "echo 0 > ${cfg.awPath}/brightness; " +
                "echo 0 > ${cfg.awPath}/hwen"
        rootExecutor.runSu(killCmd)
        isLightActive = false
        delay(offTimeMs)
        log("[${dateFormat.format(Date())}] Service restarted successfully.", LogLevel.SUCCESS)
        log("[${dateFormat.format(Date())}] Hardware service restarted. Re-initializing…", LogLevel.INFO)
        return initializeHardware(force = true)
    }

    fun markLedInactive() {
        isLightActive = false
    }
}
