package com.xiiann.ledsync.domain.model

import kotlin.math.roundToInt

/**
 * aw22xxx onboard audio-reactive gain, reverse-engineered from
 * TranLightsServiceExtImpl.getLedMusicGain() / Utils.TAN_LED_MUSIC_GAIN_1..15
 * and validated against store_tran_led_cmd case 0x21 (range 0x00..0x3f).
 */
object AudioGain {

    const val MIN_LEVEL = 1
    const val MAX_LEVEL = 8
    const val DEFAULT_LEVEL = 4
    const val MAX_KERNEL_GAIN = 0x3F

    // Index 0 = level 1 (calmest, hex 08) ... index 7 = level 8 (most sensitive usable value, hex 17).
    private val gainHexByLevel = listOf(
        "08", "0A", "0B", "0D", "10", "12", "14", "17"
    )

    // Stock TranLightsServiceExtImpl.getLedMusicGain(1..15) curve (inverse of media volume).
    private val stockVolumeGainHex = listOf(
        "32", "30", "29", "25", "22",
        "1F", "1F", "17", "14", "12",
        "10", "0D", "0B", "0A", "08"
    )

    fun hexFor(level: Int): String =
        gainHexByLevel[level.coerceIn(MIN_LEVEL, MAX_LEVEL) - 1]

    /** Maps current media volume onto stock's 1..15 inverse gain curve. */
    fun stockHexForVolume(volume: Int, maxVolume: Int = 15): String {
        if (maxVolume <= 0 || volume <= 0) return stockVolumeGainHex.first()
        val scaled = ((volume.toFloat() / maxVolume.toFloat()) * 15f).roundToInt().coerceIn(1, 15)
        return stockVolumeGainHex[scaled - 1]
    }

    fun command(level: Int): String = "00 21 ${hexFor(level)} 00 00 00"

    fun commandFromHex(hexByte: String): String {
        val clamped = (hexByte.toIntOrNull(16) ?: 0x1F).coerceIn(0, MAX_KERNEL_GAIN)
        return "00 21 %02X 00 00 00".format(clamped)
    }
}
