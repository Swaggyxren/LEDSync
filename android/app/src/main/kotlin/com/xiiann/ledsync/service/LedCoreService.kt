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
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

    private var batteryReceiverRegistered = false

    private var telephonyManager: TelephonyManager? = null
    private var phoneStateListener: PhoneStateListener? = null

    // Battery state tracking
    private var inLow = false
    private var inCritical = false
    private var inFull = false
    private val hysteresis = 2

    private var lowCritJob: Job? = null
    private var fullJob: Job? = null
    private var batteryLoopStopJob: Job? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val pct = if (level >= 0 && scale > 0) (level * 100) / scale else -1
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                if (pct >= 0) {
                    serviceScope.launch {
                        handleBatteryChanged(pct, charging)
                    }
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
        Log.d("LedCoreService", "Listener CONNECTED ✅")
        startForeground(FG_NOTIF_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)

        if (!batteryReceiverRegistered) {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            registerReceiver(batteryReceiver, filter)
            batteryReceiverRegistered = true
        }

        registerPhoneCallListener()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d("LedCoreService", "Listener DISCONNECTED ❌ — requesting rebind")
        if (batteryReceiverRegistered) {
            try {
                unregisterReceiver(batteryReceiver)
            } catch (_: Exception) {}
            batteryReceiverRegistered = false
        }
        unregisterPhoneCallListener()
        requestRebind(ComponentName(this, LedCoreService::class.java))
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        handler.removeCallbacksAndMessages(null)
        serviceScope.cancel()
        if (batteryReceiverRegistered) {
            try {
                unregisterReceiver(batteryReceiver)
            } catch (_: Exception) {}
            batteryReceiverRegistered = false
        }
        unregisterPhoneCallListener()
        super.onDestroy()
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
                        TelephonyManager.CALL_STATE_RINGING -> hardwareRepository.ringPhoneCall()
                        TelephonyManager.CALL_STATE_OFFHOOK,
                        TelephonyManager.CALL_STATE_IDLE -> hardwareRepository.endPhoneCall()
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
        val config: BatteryConfig = preferencesRepository.batteryConfig.first()
        val deviceConfig = hardwareRepository.getConfig()

        val nowCrit = level <= config.criticalThreshold
        val nowLow = level <= config.lowThreshold && !nowCrit
        val nowFull = charging && level >= config.fullThreshold

        // Exit hysteresis
        if (inCritical && level >= config.criticalThreshold + hysteresis) {
            inCritical = false
            cancelBatteryJobs()
            if (deviceConfig.loopingPatterns.contains(config.criticalEffectName)) {
                hardwareRepository.turnOffAll()
            }
        }
        if (inLow && level >= config.lowThreshold + hysteresis) {
            inLow = false
            cancelBatteryJobs()
            if (deviceConfig.loopingPatterns.contains(config.lowEffectName)) {
                hardwareRepository.turnOffAll()
            }
        }
        if (inFull && (!charging || level <= config.fullThreshold - hysteresis)) {
            inFull = false
            cancelBatteryJobs()
            if (deviceConfig.loopingPatterns.contains(config.fullEffectName)) {
                hardwareRepository.turnOffAll()
            }
        }

        // Threshold entry
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

    private suspend fun playBatteryEffect(effectName: String) {
        val deviceConfig = hardwareRepository.getConfig()
        val hex = deviceConfig.ledEffects[effectName] ?: return
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
