package com.xiiann.ledsync.domain.model

interface DeviceConfig {
    val deviceName: String
    val awPath: String
    val lbCmd: String
    val ledEffects: Map<String, String>
    val loopingPatterns: Set<String>
    val turnOffHex: String
        get() = "00 01 00 00 00 00"

    /** Ringing-call breath/blink pattern -- mode `04` on the aw22xxx
     *  protocol, reverse-engineered from stock's tranSetFlash(PHONE_CALL_FLASH_FLAG). */
    val phoneCallHex: String
        get() = "00 04 00 00 00 00"

    val defaultLowEffect: String
    val defaultCriticalEffect: String
    val defaultFullEffect: String
}
