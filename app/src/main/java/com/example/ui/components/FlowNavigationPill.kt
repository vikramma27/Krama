package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkPlumCard
import com.example.ui.theme.QuantumTeal
import com.example.ui.theme.StellarCoral
import com.example.ui.theme.NearBlackPlum
import com.example.ui.theme.WhiteOak

/**
 * Modern, premium navigation pill with distinctive design that stands out from other chat apps.
 * Features fluid animations, elevated depth, and intuitive interaction patterns.
 */
@Composable
fun FlowNavigationPill(
    selectedTab: FlowTab,
    onTabSelected: (FlowTab) -> Unit,
    modifier: Modifier = Modifier,
    unreadChatsCount: Int = 0,
    activeCallCount: Int = 0
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp) // Increased padding for premium feel
            .shadow(8.dp, RoundedCornerShape(32.dp), spotColor = NearBlackPlum.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(32.dp))
            .testTag("flow_navigation_pill"),
        color = DarkPlumCard.copy(alpha = 0.95f), // Slight transparency for depth
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab
                    
                    // Modern animated properties
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) QuantumTeal.copy(alpha = 0.2f) else Color.Transparent,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "tabBg_${tab.name}"
                    )
                    
                    val iconScale by remember { androidx.compose.animation.core.Animatable(if (isSelected) 1.2f else 1.0f) }
                    LaunchedEffect(isSelected) {
                        iconScale.animateTo(
                            targetValue = if (isSelected) 1.2f else 1.0f,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy)
                        )
                    }
                    
                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) QuantumTeal else NearBlackPlum.copy(alpha = 0.6f),
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tabIconColor_${tab.name}"
                    )
                    
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) QuantumTeal else NearBlackPlum.copy(alpha = 0.6f),
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tabTextColor_${tab.name}"
                    )
                    
                    val textScale by remember { androidx.compose.animation.core.Animatable(if (isSelected) 1.1f else 1.0f) }
                    LaunchedEffect(isSelected) {
                        textScale.animateTo(
                            targetValue = if (isSelected) 1.1f else 1.0f,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(56.dp) // Consistent sizing for better touch targets
                            .clip(RoundedCornerShape(24.dp))
                            .background(bgColor)
                            .clickable { onTabSelected(tab) }
                            .testTag("flow_tab_${tab.name.lowercase()}")
                            .graphicsLayer {
                                scaleX = iconScale.value
                                scaleY = iconScale.value
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .size(40.dp)
                                .graphicsLayer {
                                    scaleX = textScale.value
                                    scaleY = textScale.value
                                },
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (isSelected) tab.activeIcon else tab.inactiveIcon,
                                contentDescription = tab.title,
                                tint = iconColor,
                                modifier = Modifier.size(24.dp)
                            )

                            // Enhanced unread badge with modern animation
                            if (tab == FlowTab.CHATS && unreadChatsCount > 0 && !isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = -4.dp)
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(StellarCoral)
                                )
                            }

                            // Text label with modern typography
                            if (isSelected) {
                                Text(
                                    text = tab.title,
                                    color = textColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            // Enhanced count badge with pulse animation
                            if (tab == FlowTab.CHATS && unreadChatsCount > 0 && isSelected) {
                                val pulseScale by remember { androidx.compose.animation.core.Animatable(1.0f) }
                                LaunchedEffect(Unit) {
                                    pulseScale.animateTo(
                                        targetValue = 1.3f,
                                        animationSpec = androidx.compose.animation.core.tween(
                                            durationMillis = 1000,
                                            easing = androidx.compose.animation.core.fastOutSlowInEasing
                                        )
                                    ).animateBackTo(
                                        targetValue = 1.0f,
                                        animationSpec = androidx.compose.animation.core.tween(
                                            durationMillis = 1000,
                                            easing = androidx.compose.animation.core.fastOutSlowInEasing
                                        ),
                                        repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
                                        iterations = androidx.compose.animation.core.Iterations.Infinite
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 28.dp, y = -8.dp)
                                        .graphicsLayer {
                                            scaleX = pulseScale.value
                                            scaleY = pulseScale.value
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .background(Color.White)
                                            .clip(CircleShape)
                                    ) {
                                        Text(
                                            text = "$unreadChatsCount",
                                            color = StellarCoral,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class FlowTab(val title: String, val activeIcon: ImageVector, val inactiveIcon: ImageVector) {
    CHATS("Chats", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat),
    CALLS("Calls", Icons.Filled.Call, Icons.Outlined.Call),
    CONTACTS("Contacts", Icons.Filled.People, Icons.Outlined.People),
    AI("AI", Icons.Filled.Psychology, Icons.Outlined.Psychology),
    STATUS("Status", Icons.Filled.DonutLarge, Icons.Outlined.DonutLarge)
}