package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatMessage
import com.example.data.Task
import com.example.data.UserMemory
import com.example.service.JarvisVoiceService
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun JarvisScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val isOnline by viewModel.isOnlineMode.collectAsState()
    val isContinuousActive by viewModel.isContinuousListening.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val rmsLevel by viewModel.voiceRmsLevel.collectAsState()
    val batteryLvl by viewModel.batteryLevel.collectAsState()
    val chargingState by viewModel.isBatteryCharging.collectAsState()
    val totalRam by viewModel.totalRamGb.collectAsState()
    val availRam by viewModel.availableRamGb.collectAsState()
    val flashlightOn by viewModel.flashlightOn.collectAsState()
    val masterVolume by viewModel.musicVolume.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val pendingAction by viewModel.pendingAction.collectAsState()
    val telecomMsg by viewModel.telecomStateMsg.collectAsState()

    val chatHistory by viewModel.chatMessages.collectAsState()
    val localTasks by viewModel.tasks.collectAsState()
    val userMemories by viewModel.memories.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Tactical Core, 1: Telemetry Diagnostics, 2: Intel Schedule, 3: Communication Logs
    var draftMsgText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkVacuum)
            .drawBehind {
                // Futuristic grid space lines helper to make UI highly immersive
                val strokeWidth = 1f
                val stepPx = 40.dp.toPx() // smaller 40px grid matching HTML design
                for (x in 0..size.width.toInt() step stepPx.toInt()) {
                    drawLine(
                        color = Color(0x1300D2FF),
                        start = Offset(x.toFloat(), 0f),
                        end = Offset(x.toFloat(), size.height),
                        strokeWidth = strokeWidth
                    )
                }
                for (y in 0..size.height.toInt() step stepPx.toInt()) {
                    drawLine(
                        color = Color(0x1300D2FF),
                        start = Offset(0f, y.toFloat()),
                        end = Offset(size.width, y.toFloat()),
                        strokeWidth = strokeWidth
                    )
                }
                // Ambient center cyan spotlight
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x1800D2FF), Color.Transparent),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width * 1.1f
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.width * 1.1f
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // --- TOP SYSTEM BAR (Immersive UI Header) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp, top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(SafeGreen, CircleShape)
                                .shadow(6.dp, CircleShape, ambientColor = SafeGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SYSTEM ONLINE",
                            color = CyberCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "J.A.R.V.I.S.",
                        color = Color.White,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 1.sp,
                        modifier = Modifier.clickable { viewModel.toggleOnlineMode() }
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    var currentTimeText by remember { mutableStateOf("10:45 AM") }
                    LaunchedEffect(Unit) {
                        while (true) {
                            currentTimeText = SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date())
                            delay(1000)
                        }
                    }
                    Text(
                        text = currentTimeText,
                        color = CyberCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "MALIBU, CA",
                        color = Color.Gray,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Continuous wake word sub-label
            if (telecomMsg.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(AlertOrange.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .border(1.dp, AlertOrange.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = telecomMsg,
                        color = AlertOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // --- DEPLOYED PORTALS ---



            // Viewtabs dynamic anchor

            // --- DYNAMIC CONTENTS CONTAINER ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    0 -> TacticalHudView(
                        voiceState = voiceState,
                        rmsVal = rmsLevel,
                        isContinuous = isContinuousActive,
                        chatList = chatHistory,
                        userName = userName,
                        battery = batteryLvl,
                        isCharging = chargingState,
                        onActionTrigger = { viewModel.executeVocalTrigger() },
                        onContinuousToggle = { viewModel.toggleContinuousListening() }
                    )
                    1 -> DiagnosticsView(
                        battery = batteryLvl,
                        isCharging = chargingState,
                        ramAvail = availRam,
                        ramTotal = totalRam,
                        isFlashlightOn = flashlightOn,
                        masterVolume = masterVolume,
                        diagnosticsLog = systemMonitorDiagnosticsText(viewModel),
                        onVolumeChanged = { viewModel.processIncomingInput("set volume to $it%") },
                        onFlashToggle = { viewModel.processIncomingInput(if (flashlightOn) "flashlight off" else "flashlight on") }
                    )
                    2 -> IntelPlannerView(
                        tasks = localTasks,
                        onAddTask = { title, timeMs -> viewModel.addTask(title, timeMs) },
                        onToggleTask = { id, comp -> viewModel.toggleTask(id, comp) },
                        onRemoveTask = { id -> viewModel.removeTask(id) },
                        userMemories = userMemories
                    )
                    3 -> CommunicationHistoryLogsView(
                        history = chatHistory,
                        draftText = draftMsgText,
                        onDraftChanged = { draftMsgText = it },
                        onSendMessage = {
                            viewModel.processIncomingInput(draftMsgText)
                            draftMsgText = ""
                            focusManager.clearFocus()
                        }
                    )
                }
            }

            // --- NAVIGATION BAR (Immersive Floating Dock) ---
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(vertical = 10.dp, horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf(
                        Triple("HOME", Icons.Default.Home, 0),
                        Triple("MEMORY", Icons.Default.Chat, 3),
                        Triple("TASKS", Icons.Default.List, 2),
                        Triple("SETTINGS", Icons.Default.Settings, 1)
                    )

                    tabs.forEach { (title, icon, index) ->
                        val isSelected = activeTab == index
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { activeTab = index }
                                .padding(vertical = 4.dp)
                                .testTag("tab_button_$index")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(
                                        if (isSelected) CyberCyan else Color.White.copy(alpha = 0.4f),
                                        CircleShape
                                    )
                                    .shadow(
                                        if (isSelected) 4.dp else 0.dp,
                                        CircleShape,
                                        ambientColor = if (isSelected) CyberCyan else Color.Transparent
                                    )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = title,
                                color = if (isSelected) CyberCyan else Color.White.copy(alpha = 0.4f),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }

        // --- PENDING SECURITY ACTION MODAL OVERLAY ---
        if (pendingAction != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable(enabled = false) {}, // absorb clicks
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .border(2.dp, AlertOrange, RoundedCornerShape(16.dp))
                        .background(DeepSpaceNavy.copy(alpha = 0.95f), RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (pendingAction?.type == JarvisViewModel.ActionType.CALL) Icons.Default.Call else Icons.Default.Sms,
                        contentDescription = "Authorize Confirmation Required",
                        tint = AlertOrange,
                        modifier = Modifier
                            .size(56.dp)
                            .padding(bottom = 12.dp)
                    )

                    Text(
                        text = "SECURITY PROTOCOL REQUIRED",
                        color = AlertOrange,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (pendingAction?.type == JarvisViewModel.ActionType.CALL) {
                            "Initialize armed cellular dialer linking frequency directly to:\n\n'${pendingAction?.recipientName}'\n(${pendingAction?.number})?"
                        } else {
                            "Transmit encrypted SMS packet directly to '${pendingAction?.recipientName}' at ${pendingAction?.number}?\n\nPayload: \"${pendingAction?.argument}\""
                        },
                        color = TextHologram,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.cancelPendingAction() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .testTag("cancel_action_button")
                        ) {
                            Text("ABORT", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { viewModel.confirmPendingAction() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AlertOrange,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .weight(1.2f)
                                .shadow(8.dp, RoundedCornerShape(8.dp), ambientColor = AlertOrange)
                                .testTag("confirm_action_button")
                        ) {
                            Text(
                                "EXECUTE AIR",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- SUB PANEL 1: HUD TACTICAL CORE ---
@Composable
fun TacticalHudView(
    voiceState: JarvisVoiceService.VoiceState,
    rmsVal: Float,
    isContinuous: Boolean,
    chatList: List<ChatMessage>,
    userName: String,
    battery: Int,
    isCharging: Boolean,
    onActionTrigger: () -> Unit,
    onContinuousToggle: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "core_circles")
    
    // Smooth angle rotations for holo-circles
    val rotationAngle1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "circle1"
    )

    val rotationAngle2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "circle2"
    )

    // Glowing core pulse based on active state of TTS/STT
    val idlePulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "idlePulse"
    )

    val lastJarvisMessage = chatList.lastOrNull { it.sender == "jarvis" }?.message
        ?: "Awaiting active telemetry instructions, sir."

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- Micro Telemetry Glass Panel Cards ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // System Diagnostics Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .background(TechCardGlass.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .border(1.dp, CyberCyan.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxHeight()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(5.dp).background(if (isCharging) SafeGreen else CyberCyan, CircleShape))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("SYS POWER", color = CyberCyan, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isCharging) "CHARGING: $battery%" else "BATTERY: $battery%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Location Sensor Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .background(TechCardGlass.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxHeight()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(5.dp).background(AlertOrange, CircleShape))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("LOC SENSOR", color = AlertOrange, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "MALIBU, CA: 72°F",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- Holographic Center Core ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Circle 1 (Outer rotating ticks)
            Canvas(
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer(rotationZ = rotationAngle1)
            ) {
                drawCircle(
                    color = CyberCyan.copy(alpha = 0.15f),
                    style = Stroke(
                        width = 4f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 20f))
                    )
                )
                drawCircle(
                    color = HologramBlue.copy(alpha = 0.1f),
                    radius = size.width / 2.3f,
                    style = Stroke(
                        width = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 40f))
                    )
                )
            }

            // Circle 2 (Intermediate counter rotating ticks)
            Canvas(
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer(rotationZ = rotationAngle2)
            ) {
                drawCircle(
                    color = CyberCyan.copy(alpha = 0.3f),
                    style = Stroke(
                        width = 5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 15f))
                    )
                )
            }

            // Central Glowing Sphere (Interactive scale driven by RMS input or animated pulse when sleeping)
            val currentCoreScale = when (voiceState) {
                JarvisVoiceService.VoiceState.LISTENING -> 1f + (rmsVal * 0.45f)
                JarvisVoiceService.VoiceState.SPEAKING -> 1f + (0.15f * kotlin.math.sin(System.currentTimeMillis() / 80f))
                else -> idlePulseScale
            }

            val coreGlowColor = when (voiceState) {
                JarvisVoiceService.VoiceState.LISTENING -> SafeGreen
                JarvisVoiceService.VoiceState.SPEAKING -> CyberCyan
                JarvisVoiceService.VoiceState.ERROR -> Color(0xFFFF3333)
                else -> HologramBlue
            }

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(currentCoreScale)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                coreGlowColor.copy(alpha = 0.8f),
                                coreGlowColor.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black, CircleShape)
                        .border(2.dp, coreGlowColor, CircleShape)
                        .shadow(12.dp, CircleShape, ambientColor = coreGlowColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (voiceState) {
                            JarvisVoiceService.VoiceState.LISTENING -> Icons.Default.Mic
                            JarvisVoiceService.VoiceState.SPEAKING -> Icons.Default.VolumeUp
                            JarvisVoiceService.VoiceState.INITIALIZING -> Icons.Default.HourglassEmpty
                            else -> Icons.Default.Memory
                        },
                        contentDescription = "Core State",
                        tint = coreGlowColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // --- Voice Waves Display ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            contentAlignment = Alignment.Center
        ) {
            VoiceWaves(voiceState = voiceState, rmsVal = rmsVal)
        }

        // --- Terminal Info readout box ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberCyan.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                .background(TechCardGlass.copy(alpha = 0.4f))
                .padding(10.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(5.dp).background(CyberCyan, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "JARVIS TACTICAL INTELLIGENCE",
                        color = CyberCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lastJarvisMessage,
                    color = TextHologram,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )
            }
        }

        // --- Action Buttons Matrix ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Constant sensor switch
            Button(
                onClick = onContinuousToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isContinuous) CyberCyan.copy(alpha = 0.2f) else Color.Transparent,
                    contentColor = if (isContinuous) CyberCyan else TextHologram.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .border(
                        1.dp,
                        if (isContinuous) CyberCyan else CyberCyan.copy(alpha = 0.15f),
                        RoundedCornerShape(10.dp)
                    )
                    .testTag("continuous_sensor_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isContinuous) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Wake Word Sensor",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WAKE SENSOR",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // PRIMARY TRIGGER (Big action bar tap)
            Button(
                onClick = onActionTrigger,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .shadow(10.dp, RoundedCornerShape(10.dp), ambientColor = CyberCyan)
                    .testTag("vocal_trigger_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Trigger Jarvis Listening",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (voiceState == JarvisVoiceService.VoiceState.LISTENING) "LISTENING" else "ENGAGE JARVIS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Custom scale extension
fun Modifier.scale(scale: Float): Modifier = graphicsLayer(scaleX = scale, scaleY = scale)

// Animated wave visualizers
@Composable
fun VoiceWaves(voiceState: JarvisVoiceService.VoiceState, rmsVal: Float) {
    val barCount = 14
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    
    Row(
        modifier = Modifier
            .fillMaxWidth(0.6f)
            .fillMaxHeight(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            // Generate rhythmic sinus delay per bar
            val phaseDelay = (i * 150)
            val barAnimValue by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600 + (i * 20), delayMillis = phaseDelay, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ), label = "bar_$i"
            )

            val scaleMultiplier = when (voiceState) {
                JarvisVoiceService.VoiceState.LISTENING -> 0.15f + (rmsVal * 0.85f * (1.0f - (kotlin.math.abs(barCount / 2 - i) / (barCount / 2f))))
                JarvisVoiceService.VoiceState.SPEAKING -> barAnimValue * 0.8f
                else -> 0.12f // flat resting state
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(scaleMultiplier.coerceIn(0.12f, 1f))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                CyberCyan,
                                HologramBlue.copy(alpha = 0.5f)
                            )
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

// --- SUB PANEL 2: LAB TELEMETRY DIAGNOSTICS ---
@Composable
fun DiagnosticsView(
    battery: Int,
    isCharging: Boolean,
    ramAvail: Double,
    ramTotal: Double,
    isFlashlightOn: Boolean,
    masterVolume: Int,
    diagnosticsLog: String,
    onVolumeChanged: (Int) -> Unit,
    onFlashToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
        Text(
            text = "HARDWARE TELEMETRY STATIONS",
            color = CyberCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Row of gauges
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Battery Gauge Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, CyberCyan.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .background(TechCardGlass)
                    .padding(14.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CORE ENERGY",
                        color = TextHologram.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                        Canvas(modifier = Modifier.size(72.dp)) {
                            // draw dial arc background
                            drawArc(
                                color = Color.Gray.copy(alpha = 0.2f),
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter = false,
                                style = Stroke(width = 8f)
                            )
                            // draw power
                            drawArc(
                                color = if (battery < 20) Color.Red else CyberCyan,
                                startAngle = 135f,
                                sweepAngle = (270f * (battery / 100f)),
                                useCenter = false,
                                style = Stroke(width = 8f)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$battery%",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (isCharging) "CHARGING" else "BATTERY",
                                color = if (isCharging) SafeGreen else TextHologram.copy(alpha = 0.6f),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // RAM Memory Gauge Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, CyberCyan.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .background(TechCardGlass)
                    .padding(14.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SYS MEMORY",
                        color = TextHologram.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                        val usedRamPercent = ((ramTotal - ramAvail) / ramTotal).toFloat().coerceIn(0f, 1f)
                        Canvas(modifier = Modifier.size(72.dp)) {
                            drawArc(
                                color = Color.Gray.copy(alpha = 0.2f),
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter = false,
                                style = Stroke(width = 8f)
                            )
                            drawArc(
                                color = HologramBlue,
                                startAngle = 135f,
                                sweepAngle = (270f * usedRamPercent),
                                useCenter = false,
                                style = Stroke(width = 8f)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${String.format("%.1f", ramTotal - ramAvail)}G",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "USED OF ${ramTotal.toInt()}G",
                                color = TextHologram.copy(alpha = 0.6f),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Direct hardware interaction panels
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .border(1.dp, CyberCyan.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .background(TechCardGlass)
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.FlashlightOn, contentDescription = "Flashlight", tint = CyberCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("FLASHLIGHT TRANSCEIVER", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("Local camera core torch emitter", color = TextHologram.copy(alpha = 0.5f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                    Button(
                        onClick = onFlashToggle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFlashlightOn) SafeGreen else Color.DarkGray
                        ),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("flashlight_toggle_button")
                    ) {
                        Text(
                            text = if (isFlashlightOn) "ONLINE" else "OFFLINE",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = CyberCyan.copy(alpha = 0.15f))

                // VOLUME SCR0LL
                Text("AUDIO AMPLIFICATION CORE", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.VolumeDown, contentDescription = "Volume", tint = CyberCyan, modifier = Modifier.size(16.dp))
                    Slider(
                        value = masterVolume.toFloat(),
                        onValueChange = { onVolumeChanged(it.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberCyan,
                            activeTrackColor = CyberCyan,
                            inactiveTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                    )
                    Text(text = "$masterVolume%", color = CyberCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Terminal Diagnostics report
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberCyan.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = "SYSTEM MONITOR DIAGNOSTIC REGISTER",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = diagnosticsLog,
                    color = TextHologram.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// Generate diagnostic text
fun systemMonitorDiagnosticsText(viewModel: JarvisViewModel): String {
    val totalRam = viewModel.totalRamGb.value
    val availRam = viewModel.availableRamGb.value
    val batCharging = if (viewModel.isBatteryCharging.value) "Uplink Active (Charging)" else "Discharging Model"
    
    return """
        [LOG NODE: STAR_MAIN_GRID]
        - DEPLOYED OS: ANDROID SDK ${android.os.Build.VERSION.SDK_INT}
        - TELEMETRY MODEL: ${android.os.Build.MODEL}
        - POWER FREQUENCY: ${viewModel.batteryLevel.value}% [$batCharging]
        - SECTOR MEMORY: ${String.format("%.2f", totalRam - availRam)}GB USAGE / ${String.format("%.2f", totalRam)}GB TOTAL
        - LASER DEPLOYMENT TORCH: ${if (viewModel.flashlightOn.value) "ACTIVE" else "STANDBY"}
        - BUS BANDWIDTH VOLUME: ${viewModel.musicVolume.value}%
        - INTERACTION LINK STATUS: NOMINAL
        - LOCAL SECURITY FRAMEWORK: VERIFIED APPASSIST
        [COMPLETED DIAGNOSTICS DETECION: ALL CORES STABLE]
    """.trimIndent()
}

// --- SUB PANEL 3: INTEL REGISTER SCHEDULE PLANNER ---
@Composable
fun IntelPlannerView(
    tasks: List<Task>,
    onAddTask: (String, Long) -> Unit,
    onToggleTask: (Int, Boolean) -> Unit,
    onRemoveTask: (Int) -> Unit,
    userMemories: List<UserMemory>
) {
    var taskInputText by remember { mutableStateOf("") }
    var priorityToggle by remember { mutableStateOf(false) } // simulated marker

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "STARK DIGITAL AGENDA GRID",
            color = CyberCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Interactive creation block
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = taskInputText,
                onValueChange = { taskInputText = it },
                placeholder = { Text("Record reminder / command...", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = TextHologram.copy(alpha = 0.4f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = CyberCyan.copy(alpha = 0.2f),
                    focusedContainerColor = TechCardGlass,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = TextHologram
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("task_input_field"),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (taskInputText.isNotEmpty()) {
                        val calendar = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 3) }
                        onAddTask(taskInputText, calendar.timeInMillis)
                        taskInputText = ""
                    }
                })
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .border(1.dp, CyberCyan, RoundedCornerShape(10.dp))
                    .background(TechCardGlass)
                    .clickable {
                        if (taskInputText.isNotEmpty()) {
                            val calendar = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 3) }
                            onAddTask(taskInputText, calendar.timeInMillis)
                            taskInputText = ""
                        }
                    }
                    .testTag("submit_task_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Grid Entry", tint = CyberCyan)
            }
        }

        // List registries
        LazyColumn(
            modifier = Modifier
                .weight(1.8f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (tasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "[ AGENDA GRID VACANT, SIR ]",
                            color = TextHologram.copy(alpha = 0.3f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                items(tasks) { task ->
                    val dateFormatted = SimpleDateFormat("HH:mm a", Locale.UK).format(Date(task.dueDate))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyberCyan.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                            .background(TechCardGlass)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = task.isCompleted,
                                onCheckedChange = { onToggleTask(task.id, it) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = CyberCyan,
                                    checkmarkColor = Color.Black,
                                    uncheckedColor = CyberCyan.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.testTag("task_checkbox_${task.id}")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = task.title,
                                    color = if (task.isCompleted) TextHologram.copy(alpha = 0.4f) else Color.White,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "ALERT TIME: $dateFormatted",
                                    color = CyberCyan.copy(alpha = 0.6f),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        IconButton(
                            onClick = { onRemoveTask(task.id) },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("delete_task_${task.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Purge alert",
                                tint = Color(0xFFFF5555).copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 12.dp), color = CyberCyan.copy(alpha = 0.15f))

        // MEMORY BLOCKS
        Text(
            text = "PERSONALIZED COGNITIVE CORE MEMORIES",
            color = CyberCyan,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (userMemories.isEmpty()) {
                item {
                    Text(
                        text = "No user-specific memories loaded. Teach Jarvis memories by typing e.g.: \"My name is Tony Stark\" or \"Call me Iron Man\".",
                        color = TextHologram.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                items(userMemories) { mem ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "• ${mem.key.uppercase(Locale.getDefault())}: \"${mem.value}\"",
                            color = TextHologram,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// --- SUB PANEL 4: INTERACTIVE DIALOG LOG ARCHIVES ---
@Composable
fun CommunicationHistoryLogsView(
    history: List<ChatMessage>,
    draftText: String,
    onDraftChanged: (String) -> Unit,
    onSendMessage: () -> Unit
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Smooth autoscroll when logs update
    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(history.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "TRANSMISSION SECURE DIALOGUE DATABASE",
            color = CyberCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Historical entries scroll
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, CyberCyan.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (history.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DIALOG ARCHIVE VACANT",
                            color = TextHologram.copy(alpha = 0.3f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                items(history) { log ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (log.sender == "user") Alignment.End else Alignment.Start
                    ) {
                        // Speaker metadata indicator
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (log.sender == "user") CyberCyan else HologramBlue, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (log.sender == "user") "TRANSMITTER: USER" else "COGNITIVE: JARVIS",
                                color = if (log.sender == "user") CyberCyan.copy(alpha = 0.6f) else HologramBlue.copy(alpha = 0.6f),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Message Bubble
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .border(
                                    1.dp,
                                    if (log.sender == "user") CyberCyan.copy(alpha = 0.2f) else HologramBlue.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                                .background(
                                    if (log.sender == "user") TechCardGlass else Color(0x0600A2FF),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Text(
                                text = log.message,
                                color = if (log.sender == "user") Color.White else TextHologram,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Direct Text input prompt
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = draftText,
                onValueChange = onDraftChanged,
                placeholder = { Text("Deploy direct console query...", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = TextHologram.copy(alpha = 0.4f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = CyberCyan.copy(alpha = 0.2f),
                    focusedContainerColor = TechCardGlass,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = TextHologram
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (draftText.isNotEmpty()) onSendMessage()
                })
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .border(1.dp, CyberCyan, RoundedCornerShape(10.dp))
                    .background(TechCardGlass)
                    .clickable { if (draftText.isNotEmpty()) onSendMessage() }
                    .testTag("send_chat_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Transmit payload", tint = CyberCyan)
            }
        }
    }
}
