package com.xiiann.ledsync.presentation.main

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiiann.ledsync.data.repository.HardwareRepository
import com.xiiann.ledsync.data.repository.RootState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hardwareRepository: HardwareRepository
) : ViewModel() {

    val isReady: StateFlow<Boolean> = hardwareRepository.isReady
    val rootState: StateFlow<RootState> = hardwareRepository.rootState

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
}
