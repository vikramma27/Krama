package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.MessageEntity
import com.example.ui.theme.DarkPlumCard
import com.example.ui.theme.NearBlackPlum
import com.example.ui.theme.SoftTeal
import com.example.ui.theme.WarmCoral
import com.example.util.LocalAnalyticsTracker
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    chats: List<ChatEntity>,
    messages: List<MessageEntity>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val featureStats = remember { LocalAnalyticsTracker.getInstance(context).getStats() }

    var isAnimated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isAnimated = true
    }

    // Calculations based strictly on encrypted local Room DB entries
    val totalMessages = messages.size
    val totalChats = chats.size

    // Message volume per contact
    val contactVolumeMap = remember(chats, messages) {
        chats.associate { chat ->
            val count = messages.count { it.chatId == chat.id }
            chat.title to count
        }.filterValues { it > 0 }.toList().sortedByDescending { it.second }
    }

    // Peak usage hours (24-hour distribution)
    val hourCounts = remember(messages) {
        val counts = IntArray(24) { 0 }
        messages.forEach { msg ->
            val cal = Calendar.getInstance().apply { timeInMillis = msg.timestamp }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            if (hour in 0..23) {
                counts[hour]++
            }
        }
        counts
    }

    val maxHourCount = hourCounts.maxOrNull()?.coerceAtLeast(1) ?: 1
    val peakHour = hourCounts.indices.maxByOrNull { hourCounts[it] } ?: 12

    // Message Composition Breakdown
    val textCount = messages.count { it.messageType == "TEXT" || it.messageType == "SCHEDULED" }
    val voiceCount = messages.count { it.messageType == "VOICE" }
    val mediaCount = messages.count { it.messageType == "IMAGE" || it.messageType == "DOCUMENT" }
    val stegoCount = messages.count { it.messageType == "STEGANOGRAPHY" }
    val otherCount = (totalMessages - (textCount + voiceCount + mediaCount + stegoCount)).coerceAtLeast(0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Chat Activity Analytics",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "100% On-Device • Encrypted Room DB Insights",
                            color = SoftTeal,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("analytics_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkPlumCard)
            )
        },
        containerColor = NearBlackPlum,
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary KPI Cards Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnalyticsKpiCard(
                    title = "Total Messages",
                    value = "$totalMessages",
                    subtitle = "Stored in SQLCipher DB",
                    icon = Icons.Default.Message,
                    color = SoftTeal,
                    modifier = Modifier.weight(1f)
                )

                AnalyticsKpiCard(
                    title = "Peak Activity",
                    value = "%02d:00".format(peakHour),
                    subtitle = "${hourCounts[peakHour]} msgs in peak hour",
                    icon = Icons.Default.Schedule,
                    color = WarmCoral,
                    modifier = Modifier.weight(1f)
                )
            }

            // Message Volume per Contact (Horizontal Bar Chart)
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkPlumCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BarChart, contentDescription = null, tint = SoftTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Message Volume by Contact",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Text(
                            text = "${contactVolumeMap.size} Active",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val maxVolume = contactVolumeMap.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1

                    contactVolumeMap.take(6).forEach { (contactName, count) ->
                        val progressTarget = if (isAnimated) count.toFloat() / maxVolume.toFloat() else 0f
                        val animatedProgress by animateFloatAsState(
                            targetValue = progressTarget,
                            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                            label = "volumeBarAnim"
                        )

                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = contactName,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "$count msgs (${if (totalMessages > 0) (count * 100 / totalMessages) else 0}%)",
                                    color = SoftTeal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(NearBlackPlum)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedProgress)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(WarmCoral, SoftTeal)
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // Peak Usage Times (24-Hour Distribution Canvas Chart)
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkPlumCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Analytics, contentDescription = null, tint = WarmCoral)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Peak Usage Distribution (24h)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Text(
                            text = "Peak: %02d:00".format(peakHour),
                            color = WarmCoral,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val canvasAnimProgress by animateFloatAsState(
                        targetValue = if (isAnimated) 1f else 0f,
                        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                        label = "canvasAnim"
                    )

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        val width = size.width
                        val height = size.height
                        val barWidth = (width / 24) * 0.7f
                        val gap = (width / 24) * 0.3f

                        for (i in 0 until 24) {
                            val count = hourCounts[i]
                            val barHeight = ((count.toFloat() / maxHourCount.toFloat()) * (height - 30.dp.toPx())) * canvasAnimProgress
                            val x = i * (barWidth + gap)
                            val y = height - barHeight - 20.dp.toPx()

                            val barColor = if (i == peakHour) WarmCoral else SoftTeal

                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight.coerceAtLeast(4f)),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }

                        // Baseline
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.3f),
                            start = Offset(0f, height - 20.dp.toPx()),
                            end = Offset(width, height - 20.dp.toPx()),
                            strokeWidth = 2f
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("00:00", color = Color.Gray, fontSize = 10.sp)
                        Text("06:00", color = Color.Gray, fontSize = 10.sp)
                        Text("12:00", color = Color.Gray, fontSize = 10.sp)
                        Text("18:00", color = Color.Gray, fontSize = 10.sp)
                        Text("23:00", color = Color.Gray, fontSize = 10.sp)
                    }
                }
            }

            // Message Composition Breakdown Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkPlumCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PieChart, contentDescription = null, tint = SoftTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Message Composition Breakdown",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    CompositionProgressSegment(
                        label = "Text & Scheduled Messages",
                        count = textCount,
                        total = totalMessages,
                        color = SoftTeal
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CompositionProgressSegment(
                        label = "Voice Notes (Opus E2EE)",
                        count = voiceCount,
                        total = totalMessages,
                        color = WarmCoral
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CompositionProgressSegment(
                        label = "Photos & Attachments",
                        count = mediaCount,
                        total = totalMessages,
                        color = Color(0xFFFFB74D)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CompositionProgressSegment(
                        label = "Steganography Covert Payloads",
                        count = stegoCount,
                        total = totalMessages,
                        color = Color(0xFFBA68C8)
                    )
                }
            }

            // AI & Advanced Feature Adoption Card (100% On-Device)
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkPlumCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BarChart, contentDescription = null, tint = SoftTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI & Feature Usage Patterns",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SoftTeal.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "100% Local",
                                color = SoftTeal,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val totalFeatureInteractions = (
                        featureStats.aiSmartReplyCount +
                        featureStats.aiSummaryCount +
                        featureStats.steganographyHideCount +
                        featureStats.webRtcCallsCount +
                        featureStats.localBackupsCreatedCount
                    ).coerceAtLeast(1)

                    CompositionProgressSegment(
                        label = "Gemini AI Smart Replies",
                        count = featureStats.aiSmartReplyCount,
                        total = totalFeatureInteractions,
                        color = SoftTeal
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CompositionProgressSegment(
                        label = "AI Conversation Summaries",
                        count = featureStats.aiSummaryCount,
                        total = totalFeatureInteractions,
                        color = Color(0xFF81D4FA)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CompositionProgressSegment(
                        label = "WebRTC E2EE Calls Placed",
                        count = featureStats.webRtcCallsCount,
                        total = totalFeatureInteractions,
                        color = WarmCoral
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CompositionProgressSegment(
                        label = "Steganographic Covert Images",
                        count = featureStats.steganographyHideCount,
                        total = totalFeatureInteractions,
                        color = Color(0xFFCE93D8)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CompositionProgressSegment(
                        label = "Encrypted Local Backups Created",
                        count = featureStats.localBackupsCreatedCount,
                        total = totalFeatureInteractions,
                        color = Color(0xFFFFB74D)
                    )
                }
            }

            // WebRTC Call Quality & Connection Stability Analytics Chart
            com.example.ui.components.WebRtcStabilityChart()

            // E2EE Security Analytics Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkPlumCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SoftTeal.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SoftTeal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = SoftTeal)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Local Encryption Verification",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "SQLCipher AES-256 cipher page tables active. All analytics are computed strictly on-device without telemetry upload.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkPlumCard),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CompositionProgressSegment(
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    val percentage = if (total > 0) (count * 100 / total) else 0

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, color = Color.White, fontSize = 13.sp)
            }
            Text("$count ($percentage%)", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(NearBlackPlum)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (total > 0) count.toFloat() / total.toFloat() else 0f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}
