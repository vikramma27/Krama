package com.example.ui.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Flow-Inspired Custom Shape Tokens
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

object FlowBubbleShapes {
    // Asymmetric Sent Message Bubble: Rounded except bottom-right
    val SentBubble = RoundedCornerShape(
        topStart = CornerSize(18.dp),
        topEnd = CornerSize(18.dp),
        bottomStart = CornerSize(18.dp),
        bottomEnd = CornerSize(4.dp)
    )

    // Asymmetric Received Message Bubble: Rounded except bottom-left
    val ReceivedBubble = RoundedCornerShape(
        topStart = CornerSize(18.dp),
        topEnd = CornerSize(18.dp),
        bottomStart = CornerSize(4.dp),
        bottomEnd = CornerSize(18.dp)
    )

    // Pill shape for Search, Quick Actions, and Floating Navigation
    val NavigationPill = RoundedCornerShape(28.dp)
    val SecurityBadgeCard = RoundedCornerShape(14.dp)
}
