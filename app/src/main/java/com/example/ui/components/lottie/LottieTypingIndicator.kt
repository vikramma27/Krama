package com.example.ui.components.lottie

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import com.example.ui.theme.SoftTeal

/**
 * Lottie & Animated Typing Indicator for active chat threads.
 */
@Composable
fun LottieTypingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Url("https://assets9.lottiefiles.com/packages/lf20_t9gkkh2s.json")
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    if (composition != null) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = modifier.size(size)
        )
    } else {
        // Fallback Animated Dots
        val infiniteTransition = rememberInfiniteTransition(label = "TypingDotsTransition")

        val dot1Alpha by infiniteTransition.animateFloat(
            initialValue = 0.2f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
            label = "dot1"
        )
        val dot2Alpha by infiniteTransition.animateFloat(
            initialValue = 0.2f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(600, delayMillis = 200, easing = LinearEasing), RepeatMode.Reverse),
            label = "dot2"
        )
        val dot3Alpha by infiniteTransition.animateFloat(
            initialValue = 0.2f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(600, delayMillis = 400, easing = LinearEasing), RepeatMode.Reverse),
            label = "dot3"
        )

        Row(
            modifier = modifier.height(20.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SoftTeal.copy(alpha = dot1Alpha)))
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SoftTeal.copy(alpha = dot2Alpha)))
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SoftTeal.copy(alpha = dot3Alpha)))
        }
    }
}
