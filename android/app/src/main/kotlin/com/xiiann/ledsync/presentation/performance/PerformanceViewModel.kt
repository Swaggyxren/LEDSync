package com.xiiann.ledsync.presentation.performance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiiann.ledsync.data.executor.IRootExecutor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PerformanceMetrics(
    val cpuPct: Float = 0f,
    val ramUsedMb: Int = 0,
    val ramTotalMb: Int = 0,
    val batteryLevel: Int = 0,
    val isCharging: Boolean = false,
    val isWarming: Boolean = true
)

@HiltViewModel
class PerformanceViewModel @Inject constructor(
    private val rootExecutor: IRootExecutor
) : ViewModel() {

    private val _cpuHistory = MutableStateFlow<List<Float>>(List(60) { 0f })
    val cpuHistory: StateFlow<List<Float>> = _cpuHistory.asStateFlow()

    private val _metrics = MutableStateFlow(PerformanceMetrics())
    val metrics: StateFlow<PerformanceMetrics> = _metrics.asStateFlow()

    private var prevIdle = 0L
    private var prevTotal = 0L
    private var warmTicks = 0

    init {
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                readProcStats()
                val interval = if (warmTicks < 8) {
                    warmTicks++
                    500L
                } else {
                    _metrics.value = _metrics.value.copy(isWarming = false)
                    2000L
                }
                delay(interval)
            }
        }
    }

    private suspend fun readProcStats() = withContext(Dispatchers.IO) {
        val raw = rootExecutor.runSuOutput("cat /proc/stat; echo \"---\"; cat /proc/meminfo")
        if (raw.isNotBlank() && raw.contains("---")) {
            val parts = raw.split("---\n")
            if (parts.size >= 2) {
                parseCpu(parts[0])
                parseRam(parts[1])
            }
        }
    }

    private fun parseCpu(statRaw: String) {
        val line = statRaw.lineSequence().firstOrNull { it.startsWith("cpu ") } ?: return
        val nums = line.split("\\s+".toRegex()).drop(1).filter { it.isNotEmpty() }.mapNotNull { it.toLongOrNull() }
        if (nums.size < 4) return

        val idle = nums[3] + if (nums.size > 4) nums[4] else 0L
        val total = nums.sum()

        val dI = idle - prevIdle
        val dT = total - prevTotal
        prevIdle = idle
        prevTotal = total

        val pct = if (dT > 0) (1.0f - (dI.toFloat() / dT.toFloat())).coerceIn(0.0f, 1.0f) else 0.0f
        _metrics.value = _metrics.value.copy(cpuPct = pct)

        val history = _cpuHistory.value.toMutableList()
        history.add(pct)
        if (history.size > 60) history.removeAt(0)
        _cpuHistory.value = history
    }

    private fun parseRam(meminfoRaw: String) {
        var total = 0
        var avail = 0
        for (line in meminfoRaw.lineSequence()) {
            val parts = line.split("\\s+".toRegex())
            if (parts.size < 2) continue
            val v = parts[1].toIntOrNull() ?: 0
            if (line.startsWith("MemTotal:")) total = v
            if (line.startsWith("MemAvailable:")) avail = v
        }
        if (total > 0) {
            _metrics.value = _metrics.value.copy(
                ramUsedMb = (total - avail) / 1024,
                ramTotalMb = total / 1024
            )
        }
    }
}
