package com.example.ui.components

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.*
import com.example.data.repository.MessengerRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactFeaturesSheet(
    contact: ContactEntity,
    chatId: String,
    repository: MessengerRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val contactFeatureState by repository.getContactFeatureFlow(contact.id).collectAsStateWithLifecycle(
        initialValue = ContactFeatureEntity(contactId = contact.id)
    )
    val feature = contactFeatureState ?: ContactFeatureEntity(contactId = contact.id)

    val presenceLogs by repository.getPresenceLogsFlow(contact.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val voiceDiaries by repository.getVoiceDiariesFlow(contact.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val countdowns by repository.getSharedCountdownsFlow(contact.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val privateMediaList by repository.getPrivateMediaForChatFlow(chatId).collectAsStateWithLifecycle(initialValue = emptyList())

    val milestones by repository.getMilestonesFlow(contact.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val sharedEvents by repository.getSharedCalendarEventsFlow(contact.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val wallpaperProposal by repository.getLatestWallpaperProposalFlow(chatId).collectAsStateWithLifecycle(initialValue = null)
    val partnerNowPlaying by repository.getNowPlayingFlow(contact.id).collectAsStateWithLifecycle(initialValue = null)
    val myNowPlaying by repository.getNowPlayingFlow("user_me").collectAsStateWithLifecycle(initialValue = null)

    var nicknameText by remember(feature.nickname) { mutableStateOf(feature.nickname) }
    var privateNotesText by remember(feature.privateNotes) { mutableStateOf(feature.privateNotes) }
    var selectedEmoji by remember(feature.statusEmoji) { mutableStateOf(feature.statusEmoji) }
    var autoReplyEnabled by remember(feature.autoReplyDrivingEnabled) { mutableStateOf(feature.autoReplyDrivingEnabled) }
    var autoReplyText by remember(feature.autoReplyMessage) { mutableStateOf(feature.autoReplyMessage) }
    var selectedVibration by remember(feature.customVibrationPattern) { mutableStateOf(feature.customVibrationPattern) }

    var isRecordingDiary by remember { mutableStateOf(false) }
    var newCountdownTitle by remember { mutableStateOf("") }
    var showAddCountdownDialog by remember { mutableStateOf(false) }
    var showAddMilestoneDialog by remember { mutableStateOf(false) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var newMilestoneTitle by remember { mutableStateOf("") }
    var newEventTitle by remember { mutableStateOf("") }
    var isMediaVaultUnlocked by remember { mutableStateOf(false) }

    var showProfileGamesSheet by remember { mutableStateOf(false) }
    var profileGameType by remember { mutableStateOf(com.example.ui.components.GameType.NONE) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF26A69A),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = contact.name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = feature.nickname.ifEmpty { contact.name },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = feature.statusEmoji, fontSize = 18.sp)
                    }
                    if (feature.nickname.isNotEmpty()) {
                        Text(
                            text = "Real Name: ${contact.name}",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Item 1: Streak & Anniversary Banner
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF97316).copy(alpha = 0.2f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("🔥", fontSize = 20.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${feature.streakDays}-Day Chat Streak!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Keep messaging daily to keep the fire alive!",
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = Color(0xFF334155)
                            )

                            // Anniversary Card
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Celebration,
                                    contentDescription = "Anniversary",
                                    tint = Color(0xFFEC4899),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    val startDateStr = remember(feature.chatCreatedTimestamp) {
                                        val date = if (feature.chatCreatedTimestamp > 0) Date(feature.chatCreatedTimestamp) else Date()
                                        SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(date)
                                    }
                                    Text(
                                        text = "🎉 Chatting Since $startDateStr",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Creating memories together on Krama!",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }

                // Item 2: Custom Nickname & Mood Emoji Picker
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🏷️ Custom Nickname & Mood Status",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = nicknameText,
                                onValueChange = {
                                    nicknameText = it
                                    scope.launch {
                                        repository.updateContactFeature(feature.copy(nickname = it))
                                    }
                                },
                                label = { Text("Pet Name / Custom Nickname") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF26A69A),
                                    unfocusedBorderColor = Color(0xFF475569),
                                    focusedLabelColor = Color(0xFF26A69A),
                                    unfocusedLabelColor = Color(0xFF94A3B8),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Status Emoji:", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Spacer(modifier = Modifier.height(6.dp))

                            val emojis = listOf("💬", "🚀", "☕", "😴", "🎧", "🌴", "❤️", "⚡", "🎮", "📚")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(emojis) { emoji ->
                                    FilterChip(
                                        selected = selectedEmoji == emoji,
                                        onClick = {
                                            selectedEmoji = emoji
                                            scope.launch {
                                                repository.updateContactFeature(feature.copy(statusEmoji = emoji))
                                            }
                                        },
                                        label = { Text(emoji, fontSize = 16.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF26A69A),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Item 3: Private Notes Attached to Contact
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Private Notes (Local Only)",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = "Notes are saved strictly on this phone and never shared.",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = privateNotesText,
                                onValueChange = {
                                    privateNotesText = it
                                    scope.launch {
                                        repository.updateContactFeature(feature.copy(privateNotes = it))
                                    }
                                },
                                placeholder = { Text("e.g. Birthday gift idea: Sci-fi novel, prefers dark roast coffee...") },
                                minLines = 3,
                                maxLines = 5,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF26A69A),
                                    unfocusedBorderColor = Color(0xFF475569),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Item 4: Custom Vibration & Ringtone
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🔔 Ringtone & Call Vibration Pattern",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            val vibPatterns = listOf("DEFAULT", "HEARTBEAT", "DOUBLE_BUZZ", "INTENSE")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                vibPatterns.forEach { pattern ->
                                    FilterChip(
                                        selected = selectedVibration == pattern,
                                        onClick = {
                                            selectedVibration = pattern
                                            scope.launch {
                                                repository.updateContactFeature(feature.copy(customVibrationPattern = pattern))
                                            }
                                            // Test vibration
                                            triggerVibrationPattern(context, pattern)
                                        },
                                        label = { Text(pattern, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF6366F1),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Item 5: Contact Presence Heatmap (Peak Online Hours)
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.QueryStats, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "📊 Presence Heatmap (Online Probability)",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = "Usually active between 8:00 PM - 10:00 PM 🌙",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // 24-Hour Heatmap Bar Graphic
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                val hourlyCounts = remember(presenceLogs) {
                                    val map = IntArray(24) { 0 }
                                    presenceLogs.forEach { log ->
                                        if (log.hourOfDay in 0..23) map[log.hourOfDay]++
                                    }
                                    // Add baseline synthetic pattern if empty
                                    if (presenceLogs.isEmpty()) {
                                        for (h in 18..22) map[h] = (3..8).random()
                                        for (h in 8..12) map[h] = (1..4).random()
                                    }
                                    map
                                }
                                val maxVal = (hourlyCounts.maxOrNull() ?: 1).coerceAtLeast(1)

                                hourlyCounts.forEachIndexed { hour, count ->
                                    val heightFactor = count.toFloat() / maxVal
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(heightFactor.coerceAtLeast(0.15f))
                                            .padding(horizontal = 1.dp)
                                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                            .background(
                                                if (hour in 18..22) Color(0xFF26A69A) else Color(0xFF334155)
                                            )
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("12 AM", fontSize = 9.sp, color = Color(0xFF64748B))
                                Text("6 AM", fontSize = 9.sp, color = Color(0xFF64748B))
                                Text("12 PM", fontSize = 9.sp, color = Color(0xFF64748B))
                                Text("6 PM", fontSize = 9.sp, color = Color(0xFF64748B))
                                Text("11 PM", fontSize = 9.sp, color = Color(0xFF64748B))
                            }
                        }
                    }
                }

                // Item 6: Auto-Reply "Busy/Driving" Mode
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color(0xFFEAB308))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "🚗 Auto-Reply Busy/Driving Mode",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = autoReplyEnabled,
                                    onCheckedChange = {
                                        autoReplyEnabled = it
                                        scope.launch {
                                            repository.updateContactFeature(feature.copy(autoReplyDrivingEnabled = it))
                                        }
                                    }
                                )
                            }

                            if (autoReplyEnabled) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = autoReplyText,
                                    onValueChange = {
                                        autoReplyText = it
                                        scope.launch {
                                            repository.updateContactFeature(feature.copy(autoReplyMessage = it))
                                        }
                                    },
                                    label = { Text("Auto-Reply Message") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF26A69A),
                                        unfocusedBorderColor = Color(0xFF475569),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                // Item 7: Shared Countdown Timer
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "⏳ Shared Countdown Timers",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { showAddCountdownDialog = true }) {
                                    Text("+ Add Event", color = Color(0xFF26A69A), fontWeight = FontWeight.Bold)
                                }
                            }

                            if (countdowns.isEmpty()) {
                                Text(
                                    text = "No upcoming shared events set. Add one (e.g. Vacation, Concert)!",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    countdowns.forEach { event ->
                                        val daysRemaining = remember(event.targetTimestamp) {
                                            val diff = event.targetTimestamp - System.currentTimeMillis()
                                            (diff / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(event.emoji, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(event.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                                Text("In $daysRemaining days", color = Color(0xFF26A69A), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            IconButton(onClick = { scope.launch { repository.deleteSharedCountdown(event.id) } }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Item 8: Private Voice Diary Shared with Only One Person
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🎙️ Private Voice Diary (Shared Exclusively)",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Button(
                                onClick = {
                                    val newDiary = VoiceDiaryEntity(
                                        id = "diary_${System.currentTimeMillis()}",
                                        contactId = contact.id,
                                        audioPath = "content://voice_diary_${System.currentTimeMillis()}",
                                        durationMs = 12000L,
                                        title = "Personal Voice Entry #${voiceDiaries.size + 1}"
                                    )
                                    scope.launch {
                                        repository.saveVoiceDiary(newDiary)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26A69A)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Record Voice Diary Entry", fontWeight = FontWeight.Bold)
                            }

                            if (voiceDiaries.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    voiceDiaries.forEach { diary ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color(0xFF38BDF8))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(diary.title, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 12.sp)
                                                Text("Duration: 12 sec • Only visible to ${contact.name}", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                            }
                                            IconButton(onClick = { scope.launch { repository.deleteVoiceDiary(diary.id) } }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Item 9: Private Relationship Milestone Tracker
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🏆 Relationship Milestones",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { showAddMilestoneDialog = true }) {
                                    Text("+ Add", color = Color(0xFF26A69A), fontWeight = FontWeight.Bold)
                                }
                            }

                            if (milestones.isEmpty()) {
                                Text(
                                    text = "No milestones recorded yet. Add your first call, trip, or anniversary!",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    milestones.forEach { m ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(m.iconEmoji, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(m.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                                if (m.description.isNotEmpty()) {
                                                    Text(m.description, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                                }
                                            }
                                            IconButton(onClick = { scope.launch { repository.deleteMilestone(m.id) } }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Item 10: Shared Private Calendar
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📅 Shared Couple Calendar",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { showAddEventDialog = true }) {
                                    Text("+ Event", color = Color(0xFF26A69A), fontWeight = FontWeight.Bold)
                                }
                            }

                            if (sharedEvents.isEmpty()) {
                                Text(
                                    text = "No calendar events scheduled. Plan your next date night!",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    sharedEvents.forEach { ev ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(ev.emoji, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(ev.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                                if (ev.locationOrNote.isNotEmpty()) {
                                                    Text(ev.locationOrNote, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                                }
                                            }
                                            IconButton(onClick = { scope.launch { repository.deleteCalendarEvent(ev.id) } }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Item 11: Shared Wallpaper Proposal
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🖼️ Shared Chat Wallpaper (Mutual Approval)",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            val currentProp = wallpaperProposal
                            if (currentProp != null && currentProp.status == "PENDING") {
                                Surface(
                                    color = Color(0xFF3B82F6).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Proposal from ${currentProp.proposedByName}: Match Chat Wallpaper",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { scope.launch { repository.updateWallpaperProposalStatus(currentProp.id, "APPROVED") } },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Approve")
                                            }
                                            Button(
                                                onClick = { scope.launch { repository.updateWallpaperProposalStatus(currentProp.id, "REJECTED") } },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Decline")
                                            }
                                        }
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val proposal = com.example.data.local.entity.SharedWallpaperProposalEntity(
                                            id = "prop_${System.currentTimeMillis()}",
                                            chatId = chatId,
                                            wallpaperUrl = "preset_cosmic_night",
                                            proposedBy = "user_me",
                                            proposedByName = "You",
                                            status = "PENDING"
                                        )
                                        scope.launch { repository.saveWallpaperProposal(proposal) }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26A69A)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Propose New Mutual Wallpaper 🎨")
                                }
                            }
                        }
                    }
                }

                // Item 12: Shared Now Playing Music Sync Simulation / Controls
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🎧 Realtime Music Sync (Now Playing)",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Button(
                                onClick = {
                                    val nowPlaying = com.example.data.local.entity.SharedNowPlayingEntity(
                                        userId = "user_me",
                                        songTitle = "Naan Pizhai",
                                        artist = "Anirudh Ravichander",
                                        album = "Thiruchitrambalam",
                                        isPlaying = true,
                                        progressMs = 102000L,
                                        durationMs = 245000L,
                                        mood = "❤️ Romantic",
                                        timestamp = System.currentTimeMillis()
                                    )
                                    val partnerPlaying = com.example.data.local.entity.SharedNowPlayingEntity(
                                        userId = contact.id,
                                        songTitle = "Naan Pizhai",
                                        artist = "Anirudh Ravichander",
                                        album = "Thiruchitrambalam",
                                        isPlaying = true,
                                        progressMs = 102000L,
                                        durationMs = 245000L,
                                        mood = "❤️ Romantic",
                                        timestamp = System.currentTimeMillis()
                                    )
                                    scope.launch {
                                        repository.saveNowPlaying(nowPlaying)
                                        repository.saveNowPlaying(partnerPlaying)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Sync Now Playing: Naan Pizhai 🎵")
                            }
                        }
                    }
                }

                // Item 13: Scheduled Wake-Up Call & Remote Alarm System
                item {
                    var showWakeUpDialog by remember { mutableStateOf(false) }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "⏰ Accountability Wake-Up Call & Remote Alarm",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Accountability alarm system with voice recognition responses, morning checklist, and partner notifications.",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { showWakeUpDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Test Wake-Up Call 📞", fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            RemoteAlarmCard(
                                partnerName = feature.nickname.ifEmpty { contact.name },
                                onSetAlarm = { time, mode ->
                                    scope.launch {
                                        repository.sendMessage(chatId = chatId, content = "⏰ Scheduled Remote Alarm for $time ($mode)", type = "TEXT")
                                    }
                                }
                            )

                            if (showWakeUpDialog) {
                                ScheduledWakeUpCallDialog(
                                    partnerName = feature.nickname.ifEmpty { contact.name },
                                    onDismiss = { showWakeUpDialog = false }
                                )
                            }
                        }
                    }
                }

                // Item 14: Mutual Auto Greeting & 24h Private Story
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MutualAutoGreetingCard(
                            partnerName = feature.nickname.ifEmpty { contact.name },
                            onSendGreeting = { greetingMsg ->
                                scope.launch {
                                    repository.sendMessage(chatId = chatId, content = greetingMsg, type = "TEXT")
                                }
                            }
                        )

                        Private24HourStoryCard(partnerName = feature.nickname.ifEmpty { contact.name })
                    }
                }

                // Item 15: Synced Music & Silent Cue & Photo Timeline
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SyncedMusicListeningRoomCard(partnerName = feature.nickname.ifEmpty { contact.name })

                        NeedSupportSilentCueCard(
                            partnerName = feature.nickname.ifEmpty { contact.name },
                            onToggleSupport = { isActive ->
                                scope.launch {
                                    val text = if (isActive) "💖 Activated 'Need Support' Silent Cue" else "💖 Closed 'Need Support' Silent Cue"
                                    repository.sendMessage(chatId = chatId, content = text, type = "TEXT")
                                }
                            }
                        )

                        ProfilePhotoTimelineCard(partnerName = feature.nickname.ifEmpty { contact.name })
                    }
                }

                // Item 16: Kudos Ledger, SOS Alert & Interactive Games
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LocalKudosPointsCard(partnerName = feature.nickname.ifEmpty { contact.name })

                        SilentSosCard(
                            onSendSos = {
                                scope.launch {
                                    repository.sendMessage(chatId = chatId, content = "🚨 SILENT SOS ALERT: Emergency location check ping!", type = "TEXT")
                                }
                            }
                        )

                        GuessWhatImDoingGameCard(partnerName = feature.nickname.ifEmpty { contact.name })
                        WordAssociationGameCard()
                        PerfectTimeCallCard(partnerName = feature.nickname.ifEmpty { contact.name })

                        ChatHealthScoreCard(partnerName = feature.nickname.ifEmpty { contact.name })
                        SharedGroceryListCard(partnerName = feature.nickname.ifEmpty { contact.name })
                        WakeUpVoiceMemoCard(partnerName = feature.nickname.ifEmpty { contact.name })
                        RealTimeDistanceCard(partnerName = feature.nickname.ifEmpty { contact.name })
                        ReplyStreakProtectorCard(partnerName = feature.nickname.ifEmpty { contact.name })
                        GentleNudgeReminderCard(
                            partnerName = feature.nickname.ifEmpty { contact.name },
                            onSendNudge = {
                                scope.launch {
                                    repository.sendMessage(chatId = chatId, content = "👋 Gentle Nudge: Thinking of you! 💖", type = "TEXT")
                                }
                            }
                        )
                        ContactPhotoAgingCard(partnerName = feature.nickname.ifEmpty { contact.name })
                        
                        // Play Together Profile Launcher Card
                        com.example.ui.components.PlayTogetherLauncherCard(
                            partnerName = feature.nickname.ifEmpty { contact.name },
                            onOpenGames = { showProfileGamesSheet = true }
                        )

                        PrivateRulesPinnedCard()
                    }
                }
            }
        }

        if (showProfileGamesSheet) {
            com.example.ui.components.PlayTogetherBottomSheet(
                partnerName = feature.nickname.ifEmpty { contact.name },
                onDismiss = { showProfileGamesSheet = false },
                onLaunchGame = { type ->
                    profileGameType = type
                },
                onShareGameMemory = { summary ->
                    scope.launch {
                        repository.sendMessage(chatId = chatId, content = summary, type = "TEXT")
                    }
                }
            )
        }

        if (profileGameType != com.example.ui.components.GameType.NONE) {
            com.example.ui.components.ActiveGameContainerOverlay(
                gameType = profileGameType,
                partnerName = feature.nickname.ifEmpty { contact.name },
                onCloseGame = { profileGameType = com.example.ui.components.GameType.NONE },
                onShareMatchResult = { result ->
                    scope.launch {
                        repository.sendMessage(chatId = chatId, content = result, type = "TEXT")
                    }
                }
            )
        }
    }

    // Add Milestone Dialog
    if (showAddMilestoneDialog) {
        AlertDialog(
            onDismissRequest = { showAddMilestoneDialog = false },
            title = { Text("Add Relationship Milestone") },
            text = {
                OutlinedTextField(
                    value = newMilestoneTitle,
                    onValueChange = { newMilestoneTitle = it },
                    label = { Text("Milestone Title (e.g. First Date)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newMilestoneTitle.isNotBlank()) {
                            val m = com.example.data.local.entity.RelationshipMilestoneEntity(
                                id = "ms_${System.currentTimeMillis()}",
                                contactId = contact.id,
                                title = newMilestoneTitle,
                                timestamp = System.currentTimeMillis()
                            )
                            scope.launch { repository.saveMilestone(m) }
                            newMilestoneTitle = ""
                            showAddMilestoneDialog = false
                        }
                    }
                ) {
                    Text("Save Milestone")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMilestoneDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Add Event Dialog
    if (showAddEventDialog) {
        AlertDialog(
            onDismissRequest = { showAddEventDialog = false },
            title = { Text("Add Shared Calendar Event") },
            text = {
                OutlinedTextField(
                    value = newEventTitle,
                    onValueChange = { newEventTitle = it },
                    label = { Text("Event Name (e.g. Movie Night)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newEventTitle.isNotBlank()) {
                            val ev = com.example.data.local.entity.SharedCalendarEventEntity(
                                id = "ev_${System.currentTimeMillis()}",
                                contactId = contact.id,
                                title = newEventTitle,
                                dateTimestamp = System.currentTimeMillis() + 86400000L * 2
                            )
                            scope.launch { repository.saveCalendarEvent(ev) }
                            newEventTitle = ""
                            showAddEventDialog = false
                        }
                    }
                ) {
                    Text("Save Event")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEventDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Add Countdown Dialog
    if (showAddCountdownDialog) {
        AlertDialog(
            onDismissRequest = { showAddCountdownDialog = false },
            title = { Text("Add Shared Event Countdown") },
            text = {
                OutlinedTextField(
                    value = newCountdownTitle,
                    onValueChange = { newCountdownTitle = it },
                    label = { Text("Event Name (e.g. Trip to Tokyo)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCountdownTitle.isNotBlank()) {
                            val target = System.currentTimeMillis() + (12 * 24 * 3600 * 1000L) // 12 days default
                            val countdown = SharedCountdownEntity(
                                id = "cd_${System.currentTimeMillis()}",
                                contactId = contact.id,
                                title = newCountdownTitle,
                                targetTimestamp = target
                            )
                            scope.launch { repository.saveSharedCountdown(countdown) }
                            newCountdownTitle = ""
                            showAddCountdownDialog = false
                        }
                    }
                ) {
                    Text("Save Event")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCountdownDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun triggerVibrationPattern(context: Context, patternName: String) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator != null && vibrator.hasVibrator()) {
            val timings = when (patternName) {
                "HEARTBEAT" -> longArrayOf(0, 100, 100, 100, 400)
                "DOUBLE_BUZZ" -> longArrayOf(0, 150, 100, 150)
                "INTENSE" -> longArrayOf(0, 300, 100, 300, 100, 300)
                else -> longArrayOf(0, 200)
            }
            vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
        }
    } catch (e: Throwable) {}
}
