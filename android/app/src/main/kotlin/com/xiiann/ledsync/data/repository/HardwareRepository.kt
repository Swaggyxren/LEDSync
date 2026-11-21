package com.xiiann.ledsync.data.repository

import com.xiiann.ledsync.data.executor.IRootExecutor
import com.xiiann.ledsync.domain.model.AudioGain
import com.xiiann.ledsync.domain.model.AudioLedMode
import com.xiiann.ledsync.domain.model.DeviceConfig
import com.xiiann.ledsync.domain.model.LH8nConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class LogLevel { INFO, SUCCESS, WARNING, ERROR }

enum class RootState { IDLE, CHECKING, GRANTED, DENIED }

enum class LedOwner(val priority: Int) {
    MUSIC(10),
    BATTERY(20),
    NOTIFICATION(30),
    CALL(35),
    MANUAL(40)
}

data class LogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: String,
    val message: String,
    val level: LogLevel
)

@Singleton
class HardwareRepository @Inject constructor(
    private val rootExecutor: IRootExecutor,
    private val preferencesRepository: PreferencesRepository
) {
    private var activeConfig: DeviceConfig = LH8nConfig()
    private var isLightActive = false

    private val _masterEnabledFlow = MutableStateFlow(true)
    val masterEnabledFlow: StateFlow<Boolean> = _masterEnabledFlow.asStateFlow()

    var masterEnabled: Boolean
        get() = _masterEnabledFlow.value
        set(value) {
            _masterEnabledFlow.value = value
        }

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _rootState = MutableStateFlow(RootState.IDLE)
    val rootState: StateFlow<RootState> = _rootState.asStateFlow()

    private val _actionLogs = MutableStateFlow<List<LogEntry>>(emptyList())
    val actionLogs: StateFlow<List<LogEntry>> = _actionLogs.asStateFlow()

    private val _isSuppressed = MutableStateFlow(false)
    val isSuppressed: StateFlow<Boolean> = _isSuppressed.asStateFlow()

    private val _suppressionReason = MutableStateFlow<String?>(null)
    val suppressionReason: StateFlow<String?> = _suppressionReason.asStateFlow()

    private val ownerLock = Any()
    private var currentOwner: LedOwner? = null
    private var savedMusicMode: AudioLedMode? = null
    private var savedMusicGain: Int = AudioGain.DEFAULT_LEVEL

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun setConfig(config: DeviceConfig) {
        activeConfig = config
        isLightActive = false
    }

    fun getConfig(): DeviceConfig = activeConfig

    fun tryAcquire(owner: LedOwner): Boolean {
        synchronized(ownerLock) {
            val current = currentOwner
            if (current == null || owner.priority >= current.priority) {
                currentOwner = owner
                return true
            }
            log("[${dateFormat.format(Date())}] SUPPRESSED[$owner]: preempted by ${current.name}", LogLevel.INFO)
            return false
        }
    }

    private suspend fun getSavedAudioLedSettings(): Triple<Boolean, AudioLedMode, Int> {
        val enabled = preferencesRepository.audioLedEnabled.first()
        val dynamic = preferencesRepository.audioLedDynamic.first()
        val gain = preferencesRepository.audioLedGain.first()
        val mode = if (dynamic) AudioLedMode.DYNAMIC else AudioLedMode.STATIC
        return Triple(enabled, mode, gain)
    }

    suspend fun releaseAndRestore(owner: LedOwner, restoreDelayMs: Long = 500L) {
        val (shouldRestore, musicMode, musicGain) = synchronized(ownerLock) {
            if (currentOwner == owner) {
                currentOwner = null
            }
            val mode = savedMusicMode
            val gain = savedMusicGain
            Triple(currentOwner == null, mode, gain)
        }
        if (shouldRestore && masterEnabled && !_isSuppressed.value) {
            val (enabled, prefMode, prefGain) = getSavedAudioLedSettings()
            if (enabled) {
                val modeToUse = musicMode ?: prefMode
                val gainToUse = if (musicGain > 0) musicGain else prefGain
                if (restoreDelayMs > 0) {
                    delay(restoreDelayMs)
                }
                setAudioReactiveMode(modeToUse, gainToUse)
            }
        }
    }

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
        _rootState.value = RootState.CHECKING
        val rooted = rootExecutor.isRooted(forceRecheck = force)
        if (!rooted) {
            log("[${dateFormat.format(Date())}] CRITICAL: No Root Access.", LogLevel.ERROR)
            _isReady.value = false
            _rootState.value = RootState.DENIED
            return false
        }
        val ok = ensureLedEnabled(force = force)
        if (ok) {
            log("[${dateFormat.format(Date())}] Hardware initialized via sysfs", LogLevel.SUCCESS)
            log("[${dateFormat.format(Date())}] LED Controller: Active", LogLevel.SUCCESS)
            log("[${dateFormat.format(Date())}] System Ready. Awaiting effect selection.", LogLevel.SUCCESS)
            _isReady.value = true
            _rootState.value = RootState.GRANTED
            releaseAndRestore(LedOwner.MANUAL, restoreDelayMs = 200L)
        } else {
            log("[${dateFormat.format(Date())}] Hardware init failed", LogLevel.ERROR)
            _isReady.value = false
            _rootState.value = RootState.DENIED
        }
        return ok
    }

    /**
     * Primes the AW22xxx hardware once per active window.
     * Kernel driver `leds-aw22xxx.ko`:
     *  - `hwen=1` calls `aw22xxx_hw_reset()`
     *  - `imax=c` (`0x0c`) is the maximum valid ceiling before kernel `BUG_ON`
     *  - `00 00 00 00 00 00` sets `soft_show_en = 1` and queues `aw22xxx_led_lamp_init_show`
     * Intentionally avoids writing `brightness` (which queues competing `aw22xxx_brightness_work` 0x82).
     */
    suspend fun ensureLedEnabled(force: Boolean = false): Boolean {
        if (isLightActive && !force) return true
        val cfg = activeConfig
        val cmd = "echo 1 > ${cfg.awPath}/hwen; " +
            "echo c > ${cfg.awPath}/imax 2>/dev/null || true; " +
            "echo none > ${cfg.awPath}/trigger 2>/dev/null || true; " +
            "echo -n '00 00 00 00 00 00' > ${cfg.lbCmd}"
        val ok = rootExecutor.runSuWithRetry(cmd, maxRetries = 1, delayMs = 150L)
        if (ok) {
            isLightActive = true
        }
        return ok
    }

    /**
     * Sends a single effect command when already active.
     * `aw22xxx_task_work` stops the previous effect in hardware before loading the new one,
     * so we avoid the `00 01 -> 00 00` prefix and skip redundant 40ms kernel `msleep(20)` stalls.
     */
    private suspend fun writeEffectHex(hex: String): Boolean {
        ensureLedEnabled()
        return rootExecutor.runSuWithRetry(
            "echo -n '$hex' > ${activeConfig.lbCmd}",
            maxRetries = 1,
            delayMs = 150L
        )
    }

    suspend fun setMasterEnabled(enabled: Boolean): Boolean {
        masterEnabled = enabled
        if (!enabled) {
            val ok = turnOffAll()
            log("[${dateFormat.format(Date())}] Master LED service paused", LogLevel.WARNING)
            return ok
        }
        log("[${dateFormat.format(Date())}] Master LED service resumed", LogLevel.SUCCESS)
        val ok = ensureLedEnabled(force = true)
        releaseAndRestore(LedOwner.MANUAL, restoreDelayMs = 150L)
        return ok
    }

    suspend fun updateSuppression(suppressed: Boolean, reason: String? = null) {
        val prev = _isSuppressed.value
        _isSuppressed.value = suppressed
        _suppressionReason.value = if (suppressed) reason else null
        if (suppressed && !prev) {
            log("[${dateFormat.format(Date())}] Guard active (${reason ?: "suppressed"}) — pausing LED", LogLevel.INFO)
            turnOffAll()
        } else if (!suppressed && prev) {
            log("[${dateFormat.format(Date())}] Guard cleared — restoring LED state", LogLevel.INFO)
            releaseAndRestore(LedOwner.MANUAL, restoreDelayMs = 150L)
        }
    }

    suspend fun setAudioReactiveMode(mode: AudioLedMode, gainLevel: Int = AudioGain.DEFAULT_LEVEL): Boolean {
        synchronized(ownerLock) {
            savedMusicMode = mode
            savedMusicGain = gainLevel
        }
        preferencesRepository.setAudioLedEnabled(true)
        preferencesRepository.setAudioLedDynamic(mode == AudioLedMode.DYNAMIC)
        preferencesRepository.setAudioLedGain(gainLevel)

        if (!masterEnabled || _isSuppressed.value) return false
        if (!tryAcquire(LedOwner.MUSIC)) return false
        val ok = writeEffectHex(mode.hex)
        log(
            "[${dateFormat.format(Date())}] AUDIO_MODE[${mode.label}] -> ok=$ok hex='${mode.hex}'",
            if (ok) LogLevel.SUCCESS else LogLevel.ERROR
        )
        if (ok) {
            delay(100L)
            setAudioGain(gainLevel)
        }
        return ok
    }

    suspend fun activateAutoMusicSync(
        mode: AudioLedMode,
        gainHex: String? = null,
        gainLevel: Int = AudioGain.DEFAULT_LEVEL
    ): Boolean {
        if (!masterEnabled || _isSuppressed.value) return false
        if (!tryAcquire(LedOwner.MUSIC)) return false
        val ok = writeEffectHex(mode.hex)
        log(
            "[${dateFormat.format(Date())}] AUTO_MUSIC[${mode.label}] -> ok=$ok hex='${mode.hex}'",
            if (ok) LogLevel.SUCCESS else LogLevel.ERROR
        )
        if (ok) {
            delay(100L)
            val gainCmd = if (gainHex != null) {
                AudioGain.commandFromHex(gainHex)
            } else {
                AudioGain.command(gainLevel)
            }
            rootExecutor.runSuWithRetry("echo -n '$gainCmd' > ${activeConfig.lbCmd}", maxRetries = 1, delayMs = 150L)
        }
        return ok
    }

    suspend fun setAudioGain(level: Int): Boolean {
        synchronized(ownerLock) {
            savedMusicGain = level
        }
        preferencesRepository.setAudioLedGain(level)
        val cmd = AudioGain.command(level)
        val ok = rootExecutor.runSuWithRetry("echo -n '$cmd' > ${activeConfig.lbCmd}", maxRetries = 1, delayMs = 150L)
        log(
            "[${dateFormat.format(Date())}] AUDIO_GAIN[level=$level] -> ok=$ok hex='$cmd'",
            if (ok) LogLevel.SUCCESS else LogLevel.ERROR
        )
        return ok
    }

    suspend fun setAudioGainHex(hexByte: String): Boolean {
        val cmd = AudioGain.commandFromHex(hexByte)
        val ok = rootExecutor.runSuWithRetry("echo -n '$cmd' > ${activeConfig.lbCmd}", maxRetries = 1, delayMs = 150L)
        log(
            "[${dateFormat.format(Date())}] AUDIO_GAIN_VOL[hex=$hexByte] -> ok=$ok cmd='$cmd'",
            if (ok) LogLevel.INFO else LogLevel.ERROR
        )
        return ok
    }

    suspend fun turnOffAudioReactive(): Boolean {
        synchronized(ownerLock) {
            savedMusicMode = null
            if (currentOwner == LedOwner.MUSIC) currentOwner = null
        }
        preferencesRepository.setAudioLedEnabled(false)
        val ok = rootExecutor.runSuWithRetry(
            "echo -n '${activeConfig.turnOffHex}' > ${activeConfig.lbCmd}",
            maxRetries = 1,
            delayMs = 150L
        )
        if (ok) isLightActive = false
        log("[${dateFormat.format(Date())}] AUDIO_MODE[OFF] -> ok=$ok", if (ok) LogLevel.INFO else LogLevel.ERROR)
        return ok
    }

    suspend fun sendRawHex(hex: String, tag: String = "RAW", owner: LedOwner = LedOwner.MANUAL): Boolean {
        if (!masterEnabled) return false
        if (owner != LedOwner.MANUAL && _isSuppressed.value) return false
        if (!tryAcquire(owner)) return false
        val ok = writeEffectHex(hex)
        log("[${dateFormat.format(Date())}] $tag -> ok=$ok hex='$hex'", if (ok) LogLevel.SUCCESS else LogLevel.ERROR)
        return ok
    }

    suspend fun fireEffect(hex: String, tag: String = "FIRE", owner: LedOwner = LedOwner.NOTIFICATION): Boolean {
        if (!masterEnabled || _isSuppressed.value) return false
        if (!tryAcquire(owner)) return false
        val ok = writeEffectHex(hex)
        log("[${dateFormat.format(Date())}] $tag -> ok=$ok hex='$hex'", if (ok) LogLevel.SUCCESS else LogLevel.ERROR)
        return ok
    }

    suspend fun fireStop(hex: String = activeConfig.turnOffHex, tag: String = "STOP", owner: LedOwner = LedOwner.NOTIFICATION): Boolean {
        val ok = rootExecutor.runSuWithRetry(
            "echo -n '$hex' > ${activeConfig.lbCmd}",
            maxRetries = 1,
            delayMs = 150L
        )
        if (ok && hex == activeConfig.turnOffHex) {
            isLightActive = false
        }
        log("[${dateFormat.format(Date())}] $tag -> ok=$ok hex='$hex'", if (ok) LogLevel.INFO else LogLevel.ERROR)
        releaseAndRestore(owner, restoreDelayMs = 500L)
        return ok
    }

    suspend fun ringPhoneCall(patternName: String = "Soft"): Boolean {
        val hex = activeConfig.hexForEffect(patternName) ?: activeConfig.phoneCallHex
        return fireEffect(hex, "CALL_RINGING[$patternName]", owner = LedOwner.CALL)
    }

    suspend fun endPhoneCall(): Boolean =
        fireStop(activeConfig.turnOffHex, "CALL_ENDED", owner = LedOwner.CALL)

    suspend fun turnOffAll(): Boolean {
        val cfg = activeConfig
        val ok = rootExecutor.runSuWithRetry(
            "echo -n '${cfg.turnOffHex}' > ${cfg.lbCmd}",
            maxRetries = 1,
            delayMs = 150L
        )
        isLightActive = false
        synchronized(ownerLock) {
            currentOwner = null
        }
        releaseAndRestore(LedOwner.MANUAL, restoreDelayMs = 500L)
        log("[${dateFormat.format(Date())}] Soft turn-off executed", LogLevel.INFO)
        return ok
    }

    suspend fun emergencyKillAndRevive(offTimeMs: Long = 250L): Boolean {
        log("[${dateFormat.format(Date())}] Emergency Stop — killing LED service…", LogLevel.WARNING)
        val cfg = activeConfig
        val killCmd = "echo -n '${cfg.turnOffHex}' > ${cfg.lbCmd}; " +
            "echo 0 > ${cfg.awPath}/brightness; " +
            "echo 0 > ${cfg.awPath}/hwen"
        rootExecutor.runSu(killCmd)
        isLightActive = false
        synchronized(ownerLock) {
            currentOwner = null
        }
        delay(offTimeMs)
        log("[${dateFormat.format(Date())}] Service restarted successfully.", LogLevel.SUCCESS)
        log("[${dateFormat.format(Date())}] Hardware service restarted. Re-initializing…", LogLevel.INFO)
        return initializeHardware(force = true)
    }

    fun markLedInactive() {
        isLightActive = false
    }

    suspend fun recheckRootStatus(): Boolean {
        val ok = rootExecutor.isRooted(forceRecheck = true)
        _rootState.value = if (ok) RootState.GRANTED else RootState.DENIED
        return ok
    }
}
