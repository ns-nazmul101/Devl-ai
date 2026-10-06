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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGold
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCrimson
import com.example.ui.viewmodel.AiStatusState

@Composable
fun AiAvatar(
    status: AiStatusState,
    size: Dp = 96.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_anim")

    // Rotation angle
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Pulse scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = if (status == AiStatusState.LISTENING || status == AiStatusState.THINKING) 1.12f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (status == AiStatusState.THINKING) 700 else 1400,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val primaryAuraColor = when (status) {
        AiStatusState.LISTENING -> CyberCyan
        AiStatusState.THINKING -> ElectricViolet
        AiStatusState.EXECUTING -> CyberGold
        AiStatusState.COMPLETED -> CyberCyan
        AiStatusState.ERROR -> NeonCrimson
        AiStatusState.IDLE -> NeonCrimson
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing pulse ring
        Canvas(modifier = Modifier.size(size * pulseScale)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2f - 4f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryAuraColor.copy(alpha = 0.35f), Color.Transparent),
                    center = center,
                    radius = radius * 1.2f
                ),
                radius = radius,
                center = center
            )

            // Animated orbit ring
            drawCircle(
                color = primaryAuraColor.copy(alpha = 0.5f),
                radius = radius - 6f,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Inner Cyber Core Sphere
        Box(
            modifier = Modifier
                .size(size * 0.72f)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            NeonCrimson,
                            ElectricViolet,
                            Color(0xFF240046)
                        )
                    )
                )
                .border(2.dp, primaryAuraColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // High-tech AI devil visor / core
            Canvas(modifier = Modifier.size(size * 0.45f)) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                // Center neural node
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = center
                )

                // Cyber horns lines
                drawLine(
                    color = CyberCyan,
                    start = Offset(center.x - 12f, center.y - 4f),
                    end = Offset(center.x - 22f, center.y - 20f),
                    strokeWidth = 3f
                )
                drawLine(
                    color = CyberCyan,
                    start = Offset(center.x + 12f, center.y - 4f),
                    end = Offset(center.x + 22f, center.y - 20f),
                    strokeWidth = 3f
                )

                // Voice aperture slit
                drawLine(
                    color = Color.White,
                    start = Offset(center.x - 14f, center.y + 10f),
                    end = Offset(center.x + 14f, center.y + 10f),
                    strokeWidth = 2.5f
                )
            }
        }
    }
}
