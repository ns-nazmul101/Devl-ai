package com.example.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.root.RootController
import com.example.service.DevilAccessibilityService

data class PermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val isRequired: Boolean,
    val actionLabel: String,
    val intent: Intent? = null
)

class PermissionManager(
    private val context: Context,
    private val rootController: RootController
) {

    fun getPermissionsState(isRootAuthorized: Boolean): List<PermissionItem> {
        val list = mutableListOf<PermissionItem>()

        // 1. Microphone (Required for Voice)
        val micGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        list.add(
            PermissionItem(
                id = "microphone",
                title = "Microphone",
                description = "Required to capture your voice commands and natural speech input.",
                isGranted = micGranted,
                isRequired = true,
                actionLabel = if (micGranted) "Granted" else "Grant Access",
                intent = if (!micGranted) getAppSettingsIntent() else null
            )
        )

        // 2. Accessibility Service (Optional for Screenshot & Phone Global Navigation)
        val a11yGranted = DevilAccessibilityService.isAccessibilityEnabled(context)
        list.add(
            PermissionItem(
                id = "accessibility",
                title = "Devil Accessibility Service",
                description = "Enables taking screenshots, pressing Home/Back, and pulling down quick settings per voice commands.",
                isGranted = a11yGranted,
                isRequired = false,
                actionLabel = if (a11yGranted) "Active" else "Enable in Settings",
                intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        )

        // 3. Storage / File Management
        val storageGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
        val storageIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            getAppSettingsIntent()
        }
        list.add(
            PermissionItem(
                id = "storage",
                title = "Storage & File Access",
                description = "Enables Devil AI to create folders, search files, and manage directories.",
                isGranted = storageGranted,
                isRequired = false,
                actionLabel = if (storageGranted) "Granted" else "Manage Storage",
                intent = if (!storageGranted) storageIntent else null
            )
        )

        // 4. Modify System Settings (Write Settings - for Brightness)
        val writeSettingsGranted = Settings.System.canWrite(context)
        list.add(
            PermissionItem(
                id = "write_settings",
                title = "Modify System Settings",
                description = "Allows Devil AI to adjust screen brightness and display settings.",
                isGranted = writeSettingsGranted,
                isRequired = false,
                actionLabel = if (writeSettingsGranted) "Granted" else "Allow Modification",
                intent = if (!writeSettingsGranted) {
                    Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                } else null
            )
        )

        // 5. Notifications (POST_NOTIFICATIONS on Android 13+)
        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        list.add(
            PermissionItem(
                id = "notifications",
                title = "Notifications",
                description = "Allows Devil AI to show status alerts, reminders, and background completion notices.",
                isGranted = notificationsGranted,
                isRequired = false,
                actionLabel = if (notificationsGranted) "Granted" else "Enable Notifications",
                intent = if (!notificationsGranted) getAppSettingsIntent() else null
            )
        )

        // 6. Camera / Flashlight
        val cameraGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        list.add(
            PermissionItem(
                id = "camera",
                title = "Camera & Flashlight",
                description = "Used to control the device flashlight/torch and launch camera quickly.",
                isGranted = cameraGranted,
                isRequired = false,
                actionLabel = if (cameraGranted) "Granted" else "Grant Camera",
                intent = if (!cameraGranted) getAppSettingsIntent() else null
            )
        )

        // 7. Root Access Status
        val rootBinaryExists = rootController.isRootBinaryPresent()
        val rootStatusTitle = when {
            isRootAuthorized -> "Root Access (Granted)"
            rootBinaryExists -> "Root Available (Not Granted)"
            else -> "Root Access (Not Available)"
        }
        val rootDesc = when {
            isRootAuthorized -> "Devil AI has verified root authorization. Superuser commands can be executed safely."
            rootBinaryExists -> "Root binary was detected. You can request root authorization from Superuser/Magisk."
            else -> "Device is not rooted. Devil AI will use standard Android APIs and Accessibility services."
        }
        list.add(
            PermissionItem(
                id = "root",
                title = rootStatusTitle,
                description = rootDesc,
                isGranted = isRootAuthorized,
                isRequired = false,
                actionLabel = if (isRootAuthorized) "Authorized" else if (rootBinaryExists) "Request Root" else "Unavailable",
                intent = null // Handled via in-app root request
            )
        )

        return list
    }

    private fun getAppSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
