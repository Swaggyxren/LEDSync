package com.xiiann.ledsync.domain.model

/**
 * aw22xxx onboard audio-reactive gain, reverse-engineered from
 * TranLightsServiceExtImpl.getLedMusicGain() / Utils.TAN_LED_MUSIC_GAIN_1..15.
 * Stock maps *media volume* 1-15 to a gain byte (inverse -- louder volume
 * needs less onboard amplification). Exposed here as a direct "Reactivity"
 * scale instead, where higher = more sensitive, since LEDSync doesn't track
 * system volume live -- this is a user-facing slider, not an automatic
 * volume follower.
 *
 * Only the calmer half of the stock table (hex 08-17) is exposed -- the
 * upper half (1F-32) reacts to nearly anything and isn't usable in
 * practice, so the slider tops out at 8 instead of the full 15.
 */
object AudioGain {

    const val MIN_LEVEL = 1
    const val MAX_LEVEL = 8
    const val DEFAULT_LEVEL = 4

    // Index 0 = level 1 (calmest, hex 08) ... index 7 = level 8 (most sensitive usable value, hex 17).
    private val gainHexByLevel = listOf(
        "08", "0A", "0B", "0D", "10", "12", "14", "17"
    )

    fun hexFor(level: Int): String =
        gainHexByLevel[level.coerceIn(MIN_LEVEL, MAX_LEVEL) - 1]

    /** `00 21 GG 00 00 00` -- must be sent ~100ms after the trigger command,
     *  matching the stock service's tranSetFlash(MUSIC_FLASH_FLAG) timing. */
    fun command(level: Int): String = "00 21 ${hexFor(level)} 00 00 00"
}
