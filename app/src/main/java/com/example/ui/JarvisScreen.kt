package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.JarvisVoiceService
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisScreen(viewModel: JarvisViewModel) {
    val currentContext by viewModel.currentContext.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val rmsVal by viewModel.rmsVal.collectAsState()
    val batteryLev by viewModel.batteryLevel.collectAsState()
    val isCharging by viewModel.isBatteryCharging.collectAsState()
    val totalRam by viewModel.totalRamGb.collectAsState()
    val availRam by viewModel.availableRamGb.collectAsState()
    val flashlightOn by viewModel.flashlightOn.collectAsState()
    val pendingAction by viewModel.pendingAction.collectAsState()
    val telecomMsg by viewModel.telecomStateMsg.collectAsState()
    val msgLines by viewModel.chatMessages.collectAsState(initial = emptyList())
    val taskList by viewModel.tasks.collectAsState(initial = emptyList())
    val userName by viewModel.userName.collectAsState()

    val scope = rememberCoroutineScope()
    var manualInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val chatListState = rememberLazyListState()

    // Pulse animation for holographic elements
    val infiniteTransition = rememberInfiniteTransition(label = "HoloPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    // Pulse scale for Stark Reactor Core
    val coreScale by infiniteTransition.animateFloat(
        initialValue = 0.93f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CoreScale"
    )

    // Constant rotation for Stark Reactor details
    val rotationDegrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    // Auto-scroll terminal logs when a new message logs
    LaunchedEffect(msgLines.size) {
        if (msgLines.isNotEmpty()) {
            chatListState.animateScrollToItem(msgLines.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SpaceBackground
    ) { paddingValues ->
        // Futuristic radial background pattern
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .drawBehind {
                    val brush = Brush.radialGradient(
                        colors = listOf(
                            TechCardGlass.copy(alpha = 0.35f),
                            SpaceBackground
                        ),
                        center = Offset(size.width / 2f, size.height * 0.3f),
                        radius = size.width * 1.1f
                    )
                    drawRect(brush = brush)
                    
                    // Subtle background target grid lines
                    val gridDensity = 40.dp.toPx()
                    val stroke = 0.5f * density
                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            color = PrimaryCyber.copy(alpha = 0.04f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = stroke
                        )
                        y += gridDensity
                    }
                    var x = 0f
                    while (x < size.width) {
                        drawLine(
                            color = PrimaryCyber.copy(alpha = 0.04f),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = stroke
                        )
                        x += gridDensity
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Top Telemetry Header
                HeaderControlSection(
                    userName = userName,
                    currentContext = currentContext,
                    batteryLevel = batteryLev,
                    isCharging = isCharging,
                    onContextSelect = { viewModel.setContext(it) }
                )

                // Visual Core: Stark Arc Reactor + Sound Wave Visualizers
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ArcReactorHologram(
                        rotation = rotationDegrees,
                        coreScale = coreScale,
                        rmsVal = rmsVal,
                        voiceState = voiceState
                    )
                    
                    // Overlay Status Monospace Indicators
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "SYS_STABILITY_CORE: 99.87%",
                            color = SafeGreen.copy(alpha = pulseAlpha),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "SECTOR_NET: ENCRYPTED_SSL",
                            color = PrimaryCyber.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "CONTEXT: ${currentContext.name}_MODE",
                            color = AccentHologram.copy(alpha = pulseAlpha),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "HARDWARE_VOLTAGE: D_C_3.8V",
                            color = PrimaryCyber.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Dynamic Pending Airwave Communications Approval Window
                AnimatedVisibility(
                    visible = pendingAction != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    pendingAction?.let { action ->
                        ActionApprovalSection(
                            action = action,
                            telecomMsg = telecomMsg,
                            onApprove = { viewModel.confirmPendingAction() },
                            onCancel = { viewModel.cancelPendingAction() }
                        )
                    }
                }

                // Main Display Content Area
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Left Screen Component: Diagnostic Metrics & Alert Reminders
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Diagnostic Telemetry Panel
                        TerminalPanel(
                            title = "HARDWARE TERMINAL_DIAGS",
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HardwareMetricRow("CORE TEMP", "34.2 °C", SafeGreen)
                                HardwareMetricRow(
                                    label = "VOLTAGE RESERVE",
                                    value = "$batteryLev% ${if (isCharging) "CHARGING" else "STANDBY"}",
                                    color = if (batteryLev > 20) SafeGreen else DarkError
                                )
                                HardwareMetricRow(
                                    label = "RAM ALLOCATION",
                                    value = "${String.format("%.1f", totalRam - availRam)}G / ${String.format("%.1f", totalRam)}G",
                                    color = PrimaryCyber
                                )
                                HardwareMetricRow(
                                    label = "BEAM INTEGRITY",
                                    value = "OPTIMAL (100%)",
                                    color = AccentHologram
                                )
                                HardwareMetricRow(
                                    label = "ILLUMINATION",
                                    value = if (flashlightOn) "CORE_ACTIVE" else "DEACTIVATED",
                                    color = if (flashlightOn) PrimaryCyber else TextHologram.copy(alpha = 0.5f)
                                )
                                
                                Button(
                                    onClick = { viewModel.updateHardwareTelemetry() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(32.dp)
                                        .testTag("refresh_telemetry_btn"),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PrimaryCyber.copy(alpha = 0.15f),
                                        contentColor = PrimaryCyber
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, BorderCyan)
                                ) {
                                    Text("PROBE HARDWARE LOGS", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        // Schedule Grid Panel
                        TerminalPanel(
                            title = "CHRONO_MAP ALERTS",
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    var quickTaskText by remember { mutableStateOf("") }
                                    TextField(
                                        value = quickTaskText,
                                        onValueChange = { quickTaskText = it },
                                        placeholder = { Text("Task title...", color = TextHologram.copy(alpha = 0.4f), fontSize = 11.sp) },
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = PrimaryCyber, fontSize = 12.sp),
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .border(1.dp, BorderCyan, RoundedCornerShape(4.dp))
                                            .testTag("task_input_field"),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            disabledContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            if (quickTaskText.isNotBlank()) {
                                                viewModel.addTaskManual(quickTaskText)
                                                quickTaskText = ""
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(PrimaryCyber.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                            .border(1.dp, BorderCyan, RoundedCornerShape(4.dp))
                                            .testTag("add_task_btn")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add alert parameter", tint = PrimaryCyber, modifier = Modifier.size(16.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                if (taskList.isEmpty()) {
                                    Box(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "NO ALERTS IN SYSTEM SECTOR",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextHologram.copy(alpha = 0.4f)
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(taskList) { item ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(TechCardGlass, RoundedCornerShape(3.dp))
                                                    .border(0.5.dp, BorderCyan.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = item.isCompleted,
                                                    onCheckedChange = { checked ->
                                                        viewModel.toggleTaskStatus(item.id, checked)
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkmarkColor = SpaceBackground,
                                                        checkedColor = PrimaryCyber,
                                                        uncheckedColor = BorderCyan
                                                    ),
                                                    modifier = Modifier.size(24.dp).testTag("task_checkbox_item_${item.id}")
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = item.title,
                                                    color = if (item.isCompleted) TextHologram.copy(alpha = 0.4f) else PrimaryCyber,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                IconButton(
                                                    onClick = { viewModel.deleteTask(item.id) },
                                                    modifier = Modifier.size(20.dp).testTag("delete_task_btn_${item.id}")
                                                ) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = "Delete coordinates",
                                                        tint = DarkError.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Right Screen Component: Main Log terminal outputs
                    TerminalPanel(
                        title = "CENTRAL JARVIS_CORE_LOGS",
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            LazyColumn(
                                state = chatListState,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                items(msgLines) { message ->
                                    val isUser = message.sender == "USER"
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                                    ) {
                                        Text(
                                            text = if (isUser) ">> COMMAND_DIRECTIVE" else ">> J_A_R_V_I_S",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isUser) AccentHologram else PrimaryCyber.copy(alpha = 0.75f)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .background(
                                                    if (isUser) AccentHologram.copy(alpha = 0.1f) else TechCardGlass.copy(alpha = 0.8f),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .border(
                                                    0.5.dp,
                                                    if (isUser) AccentHologram.copy(alpha = 0.35f) else BorderCyan.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .padding(6.dp)
                                        ) {
                                            Text(
                                                text = message.content,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isUser) TextHologram else PrimaryCyber,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Visual Audio Wave (when speaking/listening)
                            AnimatedVisibility(
                                visible = voiceState == JarvisVoiceService.VoiceState.LISTENING || voiceState == JarvisVoiceService.VoiceState.SPEAKING
                            ) {
                                VoiceWaveLine(rmsVal = rmsVal, voiceState = voiceState)
                            }
                        }
                    }
                }

                // Interactive Bottom Terminal Prompt Area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = manualInput,
                        onValueChange = { manualInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .border(1.dp, BorderCyan, RoundedCornerShape(6.dp))
                            .testTag("manual_cyber_prompt"),
                        placeholder = {
                            Text(
                                "Tear coordinates / Type directives here...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextHologram.copy(alpha = 0.35f)
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            color = PrimaryCyber,
                            fontSize = 13.sp
                        ),
                        singleLine = true,
                        trailingIcon = {
                            if (manualInput.isNotEmpty()) {
                                IconButton(onClick = { manualInput = "" }) {
                                    Icon(Icons.Default.Cancel, contentDescription = "Clear", tint = BorderCyan)
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = TechCardGlass,
                            unfocusedContainerColor = TechCardGlass,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Simulated Microphone button
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (voiceState == JarvisVoiceService.VoiceState.LISTENING) DarkError.copy(alpha = 0.2f)
                                else PrimaryCyber.copy(alpha = 0.12f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (voiceState == JarvisVoiceService.VoiceState.LISTENING) DarkError else BorderCyan,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                if (manualInput.isNotBlank()) {
                                    viewModel.processIncomingInput(manualInput)
                                    manualInput = ""
                                    keyboardController?.hide()
                                } else {
                                    viewModel.toggleContinuousListening()
                                }
                            }
                            .testTag("voice_trigger_orb"),
                        contentAlignment = Alignment.Center
                    ) {
                        val iconTint = if (voiceState == JarvisVoiceService.VoiceState.LISTENING) DarkError 
                                       else if (voiceState == JarvisVoiceService.VoiceState.SPEAKING) AccentHologram 
                                       else PrimaryCyber
                        
                        Icon(
                            imageVector = if (manualInput.isNotBlank()) Icons.Default.Send else Icons.Default.Mic,
                            contentDescription = "Trigger central speech matrix",
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HardwareMetricRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = TextHologram.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = color
        )
    }
}

@Composable
fun HeaderControlSection(
    userName: String,
    currentContext: JarvisViewModel.JarvisContextType,
    batteryLevel: Int,
    isCharging: Boolean,
    onContextSelect: (JarvisViewModel.JarvisContextType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "J.A.R.V.I.S.",
                    style = MaterialTheme.typography.displayLarge,
                    color = PrimaryCyber,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Cybernetic Assistant to Lord ${userName.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextHologram.copy(alpha = pulseLightAlpha())
                )
            }

            // Power Cell Level display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isCharging) Icons.Default.Bolt else Icons.Default.BatteryChargingFull,
                    contentDescription = "Battery Matrix Level",
                    tint = if (batteryLevel > 20) SafeGreen else DarkError,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "$batteryLevel% POWER",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (batteryLevel > 20) SafeGreen else DarkError
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // HUD Context selectors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            JarvisViewModel.JarvisContextType.values().forEach { contextVal ->
                val selected = currentContext == contextVal
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (selected) PrimaryCyber.copy(alpha = 0.22f) else TechCardGlass
                        )
                        .border(
                            1.dp,
                            if (selected) PrimaryCyber else BorderCyan.copy(alpha = 0.4f),
                            RoundedCornerShape(3.dp)
                        )
                        .clickable { onContextSelect(contextVal) }
                        .padding(vertical = 6.dp)
                        .testTag("hud_tab_${contextVal.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contextVal.name.take(4),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) PrimaryCyber else TextHologram.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun ArcReactorHologram(
    rotation: Float,
    coreScale: Float,
    rmsVal: Float,
    voiceState: JarvisVoiceService.VoiceState
) {
    val density = LocalDensity.current.density
    
    Canvas(
        modifier = Modifier
            .size(165.dp)
            .testTag("stark_arc_reactor")
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        // Set primary dynamic metrics based on vocals
        val speechMultiplier = if (voiceState == JarvisVoiceService.VoiceState.SPEAKING) 1.2f else 1.0f
        val glowRadius = (68f * density) * coreScale * speechMultiplier
        val ring1Radius = (55f * density) * coreScale
        val ring2Radius = (42f * density)
        val coreRadius = (22f * density) + (rmsVal * 15f * density)

        // Draw Ambient Radial Reactor Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    PrimaryCyber.copy(alpha = 0.18f + (rmsVal * 0.12f)),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = glowRadius
            ),
            radius = glowRadius,
            center = Offset(centerX, centerY)
        )

        // Outer Ring Target Nodes (Stark reactor segments)
        drawCircle(
            color = BorderCyan.copy(alpha = 0.25f),
            radius = ring1Radius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 1f * density)
        )

        // Draw rotated reactor core segments (arc sectors)
        val segments = 8
        val sweepAngle = 28f
        val gapAngle = 360f / segments
        
        for (i in 0 until segments) {
            val startAngle = (i * gapAngle) + rotation
            drawArc(
                color = PrimaryCyber.copy(alpha = 0.55f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(centerX - ring2Radius, centerY - ring2Radius),
                size = androidx.compose.ui.geometry.Size(ring2Radius * 2, ring2Radius * 2),
                style = Stroke(width = 4f * density)
            )
        }

        // Concentric Core Ring
        drawCircle(
            color = AccentHologram.copy(alpha = 0.7f),
            radius = coreRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2.5f * density)
        )

        // Center solid power node
        drawCircle(
            color = PrimaryCyber.copy(alpha = 0.85f),
            radius = 12f * density,
            center = Offset(centerX, centerY)
        )

        // Outer telemetry coordinates ticks
        val ticksCount = 12
        for (i in 0 until ticksCount) {
            val tickAngle = (i * (360f / ticksCount)) * Math.PI / 180f
            val startX = centerX + ring1Radius * Math.cos(tickAngle).toFloat()
            val startY = centerY + ring1Radius * Math.sin(tickAngle).toFloat()
            val endX = centerX + (ring1Radius + 5f * density) * Math.cos(tickAngle).toFloat()
            val endY = centerY + (ring1Radius + 5f * density) * Math.sin(tickAngle).toFloat()
            drawLine(
                color = AccentHologram.copy(alpha = 0.4f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 1.5f * density
            )
        }
    }
}

@Composable
fun ActionApprovalSection(
    action: JarvisViewModel.PendingAction,
    telecomMsg: String,
    onApprove: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(DarkError.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
            .border(1.dp, AlertOrange.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Authorization Request Gateway",
                    tint = AlertOrange,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SECURITY OVERRIDE REQUIREMENT: ${action.type.name}_PROTOCOL",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = AlertOrange
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Stream target: ${action.recipientName} (${action.detail})",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = TextHologram
            )
            
            Text(
                text = "STATUS: $telecomMsg",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = TextHologram.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("approve_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SafeGreen.copy(alpha = 0.2f),
                        contentColor = SafeGreen
                    ),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, SafeGreen.copy(alpha = 0.5f))
                ) {
                    Text("AUTHORIZE COMM", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("abort_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkError.copy(alpha = 0.2f),
                        contentColor = DarkError
                    ),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, DarkError.copy(alpha = 0.5f))
                ) {
                    Text("ABORT STREAM", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun TerminalPanel(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(TechCardGlass, RoundedCornerShape(6.dp))
            .border(1.dp, BorderCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
    ) {
        // Holographic corner bracket frames
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bracketLength = 12.dp.toPx()
            val stroke = 1.5f * density
            val color = PrimaryCyber

            // Top Left
            drawLine(color, Offset(0f, 0f), Offset(bracketLength, 0f), stroke)
            drawLine(color, Offset(0f, 0f), Offset(0f, bracketLength), stroke)

            // Top Right
            drawLine(color, Offset(size.width, 0f), Offset(size.width - bracketLength, 0f), stroke)
            drawLine(color, Offset(size.width, 0f), Offset(size.width, bracketLength), stroke)

            // Bottom Left
            drawLine(color, Offset(0f, size.height), Offset(bracketLength, size.height), stroke)
            drawLine(color, Offset(0f, size.height), Offset(0f, size.height - bracketLength), stroke)

            // Bottom Right
            drawLine(color, Offset(size.width, size.height), Offset(size.width - bracketLength, size.height), stroke)
            drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - bracketLength), stroke)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryCyber.copy(alpha = 0.08f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .drawBehind {
                        drawLine(
                            color = BorderCyan,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1f * density
                        )
                    }
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryCyber,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
        }
    }
}

@Composable
fun VoiceWaveLine(rmsVal: Float, voiceState: JarvisVoiceService.VoiceState) {
    val density = LocalDensity.current.density
    
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(vertical = 4.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        
        val targetAmplitude = when (voiceState) {
            JarvisVoiceService.VoiceState.LISTENING -> (8f + (rmsVal * 24f)) * density
            JarvisVoiceService.VoiceState.SPEAKING -> (10f + (6f * kotlin.math.sin((System.currentTimeMillis() / 200f).toDouble()).toFloat())) * density
            else -> 2f * density
        }

        val baseFrequency = (2f * Math.PI.toFloat()) / width
        val path1 = androidx.compose.ui.graphics.Path()
        val path2 = androidx.compose.ui.graphics.Path()
        val path3 = androidx.compose.ui.graphics.Path()

        path1.moveTo(0f, centerY)
        path2.moveTo(0f, centerY)
        path3.moveTo(0f, centerY)

        var x = 0f
        while (x < width) {
            // Complex Sine oscillation
            val sine1 = kotlin.math.sin((x * baseFrequency * 1.5f + System.currentTimeMillis() / 150f).toDouble()).toFloat()
            val y1 = centerY + sine1 * targetAmplitude
            path1.lineTo(x, y1)

            val sine2 = kotlin.math.cos((x * baseFrequency * 2.2f - System.currentTimeMillis() / 180f).toDouble()).toFloat()
            val y2 = centerY + sine2 * (targetAmplitude * 0.65f)
            path2.lineTo(x, y2)

            val sine3 = kotlin.math.sin((x * baseFrequency * 0.8f + System.currentTimeMillis() / 220f).toDouble()).toFloat()
            val y3 = centerY + sine3 * (targetAmplitude * 0.35f)
            path3.lineTo(x, y3)

            x += 4f * density
        }

        // Draw dynamic glowing holographic path weights
        val baseAlpha = if (voiceState == JarvisVoiceService.VoiceState.LISTENING) 0.85f else 0.65f
        
        drawPath(
            path = path1,
            color = PrimaryCyber.copy(alpha = baseAlpha),
            style = Stroke(width = 2.5f * density)
        )

        drawPath(
            path = path2,
            color = AccentHologram.copy(alpha = 0.45f),
            style = Stroke(width = 1.5f * density)
        )

        drawPath(
            path = path3,
            color = SafeGreen.copy(alpha = 0.5f),
            style = Stroke(width = 1f * density)
        )
    }
}

@Composable
fun pulseLightAlpha(): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseAlpha")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaPulse"
    )
    return alpha
}
