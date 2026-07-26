package com.xiiann.ledsync.domain.model

class LH8nConfig : DeviceConfig {
    override val deviceName: String = "TECNO POVA 5 PRO 5G (LH8n)"
    override val awPath: String = "/sys/class/leds/aw22xxx_led"
    override val lbCmd: String = "/sys/led/led/tran_led_cmd"

    override val ledEffects: Map<String, String> = mapOf(
        "Soft" to "00 04 00 00 00 00",
        "Speed" to "00 30 01 00 00 00",
        "Illusion" to "00 03 01 00 00 00",
        "Pureness" to "00 05 01 00 00 00",
        "StarRiver" to "00 05 01 01 00 00",
        "Halo" to "00 05 01 02 00 00",
        "Lightning" to "00 05 01 03 00 00",
        "Rise" to "00 05 01 04 00 00",
        "Breathe" to "00 20 02 00 00 00",
        "Party" to "00 20 03 00 00 00",
        "Low Battery" to "00 02 00 00 00 00",
        "Charging" to "00 02 01 00 00 00",
        "Charged" to "00 02 02 00 00 00"
    )

    // Confirmed by live testing on hardware -- everything else is one-shot.
    override val loopingPatterns: Set<String> = setOf(
        "Soft",
        "Speed",
        "Illusion",
        "Breathe",
        "Party"
    )

    override val defaultLowEffect: String = "Rise"
    override val defaultCriticalEffect: String = "Lightning"
    override val defaultFullEffect: String = "Pureness"
    override val turnOffHex: String = "00 01 00 00 00 00"
}
