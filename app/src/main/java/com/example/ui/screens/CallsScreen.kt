package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CallLogEntity
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.ContactEntity
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DarkPlumCard
import com.example.ui.theme.NearBlackPlum
import com.example.ui.theme.SoftTeal
import com.example.ui.theme.WarmCoral
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CallsScreen(
    calls: List<CallLogEntity>,
    contacts: List<ContactEntity>,
    chats: List<ChatEntity> = emptyList(),
    onStartCall: (ContactEntity, isVideo: Boolean) -> Unit,
    onStartGroupCall: (groupChat: ChatEntity, activeMembers: List<ContactEntity>) -> Unit = { _, _ -> },
    onDeleteCallLog: (callId: String) -> Unit = {},
    onClearAllCalls: () -> Unit = {},
    onSimulateCall: (contact: ContactEntity, isVideo: Boolean, isMissed: Boolean, durationSec: Int) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    var pendingContact by remember { mutableStateOf<ContactEntity?>(null) }
    var pendingIsVideo by remember { mutableStateOf(false) }

    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, MISSED, INCOMING, OUTGOING
    var searchQuery by remember { mutableStateOf("") }
    var selectedDateRange by remember { mutableStateOf("ALL") } // ALL, TODAY, WEEK, MONTH
    var selectedCallForDetails by remember { mutableStateOf<CallLogEntity?>(null) }
    var showSimulateCallDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showGroupCallDialog by remember { mutableStateOf(false) }

    fun triggerCallWithPermissionCheck(contact: ContactEntity, isVideo: Boolean) {
        if (com.example.ui.components.MediaPermissionsChecker.hasCallPermissions(context, isVideo)) {
            onStartCall(contact, isVideo)
        } else {
            pendingContact = contact
            pendingIsVideo = isVideo
            showPermissionDialog = true
        }
    }

    if (showPermissionDialog && pendingContact != null) {
        com.example.ui.components.MediaPermissionsRationaleDialog(
            isVideoCall = pendingIsVideo,
            onPermissionsGranted = {
                val target = pendingContact
                val video = pendingIsVideo
                showPermissionDialog = false
                pendingContact = null
                if (target != null) {
                    onStartCall(target, video)
                }
            },
            onDismiss = {
                showPermissionDialog = false
                pendingContact = null
            }
        )
    }

    // Filter calls by type, search query (participant name, phone, call type), and date range
    val filteredCalls = remember(calls, selectedFilter, searchQuery, selectedDateRange) {
        val nowMs = System.currentTimeMillis()
        val dayMs = 86400000L

        calls.filter { call ->
            val matchesType = when (selectedFilter) {
                "MISSED" -> call.callStatus == "MISSED" || call.callStatus == "REJECTED"
                "VOICE" -> !call.isVideo
                "VIDEO" -> call.isVideo
                "INCOMING" -> !call.isOutgoing && call.callStatus == "COMPLETED"
                "OUTGOING" -> call.isOutgoing
                else -> true
            }

            val q = searchQuery.trim().lowercase()
            val matchesQuery = if (q.isEmpty()) {
                true
            } else {
                call.contactName.lowercase().contains(q) ||
                        call.contactId.lowercase().contains(q) ||
                        call.callStatus.lowercase().contains(q) ||
                        (if (call.isVideo) "video call" else "audio voice call").contains(q)
            }

            val diffMs = nowMs - call.timestamp
            val matchesDateRange = when (selectedDateRange) {
                "TODAY" -> diffMs <= dayMs
                "WEEK" -> diffMs <= (7 * dayMs)
                "MONTH" -> diffMs <= (30 * dayMs)
                else -> true
            }

            matchesType && matchesQuery && matchesDateRange
        }
    }

    // Calculate call stats
    val totalCallsCount = calls.size
    val missedCallsCount = remember(calls) { calls.count { it.callStatus == "MISSED" || it.callStatus == "REJECTED" } }
    val totalDurationSec = remember(calls) { calls.sumOf { it.durationSeconds } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NearBlackPlum)
            .testTag("calls_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Encrypted Call Log",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "E2E Encrypted",
                            tint = SoftTeal,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "End-to-End Encrypted Peer-to-Peer Calls",
                            color = SoftTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    com.example.ui.components.KramaLogo(
                        size = 36.dp,
                        showText = false
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { showSimulateCallDialog = true },
                        modifier = Modifier.testTag("simulate_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Simulate Call Log",
                            tint = WarmCoral
                        )
                    }

                    if (calls.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier.testTag("clear_call_logs_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear Call History",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // WebRTC Group Audio Conference Quick Action Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { showGroupCallDialog = true }
                    .testTag("group_call_banner"),
                color = DarkPlumCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, SoftTeal.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SoftTeal.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = "WebRTC Group Call",
                                tint = SoftTeal,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "WebRTC Group Audio Conference",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Multi-user encrypted audio for active group members",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { showGroupCallDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftTeal, contentColor = NearBlackPlum),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("start_group_call_button")
                    ) {
                        Icon(imageVector = Icons.Default.PhoneInTalk, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Group Call", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Call History Statistics Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = DarkPlumCard
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatBadge(label = "Total Calls", value = "$totalCallsCount", color = SoftTeal)
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.1f)))
                    StatBadge(label = "Missed", value = "$missedCallsCount", color = if (missedCallsCount > 0) WarmCoral else Color.White.copy(alpha = 0.6f))
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.1f)))
                    StatBadge(label = "Total Talk Time", value = formatDurationSummary(totalDurationSec), color = AmberGold)
                }
            }

            // Search & Date Range Filter Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search calls by participant, phone, or type...", color = Color.Gray, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search Calls", tint = SoftTeal)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear Search", tint = Color.Gray)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = SoftTeal,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedContainerColor = DarkPlumCard,
                    unfocusedContainerColor = DarkPlumCard
                ),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .testTag("calls_search_input")
            )

            // Date Range Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = SoftTeal,
                    modifier = Modifier.size(16.dp)
                )
                Text("Date Filter:", color = SoftTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                val dateRanges = listOf(
                    "ALL" to "All Dates",
                    "TODAY" to "Today",
                    "WEEK" to "Last 7 Days",
                    "MONTH" to "Last 30 Days"
                )

                dateRanges.forEach { (rangeKey, rangeLabel) ->
                    val isSelected = selectedDateRange == rangeKey
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedDateRange = rangeKey },
                        label = { Text(rangeLabel, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SoftTeal,
                            selectedLabelColor = NearBlackPlum,
                            containerColor = DarkPlumCard,
                            labelColor = Color.White.copy(alpha = 0.8f)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color.White.copy(alpha = 0.2f),
                            selectedBorderColor = SoftTeal
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            // Dedicated Calls TabRow Layout
            val tabs = listOf(
                "ALL" to "All",
                "VOICE" to "Voice",
                "VIDEO" to "Video",
                "MISSED" to "Missed ($missedCallsCount)"
            )
            val selectedTabIndex = tabs.indexOfFirst { it.first == selectedFilter }.coerceAtLeast(0)

            androidx.compose.material3.TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = DarkPlumCard,
                contentColor = SoftTeal,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .testTag("calls_tab_row")
            ) {
                tabs.forEachIndexed { index, (key, label) ->
                    androidx.compose.material3.Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedFilter = key },
                        text = {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTabIndex == index) WarmCoral else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    )
                }
            }

            // Main Call Log List
            if (filteredCalls.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = WarmCoral.copy(alpha = 0.6f),
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedFilter == "ALL") "No call history yet" else "No $selectedFilter calls found",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Calls made in Krama are securely stored in local Room DB",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 90.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredCalls, key = { it.id }) { callLog ->
                        val matchingContact = contacts.find { it.id == callLog.contactId } ?: ContactEntity(
                            id = callLog.contactId,
                            name = callLog.contactName,
                            phoneNumber = "",
                            avatarUrl = callLog.contactAvatarUrl,
                            statusText = "",
                            lastSeenTimestamp = 0L,
                            isOnline = false,
                            publicKey = ""
                        )

                        val haptic = LocalHapticFeedback.current
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onDeleteCallLog(callLog.id)
                                    false
                                } else if (dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    triggerCallWithPermissionCheck(matchingContact, callLog.isVideo)
                                    false
                                } else false
                            },
                            positionalThreshold = { totalDistance -> totalDistance * 0.25f }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = true,
                            enableDismissFromEndToStart = true,
                            backgroundContent = {
                                val direction = dismissState.dismissDirection
                                val progress = dismissState.progress
                                val alphaAnim = (progress * 2.5f).coerceIn(0f, 1f)

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (direction == SwipeToDismissBoxValue.EndToStart) WarmCoral.copy(alpha = 0.25f)
                                            else SoftTeal.copy(alpha = 0.25f)
                                        )
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = if (direction == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart
                                ) {
                                    if (direction == SwipeToDismissBoxValue.EndToStart || dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.graphicsLayer(alpha = alphaAnim)
                                        ) {
                                            Text("Delete Log", color = WarmCoral, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Call Log",
                                                tint = WarmCoral,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    } else if (direction == SwipeToDismissBoxValue.StartToEnd || dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.graphicsLayer(alpha = alphaAnim)
                                        ) {
                                            Icon(
                                                imageVector = if (callLog.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                                                contentDescription = "Redial",
                                                tint = SoftTeal,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (callLog.isVideo) "Video Call" else "Redial Call", color = SoftTeal, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { selectedCallForDetails = callLog }
                                    .testTag("call_row_${callLog.id}"),
                                color = DarkPlumCard
                            ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Participant Avatar Badge
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (callLog.callStatus == "MISSED") WarmCoral.copy(alpha = 0.25f)
                                            else SoftTeal.copy(alpha = 0.2f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = callLog.contactName.take(1).uppercase(Locale.getDefault()),
                                        color = if (callLog.callStatus == "MISSED") WarmCoral else SoftTeal,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                // Contact & Metadata Column
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = callLog.contactName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )

                                    Spacer(modifier = Modifier.height(3.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val isMissed = callLog.callStatus == "MISSED" || callLog.callStatus == "REJECTED"
                                        val icon = when {
                                            isMissed -> Icons.AutoMirrored.Filled.CallMissed
                                            callLog.isOutgoing -> Icons.AutoMirrored.Filled.CallMade
                                            else -> Icons.AutoMirrored.Filled.CallReceived
                                        }
                                        val iconColor = when {
                                            isMissed -> WarmCoral
                                            callLog.isOutgoing -> SoftTeal
                                            else -> AmberGold
                                        }

                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = iconColor,
                                            modifier = Modifier.size(14.dp)
                                        )

                                        Spacer(modifier = Modifier.width(4.dp))

                                        val formattedTime = remember(callLog.timestamp) {
                                            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(callLog.timestamp))
                                        }

                                        val durationText = if (isMissed) "Missed" else formatCallDuration(callLog.durationSeconds)

                                        Text(
                                            text = "$formattedTime • $durationText",
                                            color = if (isMissed) WarmCoral else Color.White.copy(alpha = 0.6f),
                                            fontSize = 12.sp,
                                            fontWeight = if (isMissed) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }

                                // Direct Quick Redial Action Button
                                IconButton(onClick = { triggerCallWithPermissionCheck(matchingContact, callLog.isVideo) }) {
                                    Icon(
                                        imageVector = if (callLog.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                                        contentDescription = "Redial Call",
                                        tint = WarmCoral
                                    )
                                }
                            }
                        }
                    }
                    }

                    item { Spacer(modifier = Modifier.height(88.dp)) }
                }
            }
        }
    }

    // Call Metadata Details Dialog
    if (selectedCallForDetails != null) {
        val detailCall = selectedCallForDetails!!
        val matchingContact = contacts.find { it.id == detailCall.contactId } ?: ContactEntity(
            id = detailCall.contactId,
            name = detailCall.contactName,
            phoneNumber = "",
            avatarUrl = detailCall.contactAvatarUrl,
            statusText = "",
            lastSeenTimestamp = 0L,
            isOnline = false,
            publicKey = ""
        )

        AlertDialog(
            onDismissRequest = { selectedCallForDetails = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = SoftTeal,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Call Details",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Participant Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkPlumCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(SoftTeal.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = detailCall.contactName.take(1).uppercase(Locale.getDefault()),
                                    color = SoftTeal,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = detailCall.contactName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                if (matchingContact.phoneNumber.isNotEmpty()) {
                                    Text(
                                        text = matchingContact.phoneNumber,
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // Call Metadata Entries
                    DetailRow(
                        label = "Direction",
                        value = if (detailCall.isOutgoing) "Outgoing Call" else "Incoming Call"
                    )
                    DetailRow(
                        label = "Call Status",
                        value = detailCall.callStatus,
                        valueColor = if (detailCall.callStatus == "MISSED") WarmCoral else SoftTeal
                    )
                    DetailRow(
                        label = "Call Type",
                        value = if (detailCall.isVideo) "WebRTC HD Video" else "Opus Voice HD"
                    )
                    DetailRow(
                        label = "Timestamp",
                        value = SimpleDateFormat("EEEE, MMM d, yyyy • h:mm:ss a", Locale.getDefault()).format(Date(detailCall.timestamp))
                    )
                    DetailRow(
                        label = "Duration",
                        value = formatCallDuration(detailCall.durationSeconds)
                    )

                    // System Bypassed Security Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NearBlackPlum),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = SoftTeal,
                                modifier = Modifier.size(18.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "This record is stored exclusively inside your local encrypted Room database. It is not exposed to Android's system call log provider (android.provider.CallLog).",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = matchingContact
                        val video = detailCall.isVideo
                        selectedCallForDetails = null
                        triggerCallWithPermissionCheck(target, video)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmCoral)
                ) {
                    Icon(
                        imageVector = if (detailCall.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Redial")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            val idToDelete = detailCall.id
                            selectedCallForDetails = null
                            onDeleteCallLog(idToDelete)
                        }
                    ) {
                        Text("Delete Log", color = WarmCoral)
                    }
                    TextButton(onClick = { selectedCallForDetails = null }) {
                        Text("Close", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            },
            containerColor = DarkPlumCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Simulate Call Log Dialog
    if (showSimulateCallDialog) {
        SimulateCallDialog(
            contacts = contacts,
            onDismiss = { showSimulateCallDialog = false },
            onSimulate = { contact, isVideo, isMissed, durationSec ->
                showSimulateCallDialog = false
                onSimulateCall(contact, isVideo, isMissed, durationSec)
            }
        )
    }

    // Clear All Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text("Clear Encrypted Call History?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to delete all call history logs from your local encrypted Room database? This action is permanent and cannot be undone.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        onClearAllCalls()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmCoral)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = DarkPlumCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showGroupCallDialog) {
        GroupCallLauncherDialog(
            chats = chats,
            contacts = contacts,
            onStartGroupCall = { groupChat, members ->
                showGroupCallDialog = false
                onStartGroupCall(groupChat, members)
            },
            onDismiss = { showGroupCallDialog = false }
        )
    }
}

@Composable
private fun StatBadge(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = Color.White) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        Text(text = value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SimulateCallDialog(
    contacts: List<ContactEntity>,
    onDismiss: () -> Unit,
    onSimulate: (contact: ContactEntity, isVideo: Boolean, isMissed: Boolean, durationSec: Int) -> Unit
) {
    var selectedContactIndex by remember { mutableStateOf(0) }
    var isVideo by remember { mutableStateOf(false) }
    var isMissed by remember { mutableStateOf(false) }
    var durationSec by remember { mutableStateOf(120) }

    val activeContact = contacts.getOrNull(selectedContactIndex) ?: ContactEntity(
        id = "c_demo",
        name = "Aria Vance",
        phoneNumber = "+1 555-0192",
        avatarUrl = "",
        statusText = "Encrypted Contact",
        lastSeenTimestamp = System.currentTimeMillis(),
        isOnline = true,
        publicKey = ""
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Simulate Encrypted Call", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Test call history tracking in your encrypted Room DB:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)

                // Select Participant
                Text("Participant:", color = SoftTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    contacts.forEachIndexed { index, contact ->
                        val isSelected = index == selectedContactIndex
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedContactIndex = index },
                            label = { Text(contact.name, fontSize = 12.sp, color = if (isSelected) NearBlackPlum else Color.White) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SoftTeal,
                                containerColor = NearBlackPlum
                            )
                        )
                    }
                }

                // Call Type
                Text("Call Media:", color = SoftTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = !isVideo,
                            onClick = { isVideo = false },
                            colors = RadioButtonDefaults.colors(selectedColor = SoftTeal)
                        )
                        Text("Opus Voice", color = Color.White, fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isVideo,
                            onClick = { isVideo = true },
                            colors = RadioButtonDefaults.colors(selectedColor = SoftTeal)
                        )
                        Text("WebRTC Video", color = Color.White, fontSize = 13.sp)
                    }
                }

                // Call Outcome
                Text("Call Status:", color = SoftTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = !isMissed,
                            onClick = { isMissed = false },
                            colors = RadioButtonDefaults.colors(selectedColor = SoftTeal)
                        )
                        Text("Completed", color = Color.White, fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isMissed,
                            onClick = { isMissed = true },
                            colors = RadioButtonDefaults.colors(selectedColor = WarmCoral)
                        )
                        Text("Missed", color = WarmCoral, fontSize = 13.sp)
                    }
                }

                if (!isMissed) {
                    Text("Duration: ${formatCallDuration(durationSec)}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSimulate(activeContact, isVideo, isMissed, if (isMissed) 0 else durationSec)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SoftTeal)
            ) {
                Text("Log Call", color = NearBlackPlum, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        },
        containerColor = DarkPlumCard,
        shape = RoundedCornerShape(20.dp)
    )
}

private fun formatCallDuration(seconds: Int): String {
    if (seconds <= 0) return "0s"
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
}

private fun formatDurationSummary(totalSeconds: Int): String {
    if (totalSeconds <= 0) return "0s"
    val hrs = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return when {
        hrs > 0 -> "${hrs}h ${mins}m"
        mins > 0 -> "${mins}m ${secs}s"
        else -> "${secs}s"
    }
}

@Composable
private fun GroupCallLauncherDialog(
    chats: List<ChatEntity>,
    contacts: List<ContactEntity>,
    onStartGroupCall: (groupChat: ChatEntity, activeMembers: List<ContactEntity>) -> Unit,
    onDismiss: () -> Unit
) {
    val groupChats = remember(chats) { chats.filter { it.isGroup } }
    var selectedTab by remember { mutableStateOf(if (groupChats.isNotEmpty()) 0 else 1) } // 0: Active Groups, 1: Quick Assembly
    val selectedContactIds = remember { androidx.compose.runtime.mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = SoftTeal,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        "WebRTC Group Call",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "Multi-User Encrypted Audio Conference",
                        color = SoftTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Tab Selection (Group Threads vs Custom Selection)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NearBlackPlum)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedTab = 0 },
                        color = if (selectedTab == 0) SoftTeal else Color.Transparent
                    ) {
                        Text(
                            "Group Threads (${groupChats.size})",
                            color = if (selectedTab == 0) NearBlackPlum else Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedTab = 1 },
                        color = if (selectedTab == 1) SoftTeal else Color.Transparent
                    ) {
                        Text(
                            "Instant Assembly",
                            color = if (selectedTab == 1) NearBlackPlum else Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == 0) {
                    if (groupChats.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Group, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No active group threads found",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                                Text(
                                    "Switch to Instant Assembly to select contacts",
                                    color = SoftTeal,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(240.dp)
                        ) {
                            items(groupChats, key = { it.id }) { group ->
                                val members = remember(group, contacts) {
                                    contacts.filter { group.contactId == it.id }.ifEmpty { contacts.take(3) }
                                }
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onStartGroupCall(group, members) },
                                    color = NearBlackPlum
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(SoftTeal.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Group, contentDescription = null, tint = SoftTeal, modifier = Modifier.size(20.dp))
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    group.title.ifEmpty { "Encrypted Group" },
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    "${members.size} active members • Opus Audio Mesh",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.Default.PhoneInTalk,
                                            contentDescription = "Start Call",
                                            tint = SoftTeal,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Instant Assembly Checklist
                    Text(
                        "Select active contacts to join audio room:",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.height(200.dp)
                    ) {
                        items(contacts, key = { it.id }) { contact ->
                            val isChecked = selectedContactIds.contains(contact.id)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (isChecked) selectedContactIds.remove(contact.id)
                                        else selectedContactIds.add(contact.id)
                                    },
                                color = NearBlackPlum
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    androidx.compose.material3.Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (checked) selectedContactIds.add(contact.id)
                                            else selectedContactIds.remove(contact.id)
                                        },
                                        colors = androidx.compose.material3.CheckboxDefaults.colors(
                                            checkedColor = SoftTeal,
                                            checkmarkColor = NearBlackPlum
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(contact.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(if (contact.isOnline) "Online • E2EE" else "Offline", color = if (contact.isOnline) SoftTeal else Color.Gray, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedTab == 1) {
                Button(
                    onClick = {
                        val activeMembers = contacts.filter { selectedContactIds.contains(it.id) }.ifEmpty { contacts.take(2) }
                        val adHocGroup = ChatEntity(
                            id = "group_adhoc_${System.currentTimeMillis()}",
                            contactId = "group_adhoc",
                            title = "Group Audio Call (${activeMembers.size} Users)",
                            avatarUrl = "",
                            isGroup = true,
                            lastMessage = "WebRTC Multi-User Conference Active",
                            lastMessageTimestamp = System.currentTimeMillis()
                        )
                        onStartGroupCall(adHocGroup, activeMembers)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftTeal),
                    enabled = selectedContactIds.isNotEmpty() || contacts.isNotEmpty()
                ) {
                    Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = NearBlackPlum, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start Call (${if (selectedContactIds.isEmpty()) contacts.take(2).size else selectedContactIds.size})", color = NearBlackPlum, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        },
        containerColor = DarkPlumCard,
        shape = RoundedCornerShape(20.dp)
    )
}
