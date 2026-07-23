package com.xiiann.ledsync.service

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d("BootReceiver", "Rebinding LedCoreService via action: $action")
            try {
                NotificationListenerService.requestRebind(
                    ComponentName(context, LedCoreService::class.java)
                )
            } catch (t: Throwable) {
                Log.e("BootReceiver", "requestRebind failed: ${t.message}", t)
            }
        }
    }
}
