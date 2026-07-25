package com.xiiann.ledsync.presentation.performance

import org.junit.Assert.assertEquals
import org.junit.Test

class PerformanceViewModelTest {

    @Test
    fun `parseKernelVersion extracts main version string correctly`() {
        assertEquals("5.10.257", PerformanceViewModel.parseKernelVersion("5.10.257-android12-9-g123456"))
        assertEquals("5.10.198", PerformanceViewModel.parseKernelVersion("5.10.198-g89ab"))
        assertEquals("6.1.75", PerformanceViewModel.parseKernelVersion("6.1.75-android14-11"))
        assertEquals("4.19.157", PerformanceViewModel.parseKernelVersion("4.19.157-perf+"))
        assertEquals("Unknown", PerformanceViewModel.parseKernelVersion(""))
    }
}
