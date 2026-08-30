package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable KRAMA Logo component matching the official brand identity:
 * Teal-to-Navy circular gradient background, white outline 'K' with circuit node connections,
 * attached speech bubble at bottom right, and KRAMA brand typography.
 */
@Composable
fun KramaLogo(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    showText: Boolean = true,
    textColor: Color = Color.White
) {
    val tealLight = Color(0xFF00CFA9)
    val tealMedium = Color(0xFF0D5D6E)
    val navyDark = Color(0xFF081B38)
    val nodeGlow = Color(0xFF80E8DD)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.testTag("krama_logo_component")
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val w = this.size.width
                val h = this.size.height
                val radius = w / 2f

                // 1. Background Gradient (Teal-to-Cyan)
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF00BCD4), Color(0xFF0097A7), Color(0xFF006064))
                    ),
                    radius = radius,
                    center = Offset(w / 2f, h / 2f)
                )

                // 2. White Outer Boundary Ring
                drawCircle(
                    color = Color.White.copy(alpha = 0.25f),
                    radius = radius - 1.5f,
                    center = Offset(w / 2f, h / 2f),
                    style = Stroke(width = 1.8f)
                )

                // 3. Bold Geometric 'K' with Golden Gradient
                val kScaleX = w / 108f
                val kScaleY = h / 108f
                val kPath = Path().apply {
                    moveTo(32f * kScaleX, 22f * kScaleY)
                    lineTo(44f * kScaleX, 22f * kScaleY)
                    lineTo(44f * kScaleX, 47f * kScaleY)
                    lineTo(66f * kScaleX, 22f * kScaleY)
                    lineTo(80f * kScaleX, 22f * kScaleY)
                    lineTo(52f * kScaleX, 52f * kScaleY)
                    lineTo(80f * kScaleX, 86f * kScaleY)
                    lineTo(66f * kScaleX, 86f * kScaleY)
                    lineTo(44f * kScaleX, 60f * kScaleY)
                    lineTo(44f * kScaleX, 86f * kScaleY)
                    lineTo(32f * kScaleX, 86f * kScaleY)
                    close()
                }

                drawPath(
                    path = kPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFF8E1), Color(0xFFFFE082), Color(0xFFFFB300)),
                        startY = 22f * kScaleY,
                        endY = 86f * kScaleY
                    )
                )

                // 4. Circuit Node Connections (Gold/Amber Accent)
                val goldLineColor = Color(0xFFFFD54F)
                val goldNodeColor = Color(0xFFFFCA28)
                val creamCenterColor = Color(0xFFFFECB3)

                // Bottom-left circuit line & node
                drawLine(
                    color = goldLineColor,
                    start = Offset(38f * kScaleX, 86f * kScaleY),
                    end = Offset(38f * kScaleX, 94f * kScaleY),
                    strokeWidth = 4f * kScaleX,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = goldNodeColor,
                    radius = 5f * kScaleX,
                    center = Offset(38f * kScaleX, 94f * kScaleY)
                )

                // Right upper arm circuit line & node
                drawLine(
                    color = goldLineColor,
                    start = Offset(68f * kScaleX, 30f * kScaleY),
                    end = Offset(78f * kScaleX, 30f * kScaleY),
                    strokeWidth = 3.2f * kScaleX,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = goldNodeColor,
                    radius = 4.8f * kScaleX,
                    center = Offset(78f * kScaleX, 30f * kScaleY)
                )

                // Center accent node
                drawCircle(
                    color = creamCenterColor,
                    radius = 3.5f * kScaleX,
                    center = Offset(52f * kScaleX, 52f * kScaleY)
                )
            }
        }

        if (showText) {
            Spacer(modifier = Modifier.height((size.value * 0.08f).dp))
            Text(
                text = "KRAMA",
                color = textColor,
                fontSize = (size.value * 0.20f).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (size.value * 0.06f).sp
            )
        }
    }
}

