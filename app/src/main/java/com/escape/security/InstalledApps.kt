package com.escape.security

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

data class InstalledApp(val packageName: String, val label: String)

fun launchableApps(context: Context): List<InstalledApp> = context.packageManager
    .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
    .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(context.packageManager).toString()) }
    .distinctBy { it.packageName }.sortedBy { it.label.lowercase() }

/** Never offer to lock the default home app, Android Settings, or Escape itself. */
fun excludedPackages(context: Context): Set<String> {
    val pm = context.packageManager
    val homes = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
        PackageManager.MATCH_DEFAULT_ONLY).map { it.activityInfo.packageName }
    val settings = pm.resolveActivity(Intent(android.provider.Settings.ACTION_SETTINGS), 0)?.activityInfo?.packageName
    return homes.toSet() + setOfNotNull(context.packageName, settings, "com.android.settings")
}
