package com.example.ai

sealed class DevilIntent {
    data class OpenApp(val appName: String) : DevilIntent()
    data class CloseApp(val appName: String) : DevilIntent()
    data class VolumeControl(val action: VolumeAction, val level: Int? = null) : DevilIntent()
    data class BrightnessControl(val delta: Int? = null, val level: Int? = null) : DevilIntent()
    object DeviceInfo : DevilIntent()
    data class CreateFolder(val folderName: String) : DevilIntent()
    object TakeScreenshot : DevilIntent()
    data class OpenSettings(val page: String) : DevilIntent()
    data class SearchFiles(val query: String) : DevilIntent()
    data class ToggleTorch(val enable: Boolean? = null) : DevilIntent()
    data class GlobalAction(val action: String) : DevilIntent() // "home", "back", "notifications", "quick_settings"
    data class ExecuteRootCommand(val command: String) : DevilIntent()
    data class Conversational(val userPrompt: String) : DevilIntent()
}

enum class VolumeAction {
    UP, DOWN, MUTE, SET_LEVEL
}
