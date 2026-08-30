package com.example.ui.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Krama Modern Design System - Shape Tokens
 * Refined, modern shapes with premium feel and excellent accessibility
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),     // Slightly reduced for modern feel
    large = RoundedCornerShape(16.dp),      // Standard radius for cards
    extraLarge = RoundedCornerShape(24.dp)  // For special containers
)

object KramaShapes {
    // Premium chat bubble shapes with modern asymmetry
    val SentBubble = RoundedCornerShape(
        topStart = CornerSize(16.dp),
        topEnd = CornerSize(16.dp),
        bottomStart = CornerSize(16.dp),
        bottomEnd = CornerSize(4.dp)
    )

    val ReceivedBubble = RoundedCornerShape(
        topStart = CornerSize(16.dp),
        topEnd = CornerSize(16.dp),
        bottomStart = CornerSize(4.dp),
        bottomEnd = CornerSize(16.dp)
    )

    // Modern pill shape for navigation and action containers
    val NavigationPill = RoundedCornerShape(28.dp)
    
    // Soft rounded shape for buttons and interactive elements
    val InteractiveShape = RoundedCornerShape(12.dp)
    
    // Circular shapes for avatars and badges
    val CircleShape = CircleShape
    
    // Security-focused shapes with enhanced visibility
    val SecurityBadge = RoundedCornerShape(8.dp)
    val ActionContainer = RoundedCornerShape(20.dp)
}