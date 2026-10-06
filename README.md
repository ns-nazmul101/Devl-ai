# Devil AI - Advanced Android Voice Assistant

Devil AI is a modern, production-ready AI personal voice assistant for Android. It understands natural language text and voice commands in both English and Bengali, performs supported phone actions, provides root shell execution when root access is granted, and features a dedicated Permission Manager and device diagnostics suite.

---

## 🌟 Key Features

### 1. 🎙️ AI Voice & Natural Conversations
- **Speech Recognition:** Powered by Android's native `SpeechRecognizer` API with real-time audio RMS waveform visualizer.
- **Text-to-Speech (TTS):** Dynamic voice engine with Bengali (`bn-BD`) and English (`en-US`) support.
- **Female Voice Profile:** Configurable pitch, speech rate, and female voice selection without hardcoding unavailable voices.
- **Dual-Engine Intelligence:** Works seamlessly with Google Gemini 3.5 Flash via REST API (when configured in AI Studio Secrets) and has an intelligent offline Natural Language Understanding (NLU) rule & intent parser that requires zero network connectivity.

### 2. 📱 Phone Control & Hardware Automation
- **App Management:** Open any installed application by name, or close/force-stop applications (via root `am force-stop` or opening App Info for manual stop).
- **Audio Control:** Adjust volume up/down, set specific percentage levels, or mute device audio.
- **Screen Brightness:** Adjust brightness smoothly with `WRITE_SETTINGS` permission, root shell commands, or display settings intent fallback.
- **Flashlight / Torch:** Toggle camera torch directly via `CameraManager`.
- **System Settings:** Direct shortcuts to Wi-Fi, Bluetooth, Display, Sound, and App management.
- **File Management:** Create folders in external/app storage, search files, list directories, and delete files safely.
- **Screenshot Capture:** Capture device display via `DevilAccessibilityService` (Android 9+), root `screencap`, or system shortcut guidance.

### 3. ⚡ Root Superuser Support
- **Detection:** Multi-path detection of `su` binaries (`/system/bin/su`, `/system/xbin/su`, `/sbin/su`, etc.).
- **Authorization:** Requests normal Superuser/Magisk authorization (`su -c id`) with timeout handling.
- **Security Guard:** Blocks destructive commands (`rm -rf`, `dd`, `format`, `reboot`, `shutdown`) from silent execution and enforces an explicit confirmation dialog.
- **Audit History:** Logs all executed shell commands, exit codes, and standard output/error to a persistent Room database.

### 4. 🛡️ Permission Manager
Dedicated screen monitoring:
- Microphone (`RECORD_AUDIO`) [Required]
- Notifications (`POST_NOTIFICATIONS`) [Optional]
- Devil Accessibility Service [Optional]
- Storage & All Files Access (`MANAGE_EXTERNAL_STORAGE`) [Optional]
- Modify System Settings (`WRITE_SETTINGS`) [Optional]
- Camera / Flashlight (`CAMERA`) [Optional]
- Root Superuser Status [Optional]

Each permission displays its Granted/Not Granted status, Required/Optional tag, and one-tap button to open the appropriate system settings screen.

---

## 🏗️ Architecture & Package Structure

```
com.example/
├── MainActivity.kt                  # Main entry point with Edge-to-Edge Navigation
├── ai/
│   ├── CommandParser.kt             # Dual-language intent parser (English & Bengali)
│   ├── DevilAiEngine.kt             # Execution engine & conversational agent
│   ├── DevilIntent.kt               # Sealed intent hierarchy
│   └── GeminiClient.kt              # REST client for Gemini 3.5 Flash
├── control/
│   ├── AppLauncher.kt               # App discovery & lifecycle controller
│   ├── DeviceManager.kt             # Battery, RAM, storage, network diagnostics
│   ├── FileManager.kt               # Folder creation, search, and directory management
│   ├── ScreenshotController.kt      # Screen capture via Accessibility/Root
│   └── SystemSettingsController.kt  # Volume, brightness, torch, settings pages
├── data/
│   ├── db/
│   │   ├── AssistantDao.kt          # Room DAO for messages and action logs
│   │   └── AssistantDatabase.kt     # Room database configuration
│   ├── model/
│   │   ├── ActionLog.kt             # Log entity
│   │   └── ConversationMessage.kt   # Chat entity
│   └── repository/
│       └── AssistantRepository.kt   # Repository pattern implementation
├── permission/
│   └── PermissionManager.kt         # Comprehensive permission states & intent launcher
├── root/
│   └── RootController.kt            # SU binary detection, execution & safety validator
├── service/
│   └── DevilAccessibilityService.kt # Accessibility service for screenshots & navigation
├── ui/
│   ├── components/
│   │   ├── ActionConfirmationDialog.kt # Security confirmation dialog
│   │   ├── AiAvatar.kt              # Cybernetic AI avatar with animated halos
│   │   ├── MessageBubble.kt         # Modern chat bubble with TTS read-aloud
│   │   └── VoiceVisualizer.kt       # Dynamic audio waveform visualizer
│   ├── screens/
│   │   ├── ActionLogsScreen.kt      # Audit log viewer
│   │   ├── AssistantScreen.kt       # Core voice & chat interface
│   │   ├── DeviceStatusScreen.kt    # System diagnostics and telemetry
│   │   ├── PermissionsScreen.kt     # Permission management
│   │   └── SettingsScreen.kt        # Persona, language, voice, and root settings
│   └── theme/
│       ├── Color.kt                 # Obsidian Cyber Dark palette
│       ├── Theme.kt                 # Material 3 DevilAiTheme
│       └── Type.kt                  # Bundled Poppins & Bengali typography
└── voice/
    └── VoiceManager.kt              # SpeechRecognizer & TextToSpeech controller
```

---

## 🚀 Building & Running

1. **Open in Android Studio:** Open the root project folder.
2. **Dependencies:** Gradle sync will resolve all pre-configured dependencies.
3. **Gemini API Key (Optional):** Add `GEMINI_API_KEY=YOUR_KEY` to `.env` or set it in AI Studio Secrets panel.
4. **Run on Device or Emulator:** Minimum SDK: Android 7.0 (API 24), Target SDK: Android 14+ (API 36).
