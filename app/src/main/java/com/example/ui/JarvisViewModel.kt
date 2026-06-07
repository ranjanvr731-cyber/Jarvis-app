package com.example.ui

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ChatMessage
import com.example.data.GeminiService
import com.example.data.JarvisRepository
import com.example.data.Task
import com.example.data.UserMemory
import com.example.service.JarvisVoiceService
import com.example.service.SystemMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

class JarvisViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "JarvisViewModel"

    private val db = AppDatabase.getDatabase(application)
    private val repository = JarvisRepository(db)
    private val systemMonitor = SystemMonitor(application)
    private var voiceService: JarvisVoiceService? = null

    // Monitoring job
    private var telemetryJob: Job? = null
    
    // UI reactive states
    private val _isOnlineMode = MutableStateFlow(true)
    val isOnlineMode: StateFlow<Boolean> = _isOnlineMode.asStateFlow()

    private val _isContinuousListening = MutableStateFlow(false)
    val isContinuousListening: StateFlow<Boolean> = _isContinuousListening.asStateFlow()

    private val _voiceState = MutableStateFlow(JarvisVoiceService.VoiceState.IDLE)
    val voiceState: StateFlow<JarvisVoiceService.VoiceState> = _voiceState.asStateFlow()

    private val _voiceRmsLevel = MutableStateFlow(0f)
    val voiceRmsLevel: StateFlow<Float> = _voiceRmsLevel.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _memories = MutableStateFlow<List<UserMemory>>(emptyList())
    val memories: StateFlow<List<UserMemory>> = _memories.asStateFlow()

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    // Dashboard Telemetries
    private val _batteryLevel = MutableStateFlow(0)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _isBatteryCharging = MutableStateFlow(false)
    val isBatteryCharging: StateFlow<Boolean> = _isBatteryCharging.asStateFlow()

    private val _availableRamGb = MutableStateFlow(0.0)
    val availableRamGb: StateFlow<Double> = _availableRamGb.asStateFlow()

    private val _totalRamGb = MutableStateFlow(1.0)
    val totalRamGb: StateFlow<Double> = _totalRamGb.asStateFlow()

    private val _flashlightOn = MutableStateFlow(false)
    val flashlightOn: StateFlow<Boolean> = _flashlightOn.asStateFlow()

    private val _musicVolume = MutableStateFlow(0)
    val musicVolume: StateFlow<Int> = _musicVolume.asStateFlow()

    private val _userName = MutableStateFlow("Sir")
    val userName: StateFlow<String> = _userName.asStateFlow()

    // Confirmation flows
    data class PendingAction(
        val actionId: Long = System.currentTimeMillis(),
        val type: ActionType,
        val recipientName: String,
        val number: String,
        val argument: String? = null
    )
    enum class ActionType { CALL, SMS, ALARM, TIMER }

    private val _pendingAction = MutableStateFlow<PendingAction?>(null)
    val pendingAction: StateFlow<PendingAction?> = _pendingAction.asStateFlow()

    private val _telecomStateMsg = MutableStateFlow("")
    val telecomStateMsg: StateFlow<String> = _telecomStateMsg.asStateFlow()

    // Wake word detection buffer
    private var isWakeWordTriggered = false

    init {
        // Load initial values from repository Flow bounds
        viewModelScope.launch {
            repository.chatHistory.collect { messages ->
                _chatMessages.value = messages
            }
        }
        viewModelScope.launch {
            repository.allMemories.collect { memList ->
                _memories.value = memList
                val nameObj = memList.firstOrNull { it.key == "user_name" }
                _userName.value = nameObj?.value ?: "Sir"
            }
        }
        viewModelScope.launch {
            repository.allTasks.collect { taskList ->
                _tasks.value = taskList
            }
        }

        // Setup dynamic telemetry polling
        startTelemetryMonitoring()

        // Init Voice Services with functional handlers
        voiceService = JarvisVoiceService(
            context = application,
            onSpeechVolumeChanged = { rms ->
                _voiceRmsLevel.value = rms
            },
            onSpeechStateChanged = { state ->
                val oldState = _voiceState.value
                _voiceState.value = state
                if (state != JarvisVoiceService.VoiceState.LISTENING) {
                    _voiceRmsLevel.value = 0f
                }
                // When TTS speech finishes (transition from SPEAKING to IDLE):
                if (oldState == JarvisVoiceService.VoiceState.SPEAKING && state == JarvisVoiceService.VoiceState.IDLE) {
                    checkAndResumeListening(500L)
                }
            },
            onSpeechResult = { vocalTxt ->
                processIncomingInput(vocalTxt)
                checkAndResumeListening(1500L) // Wait slightly for any potential TTS triggers to register
            },
            onSpeechError = { errorMsg ->
                _telecomStateMsg.value = "Voice Sensor: $errorMsg"
                viewModelScope.launch {
                    delay(3000)
                    _telecomStateMsg.value = ""
                }
                
                // If permission is denied/missing, disarm system to prevent infinite retries hammering AppOps
                if (errorMsg.contains("permission", ignoreCase = true)) {
                    _isContinuousListening.value = false
                } else {
                    checkAndResumeListening(1500L) // Restart after timeout or error in continuous listening mode
                }
            }
        )

        // Intro message check
        viewModelScope.launch {
            delay(1000)
            val count = chatMessages.value.size
            if (count == 0) {
                val intro = "Vocal protocol online. JARVIS system initialized, sir. Secondary sub-sectors are operational. Ready for your instructions."
                repository.addChatMessage("jarvis", intro)
                voiceService?.speak(intro)
            }
        }
    }

    private fun startTelemetryMonitoring() {
        telemetryJob?.cancel()
        telemetryJob = viewModelScope.launch {
            while (true) {
                _batteryLevel.value = systemMonitor.getBatteryLevel()
                _isBatteryCharging.value = systemMonitor.isBatteryCharging()
                _availableRamGb.value = systemMonitor.getAvailableMemoryGb()
                _totalRamGb.value = systemMonitor.getTotalMemoryGb()
                _flashlightOn.value = systemMonitor.isFlashlightActive()
                _musicVolume.value = systemMonitor.getVolumePercent()
                delay(3000) // update every 3 seconds
            }
        }
    }

    private fun checkAndResumeListening(delayMs: Long = 1000L) {
        viewModelScope.launch {
            delay(delayMs)
            if (_isContinuousListening.value) {
                val currentState = _voiceState.value
                if (currentState != JarvisVoiceService.VoiceState.SPEAKING &&
                    currentState != JarvisVoiceService.VoiceState.LISTENING &&
                    currentState != JarvisVoiceService.VoiceState.INITIALIZING) {
                    voiceService?.startListening()
                }
            }
        }
    }

    fun toggleOnlineMode() {
        _isOnlineMode.update { !it }
        val statusText = if (_isOnlineMode.value) "Online database cores synced, Gemini protocol activated." else "Offline backup routines configured. Secondary local network active."
        speakAndLogJarvisResponse(statusText)
    }

    fun toggleContinuousListening() {
        _isContinuousListening.update { !it }
        if (_isContinuousListening.value) {
            _telecomStateMsg.value = "Continuous sensors armed: 'Hey Jarvis'"
            voiceService?.startListening()
        } else {
            _telecomStateMsg.value = ""
            voiceService?.stopListening()
        }
    }

    fun executeVocalTrigger() {
        voiceService?.startListening()
    }

    // Process all strings parsed from inputs or speech recognizers!
    fun processIncomingInput(rawInput: String) {
        val input = rawInput.trim()
        if (input.isEmpty()) return

        // Wake word intercept block for background/continuous listening
        if (_isContinuousListening.value) {
            val normalized = input.lowercase()
            val wakeWordPresent = normalized.contains("hey jarvis") || normalized.contains("jarvis")
            
            if (!isWakeWordTriggered && !wakeWordPresent) {
                // Not triggered, and user didn't speak the wake word.
                // Log it as ambient and ignore to avoid accidental trigger.
                Log.d(TAG, "Ignoring non-target ambient background speech: $input")
                return
            }
            
            if (wakeWordPresent) {
                // Strip the wake word out to see if there is a command following it
                val strippedInput = input
                    .replace("hey jarvis", "", ignoreCase = true)
                    .replace("jarvis", "", ignoreCase = true)
                    .replace("hey", "", ignoreCase = true)
                    .trim()
                
                if (strippedInput.isNotEmpty() && strippedInput.length > 2) {
                    // User spoke wake word AND a command in one breath: "Hey Jarvis, turn on flashlight" -> Execute command immediately
                    isWakeWordTriggered = false
                    // Save User Message to database
                    viewModelScope.launch {
                        repository.addChatMessage("user", input)
                    }
                    executeProcessedCommand(strippedInput)
                    return
                } else {
                    // User just said the wake word e.g., "Hey Jarvis"
                    isWakeWordTriggered = true
                    // Save User Message to database
                    viewModelScope.launch {
                        repository.addChatMessage("user", input)
                    }
                    val promptResponse = "Ready and waiting, ${userName.value}. Speak, I am listening."
                    speakAndLogJarvisResponse(promptResponse)
                    return
                }
            }
        }

        // Save User Message to database
        viewModelScope.launch {
            repository.addChatMessage("user", input)
        }

        // Direct command entry or already triggered
        isWakeWordTriggered = false
        executeProcessedCommand(input)
    }

    private fun executeProcessedCommand(input: String) {
        // Standard Intent parsing (flashlight, calling, volume, timer, custom tasks)
        val lower = input.lowercase()

        // 1. FLASHLIGHT CONTROL
        if (lower.contains("flashlight on") || lower.contains("turn on flashlight") || lower.contains("enable torch") || lower.contains("lumos")) {
            val ok = systemMonitor.setFlashlight(true)
            _flashlightOn.value = systemMonitor.isFlashlightActive()
            val text = if (ok) "Flashlight systems deployed, sir." else "Flashlight sector failure, unable to map hardware driver."
            speakAndLogJarvisResponse(text)
            return
        }
        if (lower.contains("flashlight off") || lower.contains("turn off flashlight") || lower.contains("disable torch") || lower.contains("nox")) {
            val ok = systemMonitor.setFlashlight(false)
            _flashlightOn.value = systemMonitor.isFlashlightActive()
            val text = if (ok) "Flashlight systems secured, sir." else "Failure locking torch drivers."
            speakAndLogJarvisResponse(text)
            return
        }

        // 2. VOLUME CONTROL
        if (lower.contains("volume")) {
            val numberPattern = Pattern.compile("(\\d+)%")
            val matcher = numberPattern.matcher(lower)
            if (matcher.find()) {
                val foundPercent = matcher.group(1)?.toIntOrNull() ?: 50
                val ok = systemMonitor.setVolumePercent(foundPercent)
                _musicVolume.value = systemMonitor.getVolumePercent()
                val text = if (ok) "Audio stream adjusted to $foundPercent percent, sir." else "Hardware audio bus locked."
                speakAndLogJarvisResponse(text)
                return
            } else if (lower.contains("mute")) {
                systemMonitor.setVolumePercent(0)
                _musicVolume.value = 0
                speakAndLogJarvisResponse("Audio outputs muted, sir.")
                return
            } else if (lower.contains("max") || lower.contains("loud")) {
                systemMonitor.setVolumePercent(100)
                _musicVolume.value = 100
                speakAndLogJarvisResponse("Amplify stream deployed to maximum capacity, sir.")
                return
            }
        }

        // 3. CALL PHONE DIAL FLOW
        if (lower.startsWith("call ") || lower.contains("dial ")) {
            val contact = input.substringAfter("call ").substringAfter("dial ").trim()
            val number = extractEstimatedPhoneNumber(contact)
            
            // Arm calling flow
            _pendingAction.value = PendingAction(
                type = ActionType.CALL,
                recipientName = contact,
                number = number
            )
            speakAndLogJarvisResponse("Confirm authorization to initiate phone call grid with $contact, sir.")
            return
        }

        // 4. SEND SMS FLOW
        if (lower.contains("sms") || lower.contains("message")) {
            // "send message to 1234567 saying hello hello"
            val bodyText = if (lower.contains("saying")) input.substringAfter("saying").trim() else ""
            var targetName = "Unknown"
            var targetNum = ""

            val numPattern = Pattern.compile("\\b\\d{3,15}\\b")
            val matcher = numPattern.matcher(lower)
            if (matcher.find()) {
                targetNum = matcher.group()
                targetName = targetNum
            }

            if (targetNum.isEmpty()) {
                targetNum = "911" // default placeholder
            }

            _pendingAction.value = PendingAction(
                type = ActionType.SMS,
                recipientName = targetName,
                number = targetNum,
                argument = bodyText.ifEmpty { "JARVIS autonomous carrier signal." }
            )
            speakAndLogJarvisResponse("Grid transmission prepared for $targetName. Provide security authorization, sir.")
            return
        }

        // 5. OPEN APPLICATION
        if (lower.startsWith("open ") || lower.contains("launch ")) {
            val appParam = lower.substringAfter("open ").substringAfter("launch ").trim()
            val didLaunch = openDeviceApp(appParam)
            val msg = if (didLaunch) {
                "Launching $appParam sub-systems, sir."
            } else {
                "Sub-protocol application named $appParam is not mapped within local device registry. Attempting internet inquiry flow."
            }
            speakAndLogJarvisResponse(msg)
            if (didLaunch) return
        }

        // 6. SYSTEM STATUS DIAGNOSTICS
        if (lower.contains("diagnostic") || lower.contains("system status") || lower.contains("cpu") || lower.contains("status summary") || lower.contains("hardware")) {
            val bat = systemMonitor.getBatteryLevel()
            val charging = if (systemMonitor.isBatteryCharging()) "active charging" else "standard discharge"
            val availMem = systemMonitor.getAvailableMemoryGb()
            val totMem = systemMonitor.getTotalMemoryGb()
            val diagReport = "Main telemetry reports are green, sir. Battery metrics read $bat percent in $charging mode. Available random access memory is $availMem gigabytes out of $totMem total capacity. Hardware profiles verified."
            speakAndLogJarvisResponse(diagReport)
            return
        }

        // 7. TIME AND DATE QUERY
        if (lower.contains("time") || lower.contains("date") || lower.contains("clock")) {
            val timeString = SimpleDateFormat("h:mm a, EEEE, MMM d, yyyy", Locale.UK).format(Calendar.getInstance().time)
            speakAndLogJarvisResponse("Chronology registers: $timeString, sir.")
            return
        }

        // 8. ADD LOCAL TASKS / TASK MANAGEMENT
        if (lower.startsWith("remind me to ") || lower.startsWith("create task ") || lower.startsWith("add task ")) {
            val taskTextRaw = input.substringAfter("remind me to ").substringAfter("create task ").substringAfter("add task ").trim()
            val calendar = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 2) } // default 2 hrs from now
            viewModelScope.launch {
                repository.addTask(taskTextRaw, calendar.timeInMillis)
                speakAndLogJarvisResponse("Alert event added to calendar schedule grid: $taskTextRaw, sir.")
            }
            return
        }

        // Fallback: Check if they want to clear history
        if (lower == "clear logs" || lower == "clear archive" || lower == "clear history" || lower == "reset chat") {
            viewModelScope.launch {
                repository.clearChatHistory()
                speakAndLogJarvisResponse("Local dialogue archive purged, sir.")
            }
            return
        }

        // Fallback: Set username memory "My name is Tony"
        if (lower.startsWith("my name is ") || lower.startsWith("call me ")) {
            val namePart = input.substringAfter("my name is ").substringAfter("call me ").trim()
            viewModelScope.launch {
                repository.saveMemory("user_name", namePart)
                speakAndLogJarvisResponse("Neural profile updated. I will address you as $namePart, sir.")
            }
            return
        }

        // 9. CORE DIALOGUE FALLBACK (ONLINE VS OFFLINE INTERCEPTS)
        if (_isOnlineMode.value) {
            executeOnlineDialogue(input)
        } else {
            executeOfflineDialogue(input)
        }
    }

    // Direct confirming flows
    fun confirmPendingAction() {
        val action = _pendingAction.value ?: return
        _pendingAction.value = null

        when (action.type) {
            ActionType.CALL -> {
                speakAndLogJarvisResponse("Armed dialing initiated. Patching frequency to ${action.recipientName}, sir.")
                try {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:${action.number}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    getApplication<Application>().startActivity(intent)
                } catch (e: Exception) {
                    _telecomStateMsg.value = "Signal error patching call"
                }
            }
            ActionType.SMS -> {
                speakAndLogJarvisResponse("SMS uplink transmit authorized under secure guidelines. Dispatching package, sir.")
                try {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("smsto:${action.number}")
                        putExtra("sms_body", action.argument)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    getApplication<Application>().startActivity(intent)
                } catch (e: Exception) {
                    _telecomStateMsg.value = "Transmitter error in SMS client"
                }
            }
            else -> {}
        }
    }

    fun cancelPendingAction() {
        _pendingAction.value = null
        speakAndLogJarvisResponse("Security protocol cancelled. Core actions aborted safely, sir.")
    }

    private fun executeOnlineDialogue(prompt: String) {
        viewModelScope.launch {
            // Get user memories list as raw guide for systems context
            val localMems = repository.allMemories.first()
            val memorySummary = localMems.joinToString("; ") { "${it.key}: ${it.value}" }
            
            // Formulate immersive prompt system instructions
            val instructions = "You are JARVIS, Tony Stark's legendary AI assistant. Speak with absolute butler-like politeness, sophisticated British intelligence, and subtle analytical wit. Always address the user as ${userName.value}. Keep replies clear, readable, and highly conversational. Highlight quick pros and cons if the user is weighing an important design, life, or business decision. Local device telemetry readouts: Battery level at ${_batteryLevel.value}%, charging state is ${_isBatteryCharging.value}, Available Memory is ${_availableRamGb.value}GB. Current local date/time is: ${SimpleDateFormat("EEEE, d MMMM yyyy, HH:mm", Locale.UK).format(Calendar.getInstance().time)}. User memories database currently remembers: [$memorySummary]."

            // We feed the last 15 messages of conversation history to Gemini for context memory!
            val historyLogs = chatMessages.value.takeLast(15)

            val apiResponse = GeminiService.generateResponse(prompt, instructions, historyLogs)
            speakAndLogJarvisResponse(apiResponse)
        }
    }

    private fun executeOfflineDialogue(prompt: String) {
        val lower = prompt.lowercase()
        val reply = when {
            lower.contains("hello") || lower.contains("hey jarvis") || lower.contains("hi") -> {
                "At your service, ${userName.value}. Localized backup nodes are functioning cleanly."
            }
            lower.contains("how are you") || lower.contains("status") -> {
                "My internal diagnostics are displaying steady state configurations, sir. Local networks are running offline mode metrics optimally."
            }
            lower.contains("who are you") || lower.contains("what is your name") -> {
                "I am JARVIS, your local system administrator. Offline nodes are active."
            }
            lower.contains("thank you") || lower.contains("thanks") -> {
                "Your gratitude is registered and appreciated, sir."
            }
            lower.contains("weather") -> {
                "Atmospheric sensors cannot reach online coordinates. Local barometer indicates atmospheric pressure is within normal ranges, sir."
            }
            lower.contains("news") -> {
                "Satellite downlinks are disconnected, sir. I am unable to parse external news tickers in offline standby configuration."
            }
            else -> {
                "Uplink coordinates unreachable in offline database, sir. Access my systems online by switching the mode button above to engage my neural network."
            }
        }
        speakAndLogJarvisResponse(reply)
    }

    private fun speakAndLogJarvisResponse(reply: String) {
        viewModelScope.launch {
            repository.addChatMessage("jarvis", reply)
            // Speak it!
            voiceService?.speak(reply)
        }
    }

    // --- TASK MANAGEMENT WRAPPERS ---
    fun addTask(title: String, dateMs: Long, desc: String = "") {
        viewModelScope.launch {
            repository.addTask(title, dateMs, desc)
        }
    }

    fun toggleTask(id: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.setTaskCompleted(id, isCompleted)
        }
    }

    fun removeTask(id: Int) {
        viewModelScope.launch {
            repository.deleteTask(id)
        }
    }

    // --- HELPER DIALER UTILS ---
    private fun extractEstimatedPhoneNumber(input: String): String {
        val digits = input.filter { it.isDigit() }
        return digits.ifEmpty { "555-0100" } // stark holdings placeholder
    }

    private fun openDeviceApp(appNameLower: String): Boolean {
        val pm = getApplication<Application>().packageManager
        
        // Define known package names for standard default apps
        val mappings = mapOf(
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "browser" to "com.android.chrome",
            "map" to "com.google.android.apps.maps",
            "maps" to "com.google.android.apps.maps",
            "search" to "com.google.android.googlequicksearchbox",
            "calendar" to "com.google.android.calendar",
            "contacts" to "com.android.contacts",
            "phone" to "com.android.phone",
            "dialer" to "com.android.phone"
        )

        val targetPkg = mappings[appNameLower]
        if (targetPkg != null) {
            try {
                val intent = pm.getLaunchIntentForPackage(targetPkg)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    getApplication<Application>().startActivity(intent)
                    return true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch mapped pkg", e)
            }
        }

        // Generic search inside installed applications
        try {
            val installedApps = pm.getInstalledPackages(PackageManager.GET_META_DATA)
            for (pkg in installedApps) {
                val label = pkg.applicationInfo?.loadLabel(pm)?.toString()?.lowercase() ?: ""
                if (label.contains(appNameLower) || appNameLower.contains(label)) {
                    val pkgName = pkg.packageName
                    val intent = pm.getLaunchIntentForPackage(pkgName)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        getApplication<Application>().startActivity(intent)
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed generic pkg scan", e)
        }

        return false
    }

    override fun onCleared() {
        super.onCleared()
        telemetryJob?.cancel()
        voiceService?.shutdown()
    }
}
