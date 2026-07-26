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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class LogLevel { INFO, SUCCESS, WARNING, ERROR }

enum class RootState { IDLE, CHECKING, GRANTED, DENIED }

/**
 * Priority tiers for arbitrating concurrent LED triggers, declared in
 * ascending priority order (ordinal used for comparison). Without this,
 * a notification blip, a battery pulse, and audio-reactive mode all write
 * to the same sysfs node independently with nothing preventing them from
 * stomping each other mid-effect.
 *
 * MUSIC is the only tier without its own repeating timer, so it's the only
 * one that needs an explicit "resume" after being preempted -- battery
 * pulse trains re-render on their own schedule regardless, and
 * notifications are one-shot by nature.
 */
enum class LedOwner { MUSIC, BATTERY, NOTIFICATION, MANUAL }

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

    private val ownerLock = Any()
    private var currentOwner: LedOwner? = null
    private var savedMusicMode: AudioLedMode? = null
    private var savedMusicGain: Int = AudioGain.DEFAULT_LEVEL

    /** True if [owner] may proceed right now -- a lower-priority caller is
     *  blocked while a higher-priority effect is in flight (e.g. a stray
     *  battery pulse can't stomp a notification blip that's mid-animation). */
    private fun tryAcquire(owner: LedOwner): Boolean = synchronized(ownerLock) {
        val current = currentOwner
        if (current == null || owner.ordinal >= current.ordinal) {
            currentOwner = owner
            true
        } else {
            log("[${dateFormat.format(Date())}] ${owner.name} blocked -- $current active", LogLevel.WARNING)
            false
        }
    }

    /** Call when a transient (BATTERY/NOTIFICATION/MANUAL) effect fully
     *  finishes. Hands control back to continuous music mode if it was
     *  preempted and is still supposed to be playing. */
    suspend fun releaseAndRestore(owner: LedOwner) {
        var resumeMode: AudioLedMode? = null
        var resumeGain = AudioGain.DEFAULT_LEVEL
        synchronized(ownerLock) {
            if (currentOwner == owner) currentOwner = null
            if (currentOwner == null) {
                resumeMode = savedMusicMode
                resumeGain = savedMusicGain
            }
        }
        resumeMode?.let { setAudioReactiveMode(it, resumeGain) }
    }

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _rootState = MutableStateFlow(RootState.IDLE)
    val rootState: StateFlow<RootState> = _rootState.asStateFlow()

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
        } else {
            log("[${dateFormat.format(Date())}] Hardware init failed", LogLevel.ERROR)
            _isReady.value = false
            _rootState.value = RootState.DENIED
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
        val ok = rootExecutor.runSuWithRetry(cmd, maxRetries = 1, delayMs = 150L)
        if (ok) {
            isLightActive = true
        }
        return ok
    }

    suspend fun setAudioReactiveMode(mode: AudioLedMode, gainLevel: Int = AudioGain.DEFAULT_LEVEL): Boolean {
        synchronized(ownerLock) {
            savedMusicMode = mode
            savedMusicGain = gainLevel
        }
        if (!tryAcquire(LedOwner.MUSIC)) return false
        ensureLedEnabled()
        val cfg = activeConfig
        val cmd = "echo -n '00 00 00 00 00 00' > ${cfg.lbCmd}; " +
                "echo -n '${mode.hex}' > ${cfg.lbCmd}"
        val ok = rootExecutor.runSuWithRetry(cmd, maxRetries = 1, delayMs = 150L)
        log("[${dateFormat.format(Date())}] AUDIO_MODE[${mode.label}] -> ok=$ok hex='${mode.hex}'", if (ok) LogLevel.SUCCESS else LogLevel.ERROR)
        if (ok) {
            // Stock service always follows the trigger with a gain command
            // ~100ms later -- without it the chip is left at whatever its
            // internal default gain is, which reads as "too sensitive".
            delay(100L)
            setAudioGain(gainLevel)
        }
        return ok
    }

    suspend fun setAudioGain(level: Int): Boolean {
        val cfg = activeConfig
        val cmd = AudioGain.command(level)
        val ok = rootExecutor.runSuWithRetry("echo -n '$cmd' > ${cfg.lbCmd}", maxRetries = 1, delayMs = 150L)
        log("[${dateFormat.format(Date())}] AUDIO_GAIN[level=$level] -> ok=$ok hex='$cmd'", if (ok) LogLevel.SUCCESS else LogLevel.ERROR)
        return ok
    }

    suspend fun turnOffAudioReactive(): Boolean {
        synchronized(ownerLock) {
            savedMusicMode = null
            if (currentOwner == LedOwner.MUSIC) currentOwner = null
        }
        val cfg = activeConfig
        val ok = rootExecutor.runSuWithRetry("echo -n '${cfg.turnOffHex}' > ${cfg.lbCmd}", maxRetries = 1, delayMs = 150L)
        log("[${dateFormat.format(Date())}] AUDIO_MODE[OFF] -> ok=$ok", if (ok) LogLevel.INFO else LogLevel.ERROR)
        return ok
    }

    suspend fun sendRawHex(hex: String, tag: String = "RAW", owner: LedOwner = LedOwner.MANUAL): Boolean {
        if (!masterEnabled) return false
        if (!tryAcquire(owner)) return false
        ensureLedEnabled()
        val cfg = activeConfig
        val ok = rootExecutor.runSuWithRetry("echo -n '$hex' > ${cfg.lbCmd}", maxRetries = 1, delayMs = 150L)
        log("[${dateFormat.format(Date())}] $tag -> ok=$ok hex='$hex'", if (ok) LogLevel.SUCCESS else LogLevel.ERROR)
        return ok
    }

    suspend fun fireEffect(hex: String, tag: String = "FIRE", owner: LedOwner = LedOwner.NOTIFICATION): Boolean {
        if (!masterEnabled) return false
        if (!tryAcquire(owner)) return false
        ensureLedEnabled()
        val cfg = activeConfig
        val ok = rootExecutor.runSuWithRetry("echo -n '$hex' > ${cfg.lbCmd}", maxRetries = 1, delayMs = 150L)
        log("[${dateFormat.format(Date())}] $tag -> ok=$ok hex='$hex'", if (ok) LogLevel.SUCCESS else LogLevel.ERROR)
        return ok
    }

    suspend fun fireStop(hex: String = activeConfig.turnOffHex, tag: String = "STOP", owner: LedOwner = LedOwner.NOTIFICATION): Boolean {
        val cfg = activeConfig
        val ok = rootExecutor.runSuWithRetry("echo -n '$hex' > ${cfg.lbCmd}", maxRetries = 1, delayMs = 150L)
        log("[${dateFormat.format(Date())}] $tag -> ok=$ok hex='$hex'", if (ok) LogLevel.INFO else LogLevel.ERROR)
        releaseAndRestore(owner)
        return ok
    }

    /** Incoming call started ringing -- takes NOTIFICATION priority so it
     *  interrupts ambient music/battery effects, same as stock. No timer:
     *  stays lit until [endPhoneCall] is called on answer or hangup. */
    suspend fun ringPhoneCall(): Boolean =
        fireEffect(activeConfig.phoneCallHex, "CALL_RINGING", owner = LedOwner.NOTIFICATION)

    /** Call answered or ended -- stops the ring flash and hands control
     *  back to music mode if it was playing before the call interrupted it. */
    suspend fun endPhoneCall(): Boolean =
        fireStop(activeConfig.turnOffHex, "CALL_END", owner = LedOwner.NOTIFICATION)

    suspend fun turnOffAll(owner: LedOwner = LedOwner.BATTERY): Boolean {
        val cfg = activeConfig
        val ok = rootExecutor.runSuWithRetry("echo -n '${cfg.turnOffHex}' > ${cfg.lbCmd}", maxRetries = 1, delayMs = 150L)
        isLightActive = false
        log("[${dateFormat.format(Date())}] Soft turn-off executed", LogLevel.INFO)
        releaseAndRestore(owner)
        return ok
    }

    suspend fun emergencyKillAndRevive(offTimeMs: Long = 250L): Boolean {
        log("[${dateFormat.format(Date())}] Emergency Stop — killing LED service…", LogLevel.WARNING)
        synchronized(ownerLock) {
            currentOwner = null
            savedMusicMode = null
        }
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

    suspend fun recheckRootStatus(): Boolean {
        val ok = rootExecutor.isRooted(forceRecheck = true)
        _rootState.value = if (ok) RootState.GRANTED else RootState.DENIED
        return ok
    }
}
