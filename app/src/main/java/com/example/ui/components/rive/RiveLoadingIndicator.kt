package com.example.ui.components.rive

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.FlowAccentTeal
import com.example.ui.theme.FlowPrimary
import kotlin.math.cos
import kotlin.math.sin

/**
 * Rive Loading & Thinking State Indicator.
 * Renders smooth glowing orbital thinking indicator with spring motion dynamics.
 */
@Composable
fun RiveLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    color: Color = FlowAccentTeal
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RiveLoadingTransition")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RiveRotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RivePulse"
    )

    val strokeWidth = size * 0.12f

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = (this.size.width - strokeWidth.toPx()) / 2f

            // Outer glowing ring
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        color.copy(alpha = 0.1f),
                        color,
                        FlowPrimary,
                        color.copy(alpha = 0.1f)
                    )
                ),
                startAngle = rotation,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )

            // Inner orbital node
            val angleRad = Math.toRadians(rotation.toDouble())
            val orbitX = center.x + radius * 0.6f * cos(angleRad).toFloat()
            val orbitY = center.y + radius * 0.6f * sin(angleRad).toFloat()

            drawCircle(
                color = color,
                radius = (strokeWidth.toPx() * 0.8f) * pulseScale,
                center = Offset(orbitX, orbitY)
            )
        }
    }
}
