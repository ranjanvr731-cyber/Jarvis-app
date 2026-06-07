package com.example.ui

import android.content.Context
import android.hardware.camera2.CameraManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ChatMessage
import com.example.data.GeminiService
import com.example.data.Repository
import com.example.service.JarvisVoiceService
import com.example.service.SystemMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JarvisViewModel(
    private val repository: Repository,
    private val context: Context
) : ViewModel() {

    enum class JarvisContextType {
        GENERAL, WEATHER, NEWS, DIAGNOSTICS, COMMUNICATION, ALERTS
    }

    enum class ActionType { CALL, SMS }

    data class PendingAction(val type: ActionType, val recipientName: String, val detail: String)

    private val _currentContext = MutableStateFlow(JarvisContextType.GENERAL)
    val currentContext: StateFlow<JarvisContextType> = _currentContext.asStateFlow()

    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _isBatteryCharging = MutableStateFlow(false)
    val isBatteryCharging: StateFlow<Boolean> = _isBatteryCharging.asStateFlow()

    private val _totalRamGb = MutableStateFlow(0.0)
    val totalRamGb: StateFlow<Double> = _totalRamGb.asStateFlow()

    private val _availableRamGb = MutableStateFlow(0.0)
    val availableRamGb: StateFlow<Double> = _availableRamGb.asStateFlow()

    private val _flashlightOn = MutableStateFlow(false)
    val flashlightOn: StateFlow<Boolean> = _flashlightOn.asStateFlow()

    private val _pendingAction = MutableStateFlow<PendingAction?>(null)
    val pendingAction: StateFlow<PendingAction?> = _pendingAction.asStateFlow()

    private val _telecomStateMsg = MutableStateFlow("")
    val telecomStateMsg: StateFlow<String> = _telecomStateMsg.asStateFlow()

    private val _voiceState = MutableStateFlow(JarvisVoiceService.VoiceState.INITIALIZING)
    val voiceState: StateFlow<JarvisVoiceService.VoiceState> = _voiceState.asStateFlow()

    private val _rmsVal = MutableStateFlow(0f)
    val rmsVal: StateFlow<Float> = _rmsVal.asStateFlow()

    private val _userName = MutableStateFlow("sir")
    val userName: StateFlow<String> = _userName.asStateFlow()

    val chatMessages = repository.allMessages
    val tasks = repository.allTasks

    private val systemMonitor = SystemMonitor(context)
    private val geminiService = GeminiService()
    
    private var voiceService: JarvisVoiceService? = null

    init {
        updateHardwareTelemetry()
        initializeVoiceService()
        loadUserName()
    }

    private fun loadUserName() {
        viewModelScope.launch {
            val savedName = repository.getMemory("user_name")
            if (savedName != null) {
                _userName.value = savedName
            }
        }
    }

    private fun initializeVoiceService() {
        voiceService = JarvisVoiceService(
            context = context,
            onStateChange = { state -> _voiceState.value = state },
            onRmsChanged = { rms -> _rmsVal.value = rms },
            onResult = { result -> processIncomingInput(result) }
        )
    }

    fun updateHardwareTelemetry() {
        _batteryLevel.value = systemMonitor.getBatteryLevel()
        _isBatteryCharging.value = systemMonitor.isBatteryCharging()
        val mem = systemMonitor.getMemoryInfo()
        _totalRamGb.value = mem.first
        _availableRamGb.value = mem.second
    }

    fun setContext(type: JarvisContextType) {
        _currentContext.value = type
    }

    fun toggleContinuousListening() {
        // Trigger simulated listening for typical prompt configurations:
        val samplePrompts = listOf(
            "Jarvis, active flashlight mode",
            "Are there any pending alerts in Malibu?",
            "What is the system diagnostics rating?",
            "Call Colonel James Rhodes",
            "Text Pepper Potts: Engine diagnostics complete",
            "What is the weather outside the lab?",
            "Add task: Re-align arc core particle injectors"
        )
        val randomPrompt = samplePrompts.random()
        voiceService?.startListeningSimulated(randomPrompt)
    }

    fun executeVocalTrigger() {
        voiceService?.startListeningSimulated("Jarvis, summarize schedule")
    }

    fun processIncomingInput(input: String) {
        if (input.trim().isEmpty()) return

        viewModelScope.launch {
            // Save USER message to local persistent history
            repository.insertMessage(ChatMessage(sender = "USER", content = input))

            val clean = input.lowercase().trim()

            when {
                clean.contains("flashlight on") || clean.contains("torch on") || clean.contains("active flashlight") -> {
                    toggleFlash(true)
                    val reply = "Flashlight core online, ${_userName.value}. Illuminating flight path."
                    saveAndSpeak(reply, JarvisContextType.DIAGNOSTICS)
                }
                clean.contains("flashlight off") || clean.contains("torch off") || clean.contains("extinguish flashlight") -> {
                    toggleFlash(false)
                    val reply = "The physical illumination torch has been successfully deactivated, ${_userName.value}."
                    saveAndSpeak(reply, JarvisContextType.DIAGNOSTICS)
                }
                clean.contains("call ") -> {
                    val recipient = input.substringAfter("call", "Contact").trim()
                    _pendingAction.value = PendingAction(ActionType.CALL, recipient, "Establishing airwave sub-frequency audio channel.")
                    _telecomStateMsg.value = "Request queued for $recipient. Sir, authorization required."
                    val reply = "Queuing secure voice uplink to $recipient. Waiting for your manual approval, ${_userName.value}."
                    saveAndSpeak(reply, JarvisContextType.COMMUNICATION)
                }
                clean.contains("text ") || clean.contains("sms ") -> {
                    val target = input.substringAfter("text").substringAfter("sms").trim()
                    val recipient = target.substringBefore(":", "Contact").trim()
                    val msgBody = target.substringAfter(":", "").trim()
                    _pendingAction.value = PendingAction(ActionType.SMS, recipient, msgBody.ifEmpty { "Transmitting terminal report." })
                    _telecomStateMsg.value = "Secure transcript drafted for $recipient."
                    val reply = "Encryption keys verified. Message compiled for $recipient. Awaiting authorization confirmation, ${_userName.value}."
                    saveAndSpeak(reply, JarvisContextType.COMMUNICATION)
                }
                clean.contains("weather") || clean.contains("meteorological") || clean.contains("temperature") -> {
                    val reply = "Malibu cliffside is experiencing warm, clear breezes at 72 degrees Fahrenheit. Barometric pressure registers perfectly static at 29.92 inches."
                    saveAndSpeak(reply, JarvisContextType.WEATHER)
                }
                clean.contains("news") || clean.contains("feed") || clean.contains("satellite") -> {
                    val reply = "Interspatial sensory channels remain clear. Stark satellite arrays show regional flight lanes operating without any abnormal anomalies."
                    saveAndSpeak(reply, JarvisContextType.NEWS)
                }
                clean.contains("diagnostics") || clean.contains("hardware") || clean.contains("telemetry") -> {
                    updateHardwareTelemetry()
                    val reply = "Core status is outstanding. Power reserve reads ${batteryLevel.value} percent. Thread-space allocated: ${String.format("%.1f", _totalRamGb.value - _availableRamGb.value)} gigabytes used."
                    saveAndSpeak(reply, JarvisContextType.DIAGNOSTICS)
                }
                clean.contains("add task") || clean.contains("add schedule") || clean.contains("reminder ") -> {
                    val title = input.substringAfter("add task").substringAfter("add schedule").substringAfter("reminder").trim()
                    if (title.isNotEmpty()) {
                        repository.insertTask(title = title, details = "Commanded via Speech Synthesizer core", isCompleted = false)
                        val reply = "Affirmative, ${_userName.value}. Adding task parameter: \"$title\" to your active alert schedule grids."
                        saveAndSpeak(reply, JarvisContextType.ALERTS)
                    } else {
                        val reply = "No schedule subject detected. Please specify a title descriptor, sir."
                        saveAndSpeak(reply, JarvisContextType.ALERTS)
                    }
                }
                clean.contains("clear logs") || clean.contains("reset database") || clean.contains("clear conversation") -> {
                    repository.clearAllMessages()
                    val reply = "All log archives cleared from localized core sectors, sir."
                    saveAndSpeak(reply, JarvisContextType.GENERAL)
                }
                clean.contains("my name is") || clean.contains("call me") -> {
                    val name = input.substringAfter("my name is").substringAfter("call me").trim().removeSuffix(".")
                    if (name.isNotEmpty()) {
                        repository.saveMemory("user_name", name)
                        _userName.value = name
                        val reply = "Memory sector re-calibrated. I shall address you as $name going forward."
                        saveAndSpeak(reply, JarvisContextType.GENERAL)
                    } else {
                        val reply = "My registers missed your name input. Pardon, sir."
                        saveAndSpeak(reply, JarvisContextType.GENERAL)
                    }
                }
                else -> {
                    // Pass to generative AI (Gemini REST core)!
                    val systemPrompt = "You are J.A.R.V.I.S., the legendary cybernetic helper and butler of Tony Stark. Speak with absolute refinement, polite dry humor, high technological competence, and complete loyalty. Keep replies relatively concise, styled for tech readouts. Always address the user as ${_userName.value}."
                    _voiceState.value = JarvisVoiceService.VoiceState.INITIALIZING
                    val answer = geminiService.queryJarvisCore(input, systemPrompt)
                    saveAndSpeak(answer, JarvisContextType.GENERAL)
                }
            }
        }
    }

    private suspend fun saveAndSpeak(msg: String, contextType: JarvisContextType) {
        _currentContext.value = contextType
        repository.insertMessage(ChatMessage(sender = "JARVIS", content = msg))
        voiceService?.speak(msg)
    }

    private fun toggleFlash(turnOn: Boolean) {
        _flashlightOn.value = turnOn
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull()
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, turnOn)
            }
        } catch (e: Exception) {
            Log.e("JarvisViewModel", "Flashlight toggle failure: ${e.message}")
        }
    }

    fun confirmPendingAction() {
        val action = _pendingAction.value ?: return
        viewModelScope.launch {
            _telecomStateMsg.value = "${action.type.name} to ${action.recipientName} successfully transmitted, sir."
            val reply = "Security gate authenticating... Credentials cleared. Transmitting ${action.type.name} data stream to ${action.recipientName}."
            _pendingAction.value = null
            saveAndSpeak(reply, JarvisContextType.COMMUNICATION)
        }
    }

    fun cancelPendingAction() {
        val action = _pendingAction.value ?: return
        viewModelScope.launch {
            _telecomStateMsg.value = "Target stream to ${action.recipientName} terminated."
            val reply = "Transmission aborted, ${_userName.value}. Airwave frequencies have been securely closed."
            _pendingAction.value = null
            saveAndSpeak(reply, JarvisContextType.COMMUNICATION)
        }
    }

    fun addTaskManual(title: String) {
        if (title.isEmpty()) return
        viewModelScope.launch {
            repository.insertTask(title = title, details = "Logged via terminal command", isCompleted = false)
            saveAndSpeak("Grid element logged: \"$title\", sir.", JarvisContextType.ALERTS)
        }
    }

    fun toggleTaskStatus(id: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.updateTaskStatus(id, isCompleted)
        }
    }

    fun deleteTask(id: Int) {
        viewModelScope.launch {
            repository.deleteTask(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceService?.release()
    }
}

class JarvisViewModelFactory(
    private val repository: Repository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JarvisViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return JarvisViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
