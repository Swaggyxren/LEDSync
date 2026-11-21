package com.xiiann.ledsync.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.xiiann.ledsync.R
import com.xiiann.ledsync.data.repository.BatteryConfig
import com.xiiann.ledsync.data.repository.HardwareRepository
import com.xiiann.ledsync.data.repository.LedOwner
import com.xiiann.ledsync.data.repository.PreferencesRepository
import com.xiiann.ledsync.domain.model.AudioGain
import com.xiiann.ledsync.domain.model.AudioLedMode
import com.xiiann.ledsync.domain.model.StockBehaviorSettings
import com.xiiann.ledsync.domain.model.StockGuardPolicy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class LedCoreService : NotificationListenerService() {

    @Inject lateinit var hardwareRepository: HardwareRepository
    @Inject lateinit var preferencesRepository: PreferencesRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var powerManager: PowerManager
    private lateinit var notifManager: NotificationManager

    private val CHANNEL_ID = "ledsync_service"
    private val FG_NOTIF_ID = 101

    private val SEQUENCE_COOLDOWN_MS = 6000L
    private val DOUBLE_FIRE_DELAY_MS = 1500L
    private val LOOP_AUTO_STOP_MS = 5000L

    private val lastTriggerPerPkg = HashMap<String, Long>()
    private val activeLoopingPkgs = mutableSetOf<String>()

    private var receiversRegistered = false

    private var telephonyManager: TelephonyManager? = null
    private var phoneStateListener: PhoneStateListener? = null

    private var cameraManager: CameraManager? = null
    private var cameraCallback: CameraManager.AvailabilityCallback? = null
    private val busyRearCameras = mutableSetOf<String>()

    private var audioManager: AudioManager? = null
    private var audioPlaybackCallback: AudioManager.AudioPlaybackCallback? = null
    private var autoMusicActive = false

    private var settingsObserver: ContentObserver? = null
    private var scheduleTickerJob: Job? = null
    private var behaviorCollectorJob: Job? = null

    @Volatile private var stockBehavior = StockBehaviorSettings()
    @Volatile private var rearCameraBusy = false

    // Battery state tracking
    private var inLow = false
    private var inCritical = false
    private var inFull = false
    private val hysteresis = 2

    private var lowCritJob: Job? = null
    private var fullJob: Job? = null
    private var batteryLoopStopJob: Job? = null

    private var lastChargingState: Boolean? = null
    private var hasFlashedFullThisSession = false
    private var chargeTierJob: Job? = null

    private val systemEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val pct = if (level >= 0 && scale > 0) (level * 100) / scale else -1
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                    if (pct >= 0) {
                        serviceScope.launch { handleBatteryChanged(pct, charging) }
                    }
                }
                "android.media.VOLUME_CHANGED_ACTION" -> {
                    val stream = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                    if (stream == AudioManager.STREAM_MUSIC) {
                        serviceScope.launch { handleMediaVolumeChanged() }
                    }
                }
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED -> {
                    serviceScope.launch { evaluateGuards() }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        powerManager = getSystemService(PowerManager::class.java)
        notifManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("LedCoreService", "Listener CONNECTED")
        startForeground(FG_NOTIF_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)

        if (!receiversRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction("android.media.VOLUME_CHANGED_ACTION")
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            registerReceiver(systemEventReceiver, filter)
            receiversRegistered = true
        }

        registerPhoneCallListener()
        registerCameraGuard()
        registerSettingsGuards()
        registerAudioMonitor()
        startBehaviorCollectors()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d("LedCoreService", "Listener DISCONNECTED — requesting rebind")
        teardownMonitors()
        requestRebind(ComponentName(this, LedCoreService::class.java))
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        handler.removeCallbacksAndMessages(null)
        teardownMonitors()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun teardownMonitors() {
        if (receiversRegistered) {
            try {
                unregisterReceiver(systemEventReceiver)
            } catch (_: Exception) {}
            receiversRegistered = false
        }
        unregisterPhoneCallListener()
        unregisterCameraGuard()
        unregisterSettingsGuards()
        unregisterAudioMonitor()
        behaviorCollectorJob?.cancel()
        scheduleTickerJob?.cancel()
    }

    private fun startBehaviorCollectors() {
        behaviorCollectorJob?.cancel()
        behaviorCollectorJob = serviceScope.launch {
            preferencesRepository.stockBehaviorSettings.collect { settings ->
                stockBehavior = settings
                evaluateGuards()
                evaluateAutoMusicState()
            }
        }
        scheduleTickerJob?.cancel()
        scheduleTickerJob = serviceScope.launch {
            while (true) {
                evaluateGuards()
                delay(60_000L)
            }
        }
    }

    private fun registerCameraGuard() {
        if (cameraCallback != null) return
        val cm = getSystemService(CameraManager::class.java) ?: return
        val cb = object : CameraManager.AvailabilityCallback() {
            override fun onCameraUnavailable(cameraId: String) {
                if (!isFrontCamera(cm, cameraId)) {
                    synchronized(busyRearCameras) { busyRearCameras.add(cameraId) }
                    rearCameraBusy = true
                    serviceScope.launch { evaluateGuards() }
                }
            }

            override fun onCameraAvailable(cameraId: String) {
                synchronized(busyRearCameras) {
                    busyRearCameras.remove(cameraId)
                    rearCameraBusy = busyRearCameras.isNotEmpty()
                }
                serviceScope.launch { evaluateGuards() }
            }
        }
        cm.registerAvailabilityCallback(cb, handler)
        cameraManager = cm
        cameraCallback = cb
    }

    private fun unregisterCameraGuard() {
        cameraCallback?.let { cameraManager?.unregisterAvailabilityCallback(it) }
        cameraCallback = null
        cameraManager = null
        synchronized(busyRearCameras) { busyRearCameras.clear() }
        rearCameraBusy = false
    }

    private fun isFrontCamera(cm: CameraManager, cameraId: String): Boolean =
        try {
            cm.getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        } catch (_: Exception) {
            false
        }

    private fun registerSettingsGuards() {
        if (settingsObserver != null) return
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                serviceScope.launch { evaluateGuards() }
            }
        }
        val resolver = contentResolver
        resolver.registerContentObserver(Settings.Global.getUriFor("zen_mode"), false, observer)
        resolver.registerContentObserver(Settings.Global.getUriFor("low_power"), false, observer)
        settingsObserver = observer
    }

    private fun unregisterSettingsGuards() {
        settingsObserver?.let {
            try {
                contentResolver.unregisterContentObserver(it)
            } catch (_: Exception) {}
        }
        settingsObserver = null
    }

    private fun registerAudioMonitor() {
        if (audioPlaybackCallback != null) return
        val am = getSystemService(AudioManager::class.java) ?: return
        val cb = object : AudioManager.AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>) {
                serviceScope.launch { evaluateAutoMusicState(configs.isNotEmpty()) }
            }
        }
        am.registerAudioPlaybackCallback(cb, handler)
        audioManager = am
        audioPlaybackCallback = cb
    }

    private fun unregisterAudioMonitor() {
        audioPlaybackCallback?.let { audioManager?.unregisterAudioPlaybackCallback(it) }
        audioPlaybackCallback = null
        audioManager = null
    }

    private suspend fun evaluateGuards() {
        val settings = stockBehavior
        val zenActive = try {
            Settings.Global.getInt(contentResolver, "zen_mode", 0) != 0
        } catch (_: Exception) {
            false
        }
        val batterySaverActive = powerManager.isPowerSaveMode || try {
            Settings.Global.getInt(contentResolver, "low_power", 0) == 1
        } catch (_: Exception) {
            false
        }
        val cal = Calendar.getInstance()
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val reason = when {
            settings.cameraGuardEnabled && rearCameraBusy -> "Rear camera active"
            settings.respectDnd && zenActive -> "Do Not Disturb active"
            settings.respectBatterySaver && batterySaverActive -> "Battery Saver active"
            settings.scheduleEnabled && !StockGuardPolicy.isWithinActiveWindow(
                nowMinutes,
                settings.scheduleStartMinutes,
                settings.scheduleEndMinutes
            ) -> "Outside active schedule"
            else -> null
        }

        val suppress = StockGuardPolicy.shouldSuppressBackgroundLed(
            settings = settings,
            rearCameraBusy = rearCameraBusy,
            isDndActive = zenActive,
            isBatterySaverActive = batterySaverActive,
            nowMinutes = nowMinutes
        )
        hardwareRepository.updateSuppression(suppress, reason)
    }

    private suspend fun evaluateAutoMusicState(hasActivePlayback: Boolean? = null) {
        val settings = stockBehavior
        val manualMusicOn = preferencesRepository.audioLedEnabled.first()
        if (!settings.autoMusicSync || manualMusicOn) {
            if (autoMusicActive && !settings.autoMusicSync && !manualMusicOn) {
                autoMusicActive = false
                hardwareRepository.fireStop(
                    hardwareRepository.getConfig().turnOffHex,
                    "AUTO_MUSIC_DISABLED",
                    owner = LedOwner.MUSIC
                )
            }
            return
        }

        val am = audioManager ?: return
        val playing = hasActivePlayback ?: am.isMusicActive
        if (playing && !autoMusicActive) {
            autoMusicActive = true
            val dynamic = preferencesRepository.audioLedDynamic.first()
            val mode = if (dynamic) AudioLedMode.DYNAMIC else AudioLedMode.STATIC
            val gainLevel = preferencesRepository.audioLedGain.first()
            val gainHex = if (settings.autoVolumeGain) {
                val vol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                AudioGain.stockHexForVolume(vol, maxVol)
            } else {
                null
            }
            hardwareRepository.activateAutoMusicSync(mode, gainHex, gainLevel)
        } else if (!playing && autoMusicActive) {
            autoMusicActive = false
            hardwareRepository.fireStop(
                hardwareRepository.getConfig().turnOffHex,
                "AUTO_MUSIC_IDLE",
                owner = LedOwner.MUSIC
            )
        }
    }

    private suspend fun handleMediaVolumeChanged() {
        val settings = stockBehavior
        if (!settings.autoVolumeGain) return
        val am = audioManager ?: return
        val manualMusicOn = preferencesRepository.audioLedEnabled.first()
        if (!manualMusicOn && !autoMusicActive) return
        val vol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        hardwareRepository.setAudioGainHex(AudioGain.stockHexForVolume(vol, maxVol))
    }

    @Suppress("DEPRECATION")
    private fun registerPhoneCallListener() {
        if (phoneStateListener != null) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("LedCoreService", "READ_PHONE_STATE not granted -- skipping call LED")
            return
        }
        val tm = getSystemService(TelephonyManager::class.java) ?: return
        val listener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                serviceScope.launch {
                    when (state) {
                        TelephonyManager.CALL_STATE_RINGING ->
                            hardwareRepository.ringPhoneCall(stockBehavior.phoneCallEffectName)
                        TelephonyManager.CALL_STATE_OFFHOOK,
                        TelephonyManager.CALL_STATE_IDLE ->
                            hardwareRepository.endPhoneCall()
                    }
                }
            }
        }
        tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        telephonyManager = tm
        phoneStateListener = listener
    }

    @Suppress("DEPRECATION")
    private fun unregisterPhoneCallListener() {
        phoneStateListener?.let { telephonyManager?.listen(it, PhoneStateListener.LISTEN_NONE) }
        phoneStateListener = null
        telephonyManager = null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "LED Sync Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps LED effects running in the background"
            setShowBadge(false)
        }
        notifManager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pi = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("LED Sync")
            .setContentText("Listening for notifications & battery events")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        serviceScope.launch {
            try {
                val pkg = sbn.packageName ?: return@launch
                val hexMap = preferencesRepository.notifHexMap.first()
                val hex = hexMap[pkg] ?: return@launch
                if (hex.isBlank()) return@launch

                val loopingPkgs = preferencesRepository.loopingPackages.first()
                val isLooping = loopingPkgs.contains(pkg)

                if (isLooping) {
                    val alreadyActive = synchronized(activeLoopingPkgs) { activeLoopingPkgs.contains(pkg) }
                    if (alreadyActive) return@launch
                } else {
                    val now = System.currentTimeMillis()
                    val last = synchronized(lastTriggerPerPkg) { lastTriggerPerPkg[pkg] ?: 0L }
                    if (now - last < SEQUENCE_COOLDOWN_MS) return@launch
                }

                triggerNotifEffect(pkg, hex, isLooping)
            } catch (t: Throwable) {
                Log.e("LedCoreService", "onNotificationPosted failed: ${t.message}", t)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        serviceScope.launch {
            try {
                val pkg = sbn.packageName ?: return@launch
                val loopingPkgs = preferencesRepository.loopingPackages.first()
                if (!loopingPkgs.contains(pkg)) return@launch

                val stillActive = activeNotifications?.any { it.packageName == pkg } ?: false
                if (stillActive) return@launch

                val turnOffHex = preferencesRepository.turnOffHex.first()
                synchronized(activeLoopingPkgs) { activeLoopingPkgs.remove(pkg) }

                val wl = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LEDSync:stop:$pkg")
                wl.acquire(5000L)
                try {
                    hardwareRepository.fireStop(turnOffHex, "STOP_LOOP[$pkg]")
                } finally {
                    if (wl.isHeld) wl.release()
                }
            } catch (t: Throwable) {
                Log.e("LedCoreService", "onNotificationRemoved failed: ${t.message}", t)
            }
        }
    }

    private fun triggerNotifEffect(pkg: String, hex: String, isLooping: Boolean) {
        if (isLooping) {
            synchronized(activeLoopingPkgs) { activeLoopingPkgs.add(pkg) }
        }

        val wl = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LEDSync:trigger:$pkg")
        wl.acquire(10000L)

        serviceScope.launch {
            try {
                hardwareRepository.fireEffect(hex, "FIRE1[$pkg]")

                if (!isLooping) {
                    delay(DOUBLE_FIRE_DELAY_MS)
                    hardwareRepository.fireEffect(hex, "FIRE2[$pkg]")
                    synchronized(lastTriggerPerPkg) {
                        lastTriggerPerPkg[pkg] = System.currentTimeMillis()
                    }
                    hardwareRepository.releaseAndRestore(LedOwner.NOTIFICATION)
                } else {
                    handler.postDelayed({
                        val stillActive = synchronized(activeLoopingPkgs) { activeLoopingPkgs.contains(pkg) }
                        if (stillActive) {
                            synchronized(activeLoopingPkgs) { activeLoopingPkgs.remove(pkg) }
                            serviceScope.launch {
                                val offHex = preferencesRepository.turnOffHex.first()
                                hardwareRepository.fireStop(offHex, "AUTO_STOP[$pkg]")
                            }
                        }
                    }, LOOP_AUTO_STOP_MS)
                }
            } finally {
                if (wl.isHeld) wl.release()
            }
        }
    }

    private suspend fun handleBatteryChanged(level: Int, charging: Boolean) {
        if (!hardwareRepository.masterEnabled) return

        val wasCharging = lastChargingState
        if (charging && wasCharging == false) {
            hasFlashedFullThisSession = false
            fireChargeTierFlash(level)
        } else if (!charging && wasCharging == true) {
            chargeTierJob?.cancel()
            hardwareRepository.fireStop(
                hardwareRepository.getConfig().turnOffHex,
                "CHARGE_TIER_UNPLUG",
                owner = LedOwner.BATTERY
            )
            hasFlashedFullThisSession = false
        }
        if (charging && level >= 100 && !hasFlashedFullThisSession) {
            hasFlashedFullThisSession = true
            fireChargeTierFlash(level)
        }
        lastChargingState = charging

        val config: BatteryConfig = preferencesRepository.batteryConfig.first()
        val deviceConfig = hardwareRepository.getConfig()

        val nowCrit = level <= config.criticalThreshold
        val nowLow = level <= config.lowThreshold && !nowCrit
        val nowFull = charging && level >= config.fullThreshold

        if (inCritical && level >= config.criticalThreshold + hysteresis) {
            inCritical = false
            cancelBatteryJobs()
            hardwareRepository.turnOffAll()
        }
        if (inLow && level >= config.lowThreshold + hysteresis) {
            inLow = false
            cancelBatteryJobs()
            hardwareRepository.turnOffAll()
        }
        if (inFull && (!charging || level <= config.fullThreshold - hysteresis)) {
            inFull = false
            cancelBatteryJobs()
            hardwareRepository.turnOffAll()
        }

        if (nowCrit && !inCritical) {
            inCritical = true
            inLow = true
            playBatteryEffect(config.criticalEffectName)
            if (deviceConfig.loopingPatterns.contains(config.criticalEffectName)) {
                startBatteryLoopStopTimer()
            } else {
                startPulseTrain(config.criticalEffectName, 5)
            }
            return
        }

        if (nowLow && !inLow) {
            inLow = true
            playBatteryEffect(config.lowEffectName)
            if (deviceConfig.loopingPatterns.contains(config.lowEffectName)) {
                startBatteryLoopStopTimer()
            } else {
                startPulseTrain(config.lowEffectName, 5)
            }
            return
        }

        if (nowFull && !inFull) {
            inFull = true
            playBatteryEffect(config.fullEffectName)
            if (deviceConfig.loopingPatterns.contains(config.fullEffectName)) {
                startBatteryLoopStopTimer()
            } else {
                startFullPulseTrain(config.fullEffectName)
            }
            return
        }
    }

    private fun chargeTierHex(level: Int): String = when {
        level < 20 -> "00 02 00 00 00 00"
        level < 60 -> "00 02 01 00 00 00"
        else -> "00 02 02 00 00 00"
    }

    private fun fireChargeTierFlash(level: Int) {
        chargeTierJob?.cancel()
        chargeTierJob = serviceScope.launch {
            hardwareRepository.fireEffect(chargeTierHex(level), "CHARGE_TIER[$level%]", owner = LedOwner.BATTERY)
            delay(3000L)
            hardwareRepository.fireStop(
                hardwareRepository.getConfig().turnOffHex,
                "CHARGE_TIER_CLEAR",
                owner = LedOwner.BATTERY
            )
            recheckActiveBatteryEffect()
        }
    }

    private suspend fun recheckActiveBatteryEffect() {
        val config: BatteryConfig = preferencesRepository.batteryConfig.first()
        when {
            inCritical -> playBatteryEffect(config.criticalEffectName)
            inLow -> playBatteryEffect(config.lowEffectName)
            inFull -> playBatteryEffect(config.fullEffectName)
        }
    }

    private suspend fun playBatteryEffect(effectName: String) {
        val deviceConfig = hardwareRepository.getConfig()
        val hex = deviceConfig.hexForEffect(effectName) ?: return
        hardwareRepository.sendRawHex(hex, "BATT[$effectName]", owner = LedOwner.BATTERY)
    }

    private fun startBatteryLoopStopTimer() {
        batteryLoopStopJob?.cancel()
        batteryLoopStopJob = serviceScope.launch {
            delay(10000L)
            hardwareRepository.turnOffAll()
        }
    }

    private fun startPulseTrain(effectName: String, count: Int) {
        lowCritJob?.cancel()
        lowCritJob = serviceScope.launch {
            var remaining = count - 1
            while (remaining > 0) {
                delay(2000L)
                playBatteryEffect(effectName)
                remaining--
            }
            hardwareRepository.releaseAndRestore(LedOwner.BATTERY)
        }
    }

    private fun startFullPulseTrain(effectName: String) {
        fullJob?.cancel()
        fullJob = serviceScope.launch {
            while (inFull) {
                delay(3000L)
                playBatteryEffect(effectName)
            }
        }
    }

    private fun cancelBatteryJobs() {
        lowCritJob?.cancel()
        fullJob?.cancel()
        batteryLoopStopJob?.cancel()
    }
}
