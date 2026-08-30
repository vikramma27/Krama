package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.ContactEntity
import com.example.ui.theme.DarkPlumCard
import com.example.ui.theme.NearBlackPlum
import com.example.ui.theme.SoftTeal
import com.example.ui.theme.WarmCoral
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.example.data.local.entity.MessageEntity

@Composable
fun ChatsScreen(
    chats: List<ChatEntity>,
    archivedChats: List<ChatEntity> = emptyList(),
    contacts: List<ContactEntity>,
    statuses: List<com.example.data.local.entity.StatusStoryEntity> = emptyList(),
    allMessages: List<MessageEntity> = emptyList(),
    onChatClick: (String) -> Unit,
    onViewStatusStory: (com.example.data.local.entity.StatusStoryEntity) -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenSentinel: () -> Unit,
    onStartNewChat: (ContactEntity) -> Unit,
    onCreateGroupChat: (groupName: String, selectedContacts: List<ContactEntity>) -> Unit = { _, _ -> },
    onToggleArchiveChat: (chatId: String, isArchived: Boolean) -> Unit = { _, _ -> },
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showNewChatDialog by remember { mutableStateOf(false) }
    var viewingArchivedView by remember { mutableStateOf(false) }

    val activeList = remember(chats, archivedChats, searchQuery, viewingArchivedView, allMessages) {
        val rawList = if (viewingArchivedView) archivedChats else chats
        if (searchQuery.isBlank()) rawList
        else {
            val matchingChatIdsFromMessages = allMessages.filter {
                it.content.contains(searchQuery, ignoreCase = true)
            }.map { it.chatId }.toSet()

            rawList.filter { chat ->
                chat.title.contains(searchQuery, ignoreCase = true) ||
                chat.lastMessage.contains(searchQuery, ignoreCase = true) ||
                matchingChatIdsFromMessages.contains(chat.id)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NearBlackPlum)
            .testTag("chats_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (viewingArchivedView) {
                            IconButton(
                                onClick = { viewingArchivedView = false },
                                modifier = Modifier.testTag("exit_archived_button")
                            ) {
                                Icon(Icons.Default.Archive, contentDescription = "Back", tint = WarmCoral)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        } else {
                            com.example.ui.components.KramaLogo(
                                size = 32.dp,
                                showText = false
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (viewingArchivedView) "Archived Chats" else "Krama",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "E2E",
                            tint = SoftTeal,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (viewingArchivedView) "Hidden from main list • Room DB intact" else "Matrix Olm/Megolm E2E Encrypted",
                            color = SoftTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = onOpenSentinel,
                        modifier = Modifier.testTag("sentinel_header_button")
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = "Quantum Sentinel", tint = WarmCoral)
                    }
                    IconButton(
                        onClick = onOpenCamera,
                        modifier = Modifier.testTag("camera_header_button")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = Color.White)
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("settings_header_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(if (viewingArchivedView) "Search archived chats..." else "Search by contact name or message content...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.testTag("clear_chat_search_button")) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search", tint = Color.White.copy(alpha = 0.7f))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WarmCoral,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = DarkPlumCard,
                    unfocusedContainerColor = DarkPlumCard
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("chat_search_bar")
            )

            // Archived Banner Button on main list
            if (!viewingArchivedView && archivedChats.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { viewingArchivedView = true }
                        .testTag("archived_chats_banner"),
                    color = DarkPlumCard,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Archive, contentDescription = null, tint = SoftTeal, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Archived Conversations",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(SoftTeal.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${archivedChats.size}",
                                color = SoftTeal,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Chat List
            if (activeList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = WarmCoral.copy(alpha = 0.15f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AddComment,
                                    contentDescription = null,
                                    tint = WarmCoral,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (viewingArchivedView) "No archived conversations" else if (searchQuery.isNotEmpty()) "No chats match '$searchQuery'" else "No conversations yet",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (viewingArchivedView) "Archived encrypted chats will appear here" else "Invite friends to start a chat or tap below to begin an end-to-end encrypted conversation.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        if (!viewingArchivedView && searchQuery.isEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            androidx.compose.material3.Button(
                                onClick = { showNewChatDialog = true },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = WarmCoral),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.testTag("empty_state_invite_button")
                            ) {
                                Icon(Icons.Default.AddComment, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Invite Friends / Start Chat", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeList, key = { it.id }) { chat ->
                        ChatItemRow(
                            chat = chat,
                            contacts = contacts,
                            statuses = statuses,
                            isArchivedView = viewingArchivedView,
                            onClick = { onChatClick(chat.id) },
                            onViewStatusStory = onViewStatusStory,
                            onToggleArchive = { onToggleArchiveChat(chat.id, !chat.isArchived) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) } // Padding for floating pill
                }
            }
        }

        // New Chat FAB
        FloatingActionButton(
            onClick = { showNewChatDialog = true },
            containerColor = WarmCoral,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 88.dp)
                .testTag("new_chat_fab")
        ) {
            Icon(Icons.Default.AddComment, contentDescription = "New Chat")
        }

        // New Chat / New Group Selection Dialog
        if (showNewChatDialog) {
            var newContactName by remember { mutableStateOf("") }
            var newContactPhone by remember { mutableStateOf("") }
            var isGroupMode by remember { mutableStateOf(false) }
            var groupNameInput by remember { mutableStateOf("") }
            val selectedGroupContacts = remember { androidx.compose.runtime.snapshots.SnapshotStateList<ContactEntity>() }
            var showManualAdd by remember { mutableStateOf(contacts.isEmpty()) }

            AlertDialog(
                onDismissRequest = { showNewChatDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isGroupMode) "New Encrypted Group" else "New Direct Chat",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        TextButton(onClick = { isGroupMode = !isGroupMode }) {
                            Text(
                                text = if (isGroupMode) "Switch to Direct" else "New Group",
                                color = SoftTeal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (isGroupMode) {
                            Text(
                                "Create an E2E encrypted group with a shared key & member sync:",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = groupNameInput,
                                onValueChange = { groupNameInput = it },
                                label = { Text("Group Name") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmCoral),
                                modifier = Modifier.fillMaxWidth().testTag("dialog_group_name")
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Select Members (${selectedGroupContacts.size}):", color = WarmCoral, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(modifier = Modifier.height(180.dp).verticalScroll(rememberScrollState())) {
                                if (contacts.isEmpty()) {
                                    Text("No existing contacts. Add a contact first.", color = Color.Gray, fontSize = 12.sp)
                                } else {
                                    contacts.forEach { contact ->
                                        val isSelected = selectedGroupContacts.contains(contact)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    if (isSelected) selectedGroupContacts.remove(contact)
                                                    else selectedGroupContacts.add(contact)
                                                }
                                                .padding(vertical = 8.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            androidx.compose.material3.Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { checked ->
                                                    if (checked == true) selectedGroupContacts.add(contact)
                                                    else selectedGroupContacts.remove(contact)
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(contact.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(contact.phoneNumber.ifEmpty { "Public Key Verified" }, color = Color.Gray, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (showManualAdd || contacts.isEmpty()) {
                            Text("Enter contact details to establish an E2EE session:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = newContactName,
                                onValueChange = { newContactName = it },
                                label = { Text("Contact Name") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmCoral),
                                modifier = Modifier.fillMaxWidth().testTag("dialog_new_contact_name")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newContactPhone,
                                onValueChange = { newContactPhone = it },
                                label = { Text("Phone Number") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmCoral),
                                modifier = Modifier.fillMaxWidth().testTag("dialog_new_contact_phone")
                            )
                        } else {
                            Text("Select a contact to establish a Double Ratchet session:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(modifier = Modifier.height(200.dp).verticalScroll(rememberScrollState())) {
                                contacts.forEach { contact ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                onStartNewChat(contact)
                                                showNewChatDialog = false
                                            }
                                            .padding(vertical = 10.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(WarmCoral),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (contact.avatarUrl.isNotEmpty()) {
                                                AsyncImage(
                                                    model = contact.avatarUrl,
                                                    contentDescription = contact.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Text(contact.name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(contact.name, color = Color.White, fontWeight = FontWeight.Bold)
                                            Text(contact.phoneNumber.ifEmpty { "Public Key Verified" }, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (isGroupMode) {
                        Button(
                            onClick = {
                                if (groupNameInput.isNotBlank()) {
                                    onCreateGroupChat(groupNameInput.trim(), selectedGroupContacts.toList())
                                    showNewChatDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmCoral),
                            modifier = Modifier.testTag("create_group_confirm_button")
                        ) {
                            Text("Create Group", fontWeight = FontWeight.Bold)
                        }
                    } else if (showManualAdd || contacts.isEmpty()) {
                        TextButton(
                            onClick = {
                                if (newContactName.isNotBlank()) {
                                    val newContact = ContactEntity(
                                        id = "c_${System.currentTimeMillis()}",
                                        name = newContactName.trim(),
                                        phoneNumber = newContactPhone.trim(),
                                        avatarUrl = "",
                                        statusText = "Encrypted Contact",
                                        lastSeenTimestamp = System.currentTimeMillis(),
                                        isOnline = true,
                                        publicKey = "ed25519_pk_${System.currentTimeMillis()}"
                                    )
                                    onStartNewChat(newContact)
                                    showNewChatDialog = false
                                }
                            }
                        ) {
                            Text("Start Chat", color = WarmCoral, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        TextButton(onClick = { showManualAdd = true }) {
                            Text("Add New Contact", color = SoftTeal)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewChatDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                },
                containerColor = DarkPlumCard
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChatItemRow(
    chat: ChatEntity,
    contacts: List<ContactEntity> = emptyList(),
    statuses: List<com.example.data.local.entity.StatusStoryEntity> = emptyList(),
    isArchivedView: Boolean = false,
    onClick: () -> Unit,
    onViewStatusStory: (com.example.data.local.entity.StatusStoryEntity) -> Unit = {},
    onToggleArchive: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    // Check for active status story posted within 24 hours (86,400,000 ms)
    val now = remember { System.currentTimeMillis() }
    val userActiveStatus = remember(statuses, chat.contactId) {
        statuses.find { st ->
            st.userId == chat.contactId && (now - st.timestamp) <= 86_400_000L
        }
    }

    val haptic = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggleArchive()
                false
            } else if (dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
                false
            } else false
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.25f }
    )

    Box(modifier = modifier.fillMaxWidth()) {
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
                            Text(
                                text = if (isArchivedView) "Unarchive" else "Archive",
                                color = WarmCoral,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (isArchivedView) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = "Archive Chat",
                                tint = WarmCoral,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else if (direction == SwipeToDismissBoxValue.StartToEnd || dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.graphicsLayer(alpha = alphaAnim)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddComment,
                                contentDescription = "Open Chat",
                                tint = SoftTeal,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Open Chat",
                                color = SoftTeal,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = { showMenu = true }
                    )
                    .testTag("chat_row_${chat.id}"),
                color = DarkPlumCard,
                tonalElevation = 2.dp
            ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with Distinctive 24-Hour Status Ring & real-time online status badge
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .then(
                            if (userActiveStatus != null) {
                                Modifier
                                    .border(
                                        width = 2.5.dp,
                                        color = if (userActiveStatus.isViewed) SoftTeal.copy(alpha = 0.6f) else WarmCoral,
                                        shape = CircleShape
                                    )
                                    .padding(3.dp)
                                    .clickable { onViewStatusStory(userActiveStatus) }
                            } else Modifier
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(WarmCoral.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (chat.avatarUrl.isNotEmpty()) {
                            AsyncImage(
                                model = chat.avatarUrl,
                                contentDescription = chat.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            if (chat.isGroup) {
                                Icon(Icons.Default.Group, contentDescription = "Group", tint = WarmCoral)
                            } else {
                                Text(
                                    text = chat.title.take(1),
                                    color = WarmCoral,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }
                        }
                    }

                    val matchedContact = remember(contacts, chat.contactId) { contacts.find { it.id == chat.contactId } }
                    val isOnline = matchedContact?.isOnline ?: false
                    if (isOnline) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                                .border(2.dp, DarkPlumCard, CircleShape)
                                .testTag("online_indicator_${chat.id}")
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = chat.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        val formattedTime = remember(chat.lastMessageTimestamp) {
                            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(chat.lastMessageTimestamp))
                        }

                        Text(
                            text = formattedTime,
                            color = if (chat.unreadCount > 0) WarmCoral else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (chat.draftMessage.isNotEmpty()) {
                            Text(
                                text = "Draft: ",
                                color = WarmCoral,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = chat.draftMessage,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Encrypted",
                                tint = SoftTeal.copy(alpha = 0.7f),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = chat.lastMessage,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (chat.unreadCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            val badgeScale = remember { androidx.compose.animation.core.Animatable(0.5f) }
                            androidx.compose.runtime.LaunchedEffect(chat.unreadCount) {
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
                                    .size(20.dp)
                                    .graphicsLayer {
                                        scaleX = badgeScale.value
                                        scaleY = badgeScale.value
                                    }
                                    .clip(CircleShape)
                                    .background(WarmCoral)
                                    .testTag("chat_unread_badge_${chat.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${chat.unreadCount}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier
                .background(DarkPlumCard)
                .testTag("chat_context_menu_${chat.id}")
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (isArchivedView) "Unarchive Conversation" else "Archive Conversation",
                        color = Color.White
                    )
                },
                leadingIcon = {
                    Icon(
                        if (isArchivedView) Icons.Default.Unarchive else Icons.Default.Archive,
                        contentDescription = null,
                        tint = SoftTeal
                    )
                },
                onClick = {
                    showMenu = false
                    onToggleArchive()
                },
                modifier = Modifier.testTag("archive_action_item_${chat.id}")
            )
        }
    }
}
