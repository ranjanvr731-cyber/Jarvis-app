package com.example.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class JarvisVoiceService(
    private val context: Context,
    private val onStateChange: (VoiceState) -> Unit,
    private val onRmsChanged: (Float) -> Unit,
    private val onResult: (String) -> Unit
) : TextToSpeech.OnInitListener {

    enum class VoiceState {
        IDLE, INITIALIZING, LISTENING, SPEAKING, ERROR
    }

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    init {
        onStateChange(VoiceState.INITIALIZING)
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("JarvisVoiceService", "TTS English language is not supported.")
                onStateChange(VoiceState.ERROR)
            } else {
                isTtsInitialized = true
                tts?.setPitch(0.85f) // Distinct Tony Stark deep signature style
                tts?.setSpeechRate(1.05f) // Prompt pacing
                onStateChange(VoiceState.IDLE)
            }
        } else {
            Log.e("JarvisVoiceService", "TTS init failure.")
            onStateChange(VoiceState.ERROR)
        }
    }

    fun speak(text: String) {
        if (!isTtsInitialized) return
        onStateChange(VoiceState.SPEAKING)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_tts_id")
        
        // Dynamic reading feedback transition
        val speakDurationMs = (text.length * 62L).coerceIn(1200L, 8000L)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            onStateChange(VoiceState.IDLE)
        }, speakDurationMs)
    }

    fun startListeningSimulated(voicePayload: String) {
        onStateChange(VoiceState.LISTENING)
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var ticks = 0
        val runnable = object : Runnable {
            override fun run() {
                if (ticks < 12) {
                    val rms = (0.2f + Math.sin(ticks * 0.45).toFloat() * 0.7f).coerceIn(0.05f, 1f)
                    onRmsChanged(rms)
                    ticks++
                    handler.postDelayed(this, 120)
                } else {
                    onRmsChanged(0f)
                    onStateChange(VoiceState.IDLE)
                    onResult(voicePayload)
                }
            }
        }
        handler.post(runnable)
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
    }
}
