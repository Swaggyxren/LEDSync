package com.xiiann.ledsync.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StockGuardPolicyTest {

    @Test
    fun `daytime schedule window checks inclusive bounds`() {
        assertTrue(StockGuardPolicy.isWithinActiveWindow(7 * 60, 7 * 60, 22 * 60))
        assertTrue(StockGuardPolicy.isWithinActiveWindow(14 * 60, 7 * 60, 22 * 60))
        assertTrue(StockGuardPolicy.isWithinActiveWindow(22 * 60, 7 * 60, 22 * 60))
        assertFalse(StockGuardPolicy.isWithinActiveWindow(6 * 60 + 59, 7 * 60, 22 * 60))
        assertFalse(StockGuardPolicy.isWithinActiveWindow(23 * 60, 7 * 60, 22 * 60))
    }

    @Test
    fun `overnight schedule window wraps across midnight`() {
        val start = 22 * 60
        val end = 7 * 60
        assertTrue(StockGuardPolicy.isWithinActiveWindow(23 * 60, start, end))
        assertTrue(StockGuardPolicy.isWithinActiveWindow(2 * 60, start, end))
        assertFalse(StockGuardPolicy.isWithinActiveWindow(12 * 60, start, end))
    }

    @Test
    fun `shouldSuppressBackgroundLed honors camera, DND, battery saver, and schedule`() {
        val base = StockBehaviorSettings(
            cameraGuardEnabled = true,
            respectDnd = true,
            respectBatterySaver = true,
            scheduleEnabled = true,
            scheduleStartMinutes = 7 * 60,
            scheduleEndMinutes = 22 * 60
        )
        assertFalse(StockGuardPolicy.shouldSuppressBackgroundLed(base, false, false, false, 12 * 60))
        assertTrue(StockGuardPolicy.shouldSuppressBackgroundLed(base, true, false, false, 12 * 60))
        assertTrue(StockGuardPolicy.shouldSuppressBackgroundLed(base, false, true, false, 12 * 60))
        assertTrue(StockGuardPolicy.shouldSuppressBackgroundLed(base, false, false, true, 12 * 60))
        assertTrue(StockGuardPolicy.shouldSuppressBackgroundLed(base, false, false, false, 2 * 60))
    }

    @Test
    fun `AudioGain stock volume curve maps inverse volume and clamps to kernel 0x3F`() {
        assertEquals("32", AudioGain.stockHexForVolume(1, 15))
        assertEquals("08", AudioGain.stockHexForVolume(15, 15))
        assertEquals("00 21 3F 00 00 00", AudioGain.commandFromHex("FF"))
        assertEquals("00 21 17 00 00 00", AudioGain.commandFromHex("17"))
    }

    @Test
    fun `LH8nConfig includes core and extra kernel effects`() {
        val cfg = LH8nConfig()
        assertEquals(13, cfg.ledEffects.size)
        assertEquals(16, cfg.extraKernelEffects.size)
        assertEquals("00 32 05 00 00 00", cfg.hexForEffect("MLBB Penta Kill"))
    }
}
