package com.xiiann.ledsync.data.source

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class AppInfoModel(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val iconBitmap: Bitmap? = null
)

@Singleton
class AppListSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val pm: PackageManager = context.packageManager

    suspend fun getInstalledApps(includeSystemApps: Boolean): List<AppInfoModel> =
        withContext(Dispatchers.IO) {
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val appMap = mutableMapOf<String, AppInfoModel>()

            for (ri in resolveInfos) {
                val pkg = ri.activityInfo.packageName
                val appInfo = ri.activityInfo.applicationInfo
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                if (!includeSystemApps && isSystem) continue
                if (appMap.containsKey(pkg)) continue

                val label = ri.loadLabel(pm).toString()
                val iconDrawable = ri.loadIcon(pm)
                val iconBitmap = drawableToBitmap(iconDrawable)

                appMap[pkg] = AppInfoModel(
                    packageName = pkg,
                    label = label,
                    isSystem = isSystem,
                    iconBitmap = iconBitmap
                )
            }

            appMap.values.sortedBy { it.label.lowercase() }
        }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
