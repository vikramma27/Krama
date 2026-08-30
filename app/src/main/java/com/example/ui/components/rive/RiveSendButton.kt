package com.example.ui.components.rive

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.WarmCoral

/**
 * Interactive Send Button Component for Compose UI.
 * Features spring motion physics and smooth state transitions.
 */
@Composable
fun RiveSendButton(
    enabled: Boolean = true,
    isSending: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val scale by animateFloatAsState(
        targetValue = if (isSending) 0.85f else if (enabled) 1.0f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "sendScale"
    )

    val rotation by animateFloatAsState(
        targetValue = if (isSending) -45f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "sendRotation"
    )

    val buttonColor = if (enabled) WarmCoral else Color.Gray.copy(alpha = 0.4f)

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(buttonColor)
            .clickable(
                enabled = enabled && !isSending,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isSending) Icons.Default.Check else Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send Message",
            tint = Color.White,
            modifier = Modifier
                .size(22.dp)
                .rotate(rotation)
        )
    }
}
