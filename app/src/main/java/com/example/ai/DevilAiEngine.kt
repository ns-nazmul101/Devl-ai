package com.example.ai

import com.example.control.AppLauncher
import com.example.control.DeviceManager
import com.example.control.FileManager
import com.example.control.ScreenshotController
import com.example.control.SystemSettingsController
import com.example.data.repository.AssistantRepository
import com.example.root.RootController
import com.example.service.DevilAccessibilityService
import com.example.voice.AssistantLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PendingActionConfirmation(
    val title: String,
    val description: String,
    val warning: String,
    val onConfirm: suspend () -> AiExecutionResult,
    val onCancel: suspend () -> AiExecutionResult
)

data class AiExecutionResult(
    val explanation: String,
    val resultSummary: String,
    val actionTag: String? = null,
    val isSuccess: Boolean = true,
    val pendingConfirmation: PendingActionConfirmation? = null
)

class DevilAiEngine(
    private val commandParser: CommandParser,
    private val geminiClient: GeminiClient,
    private val appLauncher: AppLauncher,
    private val settingsController: SystemSettingsController,
    private val fileManager: FileManager,
    private val screenshotController: ScreenshotController,
    private val deviceManager: DeviceManager,
    private val rootController: RootController,
    private val repository: AssistantRepository
) {

    suspend fun processCommand(
        input: String,
        aiName: String,
        language: AssistantLanguage,
        isRootEnabled: Boolean
    ): AiExecutionResult = withContext(Dispatchers.IO) {
        val intent = commandParser.parse(input)
        val isBn = language == AssistantLanguage.BENGALI

        when (intent) {
            is DevilIntent.OpenApp -> {
                val appName = intent.appName
                val explanation = if (isBn) "$appName অ্যাপ্লিকেশন খোলা হচ্ছে..." else "Opening $appName on your phone..."
                val launchRes = appLauncher.launchApp(appName)
                repository.logAction("Open App: $appName", "OPEN_APP", launchRes.message, launchRes.success)
                AiExecutionResult(
                    explanation = explanation,
                    resultSummary = launchRes.message,
                    actionTag = "OPEN_APP",
                    isSuccess = launchRes.success
                )
            }

            is DevilIntent.CloseApp -> {
                val appName = intent.appName
                val explanation = if (isBn) "$appName অ্যাপ্লিকেশন বন্ধ করার চেষ্টা করা হচ্ছে..." else "Attempting to close $appName..."
                val stopRes = appLauncher.stopApp(appName, hasRoot = isRootEnabled)
                repository.logAction("Close App: $appName", "CLOSE_APP", stopRes.message, stopRes.success, isRoot = isRootEnabled)
                AiExecutionResult(
                    explanation = explanation,
                    resultSummary = stopRes.message,
                    actionTag = "CLOSE_APP",
                    isSuccess = stopRes.success
                )
            }

            is DevilIntent.VolumeControl -> {
                val explanation = if (isBn) "ভলিউম পরিবর্তন করা হচ্ছে..." else "Adjusting device audio volume..."
                val res = when (intent.action) {
                    VolumeAction.UP -> settingsController.adjustVolume(1)
                    VolumeAction.DOWN -> settingsController.adjustVolume(-1)
                    VolumeAction.MUTE -> settingsController.muteVolume()
                    VolumeAction.SET_LEVEL -> settingsController.setVolumeLevel(intent.level ?: 50)
                }
                repository.logAction("Volume Control", "VOLUME", res.message, res.success)
                AiExecutionResult(
                    explanation = explanation,
                    resultSummary = res.message,
                    actionTag = "VOLUME",
                    isSuccess = res.success
                )
            }

            is DevilIntent.BrightnessControl -> {
                val explanation = if (isBn) "স্ক্রিনের উজ্জ্বলতা পরিবর্তন করা হচ্ছে..." else "Configuring screen brightness..."
                val res = if (intent.level != null) {
                    val rawVal = (intent.level * 255) / 100
                    settingsController.setBrightness(rawVal, hasRoot = isRootEnabled)
                } else {
                    settingsController.adjustBrightness(intent.delta ?: -30, hasRoot = isRootEnabled)
                }
                repository.logAction("Brightness Control", "BRIGHTNESS", res.message, res.success, isRoot = isRootEnabled)
                AiExecutionResult(
                    explanation = explanation,
                    resultSummary = res.message,
                    actionTag = "BRIGHTNESS",
                    isSuccess = res.success
                )
            }

            is DevilIntent.DeviceInfo -> {
                val explanation = if (isBn) "আপনার ডিভাইসের তথ্য সংগ্রহ করা হচ্ছে..." else "Gathering device diagnostics and system status..."
                val status = deviceManager.getDeviceStatus()
                val summary = if (isBn) {
                    "মডেল: ${status.manufacturer} ${status.deviceModel} (Android ${status.androidVersion})\n" +
                            "ব্যাটারি: ${status.batteryLevel}% (${if (status.isCharging) "চার্জিং" else "ব্যাটারি চালিত"})\n" +
                            "র‍্যাম: ${status.ramUsedMb} MB / ${status.ramTotalMb} MB\n" +
                            "স্টোরেজ: ${status.storageUsedGb} GB / ${status.storageTotalGb} GB\n" +
                            "নেটওয়ার্ক: ${status.networkType} (IP: ${status.ipAddress})"
                } else {
                    "Model: ${status.manufacturer} ${status.deviceModel} (Android ${status.androidVersion}, API ${status.apiLevel})\n" +
                            "Battery: ${status.batteryLevel}% (${if (status.isCharging) "Charging" else "Discharging"})\n" +
                            "RAM Usage: ${status.ramUsedMb} MB / ${status.ramTotalMb} MB\n" +
                            "Internal Storage: ${status.storageUsedGb} GB / ${status.storageTotalGb} GB\n" +
                            "Network: ${status.networkType} (IP: ${status.ipAddress})"
                }
                repository.logAction("Device Diagnostics", "DEVICE_INFO", summary, true)
                AiExecutionResult(
                    explanation = explanation,
                    resultSummary = summary,
                    actionTag = "DEVICE_INFO",
                    isSuccess = true
                )
            }

            is DevilIntent.CreateFolder -> {
                val folderName = intent.folderName
                val explanation = if (isBn) "\"$folderName\" ফোল্ডার তৈরি করা হচ্ছে..." else "Creating folder named \"$folderName\"..."
                val res = fileManager.createFolder(folderName)
                repository.logAction("Create Folder: $folderName", "FILE_OP", res.message, res.success)
                AiExecutionResult(
                    explanation = explanation,
                    resultSummary = res.message,
                    actionTag = "FILE_OP",
                    isSuccess = res.success
                )
            }

            is DevilIntent.TakeScreenshot -> {
                val explanation = if (isBn) "স্ক্রিনশট নেওয়ার প্রক্রিয়া শুরু হচ্ছে..." else "Capturing screen display..."
                val res = screenshotController.takeScreenshot(hasRoot = isRootEnabled)
                repository.logAction("Take Screenshot", "SCREENSHOT", res.message, res.success, isRoot = isRootEnabled)
                AiExecutionResult(
                    explanation = explanation,
                    resultSummary = res.message,
                    actionTag = "SCREENSHOT",
                    isSuccess = res.success
                )
            }

            is DevilIntent.OpenSettings -> {
                val page = intent.page
                val explanation = if (isBn) "$page সেটিংস পৃষ্ঠা খোলা হচ্ছে..." else "Opening $page settings..."
                val res = settingsController.openSettingsPage(page)
                repository.logAction("Open Settings: $page", "SETTINGS", res.message, res.success)
                AiExecutionResult(
                    explanation = explanation,
                    resultSummary = res.message,
                    actionTag = "SETTINGS",
                    isSuccess = res.success
                )
            }

            is DevilIntent.SearchFiles -> {
                val query = intent.query
                val explanation = if (isBn) "\"$query\" ফাইল খোঁজা হচ্ছে..." else "Searching files matching \"$query\"..."
                val matches = fileManager.searchFiles(query)
                val summary = if (matches.isEmpty()) {
                    if (isBn) "\"$query\" সম্পর্কিত কোনো ফাইল পাওয়া যায়নি।" else "No files found matching \"$query\" in storage."
                } else {
                    val count = matches.size
                    val listStr = matches.take(5).joinToString("\n") { "• ${it.name} (${if (it.isDirectory) "Folder" else "${it.sizeBytes / 1024} KB"})" }
                    if (isBn) "$count টি ফাইল পাওয়া গেছে:\n$listStr" else "Found $count matching items:\n$listStr"
                }
                repository.logAction("Search Files: $query", "FILE_SEARCH", summary, true)
                AiExecutionResult(
                    explanation = explanation,
                    resultSummary = summary,
                    actionTag = "FILE_SEARCH",
                    isSuccess = true
                )
            }

            is DevilIntent.ToggleTorch -> {
                val explanation = if (isBn) "ফ্ল্যাশলাইট টগল করা হচ্ছে..." else "Toggling device flashlight..."
                val res = settingsController.toggleTorch(intent.enable)
                repository.logAction("Flashlight Toggle", "TORCH", res.message, res.success)
                AiExecutionResult(
                    explanation = explanation,
                    resultSummary = res.message,
                    actionTag = "TORCH",
                    isSuccess = res.success
                )
            }

            is DevilIntent.GlobalAction -> {
                val actionName = intent.action
                val explanation = if (isBn) "সিস্টেম অ্যাকশন এক্সিকিউট করা হচ্ছে: $actionName" else "Executing global system action: $actionName"
                val success = when (actionName) {
                    "home" -> DevilAccessibilityService.pressHome()
                    "back" -> DevilAccessibilityService.pressBack()
                    "notifications" -> DevilAccessibilityService.openNotifications()
                    "quick_settings" -> DevilAccessibilityService.openQuickSettings()
                    else -> false
                }
                val msg = if (success) "Action $actionName executed." else "Accessibility Service is required to perform global navigation actions."
                repository.logAction("Global Action: $actionName", "SYSTEM_NAV", msg, success)
                AiExecutionResult(
                    explanation = explanation,
                    resultSummary = msg,
                    actionTag = "SYSTEM_NAV",
                    isSuccess = success
                )
            }

            is DevilIntent.ExecuteRootCommand -> {
                val command = intent.command
                // Check if command is dangerous
                if (rootController.isDangerousCommand(command)) {
                    val explanation = if (isBn) "সতর্কতা: সম্ভাব্য বিপজ্জনক শেল কমান্ড শনাক্ত হয়েছে!" else "Security Alert: High-risk root command detected!"
                    return@withContext AiExecutionResult(
                        explanation = explanation,
                        resultSummary = "Confirmation required before running dangerous command: \"$command\"",
                        actionTag = "ROOT_CONFIRM",
                        isSuccess = false,
                        pendingConfirmation = PendingActionConfirmation(
                            title = "High-Risk Root Command",
                            description = "You requested to execute: $command",
                            warning = "This command could modify system partitions, wipe data, or restart the device. Proceed only if you are certain.",
                            onConfirm = {
                                val execRes = rootController.executeCommand(command, requireRoot = true)
                                val out = if (execRes.exitCode == 0) "Success (Exit ${execRes.exitCode}):\n${execRes.output}" else "Failed (Exit ${execRes.exitCode}):\n${execRes.error.ifBlank { execRes.output }}"
                                repository.logAction(command, "ROOT_SHELL", out, execRes.exitCode == 0, isRoot = true)
                                AiExecutionResult(
                                    explanation = "Executed authorized root command.",
                                    resultSummary = out,
                                    actionTag = "ROOT_SHELL",
                                    isSuccess = execRes.exitCode == 0
                                )
                            },
                            onCancel = {
                                AiExecutionResult(
                                    explanation = "Command cancelled by user.",
                                    resultSummary = "Execution of root command \"$command\" was aborted safely.",
                                    actionTag = "ROOT_SHELL",
                                    isSuccess = false
                                )
                            }
                        )
                    )
                }

                val explanation = if (isBn) "রুট কমান্ড এক্সিকিউট করা হচ্ছে: $command" else "Executing authorized root command: $command..."
                val res = rootController.executeCommand(command, requireRoot = isRootEnabled)
                val out = if (res.exitCode == 0) {
                    "Success (Exit ${res.exitCode}):\n${res.output.ifBlank { "(No output)" }}"
                } else {
                    "Failed (Exit ${res.exitCode}):\n${res.error.ifBlank { res.output.ifBlank { "Execution error" } }}"
                }
                repository.logAction(command, "ROOT_SHELL", out, res.exitCode == 0, isRoot = isRootEnabled)
                AiExecutionResult(
                    explanation = explanation,
                    resultSummary = out,
                    actionTag = "ROOT_SHELL",
                    isSuccess = res.exitCode == 0
                )
            }

            is DevilIntent.Conversational -> {
                val prompt = intent.userPrompt
                val systemPrompt = "You are $aiName, an advanced, highly intelligent Android personal voice assistant. You speak fluently in ${if (isBn) "Bengali" else "English"}. Provide helpful, concise, friendly, and accurate answers. Keep responses voice-friendly (under 3 sentences when possible)."

                // Try Gemini API first if configured
                val geminiResp = geminiClient.generateResponse(prompt, systemPrompt)
                val finalAnswer = if (!geminiResp.isNullOrBlank()) {
                    geminiResp.trim()
                } else {
                    // Intelligent contextual fallback
                    generateLocalAnswer(prompt, aiName, isBn)
                }

                repository.logAction("Conversation", "CHAT", finalAnswer, true)
                AiExecutionResult(
                    explanation = if (isBn) "$aiName চিন্তা করছে এবং উত্তর দিচ্ছে..." else "$aiName responding...",
                    resultSummary = finalAnswer,
                    actionTag = "CHAT",
                    isSuccess = true
                )
            }
        }
    }

    private fun generateLocalAnswer(prompt: String, aiName: String, isBn: Boolean): String {
        val p = prompt.lowercase()
        return if (isBn) {
            when {
                p.contains("কে তুমি") || p.contains("তোমার নাম") || p.contains("পরিচয়") ->
                    "আমি $aiName, আপনার ব্যক্তিগত অ্যান্ড্রয়েড এআই ভয়েস অ্যাসিস্ট্যান্ট। আমি আপনার ফোন নিয়ন্ত্রণ, অ্যাপ পরিচালনা, ফাইল তৈরি ও রুট কমান্ড সম্পন্ন করতে পারি।"
                p.contains("কেমন আছো") || p.contains("কি খবর") ->
                    "আমি প্রস্তুত এবং দারুণ কাজ করছি! আপনি আজ কী করতে চান?"
                p.contains("ধন্যবাদ") || p.contains("থ্যাঙ্কস") ->
                    "আপনাকে স্বাগতম! যেকোনো সময় আমাকে ডাকতে পারেন।"
                p.contains("কি করতে পারো") || p.contains("সাহায্য") ->
                    "আমি অ্যাপ খুলতে পারি, ভলিউম ও উজ্জ্বলতা সমন্বয় করতে পারি, স্ক্রিনশট নিতে পারি, ডিভাইস তথ্য দেখতে পারি, ফাইল অনুসন্ধান করতে পারি এবং রুট কমান্ড চালাতে পারি।"
                else ->
                    "আমি আপনার কমান্ড বুঝেছি: \"$prompt\"। আমাকে যেকোনো অ্যাপ খুলতে, উজ্জ্বলতা বা ভলিউম বদলাতে, স্ক্রিনশট নিতে বা সেটিংস খুলতে বলুন।"
            }
        } else {
            when {
                p.contains("who are you") || p.contains("your name") || p.contains("what are you") ->
                    "I am $aiName, your advanced personal Android voice assistant. I can control device hardware, manage files, launch apps, and execute authorized commands."
                p.contains("how are you") || p.contains("how's it going") ->
                    "I'm operating at peak performance! How can I assist you with your phone today?"
                p.contains("thank") ->
                    "You're very welcome! I'm always here to help."
                p.contains("what can you do") || p.contains("help") || p.contains("features") ->
                    "I can launch or stop apps, adjust brightness and volume, capture screenshots, manage storage folders, check battery & RAM, toggle flashlight, and execute shell commands."
                else ->
                    "I am standing by to assist. You can say things like \"Open YouTube\", \"Turn brightness down\", \"Show device info\", \"Take a screenshot\", or \"Create a folder named Test\"."
            }
        }
    }
}
