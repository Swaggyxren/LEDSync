package com.xiiann.ledsync.presentation.main

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiiann.ledsync.data.repository.HardwareRepository
import com.xiiann.ledsync.data.repository.PreferencesRepository
import com.xiiann.ledsync.data.repository.RootState
import com.xiiann.ledsync.domain.model.AppearanceMode
import com.xiiann.ledsync.domain.model.StockBehaviorSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hardwareRepository: HardwareRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val isReady: StateFlow<Boolean> = hardwareRepository.isReady
    val rootState: StateFlow<RootState> = hardwareRepository.rootState

    val appearanceMode: StateFlow<AppearanceMode> = preferencesRepository.appearanceMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppearanceMode.SYSTEM)

    val stockBehavior: StateFlow<StockBehaviorSettings> = preferencesRepository.stockBehaviorSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StockBehaviorSettings())

    private val _isNotifAccessGranted = MutableStateFlow(true)
    val isNotifAccessGranted: StateFlow<Boolean> = _isNotifAccessGranted.asStateFlow()

    init {
        viewModelScope.launch {
            hardwareRepository.initializeHardware()
        }
        checkNotificationAccess()
    }

    fun checkNotificationAccess() {
        val pkgName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        val granted = flat != null && flat.contains(pkgName)
        _isNotifAccessGranted.value = granted
    }

    fun retryRoot() {
        viewModelScope.launch {
            hardwareRepository.initializeHardware(force = true)
        }
    }

    fun setAppearanceMode(mode: AppearanceMode) {
        viewModelScope.launch {
            preferencesRepository.setAppearanceMode(mode)
        }
    }

    fun updateStockBehavior(transform: (StockBehaviorSettings) -> StockBehaviorSettings) {
        viewModelScope.launch {
            preferencesRepository.updateStockBehavior(transform)
        }
    }
}
