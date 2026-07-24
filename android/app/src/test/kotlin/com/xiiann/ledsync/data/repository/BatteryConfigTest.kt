package com.xiiann.ledsync.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BatteryConfigTest {

    @Test
    fun `BatteryConfig default values match handoff specifications`() {
        val config = BatteryConfig()
        assertEquals("Rise", config.lowEffectName)
        assertEquals("Lightning", config.criticalEffectName)
        assertEquals("Pureness", config.fullEffectName)
        assertEquals(20, config.lowThreshold)
        assertEquals(10, config.criticalThreshold)
        assertEquals(100, config.fullThreshold)
    }

    @Test
    fun `BatteryConfig thresholds clamp correctly`() {
        val lowClamped = 3.coerceIn(5, 50)
        assertEquals(5, lowClamped)

        val critClamped = 40.coerceIn(1, 30)
        assertEquals(30, critClamped)

        val fullClamped = 80.coerceIn(90, 100)
        assertEquals(90, fullClamped)
    }
}
