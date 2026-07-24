package com.xiiann.ledsync.domain.model

enum class LedEffect(
    val label: String,
    val hex: String,
    val looping: Boolean
) {
    SOFT("Soft", "00 04 00 00 00 00", true),
    SPEED("Speed", "00 30 01 00 00 00", true),
    ILLUSION("Illusion", "00 03 01 00 00 00", true),
    PURENESS("Pureness", "00 05 01 00 00 00", true),
    STAR_RIVER("StarRiver", "00 05 01 01 00 00", true),
    HALO("Halo", "00 05 01 02 00 00", true),
    LIGHTNING("Lightning", "00 05 01 03 00 00", true),
    RISE("Rise", "00 05 01 04 00 00", true),
    BREATHE("Breathe", "00 20 02 00 00 00", true),
    PARTY("Party", "00 20 03 00 00 00", true),
    LOW_BATTERY("Low Battery", "00 02 00 00 00 00", true),
    CHARGING("Charging", "00 02 01 00 00 00", true),
    CHARGED("Charged", "00 02 02 00 00 00", true);

    companion object {
        fun fromLabel(label: String): LedEffect? =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) }
    }
}
