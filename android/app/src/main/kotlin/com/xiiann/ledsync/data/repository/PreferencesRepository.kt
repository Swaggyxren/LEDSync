package com.xiiann.ledsync.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xiiann.ledsync.domain.model.AppearanceMode
import com.xiiann.ledsync.domain.model.AudioGain
import com.xiiann.ledsync.domain.model.StockBehaviorSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ledsync_settings")

data class BatteryConfig(
    val lowEffectName: String = "Rise",
    val criticalEffectName: String = "Lightning",
    val fullEffectName: String = "Halo",
    val lowThreshold: Int = 20,
    val criticalThreshold: Int = 10,
    val fullThreshold: Int = 100
)

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_NOTIF_HEX_MAP = stringPreferencesKey("notif_hex_map")
        val KEY_NOTIF_NAME_MAP = stringPreferencesKey("notif_name_map")
        val KEY_LOOPING_PKGS = stringSetPreferencesKey("notif_looping_pkgs")
        val KEY_TURN_OFF_HEX = stringPreferencesKey("notif_turnoff_hex")
        val KEY_NOTIF_DEFAULTS_SEEDED = booleanPreferencesKey("notif_defaults_seeded")

        val KEY_BATT_LOW_EFFECT = stringPreferencesKey("batt_low_effect_name")
        val KEY_BATT_CRIT_EFFECT = stringPreferencesKey("batt_critical_effect_name")
        val KEY_BATT_FULL_EFFECT = stringPreferencesKey("batt_full_effect_name")
        val KEY_BATT_LOW_THRESH = intPreferencesKey("batt_low_threshold")
        val KEY_BATT_CRIT_THRESH = intPreferencesKey("batt_critical_threshold")
        val KEY_BATT_FULL_THRESH = intPreferencesKey("batt_full_threshold")

        val KEY_SELECTED_DEVICE = stringPreferencesKey("selected_device_config")
        val KEY_SHOW_SYSTEM_APPS = booleanPreferencesKey("notif_show_system_apps")
        val KEY_TOOLTIP_SHOWN = booleanPreferencesKey("settings_tooltip_shown")
        val KEY_AUDIO_LED_ENABLED = booleanPreferencesKey("audio_led_enabled")
        val KEY_AUDIO_LED_DYNAMIC = booleanPreferencesKey("audio_led_dynamic")
        val KEY_AUDIO_LED_GAIN = intPreferencesKey("audio_led_gain")

        val KEY_APPEARANCE_MODE = stringPreferencesKey("appearance_mode")
        val KEY_CAMERA_GUARD = booleanPreferencesKey("stock_camera_guard")
        val KEY_RESPECT_DND = booleanPreferencesKey("stock_respect_dnd")
        val KEY_RESPECT_BATTERY_SAVER = booleanPreferencesKey("stock_respect_battery_saver")
        val KEY_SCHEDULE_ENABLED = booleanPreferencesKey("stock_schedule_enabled")
        val KEY_SCHEDULE_START_MIN = intPreferencesKey("stock_schedule_start_min")
        val KEY_SCHEDULE_END_MIN = intPreferencesKey("stock_schedule_end_min")
        val KEY_AUTO_MUSIC_SYNC = booleanPreferencesKey("stock_auto_music_sync")
        val KEY_AUTO_VOLUME_GAIN = booleanPreferencesKey("stock_auto_volume_gain")
        val KEY_PHONE_CALL_EFFECT = stringPreferencesKey("stock_phone_call_effect")
    }

    val notifHexMap: Flow<Map<String, String>> = dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_NOTIF_HEX_MAP] ?: return@map emptyMap()
        runCatching { Json.decodeFromString<Map<String, String>>(jsonStr) }.getOrDefault(emptyMap())
    }

    val notifNameMap: Flow<Map<String, String>> = dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_NOTIF_NAME_MAP] ?: return@map emptyMap()
        runCatching { Json.decodeFromString<Map<String, String>>(jsonStr) }.getOrDefault(emptyMap())
    }

    val loopingPackages: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[KEY_LOOPING_PKGS] ?: emptySet()
    }

    val turnOffHex: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_TURN_OFF_HEX] ?: "00 01 00 00 00 00"
    }

    val notifDefaultsSeeded: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_NOTIF_DEFAULTS_SEEDED] ?: false
    }

    suspend fun setNotifDefaultsSeeded(seeded: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_NOTIF_DEFAULTS_SEEDED] = seeded
        }
    }

    val batteryConfig: Flow<BatteryConfig> = dataStore.data.map { prefs ->
        BatteryConfig(
            lowEffectName = prefs[KEY_BATT_LOW_EFFECT] ?: "Rise",
            criticalEffectName = prefs[KEY_BATT_CRIT_EFFECT] ?: "Lightning",
            fullEffectName = prefs[KEY_BATT_FULL_EFFECT] ?: "Halo",
            lowThreshold = (prefs[KEY_BATT_LOW_THRESH] ?: 20).coerceIn(5, 50),
            criticalThreshold = (prefs[KEY_BATT_CRIT_THRESH] ?: 10).coerceIn(1, 30),
            fullThreshold = (prefs[KEY_BATT_FULL_THRESH] ?: 100).coerceIn(70, 100)
        )
    }

    val selectedDeviceConfig: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_DEVICE] ?: "TECNO POVA 5 PRO 5G (LH8n)"
    }

    val showSystemApps: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_SHOW_SYSTEM_APPS] ?: false
    }

    val tooltipShown: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_TOOLTIP_SHOWN] ?: false
    }

    val audioLedEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_AUDIO_LED_ENABLED] ?: false
    }

    val audioLedDynamic: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_AUDIO_LED_DYNAMIC] ?: false
    }

    val audioLedGain: Flow<Int> = dataStore.data.map { prefs ->
        (prefs[KEY_AUDIO_LED_GAIN] ?: AudioGain.DEFAULT_LEVEL)
            .coerceIn(AudioGain.MIN_LEVEL, AudioGain.MAX_LEVEL)
    }

    val appearanceMode: Flow<AppearanceMode> = dataStore.data.map { prefs ->
        AppearanceMode.fromStored(prefs[KEY_APPEARANCE_MODE])
    }

    val stockBehaviorSettings: Flow<StockBehaviorSettings> = dataStore.data.map { prefs ->
        StockBehaviorSettings(
            cameraGuardEnabled = prefs[KEY_CAMERA_GUARD] ?: true,
            respectDnd = prefs[KEY_RESPECT_DND] ?: true,
            respectBatterySaver = prefs[KEY_RESPECT_BATTERY_SAVER] ?: true,
            scheduleEnabled = prefs[KEY_SCHEDULE_ENABLED] ?: false,
            scheduleStartMinutes = (prefs[KEY_SCHEDULE_START_MIN] ?: 7 * 60).coerceIn(0, 1439),
            scheduleEndMinutes = (prefs[KEY_SCHEDULE_END_MIN] ?: 22 * 60).coerceIn(0, 1439),
            autoMusicSync = prefs[KEY_AUTO_MUSIC_SYNC] ?: false,
            autoVolumeGain = prefs[KEY_AUTO_VOLUME_GAIN] ?: false,
            phoneCallEffectName = prefs[KEY_PHONE_CALL_EFFECT] ?: "Soft"
        )
    }

    suspend fun saveNotifMappings(
        nameMap: Map<String, String>,
        hexMap: Map<String, String>,
        loopingPkgs: Set<String>,
        turnOffHexValue: String
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_NOTIF_NAME_MAP] = Json.encodeToString(nameMap)
            prefs[KEY_NOTIF_HEX_MAP] = Json.encodeToString(hexMap)
            prefs[KEY_LOOPING_PKGS] = loopingPkgs
            prefs[KEY_TURN_OFF_HEX] = turnOffHexValue
        }
    }

    suspend fun saveBatteryConfig(config: BatteryConfig) {
        dataStore.edit { prefs ->
            prefs[KEY_BATT_LOW_EFFECT] = config.lowEffectName
            prefs[KEY_BATT_CRIT_EFFECT] = config.criticalEffectName
            prefs[KEY_BATT_FULL_EFFECT] = config.fullEffectName
            prefs[KEY_BATT_LOW_THRESH] = config.lowThreshold
            prefs[KEY_BATT_CRIT_THRESH] = config.criticalThreshold
            prefs[KEY_BATT_FULL_THRESH] = config.fullThreshold
        }
    }

    suspend fun setSelectedDevice(deviceName: String) {
        dataStore.edit { prefs ->
            prefs[KEY_SELECTED_DEVICE] = deviceName
        }
    }

    suspend fun setShowSystemApps(show: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_SHOW_SYSTEM_APPS] = show
        }
    }

    suspend fun setTooltipShown(shown: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_TOOLTIP_SHOWN] = shown
        }
    }

    suspend fun setAudioLedEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_AUDIO_LED_ENABLED] = enabled
        }
    }

    suspend fun setAudioLedDynamic(dynamic: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_AUDIO_LED_DYNAMIC] = dynamic
        }
    }

    suspend fun setAudioLedGain(level: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_AUDIO_LED_GAIN] = level
        }
    }

    suspend fun setAppearanceMode(mode: AppearanceMode) {
        dataStore.edit { prefs ->
            prefs[KEY_APPEARANCE_MODE] = mode.name
        }
    }

    suspend fun updateStockBehavior(transform: (StockBehaviorSettings) -> StockBehaviorSettings) {
        dataStore.edit { prefs ->
            val current = StockBehaviorSettings(
                cameraGuardEnabled = prefs[KEY_CAMERA_GUARD] ?: true,
                respectDnd = prefs[KEY_RESPECT_DND] ?: true,
                respectBatterySaver = prefs[KEY_RESPECT_BATTERY_SAVER] ?: true,
                scheduleEnabled = prefs[KEY_SCHEDULE_ENABLED] ?: false,
                scheduleStartMinutes = (prefs[KEY_SCHEDULE_START_MIN] ?: 7 * 60).coerceIn(0, 1439),
                scheduleEndMinutes = (prefs[KEY_SCHEDULE_END_MIN] ?: 22 * 60).coerceIn(0, 1439),
                autoMusicSync = prefs[KEY_AUTO_MUSIC_SYNC] ?: false,
                autoVolumeGain = prefs[KEY_AUTO_VOLUME_GAIN] ?: false,
                phoneCallEffectName = prefs[KEY_PHONE_CALL_EFFECT] ?: "Soft"
            )
            val updated = transform(current)
            prefs[KEY_CAMERA_GUARD] = updated.cameraGuardEnabled
            prefs[KEY_RESPECT_DND] = updated.respectDnd
            prefs[KEY_RESPECT_BATTERY_SAVER] = updated.respectBatterySaver
            prefs[KEY_SCHEDULE_ENABLED] = updated.scheduleEnabled
            prefs[KEY_SCHEDULE_START_MIN] = updated.scheduleStartMinutes.coerceIn(0, 1439)
            prefs[KEY_SCHEDULE_END_MIN] = updated.scheduleEndMinutes.coerceIn(0, 1439)
            prefs[KEY_AUTO_MUSIC_SYNC] = updated.autoMusicSync
            prefs[KEY_AUTO_VOLUME_GAIN] = updated.autoVolumeGain
            prefs[KEY_PHONE_CALL_EFFECT] = updated.phoneCallEffectName
        }
    }
}
