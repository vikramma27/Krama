package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup

@Composable
fun MessageBubbleBalloonMenu(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    onReactionSelect: (String) -> Unit,
    onReplyClick: () -> Unit,
    onPinClick: () -> Unit,
    onForwardClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    if (!isVisible) return

    Popup(
        onDismissRequest = onDismissRequest,
        alignment = Alignment.TopCenter
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Quick Emoji Reaction Bar
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("❤️", "👍", "🔥", "😂", "😮", "🙏").forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 22.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    onReactionSelect(emoji)
                                    onDismissRequest()
                                }
                                .padding(4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(6.dp))

                // Menu Options List
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    BalloonMenuItem(
                        icon = Icons.AutoMirrored.Filled.Reply,
                        label = "Reply",
                        onClick = {
                            onReplyClick()
                            onDismissRequest()
                        }
                    )
                    BalloonMenuItem(
                        icon = Icons.Default.PushPin,
                        label = "Pin Message",
                        onClick = {
                            onPinClick()
                            onDismissRequest()
                        }
                    )
                    BalloonMenuItem(
                        icon = Icons.Default.Shortcut,
                        label = "Forward",
                        onClick = {
                            onForwardClick()
                            onDismissRequest()
                        }
                    )
                    BalloonMenuItem(
                        icon = Icons.Default.Delete,
                        label = "Delete",
                        isDestructive = true,
                        onClick = {
                            onDeleteClick()
                            onDismissRequest()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BalloonMenuItem(
    icon: ImageVector,
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val contentColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 14.sp,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
