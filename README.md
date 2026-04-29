# LED Tile

A minimal Android app that adds a single **Quick Settings tile** to toggle an LED effect via root on `/sys/led/led/tran_led_cmd`.

> ⚠ **Target device: TECNO POVA 5 Pro 5G (LH8n)**
> The byte sequences below are specific to the Tecno POVA 5 Pro 5G's LED driver. They will do nothing (or misbehave) on any other device — the sysfs path and the command layout differ per kernel/driver. For other devices, change the commands in `LedTileService.kt` to match your device's LED node.

- **Enable** (`STATE_ACTIVE`) runs:
  ```sh
  su -c "echo -n '00 00 00 00 00 00' > /sys/led/led/tran_led_cmd"
  su -c "echo -n '00 20 01 00 00 00' > /sys/led/led/tran_led_cmd"
  ```
- **Disable** (`STATE_INACTIVE`) runs:
  ```sh
  su -c "echo -n '00 01 00 00 00 00' > /sys/led/led/tran_led_cmd"
  ```

## Requirements
- Root (Magisk / KernelSU) — the tile shells out through `su` and will toast "root command failed" if root is denied.
- **TECNO POVA 5 Pro 5G** (or another device that exposes the exact same `/sys/led/led/tran_led_cmd` interface).

## Usage
1. Install the APK.
2. Grant root to the app when prompted.
3. Open Quick Settings, tap the edit (pencil) icon, and drag the **LED** tile into your active tiles.
4. Tap the tile to toggle.

## Build
```sh
./gradlew :app:assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

## Structure
- `app/src/main/kotlin/com/xiannn/ledtile/LedTileService.kt` — `TileService` that writes the bytes to the sysfs node via `su`.
- `app/src/main/kotlin/com/xiannn/ledtile/MainActivity.kt` — launcher activity with a short info screen.
- `app/src/main/AndroidManifest.xml` — declares the tile service with `BIND_QUICK_SETTINGS_TILE`.

## Package
`com.xiannn.ledtile`
