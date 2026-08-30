package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SoftTeal
import com.example.ui.theme.WarmCoral

@Composable
fun BurndownStatusRing(
    timestamp: Long,
    expiresAt: Long,
    isViewed: Boolean,
    modifier: Modifier = Modifier,
    ringSize: Dp = 56.dp,
    strokeWidth: Dp = 3.dp,
    content: @Composable () -> Unit
) {
    val now = System.currentTimeMillis()
    val totalLife = (expiresAt - timestamp).coerceAtLeast(1L)
    val remainingLife = (expiresAt - now).coerceIn(0L, totalLife)
    val fractionRemaining = remainingLife.toFloat() / totalLife.toFloat()

    val ringColor = if (isViewed) SoftTeal else WarmCoral

    Box(
        modifier = modifier.size(ringSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(ringSize)) {
            val sweepAngle = 360f * fractionRemaining

            // Background subtle track
            drawArc(
                color = ringColor.copy(alpha = 0.2f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )

            // Depleting Burn-Down Arc
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }

        Box(
            modifier = Modifier
                .padding(strokeWidth + 2.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
