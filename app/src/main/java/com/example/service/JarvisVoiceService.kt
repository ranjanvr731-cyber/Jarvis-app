package com.example.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.*

class JarvisVoiceService(
    private val context: Context,
    private val onSpeechVolumeChanged: (Float) -> Unit,
    private val onSpeechStateChanged: (VoiceState) -> Unit,
    private val onSpeechResult: (String) -> Unit,
    private val onSpeechError: (String) -> Unit
) {
    private val TAG = "JarvisVoiceService"

    enum class VoiceState {
        IDLE,
        INITIALIZING,
        LISTENING,
        RECOGNIZED,
        SPEAKING,
        ERROR
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private val utteranceId = "JARVIS_VOICE_OUT"

    init {
        initializeTts()
        initializeStt()
    }

    // --- TEXT TO SPEECH (TTS) SETUP ---
    private fun initializeTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Pre-configure elegant JARVIS voice characteristics (UK english is highly sophisticated)
                val result = textToSpeech?.setLanguage(Locale.UK)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    textToSpeech?.setLanguage(Locale.getDefault())
                }
                
                // Fine-tune tone parameters
                textToSpeech?.setPitch(0.95f) // Slightly deeper masculine tone
                textToSpeech?.setSpeechRate(0.98f) // Intelligent, steady pacing
                
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        onSpeechStateChanged(VoiceState.SPEAKING)
                    }

                    override fun onDone(utteranceId: String?) {
                        onSpeechStateChanged(VoiceState.IDLE)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        onSpeechStateChanged(VoiceState.ERROR)
                    }
                })
                isTtsInitialized = true
                Log.d(TAG, "TTS initialized successfully")
            } else {
                Log.e(TAG, "TTS Initialization failed")
            }
        }
    }

    fun speak(text: String) {
        if (!isTtsInitialized) {
            Log.e(TAG, "TTS not ready")
            return
        }
        
        // Stop any active speech before talking
        stopSpeaking()
        
        val params = HashMap<String, String>().apply {
            put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        
        onSpeechStateChanged(VoiceState.SPEAKING)
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params)
    }

    fun stopSpeaking() {
        if (textToSpeech?.isSpeaking == true) {
            textToSpeech?.stop()
            onSpeechStateChanged(VoiceState.IDLE)
        }
    }

    // --- SPEECH TO TEXT (STT) SETUP ---
    private fun initializeStt() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    onSpeechStateChanged(VoiceState.LISTENING)
                }

                override fun onBeginningOfSpeech() {
                    onSpeechStateChanged(VoiceState.LISTENING)
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Pass current speaking decibels to animate voice waves beautifully!
                    // Map rmsdB roughly from [-2, 10] range to [0.0, 1.0] intensity
                    val mapped = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    onSpeechVolumeChanged(mapped)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    onSpeechStateChanged(VoiceState.RECOGNIZED)
                }

                override fun onError(error: Int) {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client resource error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions missing"
                        SpeechRecognizer.ERROR_NETWORK -> "Network issue"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout occurred"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Core engine busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server protocol failed"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Silence timed out"
                        else -> "Internal recognition error"
                    }
                    Log.e(TAG, "Speech Recognition Error: $message")
                    onSpeechStateChanged(VoiceState.ERROR)
                    onSpeechError(message)
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val bestMatch = matches?.firstOrNull()
                    if (!bestMatch.isNullOrBlank()) {
                        onSpeechResult(bestMatch)
                    } else {
                        onSpeechError("Unable to extract vocal commands")
                    }
                    onSpeechStateChanged(VoiceState.IDLE)
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        } else {
            Log.e(TAG, "Speech recognition is not available on this device")
        }
    }

    fun startListening() {
        // Stop any active output voice
        stopSpeaking()
        
        if (speechRecognizer == null) {
            onSpeechError("Speech recognition unavailable on this device")
            return
        }

        // Cancel any active/hung speech session to release mic hardware safely
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "SpeechRecognizer cancel warning", e)
        }

        // Verify microphone permission before recording to prevent AppOps security log errors
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            onSpeechError("Microphone permission has not been granted.")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.UK.toString())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        try {
            onSpeechStateChanged(VoiceState.INITIALIZING)
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start speech recognizer", e)
            onSpeechStateChanged(VoiceState.ERROR)
            onSpeechError(e.localizedMessage ?: "Core binder failed")
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        onSpeechStateChanged(VoiceState.IDLE)
    }

    fun shutdown() {
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
    }
}
