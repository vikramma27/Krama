package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.SoftTeal
import com.example.ui.theme.WarmCoral
import com.example.util.AudioAmplitudeMonitor

/**
 * Real-time audio waveform visualizer component that uses microphone input amplitude
 * to display dynamic animated bars during active calls.
 */
@Composable
fun AudioWaveformVisualizer(
    isMuted: Boolean,
    isCallConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val amplitude by AudioAmplitudeMonitor.instance.amplitude.collectAsStateWithLifecycle()
    val waveformBars by AudioAmplitudeMonitor.instance.waveformBars.collectAsStateWithLifecycle()

    DisposableEffect(isCallConnected, isMuted) {
        if (isCallConnected && !isMuted) {
            AudioAmplitudeMonitor.instance.startMonitoring(context)
        } else {
            AudioAmplitudeMonitor.instance.stopMonitoring()
        }
        onDispose {
            AudioAmplitudeMonitor.instance.stopMonitoring()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(vertical = 8.dp)
            .testTag("audio_waveform_visualizer")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.height(44.dp)
        ) {
            waveformBars.forEachIndexed { idx, barFactor ->
                val activeHeight = if (isCallConnected && !isMuted) {
                    (barFactor * 36.dp.value).coerceIn(6f, 40f).dp
                } else {
                    6.dp
                }

                val animatedHeight by animateDpAsState(
                    targetValue = activeHeight,
                    animationSpec = tween(durationMillis = 60),
                    label = "waveform_bar_$idx"
                )

                val barColor = when {
                    isMuted -> WarmCoral.copy(alpha = 0.6f)
                    !isCallConnected -> Color.Gray.copy(alpha = 0.4f)
                    barFactor > 0.6f -> SoftTeal
                    else -> SoftTeal.copy(alpha = 0.75f)
                }

                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .height(animatedHeight)
                        .clip(RoundedCornerShape(3.dp))
                        .background(barColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = when {
                isMuted -> "Microphone Muted"
                !isCallConnected -> "Audio Channel Ready"
                amplitude > 0.4f -> "🎙️ Live Audio Input • High Volume (${(amplitude * 100).toInt()}%)"
                amplitude > 0.1f -> "🎙️ Live Audio Input • Speaking (${(amplitude * 100).toInt()}%)"
                else -> "🎙️ Live Audio Input • Active"
            },
            color = if (isMuted) WarmCoral else SoftTeal.copy(alpha = 0.9f),
            fontSize = 11.sp,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
