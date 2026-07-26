package com.xiiann.ledsync.presentation.notif

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiiann.ledsync.data.repository.HardwareRepository
import com.xiiann.ledsync.data.repository.PreferencesRepository
import com.xiiann.ledsync.data.source.AppInfoModel
import com.xiiann.ledsync.data.source.AppListSource
import com.xiiann.ledsync.domain.model.NotifDefaults
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotifMappingViewModel @Inject constructor(
    private val appListSource: AppListSource,
    private val preferencesRepository: PreferencesRepository,
    private val hardwareRepository: HardwareRepository
) : ViewModel() {

    private val _appList = MutableStateFlow<List<AppInfoModel>>(emptyList())
    val appList: StateFlow<List<AppInfoModel>> = _appList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val showSystemApps = preferencesRepository.showSystemApps

    private val _workingMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val workingMap: StateFlow<Map<String, String>> = _workingMap.asStateFlow()

    private val _savedMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val savedMap: StateFlow<Map<String, String>> = _savedMap.asStateFlow()

    private val _isDirty = MutableStateFlow(false)
    val isDirty: StateFlow<Boolean> = _isDirty.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var isFirstLaunch = true

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            val showSys = showSystemApps.first()
            var saved = preferencesRepository.notifNameMap.first()
            val apps = appListSource.getInstalledApps(showSys)

            // One-time seed of reference default mappings, gated on the
            // package actually being installed -- never re-applied after
            // this first run, so a mapping the user deliberately clears
            // later doesn't silently come back.
            val alreadySeeded = preferencesRepository.notifDefaultsSeeded.first()
            if (!alreadySeeded) {
                // Independent of the show-system-apps toggle -- some OEM
                // builds flag preinstalled Chrome/Gmail as system apps, and
                // seeding shouldn't silently skip them based on whatever
                // that toggle happens to be set to on first launch.
                val installedPkgs = appListSource.getInstalledApps(includeSystemApps = true)
                    .map { it.packageName }.toSet()
                val seeded = saved.toMutableMap()
                var changed = false
                NotifDefaults.PACKAGE_TO_EFFECT.forEach { (pkg, effect) ->
                    if (pkg in installedPkgs && pkg !in seeded) {
                        seeded[pkg] = effect
                        changed = true
                    }
                }
                preferencesRepository.setNotifDefaultsSeeded(true)
                if (changed) {
                    val cfg = hardwareRepository.getConfig()
                    val hexMap = mutableMapOf<String, String>()
                    val loopingPkgs = mutableSetOf<String>()
                    for ((pkg, effectName) in seeded) {
                        cfg.ledEffects[effectName]?.let { hexMap[pkg] = it }
                        if (cfg.loopingPatterns.contains(effectName)) loopingPkgs.add(pkg)
                    }
                    preferencesRepository.saveNotifMappings(
                        nameMap = seeded,
                        hexMap = hexMap,
                        loopingPkgs = loopingPkgs,
                        turnOffHexValue = cfg.turnOffHex ?: ""
                    )
                    saved = seeded
                }
            }

            _savedMap.value = saved
            _workingMap.value = saved

            if (isFirstLaunch) {
                delay(5000L)
                isFirstLaunch = false
            } else {
                delay(2500L)
            }

            _appList.value = apps
            _isLoading.value = false
        }
    }

    fun reloadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            val showSys = showSystemApps.first()
            val apps = appListSource.getInstalledApps(showSys)
            delay(2500L)
            _appList.value = apps
            _isLoading.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSystemApps(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowSystemApps(show)
            reloadApps()
        }
    }

    fun updateMapping(packageName: String, effectName: String?) {
        val current = _workingMap.value.toMutableMap()
        if (effectName.isNullOrBlank()) {
            current.remove(packageName)
        } else {
            current[packageName] = effectName
        }
        _workingMap.value = current
        _isDirty.value = _workingMap.value != _savedMap.value
    }

    fun saveMappings() {
        viewModelScope.launch {
            val cfg = hardwareRepository.getConfig()
            val working = _workingMap.value

            val hexMap = mutableMapOf<String, String>()
            val loopingPkgs = mutableSetOf<String>()

            for ((pkg, effectName) in working) {
                val hex = cfg.ledEffects[effectName]
                if (!hex.isNullOrBlank()) {
                    hexMap[pkg] = hex
                }
                if (cfg.loopingPatterns.contains(effectName)) {
                    loopingPkgs.add(pkg)
                }
            }

            preferencesRepository.saveNotifMappings(
                nameMap = working,
                hexMap = hexMap,
                loopingPkgs = loopingPkgs,
                turnOffHexValue = cfg.turnOffHex ?: ""
            )

            _savedMap.value = working
            _isDirty.value = false
        }
    }

    fun getDeviceConfig() = hardwareRepository.getConfig()
}
