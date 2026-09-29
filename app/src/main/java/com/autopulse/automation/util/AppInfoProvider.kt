package com.autopulse.automation.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean
)

object AppInfoProvider {

    suspend fun getInstalledApps(context: Context, includeSystemApps: Boolean = false): List<AppInfo> {
        return withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
            }

            val apps = mutableListOf<AppInfo>()
            val seenPackages = mutableSetOf<String>()

            for (resolveInfo in resolveInfos) {
                val pkgName = resolveInfo.activityInfo.packageName
                if (pkgName == context.packageName) continue // Skip our own app
                if (seenPackages.contains(pkgName)) continue
                seenPackages.add(pkgName)

                val label = resolveInfo.loadLabel(pm).toString()
                val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                if (includeSystemApps || !isSystem) {
                    apps.add(AppInfo(packageName = pkgName, appName = label, isSystemApp = isSystem))
                }
            }

            apps.sortedBy { it.appName.lowercase() }
        }
    }
}
