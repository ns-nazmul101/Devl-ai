package com.example.control

import android.content.Context
import android.os.Build
import android.os.Environment
import com.example.root.RootController
import com.example.service.DevilAccessibilityService
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenshotController(
    private val context: Context,
    private val rootController: RootController
) {
    suspend fun takeScreenshot(hasRoot: Boolean): ControllerResult {
        // 1. Accessibility Service action (Android 9+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val success = DevilAccessibilityService.takeScreenshot()
            if (success) {
                return ControllerResult(true, "Screenshot captured via Devil Accessibility Service.")
            }
        }

        // 2. Root action if available
        if (hasRoot) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val picturesDir = File(Environment.getExternalStorageDirectory(), "Pictures/Screenshots")
            picturesDir.mkdirs()
            val file = File(picturesDir, "devil_ai_$timestamp.png")
            val shellRes = rootController.executeCommand(
                "screencap -p \"${file.absolutePath}\"",
                requireRoot = true
            )
            if (shellRes.exitCode == 0) {
                return ControllerResult(true, "Screenshot captured via Root and saved to ${file.name}")
            }
        }

        // 3. Fallback instruction
        val isA11yEnabled = DevilAccessibilityService.isAccessibilityEnabled(context)
        return if (!isA11yEnabled) {
            ControllerResult(
                false,
                "To take screenshots without root, please enable Devil AI in Accessibility Settings (Go to Permissions > Accessibility)."
            )
        } else {
            ControllerResult(
                false,
                "Screenshot couldn't be triggered automatically. You can also press Power + Volume Down on your phone."
            )
        }
    }
}
