package com.xiannn.ledtile

import android.content.SharedPreferences
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import java.io.DataOutputStream

class LedTileService : TileService() {

    private val prefsName = "led_tile_prefs"
    private val keyEnabled = "led_enabled"

    private val prefs: SharedPreferences by lazy {
        applicationContext.getSharedPreferences(prefsName, MODE_PRIVATE)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile(prefs.getBoolean(keyEnabled, false))
    }

    override fun onClick() {
        super.onClick()
        val newState = !prefs.getBoolean(keyEnabled, false)

        // Reflect intended state immediately so the tile animates.
        updateTile(newState)

        val ok = if (newState) runEnableCommands() else runDisableCommands()

        if (ok) {
            prefs.edit().putBoolean(keyEnabled, newState).apply()
        } else {
            // Revert tile if commands failed.
            updateTile(!newState)
            showToast("LED Tile: root command failed. Is root granted?")
        }
    }

    private fun updateTile(enabled: Boolean) {
        qsTile?.apply {
            state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = getString(R.string.tile_label)
            contentDescription = getString(R.string.tile_label)
            icon = Icon.createWithResource(this@LedTileService, R.drawable.ic_tile)
            updateTile()
        }
    }

    private fun runEnableCommands(): Boolean {
        val cmds = listOf(
            "echo -n '00 00 00 00 00 00' > /sys/led/led/tran_led_cmd",
            "echo -n '00 20 01 00 00 00' > /sys/led/led/tran_led_cmd"
        )
        return runAsRoot(cmds)
    }

    private fun runDisableCommands(): Boolean {
        val cmds = listOf(
            "echo -n '00 01 00 00 00 00' > /sys/led/led/tran_led_cmd"
        )
        return runAsRoot(cmds)
    }

    private fun runAsRoot(commands: List<String>): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            DataOutputStream(process.outputStream).use { os ->
                for (cmd in commands) {
                    os.writeBytes(cmd + "\n")
                }
                os.writeBytes("exit\n")
                os.flush()
            }
            val exit = process.waitFor()
            if (exit != 0) {
                Log.w(TAG, "su exited with code $exit")
            }
            exit == 0
        } catch (t: Throwable) {
            Log.e(TAG, "runAsRoot failed", t)
            false
        }
    }

    private fun showToast(msg: String) {
        try {
            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
        } catch (_: Throwable) {
        }
    }

    companion object {
        private const val TAG = "LedTileService"
    }
}
