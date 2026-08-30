package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.ui.theme.WarmCoral

enum class FlowTab(val title: String, val activeIcon: ImageVector, val inactiveIcon: ImageVector) {
    CHATS("Chats", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat),
    CALLS("Calls", Icons.Filled.Call, Icons.Outlined.Call),
    CONTACTS("Contacts", Icons.Filled.People, Icons.Outlined.People),
    AI("AI", Icons.Filled.Psychology, Icons.Outlined.Psychology),
    STATUS("Status", Icons.Filled.DonutLarge, Icons.Outlined.DonutLarge)
}

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
            .padding(horizontal = 16.dp)
            .shadow(20.dp, RoundedCornerShape(28.dp), spotColor = WarmCoral.copy(alpha = 0.5f))
            .clip(RoundedCornerShape(28.dp))
            .testTag("flow_navigation_pill"),
        color = DarkPlumCard,
        tonalElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlowTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) WarmCoral else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "tabBg"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    label = "tabContent"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(bgColor)
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = if (isSelected) 14.dp else 10.dp, vertical = 10.dp)
                        .testTag("flow_tab_${tab.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = if (isSelected) tab.activeIcon else tab.inactiveIcon,
                                contentDescription = tab.title,
                                tint = contentColor,
                                modifier = Modifier.size(22.dp)
                            )

                            // Unread badge dot for unselected chats tab
                            if (tab == FlowTab.CHATS && unreadChatsCount > 0 && !isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(WarmCoral)
                                )
                            }
                        }

                        if (isSelected) {
                            Text(
                                text = tab.title,
                                color = contentColor,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                        }

                        // Count badge for selected chats tab
                        if (tab == FlowTab.CHATS && unreadChatsCount > 0 && isSelected) {
                            val badgeScale = remember { androidx.compose.animation.core.Animatable(0.6f) }
                            LaunchedEffect(unreadChatsCount) {
                                badgeScale.animateTo(
                                    targetValue = 1.0f,
                                    animationSpec = androidx.compose.animation.core.spring(
                                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = badgeScale.value
                                        scaleY = badgeScale.value
                                    }
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$unreadChatsCount",
                                    color = WarmCoral,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
