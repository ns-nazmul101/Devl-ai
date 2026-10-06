package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCrimson

@Composable
fun VoiceVisualizer(
    isListening: Boolean,
    isSpeaking: Boolean,
    rmsLevel: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )

    val barCount = 18

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(horizontal = 24.dp)
    ) {
        val totalWidth = size.width
        val canvasHeight = size.height
        val barWidth = (totalWidth / barCount) * 0.6f
        val spacing = (totalWidth / barCount) * 0.4f

        val active = isListening || isSpeaking
        val baseMultiplier = if (active) (0.25f + rmsLevel * 0.75f) else 0.08f

        for (i in 0 until barCount) {
            val distFromCenter = Math.abs(i - (barCount / 2f)) / (barCount / 2f)
            val bellFactor = (1f - distFromCenter * 0.6f).coerceIn(0.1f, 1f)
            val waveOscillation = if (active) {
                Math.sin((i.toDouble() / barCount * 2 * Math.PI) + (phase * Math.PI)).toFloat() * 0.3f
            } else {
                0f
            }

            val barHeight = ((canvasHeight * baseMultiplier * bellFactor) + (waveOscillation * canvasHeight * 0.4f))
                .coerceIn(4.dp.toPx(), canvasHeight)

            val x = i * (barWidth + spacing)
            val y = (canvasHeight - barHeight) / 2f

            val brush = Brush.verticalGradient(
                colors = listOf(
                    NeonCrimson,
                    ElectricViolet,
                    CyberCyan
                ),
                startY = y,
                endY = y + barHeight
            )

            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
