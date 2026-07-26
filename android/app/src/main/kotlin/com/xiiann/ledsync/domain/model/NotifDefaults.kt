package com.xiiann.ledsync.domain.model

/**
 * Reference: stock LED_NOTIFICATION_WHITE_LIST (from the decompiled
 * transsion-light-services.jar Utils.java) mapped onto LEDSync's own
 * effect table. Seeded once on first-ever launch as a starting point --
 * the effect choices are a first-pass guess, since there's no way to
 * preview what each named pattern actually looks like on real hardware
 * without testing it live. Fully user-overridable afterward, and never
 * re-applied once seeded (see PreferencesRepository.notifDefaultsSeeded).
 */
object NotifDefaults {
    val PACKAGE_TO_EFFECT: Map<String, String> = mapOf(
        "com.whatsapp" to "Rise",
        "com.gbwhatsapp" to "Rise",
        "com.facebook.orca" to "Halo",
        "com.facebook.katana" to "Illusion",
        "com.facebook.lite" to "Illusion",
        "com.instagram.android" to "Party",
        "com.android.chrome" to "Soft",
        "org.telegram.messenger" to "Lightning",
        "com.snapchat.android" to "StarRiver",
        "com.google.android.gm" to "Pureness",
        "com.vkontakte.android" to "Speed",
    )
}
