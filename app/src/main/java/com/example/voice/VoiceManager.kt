package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

enum class AssistantLanguage(val code: String, val displayName: String, val locale: Locale) {
    ENGLISH("en", "English", Locale.US),
    BENGALI("bn", "বাংলা (Bengali)", Locale.forLanguageTag("bn-BD"))
}

class VoiceManager(private val context: Context) {

    // --- State ---
    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _audioRmsLevel = MutableStateFlow(0f)
    val audioRmsLevel: StateFlow<Float> = _audioRmsLevel

    private val _availableVoices = MutableStateFlow<List<String>>(emptyList())
    val availableVoices: StateFlow<List<String>> = _availableVoices

    var currentLanguage: AssistantLanguage = AssistantLanguage.ENGLISH
        set(value) {
            field = value
            applyLanguageAndVoice()
        }

    var femaleVoicePreference: Boolean = true
        set(value) {
            field = value
            applyLanguageAndVoice()
        }

    var speechRate: Float = 1.0f
        set(value) {
            field = value
            tts?.setSpeechRate(value)
        }

    var pitch: Float = 1.15f // Slightly higher pitch for clear & pleasant female-leaning voice
        set(value) {
            field = value
            tts?.setPitch(value)
        }

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                _isTtsReady.value = true
                applyLanguageAndVoice()
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
            }
        }
    }

    private fun applyLanguageAndVoice() {
        val engine = tts ?: return
        val targetLocale = currentLanguage.locale

        val result = engine.setLanguage(targetLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to default if specific country locale is not available
            if (currentLanguage == AssistantLanguage.BENGALI) {
                engine.setLanguage(Locale.forLanguageTag("bn"))
            } else {
                engine.setLanguage(Locale.US)
            }
        }

        // Voice selection: search for female voice if requested
        try {
            val voices: Set<Voice>? = engine.voices
            if (!voices.isNullOrEmpty()) {
                _availableVoices.value = voices.map { it.name }
                val targetVoice = if (femaleVoicePreference) {
                    voices.firstOrNull { voice ->
                        voice.locale.language == targetLocale.language &&
                                (voice.name.contains("female", ignoreCase = true) ||
                                        voice.name.contains("#female", ignoreCase = true) ||
                                        voice.name.contains("-sfg-", ignoreCase = true) ||
                                        voice.name.contains("en-us-x-sfg", ignoreCase = true))
                    } ?: voices.firstOrNull { it.locale.language == targetLocale.language }
                } else {
                    voices.firstOrNull { it.locale.language == targetLocale.language }
                }

                if (targetVoice != null) {
                    engine.voice = targetVoice
                }
            }
        } catch (_: Exception) {}

        // Set pitch
        // If female preference is set and device did not have an explicit named female voice,
        // pitch adjustment provides a clear female-style voice!
        val effectivePitch = if (femaleVoicePreference) (pitch.coerceAtLeast(1.2f)) else pitch
        engine.setPitch(effectivePitch)
        engine.setSpeechRate(speechRate)
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!_isTtsReady.value || text.isBlank()) {
            onDone?.invoke()
            return
        }

        stopSpeaking()

        val utteranceId = "devil_ai_${System.currentTimeMillis()}"
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    // --- Speech Recognition ---
    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition is not supported or enabled on this device/emulator.")
            return
        }

        stopListening()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                }

                override fun onBeginningOfSpeech() {
                    _isListening.value = true
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Normalize -2dB to +10dB into 0.0 to 1.0 range
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.1f, 1f)
                    _audioRmsLevel.value = normalized
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                    _audioRmsLevel.value = 0f
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    _audioRmsLevel.value = 0f
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                        SpeechRecognizer.ERROR_CLIENT -> "Client error in speech recognition."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                        SpeechRecognizer.ERROR_NETWORK -> "Network error occurred."
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout."
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please speak again."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy."
                        SpeechRecognizer.ERROR_SERVER -> "Server error occurred."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
                        else -> "Speech recognition error code: $error"
                    }
                    onError(message)
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    _audioRmsLevel.value = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val bestMatch = matches?.firstOrNull() ?: ""
                    if (bestMatch.isNotBlank()) {
                        onResult(bestMatch)
                    } else {
                        onError("Could not understand speech.")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (currentLanguage == AssistantLanguage.BENGALI) "bn-BD" else "en-US")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, if (currentLanguage == AssistantLanguage.BENGALI) "bn-BD" else "en-US")
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
            onError("Failed to start listening: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        _isListening.value = false
        _audioRmsLevel.value = 0f
    }

    fun shutdown() {
        stopSpeaking()
        stopListening()
        tts?.shutdown()
        tts = null
    }
}
