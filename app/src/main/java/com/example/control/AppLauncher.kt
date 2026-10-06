package com.example.control

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import com.example.root.RootController

data class InstalledAppInfo(
    val appName: String,
    val packageName: String
)

data class AppActionResult(
    val success: Boolean,
    val message: String,
    val packageName: String? = null
)

class AppLauncher(private val context: Context, private val rootController: RootController) {

    // Common app aliases mapping to well-known package names or intent actions
    private val wellKnownApps = mapOf(
        "youtube" to "com.google.android.youtube",
        "chrome" to "com.android.chrome",
        "camera" to "com.android.camera",
        "settings" to "com.android.settings",
        "maps" to "com.google.android.apps.maps",
        "whatsapp" to "com.whatsapp",
        "calculator" to "com.google.android.calculator",
        "calendar" to "com.google.android.calendar",
        "clock" to "com.google.android.deskclock",
        "gallery" to "com.google.android.apps.photos",
        "photos" to "com.google.android.apps.photos",
        "gmail" to "com.google.android.gm",
        "play store" to "com.android.vending",
        "files" to "com.google.android.documentsui",
        "telegram" to "org.telegram.messenger"
    )

    fun getInstalledApps(): List<InstalledAppInfo> {
        val pm = context.packageManager
        val apps = mutableListOf<InstalledAppInfo>()
        try {
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (app in packages) {
                // Filter to launchable apps
                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    val label = pm.getApplicationLabel(app).toString()
                    apps.add(InstalledAppInfo(label, app.packageName))
                }
            }
        } catch (_: Exception) {}
        return apps.sortedBy { it.appName }
    }

    fun launchApp(query: String): AppActionResult {
        val pm = context.packageManager
        val normalizedQuery = query.trim().lowercase()

        // 1. Check well-known apps map
        for ((name, pkg) in wellKnownApps) {
            if (normalizedQuery.contains(name)) {
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return AppActionResult(true, "Launching ${name.replaceFirstChar { it.uppercase() }} ($pkg)", pkg)
                }
            }
        }

        // 2. Search installed applications
        val installed = getInstalledApps()
        val matchedApp = installed.firstOrNull {
            it.appName.lowercase().contains(normalizedQuery) ||
                    normalizedQuery.contains(it.appName.lowercase()) ||
                    it.packageName.lowercase().contains(normalizedQuery)
        }

        if (matchedApp != null) {
            val launchIntent = pm.getLaunchIntentForPackage(matchedApp.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return AppActionResult(true, "Launching ${matchedApp.appName}", matchedApp.packageName)
            }
        }

        // 3. Special fallback for camera/settings
        if (normalizedQuery.contains("camera")) {
            val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
                return AppActionResult(true, "Opening Camera")
            }
        }

        if (normalizedQuery.contains("setting")) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return AppActionResult(true, "Opening System Settings")
        }

        return AppActionResult(false, "Could not find an app matching \"$query\" on this device.")
    }

    suspend fun stopApp(query: String, hasRoot: Boolean): AppActionResult {
        val normalizedQuery = query.trim().lowercase()
        val installed = getInstalledApps()
        val targetApp = installed.firstOrNull {
            it.appName.lowercase().contains(normalizedQuery) ||
                    normalizedQuery.contains(it.appName.lowercase()) ||
                    it.packageName.lowercase().contains(normalizedQuery)
        } ?: wellKnownApps.entries.firstOrNull { normalizedQuery.contains(it.key) }?.let {
            InstalledAppInfo(it.key, it.value)
        }

        if (targetApp == null) {
            return AppActionResult(false, "Could not identify target app \"$query\" to close.")
        }

        if (hasRoot) {
            val shellResult = rootController.executeCommand("am force-stop ${targetApp.packageName}", requireRoot = true)
            return if (shellResult.exitCode == 0) {
                AppActionResult(true, "Force-stopped ${targetApp.appName} using root privileges.", targetApp.packageName)
            } else {
                AppActionResult(false, "Failed to force-stop ${targetApp.appName}: ${shellResult.error}")
            }
        } else {
            // Android non-root security restricts killing external apps.
            // Direct user to App Info page where Force Stop is available.
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", targetApp.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return AppActionResult(
                true,
                "Opening App Info for ${targetApp.appName}. You can tap 'Force Stop' here (Root access is required to stop apps directly).",
                targetApp.packageName
            )
        }
    }
}
