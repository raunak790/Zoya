package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.SpatialAudio
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AssistantState
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPurple
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ZoyaOrbVisualizer(
    state: AssistantState,
    audioRms: Float,
    outputAmplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbAnimations")

    // Idle breathing scale
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Continuous rotation for processing ring
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Reverse rotation
    val reverseRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reverseRotation"
    )

    // Animated reactive pulse for smooth UI
    val smoothRms = remember { Animatable(0f) }
    LaunchedEffect(audioRms) {
        smoothRms.animateTo(audioRms, tween(60))
    }

    val smoothAmp = remember { Animatable(0f) }
    LaunchedEffect(outputAmplitude) {
        smoothAmp.animateTo(outputAmplitude, tween(60))
    }

    val currentRmsVal = smoothRms.value
    val currentAmpVal = smoothAmp.value

    val primaryGlowColor = when (state) {
        AssistantState.IDLE -> NeonCyan
        AssistantState.LISTENING -> NeonEmerald
        AssistantState.PROCESSING -> NeonPurple
        AssistantState.SPEAKING -> NeonMagenta
        AssistantState.ERROR -> NeonAmber
    }

    val secondaryGlowColor = when (state) {
        AssistantState.IDLE -> NeonPurple
        AssistantState.LISTENING -> NeonCyan
        AssistantState.PROCESSING -> NeonCyan
        AssistantState.SPEAKING -> NeonPurple
        AssistantState.ERROR -> Color(0xFFFF4444)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(280.dp)
            .testTag("zoya_orb_container")
    ) {
        // Multi-layer dynamic Canvas
        Canvas(modifier = Modifier.size(280.dp)) {
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension / 3.4f

            when (state) {
                AssistantState.IDLE -> {
                    // Subtle breathing radial aura
                    val auraRadius = baseRadius * (1.15f * breathingScale)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryGlowColor.copy(alpha = 0.35f),
                                secondaryGlowColor.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = centerOffset,
                            radius = auraRadius * 1.5f
                        ),
                        center = centerOffset,
                        radius = auraRadius * 1.5f
                    )

                    // Outer thin dashed halo
                    drawCircle(
                        color = primaryGlowColor.copy(alpha = 0.3f),
                        radius = baseRadius * 1.25f * breathingScale,
                        center = centerOffset,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), rotationAngle)
                        )
                    )
                }

                AssistantState.LISTENING -> {
                    // Reactive acoustic waveform expanding from user voice
                    val boost = 1f + (currentRmsVal * 0.8f)
                    val auraRadius = baseRadius * 1.4f * boost

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NeonEmerald.copy(alpha = 0.5f),
                                NeonCyan.copy(alpha = 0.2f),
                                Color.Transparent
                            ),
                            center = centerOffset,
                            radius = auraRadius
                        ),
                        center = centerOffset,
                        radius = auraRadius
                    )

                    // Draw 3 layered soundwave ripples
                    for (i in 1..3) {
                        val rippleRadius = baseRadius * (1.1f + (i * 0.2f * boost))
                        val alpha = (0.7f - (i * 0.18f)).coerceIn(0.1f, 0.8f)
                        drawCircle(
                            color = NeonEmerald.copy(alpha = alpha),
                            radius = rippleRadius,
                            center = centerOffset,
                            style = Stroke(width = (3.dp - (i * 0.5f).dp).toPx())
                        )
                    }

                    // Spiky soundwave points around the perimeter
                    val pointCount = 24
                    for (idx in 0 until pointCount) {
                        val angle = (idx.toDouble() / pointCount) * 2 * PI
                        val spike = (sin(angle * 4 + rotationAngle) * currentRmsVal * 30f).toFloat()
                        val r1 = baseRadius * 1.15f
                        val r2 = r1 + 10f + spike
                        val start = Offset(
                            (centerOffset.x + r1 * cos(angle)).toFloat(),
                            (centerOffset.y + r1 * sin(angle)).toFloat()
                        )
                        val end = Offset(
                            (centerOffset.x + r2 * cos(angle)).toFloat(),
                            (centerOffset.y + r2 * sin(angle)).toFloat()
                        )
                        drawLine(
                            color = NeonCyan.copy(alpha = 0.8f),
                            start = start,
                            end = end,
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                }

                AssistantState.PROCESSING -> {
                    // Pulsing cybernetic vortex
                    val auraRadius = baseRadius * 1.35f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NeonPurple.copy(alpha = 0.5f),
                                NeonCyan.copy(alpha = 0.2f),
                                Color.Transparent
                            ),
                            center = centerOffset,
                            radius = auraRadius
                        ),
                        center = centerOffset,
                        radius = auraRadius
                    )

                    // Inner spinning dashed ring
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(NeonPurple, NeonCyan, NeonMagenta, NeonPurple)
                        ),
                        radius = baseRadius * 1.25f,
                        center = centerOffset,
                        style = Stroke(
                            width = 4.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 20f), rotationAngle * 3)
                        )
                    )

                    // Outer counter-rotating dashed ring
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.6f),
                        radius = baseRadius * 1.45f,
                        center = centerOffset,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 25f), reverseRotation * 2)
                        )
                    )
                }

                AssistantState.SPEAKING -> {
                    // Dynamic harmonic waveform matching Zoya's speech
                    val voiceBoost = 1f + (currentAmpVal * 0.9f)
                    val auraRadius = baseRadius * 1.55f * voiceBoost

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NeonMagenta.copy(alpha = 0.55f),
                                NeonPurple.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            center = centerOffset,
                            radius = auraRadius
                        ),
                        center = centerOffset,
                        radius = auraRadius
                    )

                    // Undulating harmonic rings
                    for (i in 1..4) {
                        val phaseOffset = (i * PI / 4).toDouble()
                        val ringR = baseRadius * (1f + (i * 0.15f * voiceBoost))
                        drawCircle(
                            brush = Brush.linearGradient(
                                colors = listOf(NeonMagenta, NeonCyan)
                            ),
                            radius = ringR,
                            center = centerOffset,
                            style = Stroke(width = (4f - i * 0.6f).dp.toPx())
                        )
                    }
                }

                AssistantState.ERROR -> {
                    drawCircle(
                        color = NeonAmber.copy(alpha = 0.25f),
                        radius = baseRadius * 1.2f,
                        center = centerOffset
                    )
                }
            }
        }

        // Central Glassmorphic Core Button
        val coreScale = when (state) {
            AssistantState.IDLE -> breathingScale
            AssistantState.LISTENING -> (1f + (currentRmsVal * 0.25f)).coerceIn(1f, 1.3f)
            AssistantState.SPEAKING -> (1f + (currentAmpVal * 0.25f)).coerceIn(1f, 1.3f)
            AssistantState.PROCESSING -> 1.05f
            AssistantState.ERROR -> 0.98f
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(140.dp)
                .scale(coreScale)
                .shadow(elevation = 24.dp, shape = CircleShape, spotColor = primaryGlowColor)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1E2238),
                            Color(0xFF121422)
                        )
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = primaryGlowColor),
                    onClick = onClick
                )
                .testTag("zoya_orb_button")
        ) {
            // Inner gradient border
            Canvas(modifier = Modifier.size(140.dp)) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            primaryGlowColor,
                            secondaryGlowColor,
                            primaryGlowColor
                        )
                    ),
                    radius = (size.minDimension / 2f) - 2f,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // Central State Icon
            val icon = when (state) {
                AssistantState.IDLE -> Icons.Rounded.Mic
                AssistantState.LISTENING -> Icons.Rounded.GraphicEq
                AssistantState.PROCESSING -> Icons.Rounded.Psychology
                AssistantState.SPEAKING -> Icons.Rounded.SpatialAudio
                AssistantState.ERROR -> Icons.Rounded.Mic
            }

            Icon(
                imageVector = icon,
                contentDescription = "Zoya Voice Orb (${state.name})",
                tint = primaryGlowColor,
                modifier = Modifier.size(54.dp)
            )
        }
    }
}
