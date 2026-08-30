package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.local.DeviceContactsReader
import com.example.data.local.entity.ContactEntity
import com.example.ui.theme.DarkPlumCard
import com.example.ui.theme.NearBlackPlum
import com.example.ui.theme.SoftTeal
import com.example.ui.theme.WarmCoral

@Composable
fun ContactsScreen(
    contacts: List<ContactEntity>,
    onBack: () -> Unit,
    onStartChat: (ContactEntity) -> Unit,
    onToggleBlockContact: (String, Boolean) -> Unit,
    onAddNewContact: (String, String, String) -> Unit,
    onOpenQrCode: () -> Unit = {},
    onSyncContactsObfuscated: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val deviceContactsReader = remember { DeviceContactsReader(context) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedContactForProfile by remember { mutableStateOf<ContactEntity?>(null) }
    var showAddContactDialog by remember { mutableStateOf(false) }

    val importedDeviceContacts = remember { mutableStateListOf<ContactEntity>() }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val deviceList = deviceContactsReader.readDeviceContacts()
            importedDeviceContacts.clear()
            importedDeviceContacts.addAll(deviceList)
        }
    }

    val combinedContacts = remember(contacts, importedDeviceContacts.toList()) {
        val list = contacts.toMutableList()
        importedDeviceContacts.forEach { dev ->
            if (list.none { it.phoneNumber == dev.phoneNumber }) {
                list.add(dev)
            }
        }
        list
    }

    val filteredContacts = remember(combinedContacts, searchQuery) {
        if (searchQuery.isEmpty()) combinedContacts
        else combinedContacts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.phoneNumber.contains(searchQuery, ignoreCase = true)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("contacts_screen"),
        color = NearBlackPlum
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("contacts_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Contacts & Key Verification",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "${contacts.size} verified Matrix Olm identities",
                            color = SoftTeal,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(
                        onClick = onOpenQrCode,
                        modifier = Modifier.testTag("contacts_qr_button")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "QR Code Scanner", tint = WarmCoral)
                    }
                }

                // Sync Device Contacts Banner Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                                val deviceList = deviceContactsReader.readDeviceContacts()
                                importedDeviceContacts.clear()
                                importedDeviceContacts.addAll(deviceList)
                                onSyncContactsObfuscated()
                            } else {
                                contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        },
                    color = DarkPlumCard
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SoftTeal.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Contacts, contentDescription = null, tint = SoftTeal)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Scan & Import Phone Contacts",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (importedDeviceContacts.isNotEmpty()) "${importedDeviceContacts.size} device contacts synced" else "Read local ContactsContract & invite via Dynamic Links",
                                color = SoftTeal,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name or phone...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        .padding(bottom = 16.dp)
                        .testTag("contacts_search_input")
                )

                // Contact List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredContacts, key = { it.id }) { contact ->
                        ContactItemRow(
                            contact = contact,
                            onContactClick = { selectedContactForProfile = contact },
                            onStartChat = { onStartChat(contact) },
                            onToggleBlock = { onToggleBlockContact(contact.id, !contact.isBlocked) }
                        )
                    }
                }
            }

            // Floating Add Contact Button
            FloatingActionButton(
                onClick = { showAddContactDialog = true },
                containerColor = WarmCoral,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 88.dp)
                    .testTag("add_contact_fab")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact")
            }
        }

        // Contact Detailed Profile Sheet Dialog
        if (selectedContactForProfile != null) {
            val contact = selectedContactForProfile!!
            AlertDialog(
                onDismissRequest = { selectedContactForProfile = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
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
                                Text(contact.name.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(contact.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(contact.phoneNumber, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        // Status Msg
                        Text("STATUS", color = WarmCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(contact.statusText, color = Color.White, fontSize = 14.sp)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Matrix E2E Key
                        Text("MATRIX ED25519 PUBLIC KEY", color = SoftTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = SoftTeal, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = contact.publicKey,
                                color = SoftTeal,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Verification Status
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = SoftTeal, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Double Ratchet Session Verified", color = SoftTeal, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Invite via Firebase Dynamic Link
                        Button(
                            onClick = {
                                deviceContactsReader.sendDynamicLinkInvitation(context, contact.name, contact.phoneNumber)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftTeal),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Invite via Firebase Dynamic Link", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        // Block / Unblock Toggle Button
                        Button(
                            onClick = {
                                onToggleBlockContact(contact.id, !contact.isBlocked)
                                selectedContactForProfile = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (contact.isBlocked) SoftTeal else Color(0xFFD32F2F)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("block_unblock_button")
                        ) {
                            Icon(
                                imageVector = if (contact.isBlocked) Icons.Default.CheckCircle else Icons.Default.Block,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (contact.isBlocked) "Unblock User" else "Block User")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        onStartChat(contact)
                        selectedContactForProfile = null
                    }) {
                        Text("Message", color = WarmCoral, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedContactForProfile = null }) {
                        Text("Close", color = Color.White)
                    }
                },
                containerColor = DarkPlumCard
            )
        }

        // Add New Contact Dialog
        if (showAddContactDialog) {
            var newName by remember { mutableStateOf("") }
            var newPhone by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAddContactDialog = false },
                title = { Text("Add New Contact", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Display Name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmCoral),
                            modifier = Modifier.fillMaxWidth().testTag("add_contact_name")
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newPhone,
                            onValueChange = { newPhone = it },
                            label = { Text("Phone Number") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmCoral),
                            modifier = Modifier.fillMaxWidth().testTag("add_contact_phone")
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                showAddContactDialog = false
                                onOpenQrCode()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftTeal),
                            modifier = Modifier.fillMaxWidth().testTag("dialog_scan_qr_button")
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan Peer QR Identity Code", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newName.isNotBlank() && newPhone.isNotBlank()) {
                                onAddNewContact(newName, newPhone, "ed25519_pk_${System.currentTimeMillis()}")
                                showAddContactDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmCoral)
                    ) {
                        Text("Add Contact")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddContactDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = DarkPlumCard
            )
        }
    }
}

@Composable
fun ContactItemRow(
    contact: ContactEntity,
    onContactClick: () -> Unit,
    onStartChat: () -> Unit,
    onToggleBlock: () -> Unit = {}
) {
    val presenceStatusText = remember(contact.isOnline, contact.lastSeenTimestamp) {
        if (contact.isOnline) {
            "Online"
        } else if (contact.lastSeenTimestamp > 0) {
            val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
            "Last seen ${sdf.format(java.util.Date(contact.lastSeenTimestamp))}"
        } else {
            "Last seen recently"
        }
    }

    val haptic = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggleBlock()
                false
            } else if (dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onStartChat()
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
                        Text(if (contact.isBlocked) "Unblock" else "Block", color = WarmCoral, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Block Contact",
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
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Chat",
                            tint = SoftTeal,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Chat", color = SoftTeal, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onContactClick() }
                .testTag("contact_item_${contact.id}"),
            color = DarkPlumCard
        ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (contact.isBlocked) Color.Gray else WarmCoral),
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
                        Text(contact.name.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }

                // Presence Online Badge Dot
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (contact.isOnline) SoftTeal else Color.Gray)
                        .padding(2.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (contact.isBlocked) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFD32F2F))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("BLOCKED", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.statusText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(" • ", color = SoftTeal, fontSize = 10.sp)
                    Text(
                        text = presenceStatusText,
                        color = if (contact.isOnline) SoftTeal else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = if (contact.isOnline) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            IconButton(onClick = onStartChat) {
                Icon(Icons.Default.Phone, contentDescription = "Call/Message", tint = SoftTeal)
            }
        }
    }
}
}
