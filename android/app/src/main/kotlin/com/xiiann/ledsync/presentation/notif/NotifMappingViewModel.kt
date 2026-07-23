package com.xiiann.ledsync.presentation.notif

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiiann.ledsync.data.repository.HardwareRepository
import com.xiiann.ledsync.data.repository.PreferencesRepository
import com.xiiann.ledsync.data.source.AppInfoModel
import com.xiiann.ledsync.data.source.AppListSource
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
            val saved = preferencesRepository.notifNameMap.first()

            _savedMap.value = saved
            _workingMap.value = saved
            val apps = appListSource.getInstalledApps(showSys)

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
