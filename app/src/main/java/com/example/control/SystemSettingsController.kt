package com.example.control

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.root.RootController

data class ControllerResult(
    val success: Boolean,
    val message: String
)

class SystemSettingsController(
    private val context: Context,
    private val rootController: RootController
) {
    private var isTorchOn = false

    // --- Volume Control ---
    fun adjustVolume(direction: Int): ControllerResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ControllerResult(false, "Audio service not available.")

        val flag = AudioManager.FLAG_SHOW_UI
        val adjustDir = when {
            direction > 0 -> AudioManager.ADJUST_RAISE
            direction < 0 -> AudioManager.ADJUST_LOWER
            else -> AudioManager.ADJUST_SAME
        }

        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, adjustDir, flag)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val pct = (current * 100) / max
        return ControllerResult(true, "Media volume adjusted to $pct%")
    }

    fun setVolumeLevel(percentage: Int): ControllerResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ControllerResult(false, "Audio service not available.")

        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val clampedPct = percentage.coerceIn(0, 100)
        val targetVolume = (max * clampedPct) / 100

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, AudioManager.FLAG_SHOW_UI)
        return ControllerResult(true, "Media volume set to $clampedPct%")
    }

    fun muteVolume(): ControllerResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ControllerResult(false, "Audio service not available.")

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
        return ControllerResult(true, "Media volume muted.")
    }

    // --- Brightness Control ---
    suspend fun adjustBrightness(delta: Int, hasRoot: Boolean): ControllerResult {
        val currentBrightness = try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (_: Exception) {
            128
        }
        val targetBrightness = (currentBrightness + delta).coerceIn(10, 255)
        return setBrightness(targetBrightness, hasRoot)
    }

    suspend fun setBrightness(brightnessValue: Int, hasRoot: Boolean): ControllerResult {
        val clamped = brightnessValue.coerceIn(5, 255)
        val pct = (clamped * 100) / 255

        // Check if WRITE_SETTINGS is granted
        if (Settings.System.canWrite(context)) {
            try {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    clamped
                )
                return ControllerResult(true, "Screen brightness set to $pct% ($clamped/255)")
            } catch (e: Exception) {
                // fallback to root or settings intent
            }
        }

        // Try root command if available
        if (hasRoot) {
            val rootRes = rootController.executeCommand(
                "settings put system screen_brightness $clamped",
                requireRoot = true
            )
            if (rootRes.exitCode == 0) {
                return ControllerResult(true, "Screen brightness set to $pct% via Root.")
            }
        }

        // Direct user to Write Settings permission or Display settings
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            return ControllerResult(
                false,
                "Permission required: Please grant 'Modify system settings' in the opened screen to allow automatic brightness control."
            )
        } catch (_: Exception) {
            val displayIntent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(displayIntent)
            return ControllerResult(true, "Opened Display Settings to adjust brightness.")
        }
    }

    // --- Flashlight / Torch Control ---
    fun toggleTorch(enable: Boolean? = null): ControllerResult {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return ControllerResult(false, "Camera hardware not accessible for flashlight.")

        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull()
                ?: return ControllerResult(false, "No flashlight camera unit found.")

            val targetState = enable ?: !isTorchOn
            cameraManager.setTorchMode(cameraId, targetState)
            isTorchOn = targetState
            val stateText = if (targetState) "ON" else "OFF"
            return ControllerResult(true, "Flashlight turned $stateText.")
        } catch (e: Exception) {
            return ControllerResult(false, "Flashlight error: ${e.localizedMessage}")
        }
    }

    // --- Settings Pages ---
    fun openSettingsPage(page: String): ControllerResult {
        val intent = when (page.lowercase()) {
            "wifi", "wi-fi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
            "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            "display" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
            "sound", "volume" -> Intent(Settings.ACTION_SOUND_SETTINGS)
            "apps", "applications" -> Intent(Settings.ACTION_APPLICATION_SETTINGS)
            "date", "time" -> Intent(Settings.ACTION_DATE_SETTINGS)
            "security" -> Intent(Settings.ACTION_SECURITY_SETTINGS)
            else -> Intent(Settings.ACTION_SETTINGS)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            ControllerResult(true, "Opening ${page.replaceFirstChar { it.uppercase() }} settings.")
        } catch (e: Exception) {
            ControllerResult(false, "Failed to open settings: ${e.localizedMessage}")
        }
    }
}
