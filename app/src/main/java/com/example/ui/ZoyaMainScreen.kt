package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AssistantState
import com.example.ui.components.PermissionsOnboardingSheet
import com.example.ui.components.ZoyaOrbVisualizer
import com.example.ui.components.ZoyaToolTesterSheet
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ZoyaDarkBackground
import com.example.ui.theme.ZoyaSurface
import com.example.ui.theme.ZoyaSurfaceVariant
import com.example.viewmodel.ZoyaViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ZoyaMainScreen(
    viewModel: ZoyaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showPermissionsDialog by remember { mutableStateOf(!uiState.permissionsGranted) }
    var showToolTesterDialog by remember { mutableStateOf(false) }

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        color = ZoyaDarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- TOP BAR ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Brand Info
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(NeonMagenta, NeonCyan))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Z",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ZOYA",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonMagenta,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NeonMagenta.copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "gemini-3.1-flash-live • bi-directional",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Top Action Buttons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showToolTesterDialog = true },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Build,
                                contentDescription = "Test Tools",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { showPermissionsDialog = true },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = "Permissions",
                                tint = NeonMagenta,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Status Badge Indicator
                val statusDotColor = when (uiState.state) {
                    AssistantState.IDLE -> NeonCyan
                    AssistantState.LISTENING -> NeonEmerald
                    AssistantState.PROCESSING -> NeonPurple
                    AssistantState.SPEAKING -> NeonMagenta
                    AssistantState.ERROR -> NeonAmber
                }

                val statusText = when (uiState.state) {
                    AssistantState.IDLE -> if (uiState.isWakeWordActive) "Online • Wake-word 'Hey Zoya' active" else "Idle • Tap Orb to awaken"
                    AssistantState.LISTENING -> "Listening to your voice..."
                    AssistantState.PROCESSING -> "Zoya is thinking..."
                    AssistantState.SPEAKING -> "Zoya is speaking (Interrupt anytime)"
                    AssistantState.ERROR -> "Offline or Connection error"
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ZoyaSurfaceVariant)
                        .border(1.dp, statusDotColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                            .then(
                                if (uiState.state != AssistantState.IDLE) {
                                    Modifier.alpha(pulseAlpha)
                                } else Modifier
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
            }

            // --- CENTER: ORB PRESENCE & SUBTITLE ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Interactive Animated Voice Orb
                ZoyaOrbVisualizer(
                    state = uiState.state,
                    audioRms = uiState.audioRms,
                    outputAmplitude = uiState.outputAmplitude,
                    onClick = {
                        val hasMic = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.RECORD_AUDIO
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (!hasMic) {
                            showPermissionsDialog = true
                        } else {
                            viewModel.onOrbClicked(context)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Sassy Subtitle / Conversation Pill
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = ZoyaSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                listOf(NeonMagenta.copy(alpha = 0.4f), NeonCyan.copy(alpha = 0.4f))
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (uiState.state == AssistantState.SPEAKING) Icons.AutoMirrored.Rounded.VolumeUp else Icons.Rounded.Hearing,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ZOYA'S VOICE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = uiState.currentSubtitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            lineHeight = 21.sp
                        )
                    }
                }

                // Tool execution announcement banner
                AnimatedVisibility(
                    visible = uiState.lastToolAction != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    uiState.lastToolAction?.let { actionDesc ->
                        Row(
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonEmerald.copy(alpha = 0.15f))
                                .border(1.dp, NeonEmerald.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = null,
                                tint = NeonEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Native Tool: $actionDesc",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeonEmerald
                            )
                        }
                    }
                }
            }

            // --- BOTTOM CONTROLS & DOCK ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Quick voice prompt suggestions
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    maxItemsInEachRow = 3
                ) {
                    VoicePromptChip(text = "“Open YouTube”") {
                        viewModel.executeQuickTestTool("openYouTube", context)
                    }
                    VoicePromptChip(text = "“Call Mom”") {
                        viewModel.executeQuickTestTool("callContact", context)
                    }
                    VoicePromptChip(text = "“WhatsApp Friend”") {
                        viewModel.executeQuickTestTool("whatsappMsg", context)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Dock Container
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = ZoyaSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ZoyaSurfaceVariant, RoundedCornerShape(22.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Wake Word Toggle
                        DockButton(
                            icon = if (uiState.isWakeWordActive) Icons.Rounded.Mic else Icons.Rounded.MicOff,
                            label = if (uiState.isWakeWordActive) "Wake 'Zoya': ON" else "Wake 'Zoya': OFF",
                            isActive = uiState.isWakeWordActive,
                            activeColor = NeonCyan,
                            onClick = { viewModel.setWakeWordEnabled(!uiState.isWakeWordActive) }
                        )

                        // Central Stop / Push to Talk
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    if (uiState.state != AssistantState.IDLE) NeonMagenta else NeonCyan.copy(alpha = 0.2f)
                                )
                                .clickable {
                                    val hasMic = androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.RECORD_AUDIO
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    if (!hasMic) {
                                        showPermissionsDialog = true
                                    } else {
                                        viewModel.onOrbClicked(context)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (uiState.state != AssistantState.IDLE) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                contentDescription = "Toggle session",
                                tint = if (uiState.state != AssistantState.IDLE) Color.White else NeonCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Background Service Toggle
                        DockButton(
                            icon = Icons.Rounded.Bolt,
                            label = if (uiState.isForegroundServiceRunning) "24/7 BG: ON" else "24/7 BG: OFF",
                            isActive = uiState.isForegroundServiceRunning,
                            activeColor = NeonEmerald,
                            onClick = { viewModel.toggleForegroundService(context) }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showPermissionsDialog) {
        PermissionsOnboardingSheet(
            onDismiss = { showPermissionsDialog = false },
            onPermissionsUpdated = { granted ->
                viewModel.onPermissionsUpdated(granted)
            }
        )
    }

    if (showToolTesterDialog) {
        ZoyaToolTesterSheet(
            recentActions = uiState.actionHistory,
            onExecuteTest = { toolName ->
                viewModel.executeQuickTestTool(toolName, context)
            },
            onDismiss = { showToolTesterDialog = false }
        )
    }
}

@Composable
private fun VoicePromptChip(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ZoyaSurfaceVariant)
            .border(1.dp, NeonCyan.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
    }
}

@Composable
private fun DockButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isActive) activeColor.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) activeColor else TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isActive) activeColor else TextSecondary
        )
    }
}
