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
    val fullEffectName: String = "Pureness",
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

    val batteryConfig: Flow<BatteryConfig> = dataStore.data.map { prefs ->
        BatteryConfig(
            lowEffectName = prefs[KEY_BATT_LOW_EFFECT] ?: "Rise",
            criticalEffectName = prefs[KEY_BATT_CRIT_EFFECT] ?: "Lightning",
            fullEffectName = prefs[KEY_BATT_FULL_EFFECT] ?: "Pureness",
            lowThreshold = (prefs[KEY_BATT_LOW_THRESH] ?: 20).coerceIn(5, 50),
            criticalThreshold = (prefs[KEY_BATT_CRIT_THRESH] ?: 10).coerceIn(1, 30),
            fullThreshold = (prefs[KEY_BATT_FULL_THRESH] ?: 100).coerceIn(90, 100)
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
}
