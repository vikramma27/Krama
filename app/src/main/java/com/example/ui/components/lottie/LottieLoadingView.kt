package com.example.ui.components.lottie

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*

/**
 * General Lottie Loading Spinner View.
 */
@Composable
fun LottieLoadingView(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Url("https://assets2.lottiefiles.com/packages/lf20_us149vso.json")
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.fillMaxSize()
        )
    }
}
