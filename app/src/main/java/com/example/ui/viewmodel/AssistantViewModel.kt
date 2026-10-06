package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.CommandParser
import com.example.ai.DevilAiEngine
import com.example.ai.GeminiClient
import com.example.ai.PendingActionConfirmation
import com.example.control.AppLauncher
import com.example.control.DeviceManager
import com.example.control.DeviceStatus
import com.example.control.FileManager
import com.example.control.ScreenshotController
import com.example.control.SystemSettingsController
import com.example.data.db.AssistantDatabase
import com.example.data.model.ActionLog
import com.example.data.model.ConversationMessage
import com.example.data.repository.AssistantRepository
import com.example.permission.PermissionItem
import com.example.permission.PermissionManager
import com.example.root.RootController
import com.example.voice.AssistantLanguage
import com.example.voice.VoiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AiStatusState {
    IDLE, LISTENING, THINKING, EXECUTING, COMPLETED, ERROR
}

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AssistantDatabase.getInstance(application)
    private val repository = AssistantRepository(db.assistantDao())
    val rootController = RootController()
    val voiceManager = VoiceManager(application)
    private val deviceManager = DeviceManager(application)
    private val appLauncher = AppLauncher(application, rootController)
    private val settingsController = SystemSettingsController(application, rootController)
    private val fileManager = FileManager(application)
    private val screenshotController = ScreenshotController(application, rootController)
    private val geminiClient = GeminiClient()
    private val commandParser = CommandParser()
    val permissionManager = PermissionManager(application, rootController)

    val aiEngine = DevilAiEngine(
        commandParser = commandParser,
        geminiClient = geminiClient,
        appLauncher = appLauncher,
        settingsController = settingsController,
        fileManager = fileManager,
        screenshotController = screenshotController,
        deviceManager = deviceManager,
        rootController = rootController,
        repository = repository
    )

    // --- StateFlows ---
    val messages: StateFlow<List<ConversationMessage>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val actionLogs: StateFlow<List<ActionLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _aiStatus = MutableStateFlow(AiStatusState.IDLE)
    val aiStatus: StateFlow<AiStatusState> = _aiStatus

    private val _statusText = MutableStateFlow("Idle")
    val statusText: StateFlow<String> = _statusText

    private val _aiName = MutableStateFlow("[Devil]")
    val aiName: StateFlow<String> = _aiName

    private val _currentLanguage = MutableStateFlow(AssistantLanguage.ENGLISH)
    val currentLanguage: StateFlow<AssistantLanguage> = _currentLanguage

    private val _femaleVoiceEnabled = MutableStateFlow(true)
    val femaleVoiceEnabled: StateFlow<Boolean> = _femaleVoiceEnabled

    private val _autoSpeakEnabled = MutableStateFlow(true)
    val autoSpeakEnabled: StateFlow<Boolean> = _autoSpeakEnabled

    private val _isRootAvailable = MutableStateFlow(false)
    val isRootAvailable: StateFlow<Boolean> = _isRootAvailable

    private val _isRootAuthorized = MutableStateFlow(false)
    val isRootAuthorized: StateFlow<Boolean> = _isRootAuthorized

    private val _deviceStatus = MutableStateFlow<DeviceStatus?>(null)
    val deviceStatus: StateFlow<DeviceStatus?> = _deviceStatus

    private val _permissionsList = MutableStateFlow<List<PermissionItem>>(emptyList())
    val permissionsList: StateFlow<List<PermissionItem>> = _permissionsList

    private val _pendingConfirmation = MutableStateFlow<PendingActionConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingActionConfirmation?> = _pendingConfirmation

    val isListening: StateFlow<Boolean> = voiceManager.isListening
    val isSpeaking: StateFlow<Boolean> = voiceManager.isSpeaking
    val audioRmsLevel: StateFlow<Float> = voiceManager.audioRmsLevel

    init {
        checkRootStatus()
        refreshDeviceStatus()
        refreshPermissions()

        // Seed friendly welcome message if empty
        viewModelScope.launch {
            if (repository.allMessages.stateIn(viewModelScope).value.isEmpty()) {
                repository.insertMessage(
                    ConversationMessage(
                        role = "assistant",
                        text = "Hello! I am ${_aiName.value}, your advanced AI voice assistant. I can control your phone, launch apps, adjust brightness and volume, take screenshots, manage files, and execute shell commands. How can I assist you today?",
                        actionTag = "WELCOME"
                    )
                )
            }
        }
    }

    fun checkRootStatus() {
        val present = rootController.isRootBinaryPresent()
        _isRootAvailable.value = present
    }

    fun requestRoot() {
        viewModelScope.launch {
            _statusText.value = "Requesting root..."
            val granted = rootController.requestRootPermission()
            _isRootAuthorized.value = granted
            refreshPermissions()
            _statusText.value = if (granted) "Root Granted" else "Root Denied"
            repository.logAction("Root Access Request", "ROOT_AUTH", if (granted) "Root access granted" else "Root access denied", granted, isRoot = true)
        }
    }

    fun refreshDeviceStatus() {
        _deviceStatus.value = deviceManager.getDeviceStatus()
    }

    fun refreshPermissions() {
        _permissionsList.value = permissionManager.getPermissionsState(_isRootAuthorized.value)
    }

    fun setLanguage(lang: AssistantLanguage) {
        _currentLanguage.value = lang
        voiceManager.currentLanguage = lang
    }

    fun setFemaleVoice(enabled: Boolean) {
        _femaleVoiceEnabled.value = enabled
        voiceManager.femaleVoicePreference = enabled
    }

    fun setAutoSpeak(enabled: Boolean) {
        _autoSpeakEnabled.value = enabled
    }

    fun setAiName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank()) {
            _aiName.value = trimmed
        }
    }

    fun startListening() {
        _aiStatus.value = AiStatusState.LISTENING
        _statusText.value = if (_currentLanguage.value == AssistantLanguage.BENGALI) "শুনছি..." else "Listening..."
        voiceManager.startListening(
            onResult = { recognizedText ->
                _aiStatus.value = AiStatusState.THINKING
                _statusText.value = if (_currentLanguage.value == AssistantLanguage.BENGALI) "চিন্তা করছি..." else "Thinking..."
                processInput(recognizedText, isVoice = true)
            },
            onError = { err ->
                _aiStatus.value = AiStatusState.ERROR
                _statusText.value = err
                viewModelScope.launch {
                    kotlinx.coroutines.delay(2000)
                    _aiStatus.value = AiStatusState.IDLE
                    _statusText.value = "Idle"
                }
            }
        )
    }

    fun stopListening() {
        voiceManager.stopListening()
        _aiStatus.value = AiStatusState.IDLE
        _statusText.value = "Idle"
    }

    fun sendTextCommand(text: String) {
        if (text.isBlank()) return
        processInput(text, isVoice = false)
    }

    private fun processInput(input: String, isVoice: Boolean) {
        viewModelScope.launch {
            // 1. Record user message
            repository.insertMessage(
                ConversationMessage(
                    role = "user",
                    text = input,
                    isVoice = isVoice
                )
            )

            _aiStatus.value = AiStatusState.THINKING
            _statusText.value = if (_currentLanguage.value == AssistantLanguage.BENGALI) "চিন্তা করছি..." else "Thinking..."

            // Short transition for responsiveness
            kotlinx.coroutines.delay(200)

            _aiStatus.value = AiStatusState.EXECUTING
            _statusText.value = if (_currentLanguage.value == AssistantLanguage.BENGALI) "কাজ চলছে..." else "Executing..."

            val result = aiEngine.processCommand(
                input = input,
                aiName = _aiName.value,
                language = _currentLanguage.value,
                isRootEnabled = _isRootAuthorized.value
            )

            if (result.pendingConfirmation != null) {
                _pendingConfirmation.value = result.pendingConfirmation
                _aiStatus.value = AiStatusState.IDLE
                _statusText.value = "Confirmation Required"
                return@launch
            }

            // Store assistant response
            val responseText = if (result.resultSummary.isNotBlank()) {
                "${result.explanation}\n\n${result.resultSummary}"
            } else {
                result.explanation
            }

            repository.insertMessage(
                ConversationMessage(
                    role = "assistant",
                    text = responseText,
                    actionTag = result.actionTag,
                    actionStatus = if (result.isSuccess) "completed" else "failed"
                )
            )

            _aiStatus.value = if (result.isSuccess) AiStatusState.COMPLETED else AiStatusState.ERROR
            _statusText.value = if (result.isSuccess) {
                if (_currentLanguage.value == AssistantLanguage.BENGALI) "সম্পন্ন" else "Completed"
            } else {
                if (_currentLanguage.value == AssistantLanguage.BENGALI) "ব্যর্থ হয়েছে" else "Failed"
            }

            // TTS playback if auto-speak enabled
            if (_autoSpeakEnabled.value) {
                // Speak the concise explanation
                voiceManager.speak(result.explanation)
            }

            // Return to Idle after 3 seconds
            kotlinx.coroutines.delay(3000)
            _aiStatus.value = AiStatusState.IDLE
            _statusText.value = "Idle"
        }
    }

    fun confirmPendingAction() {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        viewModelScope.launch {
            _aiStatus.value = AiStatusState.EXECUTING
            _statusText.value = "Executing confirmed action..."
            val res = pending.onConfirm()
            val text = "${res.explanation}\n\n${res.resultSummary}"
            repository.insertMessage(
                ConversationMessage(
                    role = "assistant",
                    text = text,
                    actionTag = res.actionTag,
                    actionStatus = if (res.isSuccess) "completed" else "failed"
                )
            )
            _aiStatus.value = AiStatusState.COMPLETED
            _statusText.value = "Completed"
            if (_autoSpeakEnabled.value) {
                voiceManager.speak(res.explanation)
            }
            kotlinx.coroutines.delay(3000)
            _aiStatus.value = AiStatusState.IDLE
            _statusText.value = "Idle"
        }
    }

    fun cancelPendingAction() {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        viewModelScope.launch {
            val res = pending.onCancel()
            repository.insertMessage(
                ConversationMessage(
                    role = "assistant",
                    text = "${res.explanation} ${res.resultSummary}",
                    actionTag = res.actionTag,
                    actionStatus = "completed"
                )
            )
            _aiStatus.value = AiStatusState.IDLE
            _statusText.value = "Cancelled"
        }
    }

    fun speakMessage(text: String) {
        voiceManager.speak(text)
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearMessages()
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.shutdown()
    }
}
