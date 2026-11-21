package com.xiiann.ledsync.presentation

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.xiiann.ledsync.presentation.main.MainShell
import com.xiiann.ledsync.presentation.main.MainViewModel
import com.xiiann.ledsync.presentation.theme.LEDSyncTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val appearanceMode by mainViewModel.appearanceMode.collectAsState()
            LEDSyncTheme(appearanceMode = appearanceMode) {
                MainShell(mainViewModel = mainViewModel)
            }
        }
    }
}
