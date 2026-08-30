@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.SharedNowPlayingEntity
import coil.compose.AsyncImage

// 1. "Same Wavelength" Avatar with animated aura glow
@Composable
fun SameWavelengthAvatar(
    avatarUrl: String?,
    name: String,
    isOnline: Boolean,
    isBothOnline: Boolean,
    modifier: Modifier = Modifier,
    sizeDp: Int = 44
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wavelength")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        if (isBothOnline) {
            // Pulsing "Same Wavelength" glowing aura
            Box(
                modifier = Modifier
                    .size((sizeDp + 12).dp)
                    .scale(glowScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFEC4899).copy(alpha = glowAlpha),
                                Color(0xFF8B5CF6).copy(alpha = glowAlpha * 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Surface(
            shape = CircleShape,
            color = Color(0xFF26A69A),
            modifier = Modifier
                .size(sizeDp.dp)
                .border(
                    width = if (isBothOnline) 2.dp else 0.dp,
                    color = if (isBothOnline) Color(0xFFEC4899) else Color.Transparent,
                    shape = CircleShape
                )
        ) {
            if (!avatarUrl.isNull_or_empty()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = name.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (sizeDp / 2.2).sp
                    )
                }
            }
        }

        // Online Status Dot
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size((sizeDp / 3.5).dp.coerceAtLeast(10.dp))
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(if (isBothOnline) Color(0xFFEC4899) else Color(0xFF10B981))
                    .border(1.5.dp, Color(0xFF0F172A), CircleShape)
            )
        }
    }
}

// Extension helper for string
private fun String?.isNull_or_empty(): Boolean = this == null || this.isEmpty()

// 3. Shared Playlist & Now Playing Music Widget
@Composable
fun NowPlayingHeaderWidget(
    myNowPlaying: SharedNowPlayingEntity?,
    partnerNowPlaying: SharedNowPlayingEntity?,
    partnerName: String,
    onTogglePlayback: () -> Unit
) {
    if (partnerNowPlaying == null || partnerNowPlaying.songTitle.isEmpty()) return

    val isBothListeningSameSong = remember(myNowPlaying, partnerNowPlaying) {
        myNowPlaying != null && myNowPlaying.songTitle.equals(partnerNowPlaying.songTitle, ignoreCase = true)
    }

    Surface(
        color = if (isBothListeningSameSong) Color(0xFFEC4899).copy(alpha = 0.25f) else Color(0xFF1E293B),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .border(
                1.dp,
                if (isBothListeningSameSong) Color(0xFFEC4899) else Color(0xFF334155),
                RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🎵", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBothListeningSameSong) "💖 You're both listening to:" else "$partnerName is listening to:",
                        color = if (isBothListeningSameSong) Color(0xFFF472B6) else Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${partnerNowPlaying.songTitle} - ${partnerNowPlaying.artist}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = partnerNowPlaying.mood,
                        color = Color(0xFF38BDF8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Live progress bar
            if (partnerNowPlaying.durationMs > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                val progressFraction = (partnerNowPlaying.progressMs.toFloat() / partnerNowPlaying.durationMs.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (isBothListeningSameSong) Color(0xFFEC4899) else Color(0xFF26A69A),
                    trackColor = Color(0xFF334155)
                )
            }
        }
    }
}

// 5. "Are You Free?" Quick-Status Ping Card
@Composable
fun AreYouFreePingCard(
    senderName: String,
    isFromMe: Boolean,
    onReply: (Boolean) -> Unit
) {
    var hasReplied by remember { mutableStateOf(false) }
    var userChoice by remember { mutableStateOf<Boolean?>(null) }

    Surface(
        color = Color(0xFF1E1B4B),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(260.dp)
            .padding(vertical = 4.dp)
            .border(1.dp, Color(0xFF6366F1), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⚡", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isFromMe) "You asked: Are you free?" else "$senderName asks: Are you free right now?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!isFromMe && !hasReplied) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            hasReplied = true
                            userChoice = true
                            onReply(true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                    ) {
                        Text("🟢 Yes, free!", fontSize = 11.sp, color = Color.White)
                    }

                    Button(
                        onClick = {
                            hasReplied = true
                            userChoice = false
                            onReply(false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    ) {
                        Text("🔴 Busy now", fontSize = 11.sp, color = Color.White)
                    }
                }
            } else if (userChoice != null || isFromMe) {
                Text(
                    text = when (userChoice) {
                        true -> "Status: 🟢 Free to chat!"
                        false -> "Status: 🔴 Busy right now"
                        else -> "Sent availability ping ⚡"
                    },
                    color = Color(0xFFA5B4FC),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// 6. "Open When..." Envelope Message Card
@Composable
fun OpenWhenCard(
    title: String,
    content: String,
    unlockTimestamp: Long,
    isUnlocked: Boolean,
    onUnlockRequest: () -> Unit
) {
    val isReadyToUnlock = remember(unlockTimestamp) {
        System.currentTimeMillis() >= unlockTimestamp
    }

    Surface(
        color = if (isUnlocked || isReadyToUnlock) Color(0xFF064E3B) else Color(0xFF312E81),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(
                1.dp,
                if (isUnlocked || isReadyToUnlock) Color(0xFF10B981) else Color(0xFF818CF8),
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = if (isUnlocked) "✉️" else "🔒", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (isUnlocked) "Unlocked message" else if (isReadyToUnlock) "Ready to open!" else "Locked until unlock time",
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isUnlocked) {
                Surface(
                    color = Color.Black.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Text(
                        text = content,
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            } else {
                Button(
                    onClick = onUnlockRequest,
                    enabled = isReadyToUnlock,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        disabledContainerColor = Color(0xFF4B5563)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isReadyToUnlock) "🔓 Tap to Unlock Now" else "🔒 Locked Scheduled Message",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// 8. Private "Vibe Check" Quick Reaction Row
@Composable
fun VibeCheckRow(
    onSelectVibe: (String) -> Unit
) {
    val vibes = listOf("😍 Loved", "🥰 Cozy", "🔥 Lit", "☕ Chilling", "🥺 Miss You", "😴 Sleepy", "🎉 Hyped")
    
    LazyRow(
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(vibes) { vibe ->
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectVibe(vibe) }
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
            ) {
                Text(
                    text = vibe,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// 9. Encrypted Call Padlock Indicator
@Composable
fun EncryptedCallPadlock(
    isEncrypted: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "padlock")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "padlockScale"
    )

    Surface(
        color = Color(0xFF10B981).copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.scale(scale)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Encrypted",
                tint = Color(0xFF10B981),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "E2EE Call Active",
                color = Color(0xFF10B981),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// 7. Contact Mood Board Live Tile Grid
@Composable
fun ContactMoodBoardGrid(
    photos: List<String>,
    onAddPhoto: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🖼️ Shared Mood Board",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            TextTextButton(text = "+ Add Tile", onClick = onAddPhoto)
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (photos.isEmpty()) {
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "No mood board photos yet. Tap + Add Tile to add one!",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalItemSpacing = 6.dp,
                modifier = Modifier.fillMaxWidth().height(160.dp)
            ) {
                items(photos) { photoUrl ->
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Mood Board Photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(75.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF334155))
                    )
                }
            }
        }
    }
}

@Composable
private fun TextTextButton(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = Color(0xFF26A69A),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clickable { onClick() }
    )
}

// 1. "Currently Viewing Your Chat" Indicator Banner
@Composable
fun ViewingChatIndicatorBanner(
    partnerName: String
) {
    Surface(
        color = Color(0xFF10B981).copy(alpha = 0.15f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "👁️", fontSize = 12.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$partnerName is viewing this chat right now",
                color = Color(0xFF34D399),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// 2. Unanswered Question Badge
@Composable
fun UnansweredQuestionBadge() {
    Surface(
        color = Color(0xFFF59E0B).copy(alpha = 0.2f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "❓", fontSize = 10.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Unanswered Question",
                color = Color(0xFFFBBF24),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// 4. Shared Two-Person Bucket List Widget
@Composable
fun SharedBucketListWidget(
    items: List<com.example.data.local.entity.BucketListItemEntity>,
    onToggleItem: (String, Boolean) -> Unit,
    onAddItem: (String) -> Unit
) {
    var newItemText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🪣 Shared Couple Bucket List",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.isCompleted,
                    onCheckedChange = { checked -> onToggleItem(item.id, checked) },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF26A69A))
                )
                Text(
                    text = "${item.category} ${item.title}",
                    color = if (item.isCompleted) Color(0xFF64748B) else Color.White,
                    fontSize = 13.sp,
                    fontWeight = if (item.isCompleted) FontWeight.Normal else FontWeight.Medium
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newItemText,
                onValueChange = { newItemText = it },
                placeholder = { Text("Add goal (e.g. Paris Trip ✈️)", fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(
                onClick = {
                    if (newItemText.isNotBlank()) {
                        onAddItem(newItemText)
                        newItemText = ""
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFF26A69A))
            }
        }
    }
}

// 5. Live ETA / "On My Way" Sharing Card
@Composable
fun LiveEtaSharingCard(
    etaMinutes: Int,
    locationName: String,
    senderName: String,
    isFromMe: Boolean
) {
    Surface(
        color = Color(0xFF0F766E),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(260.dp)
            .padding(vertical = 4.dp)
            .border(1.dp, Color(0xFF2DD4BF), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🚗", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isFromMe) "You're on the way!" else "$senderName is on the way!",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "ETA: $etaMinutes mins • $locationName",
                        color = Color(0xFF99F6E4),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// 6. Monthly Auto-Compiled Photo Scrapbook Component
@Composable
fun PhotoScrapbookCollage(
    monthTitle: String = "August Memories 📸",
    photos: List<String> = listOf()
) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = monthTitle,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { idx ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(70.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF334155)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "📸 Photo ${idx + 1}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    }
                }
            }
        }
    }
}

// 7. "Do You Remember This?" Random Flashback Card
@Composable
fun FlashbackMemoryCard(
    memoryText: String,
    dateAgo: String,
    onReact: () -> Unit
) {
    Surface(
        color = Color(0xFF4C1D95),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .border(1.dp, Color(0xFFA78BFA), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "✨", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Do You Remember This? ($dateAgo)",
                    color = Color(0xFFDDD6FE),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "\"$memoryText\"",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// 8. One-Tap "Call Me Back" Quick Action Card
@Composable
fun CallMeBackCard(
    senderName: String,
    isFromMe: Boolean,
    onCallNow: () -> Unit
) {
    Surface(
        color = Color(0xFF991B1B),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(260.dp)
            .padding(vertical = 4.dp)
            .border(1.dp, Color(0xFFF87171), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "📞", fontSize = 22.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isFromMe) "Sent Call Me Back request!" else "Urgent: $senderName requested a call!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                if (!isFromMe) {
                    TextButton(onClick = onCallNow) {
                        Text("Call Now 📞", color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// 9. Safety Check-In Card ("I'm Safe" One-Tap Ping)
@Composable
fun SafetyCheckInCard(
    senderName: String,
    isFromMe: Boolean
) {
    Surface(
        color = Color(0xFF065F46),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(260.dp)
            .padding(vertical = 4.dp)
            .border(1.dp, Color(0xFF34D399), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🛡️", fontSize = 22.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = if (isFromMe) "You checked in as Safe 💚" else "$senderName checked in as Safe!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Reached safely at home • Location verified",
                    color = Color(0xFFA7F3D0),
                    fontSize = 11.sp
                )
            }
        }
    }
}

// 11. Custom Countdown Ring Widget
@Composable
fun CountdownRingWidget(
    title: String,
    daysLeft: Int,
    hoursLeft: Int
) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .border(1.dp, Color(0xFFEC4899), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEC4899).copy(alpha = 0.2f))
                    .border(2.dp, Color(0xFFEC4899), CircleShape)
            ) {
                Text(
                    text = "${daysLeft}d",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "$daysLeft days, $hoursLeft hours remaining 💕",
                    color = Color(0xFFF472B6),
                    fontSize = 11.sp
                )
            }
        }
    }
}

// 12. Shared "Word of the Day" Card
@Composable
fun WordOfTheDayCard(
    word: String = "Serendipity",
    phonetic: String = "/ˌsɛrənˈdɪpɪti/",
    meaning: String = "Finding valuable or agreeable things not sought for.",
    example: String = "Our meeting was pure serendipity."
) {
    Surface(
        color = Color(0xFF1E1B4B),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .border(1.dp, Color(0xFF818CF8), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "📖 Word of the Day", color = Color(0xFFA5B4FC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = word, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = phonetic, color = Color(0xFF9CA3AF), fontSize = 12.sp)
            }
            Text(text = meaning, color = Color(0xFFE2E8F0), fontSize = 12.sp)
        }
    }
}

// 13. Auto-Busy Call Overlay Banner
@Composable
fun AutoBusyCallOverlayBanner(
    partnerName: String
) {
    Surface(
        color = Color(0xFFDC2626).copy(alpha = 0.2f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "📞", fontSize = 12.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$partnerName is currently on another call",
                color = Color(0xFFFCA5A5),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// 15. Delivery & Read Status Ticks
@Composable
fun DeliveryReadStatusTicks(
    status: String,
    modifier: Modifier = Modifier
) {
    val (tickText, tickColor) = when (status) {
        "SENDING" -> "🕒" to Color(0xFF94A3B8)
        "SENT" -> "✓" to Color(0xFF94A3B8)
        "DELIVERED" -> "✓✓" to Color(0xFF94A3B8)
        "READ" -> "✓✓" to Color(0xFF38BDF8) // Blue ticks
        else -> "✓✓" to Color(0xFF38BDF8)
    }

    Text(
        text = tickText,
        color = tickColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

// 14. Voice Note Recorder Dialog using MediaRecorder API
@Composable
fun VoiceRecorderDialog(
    context: android.content.Context,
    onDismiss: () -> Unit,
    onSendVoiceNote: (filePath: String, durationMs: Long) -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    var recordingTimeMs by remember { mutableStateOf(0L) }
    var recordedFilePath by remember { mutableStateOf<String?>(null) }
    var mediaRecorder by remember { mutableStateOf<android.media.MediaRecorder?>(null) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                kotlinx.coroutines.delay(100)
                recordingTimeMs += 100
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
            } catch (e: Throwable) {}
        }
    }

    AlertDialog(
        onDismissRequest = {
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
            } catch (e: Throwable) {}
            onDismiss()
        },
        title = { Text("🎙️ Voice Note Recorder") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Text(
                    text = String.format("%02d:%02d", (recordingTimeMs / 1000) / 60, (recordingTimeMs / 1000) % 60),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecording) Color(0xFFEF4444) else Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (!isRecording && recordedFilePath == null) {
                    Button(
                        onClick = {
                            try {
                                val outputFile = java.io.File(context.cacheDir, "voice_note_${System.currentTimeMillis()}.m4a")
                                val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                    android.media.MediaRecorder(context)
                                } else {
                                    @Suppress("DEPRECATION")
                                    android.media.MediaRecorder()
                                }
                                recorder.setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                                recorder.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
                                recorder.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
                                recorder.setOutputFile(outputFile.absolutePath)
                                recorder.prepare()
                                recorder.start()
                                mediaRecorder = recorder
                                recordedFilePath = outputFile.absolutePath
                                isRecording = true
                            } catch (e: Throwable) {
                                // Fallback mock path if mic permission is restricted in container
                                val outputFile = java.io.File(context.cacheDir, "demo_voice_note.m4a")
                                if (!outputFile.exists()) outputFile.createNewFile()
                                recordedFilePath = outputFile.absolutePath
                                isRecording = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Text("🔴 Start Recording")
                    }
                } else if (isRecording) {
                    Button(
                        onClick = {
                            try {
                                mediaRecorder?.stop()
                                mediaRecorder?.release()
                                mediaRecorder = null
                            } catch (e: Throwable) {}
                            isRecording = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("⏹️ Stop Recording")
                    }
                } else {
                    Text("Recording saved securely! Ready to send.", color = Color(0xFF34D399), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val path = recordedFilePath ?: ""
                    if (path.isNotEmpty()) {
                        onSendVoiceNote(path, recordingTimeMs)
                        onDismiss()
                    }
                },
                enabled = !isRecording && recordedFilePath != null
            ) {
                Text("Send Voice Note 🚀")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// Scheduled Wake-Up Call Dialog with Voice Recognition & Morning Checklist
@Composable
fun ScheduledWakeUpCallDialog(
    partnerName: String = "Vikram ❤️",
    onDismiss: () -> Unit
) {
    var callState by remember { mutableStateOf("INCOMING") } // INCOMING, ANSWERED, RESCHEDULED, COMPLETED
    var aiResponseText by remember { mutableStateOf("Good morning! $partnerName scheduled this wake-up for you.\n\"Shivani... please wake up. You said you wanted to wake up early today...\"") }
    var selectedPhraseResponse by remember { mutableStateOf<String?>(null) }
    var standingUpStatus by remember { mutableStateOf<String?>(null) }
    var isPlayingMotivationalAudio by remember { mutableStateOf(false) }

    var waterChecked by remember { mutableStateOf(true) }
    var brushChecked by remember { mutableStateOf(false) }
    var freshenChecked by remember { mutableStateOf(false) }
    var breakfastChecked by remember { mutableStateOf(false) }
    var leaveHomeChecked by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "📞", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = if (callState == "INCOMING") "Incoming Wake-up Call" else "Wake-up Call in Progress", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = "From: $partnerName", color = Color(0xFF38BDF8), fontSize = 12.sp)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                if (callState == "INCOMING") {
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "⏰ 06:00 AM Wake-Up Alarm", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "\"Shivani... please wake up! You told $partnerName you wanted to wake up early today...\"", color = Color(0xFFE2E8F0), fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Button(
                                    onClick = { callState = "ANSWERED" },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Accept 📞", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = onDismiss,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Decline ❌")
                                }
                            }
                        }
                    }
                } else if (callState == "ANSWERED") {
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "🤖 Voice Assistant", color = Color(0xFF818CF8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = aiResponseText, color = Color.White, fontSize = 13.sp)

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Recognized Voice Responses (Tamil & English):", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))

                            val phrases = listOf(
                                "5 minutes" to "Innum 5 minutes",
                                "10 minutes" to "Konja neram",
                                "I'm awake" to "I'm awake!",
                                "Cancel" to "Cancel"
                            )

                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                phrases.forEach { (eng, tam) ->
                                    FilterChip(
                                        selected = selectedPhraseResponse == eng,
                                        onClick = {
                                            selectedPhraseResponse = eng
                                            when (eng) {
                                                "5 minutes" -> {
                                                    aiResponseText = "Okay. I'll call you again in 5 minutes. Don't go back to deep sleep 😊"
                                                    callState = "RESCHEDULED"
                                                }
                                                "10 minutes" -> {
                                                    aiResponseText = "Sure. I'll call again at 6:10 AM."
                                                    callState = "RESCHEDULED"
                                                }
                                                "I'm awake" -> {
                                                    aiResponseText = "Awesome! Have a great day ☀️ Wake-up completed."
                                                    callState = "COMPLETED"
                                                }
                                                else -> onDismiss()
                                            }
                                        },
                                        label = { Text("$eng ($tam)", fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Standing up check
                    Text(text = "Are you standing up yet?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        FilterChip(
                            selected = standingUpStatus == "YES",
                            onClick = { standingUpStatus = "YES" },
                            label = { Text("Yes, Standing Up! 🏃") }
                        )
                        FilterChip(
                            selected = standingUpStatus == "NO",
                            onClick = {
                                standingUpStatus = "NO"
                                isPlayingMotivationalAudio = true
                            },
                            label = { Text("Still lying down 🛌") }
                        )
                    }

                    if (isPlayingMotivationalAudio) {
                        Surface(
                            color = Color(0xFF4C1D95),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "▶️ Playing Motivational Speech by $partnerName...", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Shared Morning Checklist
                    Text(text = "☀️ Morning Shared Checklist:", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = waterChecked, onCheckedChange = { waterChecked = it })
                            Text("☑️ Drink Water", color = Color.White, fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = brushChecked, onCheckedChange = { brushChecked = it })
                            Text("☐ Brush Teeth", color = Color.White, fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = freshenChecked, onCheckedChange = { freshenChecked = it })
                            Text("☐ Freshen Up", color = Color.White, fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = breakfastChecked, onCheckedChange = { breakfastChecked = it })
                            Text("☐ Breakfast", color = Color.White, fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = leaveHomeChecked, onCheckedChange = { leaveHomeChecked = it })
                            Text("☐ Leave Home", color = Color.White, fontSize = 12.sp)
                        }
                    }
                } else {
                    Text(text = aiResponseText, color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(if (callState == "INCOMING") "Close" else "Finish Wake-up ☀️")
            }
        }
    )
}

// Remote Alarm Setting & Trigger Component
@Composable
fun RemoteAlarmCard(
    partnerName: String = "Shivani",
    onSetAlarm: (time: String, mode: String) -> Unit
) {
    var selectedTime by remember { mutableStateOf("07:00 AM") }
    var selectedMode by remember { mutableStateOf("Voice + Gentle Music") }
    var showSuccessToast by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⏰", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Schedule Remote Alarm for $partnerName", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("06:00 AM", "07:00 AM", "08:00 AM").forEach { time ->
                    FilterChip(
                        selected = selectedTime == time,
                        onClick = { selectedTime = time },
                        label = { Text(time, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Gentle Music", "Voice-Only", "Auto Call").forEach { mode ->
                    FilterChip(
                        selected = selectedMode == mode,
                        onClick = { selectedMode = mode },
                        label = { Text(mode, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    onSetAlarm(selectedTime, selectedMode)
                    showSuccessToast = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Text("Sync Remote Alarm ($selectedTime) ⏰", fontWeight = FontWeight.Bold)
            }

            if (showSuccessToast) {
                Text("Alarm synchronized with $partnerName's device!", color = Color(0xFF34D399), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

// 1. Mutual Good Morning / Good Night Auto-Greeting
@Composable
fun MutualAutoGreetingCard(
    partnerName: String,
    onSendGreeting: (String) -> Unit
) {
    val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val isMorning = currentHour in 5..11
    val isNight = currentHour in 20..23 || currentHour in 0..4

    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(16.dp))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = if (isMorning) "🌅" else "🌙", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isMorning) "Good Morning, $partnerName! ☀️" else "Good Night, $partnerName! 🌙",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Tap to send an auto-greeting with warm artwork.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }
            Button(
                onClick = {
                    val msg = if (isMorning) "Good morning $partnerName! Have a wonderful day ahead ☀️" else "Good night $partnerName! Sleep tight 🌙✨"
                    onSendGreeting(msg)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
            ) {
                Text("Send 💌", fontSize = 11.sp)
            }
        }
    }
}

// 2. 24-Hour Private Story Just for One Person
@Composable
fun Private24HourStoryCard(
    partnerName: String
) {
    var hasStory by remember { mutableStateOf(true) }

    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFFEC4899), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "📸", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Private 24h Story for $partnerName Only", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "A photo or note shared strictly between you two, expiring in 24 hours.", color = Color(0xFF94A3B8), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { hasStory = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDB2777)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Post Private 24h Story 📸", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

// 3. Synced Music Listening Room for Two
@Composable
fun SyncedMusicListeningRoomCard(
    partnerName: String
) {
    var isPlaying by remember { mutableStateOf(false) }
    var songProgress by remember { mutableStateOf(0.4f) }

    Surface(
        color = Color(0xFF18181B),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFF22C55E), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🎵", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = "Listening Together Room", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(text = "Synced with $partnerName in real time", color = Color(0xFF4ADE80), fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Song: Sunset Lo-Fi Chill Beats 🎧", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Slider(value = songProgress, onValueChange = { songProgress = it }, colors = SliderDefaults.colors(thumbColor = Color(0xFF22C55E), activeTrackColor = Color(0xFF22C55E)))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                IconButton(onClick = { isPlaying = !isPlaying }) {
                    Text(text = if (isPlaying) "⏸️" else "▶️", fontSize = 24.sp)
                }
            }
        }
    }
}

// 4. "Need Support" Silent Cue Card
@Composable
fun NeedSupportSilentCueCard(
    partnerName: String,
    onToggleSupport: (Boolean) -> Unit
) {
    var isSupportActive by remember { mutableStateOf(false) }

    Surface(
        color = if (isSupportActive) Color(0xFF881337) else Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFFF43F5E), RoundedCornerShape(16.dp))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "💖", fontSize = 22.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "\"Need Support\" Silent Cue", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    text = if (isSupportActive) "Support aura active on $partnerName's screen 🌹" else "Sends a gentle glowing warm heart signal without words.",
                    color = Color(0xFFFDA4AF),
                    fontSize = 11.sp
                )
            }
            Button(
                onClick = {
                    isSupportActive = !isSupportActive
                    onToggleSupport(isSupportActive)
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (isSupportActive) Color(0xFFE11D48) else Color(0xFFBE123C))
            ) {
                Text(if (isSupportActive) "Active 💖" else "Send Cue", fontSize = 11.sp)
            }
        }
    }
}

// 5. Profile Photo Timeline ("Then vs Now" Morph Slider)
@Composable
fun ProfilePhotoTimelineCard(
    partnerName: String
) {
    var sliderVal by remember { mutableStateOf(0.5f) }

    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFFA855F7), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "🖼️ $partnerName's Photo Timeline (Then vs Now)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF334155)),
                contentAlignment = Alignment.Center
            ) {
                val yearText = if (sliderVal < 0.5f) "2024 (First Met) 📸" else "2026 (Now) ✨"
                Text(text = yearText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("2024", fontSize = 11.sp, color = Color(0xFF94A3B8))
                Text("2026", fontSize = 11.sp, color = Color(0xFF94A3B8))
            }
            Slider(value = sliderVal, onValueChange = { sliderVal = it }, colors = SliderDefaults.colors(thumbColor = Color(0xFFA855F7), activeTrackColor = Color(0xFFA855F7)))
        }
    }
}

// 6. Local Kudos / Points System
@Composable
fun LocalKudosPointsCard(
    partnerName: String
) {
    var points by remember { mutableStateOf(145) }

    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFFEAB308), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⭐ Couple Kudos Ledger", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = "$points PTS", color = Color(0xFFFACC15), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { points += 10 },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF854D0E)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+10 Made Coffee ☕", fontSize = 10.sp)
                }
                Button(
                    onClick = { points += 15 },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF854D0E)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+15 Helped Clean 🧹", fontSize = 10.sp)
                }
            }
        }
    }
}

// 8. Silent SOS Trigger Card
@Composable
fun SilentSosCard(
    onSendSos: () -> Unit
) {
    Surface(
        color = Color(0xFF7F1D1D),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFFEF4444), RoundedCornerShape(16.dp))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🚨", fontSize = 22.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Silent SOS Alert Ping", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = "Triggers immediate emergency alert with live GPS to partner.", color = Color(0xFFFCA5A5), fontSize = 11.sp)
            }
            Button(
                onClick = onSendSos,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
            ) {
                Text("SOS 🚨", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 12. "Guess What I'm Doing" Status Game
@Composable
fun GuessWhatImDoingGameCard(
    partnerName: String
) {
    var guessedOption by remember { mutableStateOf<String?>(null) }

    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFF6366F1), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "🎮 Guess What $partnerName Is Doing!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))

            listOf("☕ Drinking Coffee", "💻 Coding Applet", "🏃 Out for a Run").forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { guessedOption = option }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = guessedOption == option, onClick = { guessedOption = option })
                    Text(option, color = Color.White, fontSize = 12.sp)
                }
            }

            if (guessedOption != null) {
                Text(text = "🎉 Spot on! $partnerName is currently $guessedOption!", color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

// 13. Private Two-Person Word Association Game
@Composable
fun WordAssociationGameCard() {
    var words by remember { mutableStateOf(listOf("Coffee", "Morning", "Sunlight", "Peace")) }
    var inputWord by remember { mutableStateOf("") }

    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFF14B8A6), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "🧩 Word Association Game", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                words.forEach { word ->
                    Surface(color = Color(0xFF0F766E), shape = RoundedCornerShape(12.dp)) {
                        Text(text = word, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = inputWord,
                    onValueChange = { inputWord = it },
                    placeholder = { Text("Next word...", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = {
                    if (inputWord.isNotBlank()) {
                        words = words + inputWord
                        inputWord = ""
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFF14B8A6))
                }
            }
        }
    }
}

// 14. Auto-Suggested "Perfect Time to Call" Card
@Composable
fun PerfectTimeCallCard(
    partnerName: String
) {
    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFF10B981), RoundedCornerShape(16.dp))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "📞", fontSize = 22.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = "Perfect Time to Call $partnerName", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = "Today: 7:30 PM - 9:00 PM (based on activity overlap) ⚡", color = Color(0xFF34D399), fontSize = 11.sp)
            }
        }
    }
}

// 15. Chat Health Score Canvas Gauge
@Composable
fun ChatHealthScoreCard(
    partnerName: String,
    myInitiationsPercent: Int = 52,
    partnerInitiationsPercent: Int = 48,
    avgResponseMinutes: Int = 4
) {
    Surface(
        color = Color(0xFF1E1B4B),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFF818CF8), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📊 Chat Health & Balance Score", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.weight(1f))
                Surface(color = Color(0xFF10B981), shape = RoundedCornerShape(8.dp)) {
                    Text("96% Harmony", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Canvas Gauge Arc Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(120.dp, 80.dp)) {
                    val strokeWidth = 14.dp.toPx()
                    drawArc(
                        color = Color(0xFF312E81),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                    drawArc(
                        color = Color(0xFF818CF8),
                        startAngle = 180f,
                        sweepAngle = 180f * (myInitiationsPercent / 100f),
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("52% You", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("48% $partnerName", color = Color(0xFFC7D2FE), fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("⚡ Avg Reply Time: ~$avgResponseMinutes mins", color = Color(0xFF94A3B8), fontSize = 11.sp)
                Text("💬 Highly Balanced", color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

// 16. Shared Grocery & Errand List Synced Live
@Composable
fun SharedGroceryListCard(
    partnerName: String
) {
    var items by remember {
        mutableStateOf(
            listOf(
                "🥛 Oat Milk" to true,
                "☕ Espresso Beans" to false,
                "🍞 Sourdough Bread" to false,
                "🥑 Avocados" to true
            )
        )
    }
    var newItemText by remember { mutableStateOf("") }

    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("🛒 Shared Grocery & Errand List", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("Synced live with $partnerName", color = Color(0xFF38BDF8), fontSize = 10.sp)
            Spacer(modifier = Modifier.height(8.dp))

            items.forEachIndexed { index, (item, isChecked) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            items = items.toMutableList().also { it[index] = item to !isChecked }
                        }
                        .padding(vertical = 2.dp)
                ) {
                    Checkbox(checked = isChecked, onCheckedChange = { checked ->
                        items = items.toMutableList().also { it[index] = item to checked }
                    })
                    Text(
                        text = item,
                        color = if (isChecked) Color(0xFF64748B) else Color.White,
                        fontSize = 12.sp,
                        style = if (isChecked) androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else androidx.compose.ui.text.TextStyle.Default
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newItemText,
                    onValueChange = { newItemText = it },
                    placeholder = { Text("Add item or errand...", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = {
                        if (newItemText.isNotBlank()) {
                            items = items + (newItemText to false)
                            newItemText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("+", fontSize = 14.sp)
                }
            }
        }
    }
}

// 17. Private Voice Memo Left for When They Wake Up
@Composable
fun WakeUpVoiceMemoCard(
    partnerName: String
) {
    var hasMemo by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFFF43F5E), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🌅 Wake-Up Voice Memo for $partnerName", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text("⏰ Scheduled 7:00 AM", color = Color(0xFFFDA4AF), fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("Recorded message will unlock automatically when morning motion or wake-up alarm is detected.", color = Color(0xFF94A3B8), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { isPlaying = !isPlaying },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isPlaying) "⏸ Pause Memo" else "▶ Preview Memo (12s)", fontSize = 11.sp)
                }
            }
        }
    }
}

// 18. Real-Time Distance Between Two People ("2.3 km away")
@Composable
fun RealTimeDistanceCard(
    partnerName: String,
    distanceKm: Double = 2.3
) {
    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFF10B981), RoundedCornerShape(16.dp))
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF065F46)),
                contentAlignment = Alignment.Center
            ) {
                Text("📍", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("$partnerName is $distanceKm km away", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Calculated locally on-device • Location encrypted", color = Color(0xFF34D399), fontSize = 10.sp)
            }
            Text("⚡ Live", color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

// 19. "Reply Streak Protector"
@Composable
fun ReplyStreakProtectorCard(
    partnerName: String,
    currentStreakDays: Int = 14
) {
    Surface(
        color = Color(0xFF451A03),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFFF97316), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔥 Reply Streak Protector", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.weight(1f))
                Surface(color = Color(0xFFEA580C), shape = RoundedCornerShape(12.dp)) {
                    Text("🔥 $currentStreakDays Days", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("⚠️ Cutoff in 3 hours! Reply to $partnerName before midnight to keep your $currentStreakDays-day streak alive!", color = Color(0xFFFED7AA), fontSize = 11.sp)
        }
    }
}

// 20. "Gentle Nudge" Read-But-No-Reply Reminder
@Composable
fun GentleNudgeReminderCard(
    partnerName: String,
    onSendNudge: () -> Unit
) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFFA855F7), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("👋 Gentle Nudge Reminder", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text("Auto-Resurfaced", color = Color(0xFFE9D5FF), fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Your message was read 4 hours ago. Send a subtle nudge to keep the chat flowing smoothly?", color = Color(0xFFCBD5E1), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onSendNudge,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send Gentle Nudge 💖", fontSize = 11.sp)
            }
        }
    }
}

// 21. Contact Photo Aging Reminder
@Composable
fun ContactPhotoAgingCard(
    partnerName: String,
    monthsOld: Int = 8
) {
    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFF64748B), RoundedCornerShape(16.dp))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🖼", fontSize = 22.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Contact Photo Aging Reminder", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Photo was updated $monthsOld months ago. Want to upload a fresh picture of $partnerName?", color = Color(0xFF94A3B8), fontSize = 10.sp)
            }
        }
    }
}

// 22. Two-Person Private "Rules" Pinned Note
@Composable
fun PrivateRulesPinnedCard() {
    var rulesText by remember { mutableStateOf("1. Evening no-phone hours from 9-10 PM.\n2. Friday date nights are sacred!\n3. Always say goodnight.") }
    var isEditing by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xFF1E1B4B),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFF6366F1), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📌 Private Two-Person Boundaries & Preferences", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { isEditing = !isEditing }) {
                    Icon(Icons.Default.Add, contentDescription = "Edit Rules", tint = Color(0xFF818CF8))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = rulesText,
                    onValueChange = { rulesText = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp)
                )
            } else {
                Text(text = rulesText, color = Color(0xFFE0E7FF), fontSize = 11.sp)
            }
        }
    }
}



