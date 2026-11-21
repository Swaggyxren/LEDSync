package com.xiiann.ledsync.domain.model

enum class AppearanceMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark");

    companion object {
        fun fromStored(value: String?): AppearanceMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SYSTEM
    }
}

data class StockBehaviorSettings(
    val cameraGuardEnabled: Boolean = true,
    val respectDnd: Boolean = true,
    val respectBatterySaver: Boolean = true,
    val scheduleEnabled: Boolean = false,
    val scheduleStartMinutes: Int = 7 * 60,
    val scheduleEndMinutes: Int = 22 * 60,
    val autoMusicSync: Boolean = false,
    val autoVolumeGain: Boolean = false,
    val phoneCallEffectName: String = "Soft"
)

/**
 * Mirrors stock TranLightsServiceExtImpl.checkCurrentTime() & observer guards:
 * supports daytime windows (07:00..22:00) and overnight wraparound (22:00..07:00).
 */
object StockGuardPolicy {

    val PHONE_CALL_PATTERNS: List<String> = listOf("Soft", "Speed", "Illusion", "Breathe", "Party")

    fun isWithinActiveWindow(nowMinutes: Int, startMinutes: Int, endMinutes: Int): Boolean {
        val now = nowMinutes.coerceIn(0, 1439)
        val start = startMinutes.coerceIn(0, 1439)
        val end = endMinutes.coerceIn(0, 1439)
        if (start == end) return true
        return if (start < end) {
            now in start..end
        } else {
            now >= start || now <= end
        }
    }

    fun shouldSuppressBackgroundLed(
        settings: StockBehaviorSettings,
        rearCameraBusy: Boolean,
        isDndActive: Boolean,
        isBatterySaverActive: Boolean,
        nowMinutes: Int
    ): Boolean {
        if (settings.cameraGuardEnabled && rearCameraBusy) return true
        if (settings.respectDnd && isDndActive) return true
        if (settings.respectBatterySaver && isBatterySaverActive) return true
        if (settings.scheduleEnabled &&
            !isWithinActiveWindow(nowMinutes, settings.scheduleStartMinutes, settings.scheduleEndMinutes)
        ) {
            return true
        }
        return false
    }

    fun formatMinutes(minutes: Int): String {
        val clamped = minutes.coerceIn(0, 1439)
        return "%02d:%02d".format(clamped / 60, clamped % 60)
    }
}
