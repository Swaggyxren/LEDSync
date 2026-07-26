# LEDSync

<p align="center">
  <img src="assets/card_banner.png" alt="LEDSync Banner" width="480" />
</p>

[![Android Release](https://img.shields.io/badge/Android-Release%20v2.1.1-brightgreen.svg)](https://github.com/Swaggyxren/LEDSync)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25%20Native-blue.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Material Design 3](https://img.shields.io/badge/Design-M3%20Expressive-FF7043.svg)](https://m3.material.io/)
[![Built with AI](https://img.shields.io/badge/Built%20with-AI-8A2BE2.svg)](#ai-assisted-development)

Native Android application built with Kotlin, Jetpack Compose, and Hilt that interfaces directly with kernel sysfs LED hardware drivers (`/sys/class/leds/`).

---

## Features

- **Audio Reactive LED**: Real-time audio spectrum visualization and static white modes, featuring a Quick Settings tile and an adjustable Reactivity slider.
- **Notification LED Sync**: Per-app custom LED effect mapping with 24/7 background sync.
- **Battery LED Studio**: Low, Critical, Charging, and Full battery LED triggers with discrete threshold sliders.
- **Material 3 Expressive UI**: Physics spring animations, floating nav dock, and dynamic Light/Dark system theme support.
- **Performance Monitor**: Real-time CPU sparkline chart, RAM allocation, storage capacity, and system uptime.
- **Security & Reliability**: Root permission gate and notification listener access checks.

---

## Requirements

- Android 8.0+ (API 26+)
- Root access (Magisk, KernelSU, or APatch)
- Notification Access enabled (Settings → Special app access → Notification access)

---

## Building from Source

```bash
git clone https://github.com/Swaggyxren/LEDSync.git
cd LEDSync/android
./gradlew assembleRelease
```

---

## AI-Assisted Development

This project is built mostly with AI assistance — Google Antigravity and Claude (Anthropic) handled the bulk of the implementation, including the native Kotlin rewrite, hardware protocol reverse-engineering, and ongoing feature work. Direction, testing, and hardware validation are human-driven.

---

## Author

- **Developer**: Xi'annnnnn ([@kasajin001](https://github.com/Swaggyxren))
- **License**: Open Source
