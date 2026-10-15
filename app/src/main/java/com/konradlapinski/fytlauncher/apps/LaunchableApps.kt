package com.konradlapinski.fytlauncher.apps

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

data class LaunchableApp(
    val packageName: String,
    val label: String,
)

@Suppress("DEPRECATION")
fun loadLaunchableApps(packageManager: PackageManager): List<LaunchableApp> {
    val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.queryIntentActivities(
            query,
            PackageManager.ResolveInfoFlags.of(0),
        )
    } else {
        packageManager.queryIntentActivities(query, 0)
    }

    return resolved.mapNotNull { info ->
        val packageName = info.activityInfo?.packageName ?: return@mapNotNull null
        val label = info.loadLabel(packageManager)?.toString()?.trim().orEmpty()
        if (label.isBlank()) null else LaunchableApp(packageName, label)
    }
        .distinctBy(LaunchableApp::packageName)
        .sortedBy { it.label.lowercase() }
}
