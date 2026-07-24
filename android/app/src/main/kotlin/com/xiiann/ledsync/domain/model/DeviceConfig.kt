package com.xiiann.ledsync.domain.model

interface DeviceConfig {
    val deviceName: String
    val awPath: String
    val lbCmd: String
    val ledEffects: Map<String, String>
    val loopingPatterns: Set<String>
    val turnOffHex: String
        get() = "00 01 00 00 00 00"

    val defaultLowEffect: String
    val defaultCriticalEffect: String
    val defaultFullEffect: String
}
