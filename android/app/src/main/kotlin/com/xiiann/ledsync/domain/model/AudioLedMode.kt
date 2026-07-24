package com.xiiann.ledsync.domain.model

enum class AudioLedMode(val label: String, val hex: String) {
    STATIC("Static White", "00 20 01 00 00 00"),
    DYNAMIC("Dynamic RGB", "00 20 00 00 00 00")
}
