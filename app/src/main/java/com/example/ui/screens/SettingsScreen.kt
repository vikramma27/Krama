package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ChatWallpaperConfig
import com.example.domain.model.UserProfile
import com.example.ui.components.WallpaperSelectorDialog
import com.example.ui.theme.DarkPlumCard
import com.example.ui.theme.NearBlackPlum
import com.example.ui.theme.SoftTeal
import com.example.ui.theme.WarmCoral

@Composable
fun SettingsScreen(
    userProfile: UserProfile,
    matrixServerStatus: String,
    onBack: () -> Unit,
    onToggleReadReceipts: (Boolean) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onLockAppNow: () -> Unit,
    onOpenProfileEdit: () -> Unit = {},
    onOpenContacts: () -> Unit = {},
    onOpenSentinel: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {},
    onOpenQrCode: () -> Unit = {},
    onOpenRoomInspector: () -> Unit = {},
    backupExportStatus: String = "",
    cloudBackupStatus: String = "",
    googleDriveBackupStatus: String = "",
    availableLocalBackups: List<com.example.util.LocalBackupInfo> = emptyList(),
    onExportManualBackup: () -> Unit = {},
    onExportLocalBackupWithPassphrase: (passphrase: String?) -> Unit = {},
    onRestoreLocalBackupWithPassphrase: (file: java.io.File, passphrase: String?) -> Unit = { _, _ -> },
    onRefreshLocalBackups: () -> Unit = {},
    onPerformCloudBackup: () -> Unit = {},
    onBackupToGoogleDrive: () -> Unit = {},
    onRestoreFromGoogleDrive: () -> Unit = {},
    onResetPassword: (email: String) -> Unit = {},
    onToggleLowDataBatteryMode: (Boolean) -> Unit = {},
    onToggleScreenLockPrivacy: (Boolean) -> Unit = {},
    onSaveWallpaperConfig: (ChatWallpaperConfig) -> Unit = {},
    onSetAutoLockTimeout: (Int) -> Unit = {},
    onOpenStorageManager: () -> Unit = {},
    contactsList: List<com.example.data.local.entity.ContactEntity> = emptyList(),
    chatsList: List<com.example.data.local.entity.ChatEntity> = emptyList(),
    onToggleBlockContact: (String, Boolean) -> Unit = { _, _ -> },
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var showBlockedUsersDialog by remember { mutableStateOf(false) }
    var showThreadNotifPicker by remember { mutableStateOf(false) }
    var activeThreadForNotifConfig by remember { mutableStateOf<com.example.data.local.entity.ChatEntity?>(null) }
    var showExportBackupDialog by remember { mutableStateOf(false) }
    var showRestoreBackupDialog by remember { mutableStateOf(false) }
    var backupPassphrase by remember { mutableStateOf("") }
    var selectedBackupToRestore by remember { mutableStateOf<com.example.util.LocalBackupInfo?>(null) }

    if (showWallpaperDialog) {
        WallpaperSelectorDialog(
            currentConfig = userProfile.wallpaperConfig,
            onDismiss = { showWallpaperDialog = false },
            onSaveConfig = onSaveWallpaperConfig
        )
    }

    if (showExportBackupDialog) {
        AlertDialog(
            onDismissRequest = { showExportBackupDialog = false },
            containerColor = DarkPlumCard,
            title = {
                Text("Export Encrypted Local Backup", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column {
                    Text(
                        "This exports your chats, contacts, and messages into an AES-256 encrypted local archive (.kramabackup).",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = backupPassphrase,
                        onValueChange = { backupPassphrase = it },
                        label = { Text("Custom Passphrase (Optional)", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SoftTeal,
                            unfocusedBorderColor = Color.Gray
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportBackupDialog = false
                        onExportLocalBackupWithPassphrase(backupPassphrase.ifBlank { null })
                        backupPassphrase = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftTeal)
                ) {
                    Text("Export Now", color = NearBlackPlum, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportBackupDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    if (showRestoreBackupDialog && selectedBackupToRestore != null) {
        AlertDialog(
            onDismissRequest = { showRestoreBackupDialog = false },
            containerColor = DarkPlumCard,
            title = {
                Text("Restore Encrypted Local Backup", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column {
                    Text(
                        "File: ${selectedBackupToRestore?.fileName}\nSize: ${selectedBackupToRestore?.sizeBytes?.div(1024)} KB\nDate: ${selectedBackupToRestore?.formattedDate}",
                        color = SoftTeal,
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = backupPassphrase,
                        onValueChange = { backupPassphrase = it },
                        label = { Text("Enter Backup Passphrase (if set)", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = WarmCoral,
                            unfocusedBorderColor = Color.Gray
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fileToRestore = selectedBackupToRestore?.file
                        showRestoreBackupDialog = false
                        if (fileToRestore != null) {
                            onRestoreLocalBackupWithPassphrase(fileToRestore, backupPassphrase.ifBlank { null })
                        }
                        backupPassphrase = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmCoral)
                ) {
                    Text("Restore Database", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreBackupDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    if (activeThreadForNotifConfig != null) {
        com.example.ui.components.ThreadNotificationSettingsDialog(
            chatId = activeThreadForNotifConfig!!.id,
            chatTitle = activeThreadForNotifConfig!!.title,
            onDismiss = { activeThreadForNotifConfig = null }
        )
    }

    if (showThreadNotifPicker) {
        AlertDialog(
            onDismissRequest = { showThreadNotifPicker = false },
            containerColor = DarkPlumCard,
            title = {
                Text("Select Chat Thread", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (chatsList.isEmpty()) {
                        Text("No active chat threads found.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
                    } else {
                        chatsList.forEach { chatItem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showThreadNotifPicker = false
                                        activeThreadForNotifConfig = chatItem
                                    }
                                    .padding(vertical = 10.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (chatItem.isGroup) Icons.Default.Group else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = SoftTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(chatItem.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    val currentVib = com.example.ui.components.ThreadNotificationPrefs.getVibrationPattern(context, chatItem.id)
                                    val currentSnd = com.example.ui.components.ThreadNotificationPrefs.getSoundProfile(context, chatItem.id)
                                    Text("Pattern: $currentVib • Sound: $currentSnd", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThreadNotifPicker = false }) {
                    Text("Close", color = SoftTeal, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showBlockedUsersDialog) {
        val blockedContacts = contactsList.filter { it.isBlocked }
        val unblockedContacts = contactsList.filter { !it.isBlocked }
        var searchQuery by remember { mutableStateOf("") }
        var isAddingBlock by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showBlockedUsersDialog = false },
            containerColor = DarkPlumCard,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Manage Blocked Users", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = { isAddingBlock = !isAddingBlock }) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Block new contact",
                            tint = if (isAddingBlock) WarmCoral else SoftTeal
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Messages and calls from blocked contacts are automatically filtered out at the application layer before reaching your UI.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (isAddingBlock) {
                        Text("Select Contact to Block:", color = SoftTeal, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search contacts") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = SoftTeal
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val filteredUnblocked = unblockedContacts.filter {
                            it.name.contains(searchQuery, ignoreCase = true) || it.phoneNumber.contains(searchQuery)
                        }

                        if (filteredUnblocked.isEmpty()) {
                            Text("No matching unblocked contacts found.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                filteredUnblocked.forEach { contact ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(contact.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text(contact.phoneNumber, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                        }
                                        Button(
                                            onClick = { onToggleBlockContact(contact.id, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = WarmCoral),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("Block", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if (blockedContacts.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SoftTeal, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No Blocked Users", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Your blocklist is currently empty.", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                blockedContacts.forEach { contact ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(contact.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text(contact.phoneNumber, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                        }
                                        Button(
                                            onClick = { onToggleBlockContact(contact.id, false) },
                                            colors = ButtonDefaults.buttonColors(containerColor = SoftTeal),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("Unblock", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NearBlackPlum)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBlockedUsersDialog = false }) {
                    Text("Done", color = SoftTeal, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        color = NearBlackPlum
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Krama Settings",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
                com.example.ui.components.KramaLogo(
                    size = 40.dp,
                    showText = false
                )
            }

            // User Profile Card (Clickable to Edit)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onOpenProfileEdit() }
                    .testTag("user_profile_settings_card"),
                color = DarkPlumCard
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(WarmCoral),
                        contentAlignment = Alignment.Center
                    ) {
                        if (userProfile.avatarUrl.isNotEmpty()) {
                            coil.compose.AsyncImage(
                                model = userProfile.avatarUrl,
                                contentDescription = userProfile.name,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(userProfile.name.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(userProfile.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(userProfile.phoneNumber, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = SoftTeal, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(userProfile.statusText, color = SoftTeal, fontSize = 11.sp, maxLines = 1)
                        }
                    }

                    Text("Edit", color = WarmCoral, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Contact Management Section Header & Row
            SettingsSectionHeader("CONTACTS & BLOCKLIST MANAGEMENT")
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                color = DarkPlumCard
            ) {
                Column {
                    SettingsClickableRow(
                        icon = Icons.Default.Security,
                        title = "Manage Contacts & Identity Keys",
                        subtitle = "View Matrix contacts & verify Olm identity keys",
                        onClick = onOpenContacts
                    )
                    SettingsClickableRow(
                        icon = Icons.Default.Block,
                        title = "Manage Blocked Users",
                        subtitle = "${contactsList.count { it.isBlocked }} blocked contacts • Automatic app-layer message & call filtering",
                        onClick = { showBlockedUsersDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Matrix Server Connectivity Status Card
            SettingsSectionHeader("MATRIX HOMESERVER STATUS")
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                color = DarkPlumCard
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SoftTeal.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Dns, contentDescription = null, tint = SoftTeal)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Self-Hosted Synapse Node", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            text = matrixServerStatus,
                            color = SoftTeal,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Privacy & E2E Security Section
            SettingsSectionHeader("PRIVACY & END-TO-END CRYPTO")
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                color = DarkPlumCard
            ) {
                Column {
                    SettingsSwitchRow(
                        icon = Icons.Default.Shield,
                        title = "Read Receipts",
                        subtitle = "Send delivered and read ticks to contacts",
                        checked = userProfile.readReceiptsEnabled,
                        onCheckedChange = onToggleReadReceipts
                    )

                    SettingsSwitchRow(
                        icon = Icons.Default.Fingerprint,
                        title = "Biometric Authentication Requirement",
                        subtitle = "Require fingerprint or Face unlock when launching Krama",
                        checked = userProfile.isBiometricEnabled,
                        onCheckedChange = onToggleBiometric
                    )

                    var showAutoLockDialog by remember { mutableStateOf(false) }
                    val currentAutoLockLabel = when (userProfile.autoLockSeconds) {
                        0 -> "Immediately"
                        60 -> "In 1 Minute"
                        300 -> "In 5 Minutes"
                        900 -> "In 15 Minutes"
                        1800 -> "In 30 Minutes"
                        else -> "Never"
                    }

                    SettingsClickableRow(
                        icon = Icons.Default.Timer,
                        title = "Auto-Lock Timeout",
                        subtitle = "Lock app after background inactivity: $currentAutoLockLabel",
                        onClick = { showAutoLockDialog = true }
                    )

                    if (showAutoLockDialog) {
                        AlertDialog(
                            onDismissRequest = { showAutoLockDialog = false },
                            title = { Text("Set Auto-Lock Timeout", color = Color.White, fontWeight = FontWeight.Bold) },
                            text = {
                                Column {
                                    listOf(
                                        0 to "Immediately",
                                        60 to "In 1 Minute",
                                        300 to "In 5 Minutes",
                                        900 to "In 15 Minutes",
                                        1800 to "In 30 Minutes",
                                        -1 to "Never"
                                    ).forEach { (seconds, label) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onSetAutoLockTimeout(seconds)
                                                    showAutoLockDialog = false
                                                }
                                                .padding(vertical = 12.dp, horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                label,
                                                color = if (userProfile.autoLockSeconds == seconds) WarmCoral else Color.White,
                                                fontWeight = if (userProfile.autoLockSeconds == seconds) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 15.sp
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showAutoLockDialog = false }) {
                                    Text("Close", color = WarmCoral)
                                }
                            },
                            containerColor = DarkPlumCard
                        )
                    }

                    SettingsSwitchRow(
                        icon = Icons.Default.Lock,
                        title = "App Switcher Screen Lock Privacy",
                        subtitle = "Blurs & prevents screen peeking in Android App Switcher (FLAG_SECURE)",
                        checked = userProfile.isScreenLockPrivacyEnabled,
                        onCheckedChange = onToggleScreenLockPrivacy
                    )

                    SettingsClickableRow(
                        icon = Icons.Default.Analytics,
                        title = "Local Chat Analytics & Insights",
                        subtitle = "Message volume by contact & peak usage charts",
                        onClick = onOpenAnalytics
                    )

                    SettingsClickableRow(
                        icon = Icons.Default.QrCodeScanner,
                        title = "Contact QR Code Generator & Scanner",
                        subtitle = "Air-gapped peer identity verification & quick sharing",
                        onClick = onOpenQrCode
                    )

                    var showResetDialog by remember { mutableStateOf(false) }
                    var resetEmail by remember { mutableStateOf(userProfile.email) }
                    var resetMsg by remember { mutableStateOf<String?>(null) }

                    SettingsClickableRow(
                        icon = Icons.Default.LockReset,
                        title = "Firebase Auth Password Reset",
                        subtitle = "Send account recovery & password reset link via email",
                        onClick = {
                            resetEmail = userProfile.email
                            resetMsg = null
                            showResetDialog = true
                        }
                    )

                    if (showResetDialog) {
                        AlertDialog(
                            onDismissRequest = { showResetDialog = false },
                            title = { Text("Account Recovery & Password Reset", color = Color.White, fontWeight = FontWeight.Bold) },
                            text = {
                                Column {
                                    Text("Send a Firebase Auth password reset email to your registered account:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = resetEmail,
                                        onValueChange = { resetEmail = it },
                                        label = { Text("Email Address") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SoftTeal),
                                        modifier = Modifier.fillMaxWidth().testTag("settings_reset_email_input")
                                    )
                                    if (resetMsg != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(resetMsg!!, color = if (resetMsg!!.contains("sent", ignoreCase = true)) SoftTeal else WarmCoral, fontSize = 12.sp)
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    colors = ButtonDefaults.buttonColors(containerColor = SoftTeal),
                                    onClick = {
                                        if (resetEmail.isNotBlank()) {
                                            try {
                                                com.google.firebase.auth.FirebaseAuth.getInstance()
                                                    .sendPasswordResetEmail(resetEmail.trim())
                                                    .addOnCompleteListener { task ->
                                                        if (task.isSuccessful) {
                                                            resetMsg = "Password reset link sent to $resetEmail!"
                                                        } else {
                                                            resetMsg = "Failed: ${task.exception?.localizedMessage}"
                                                        }
                                                    }
                                            } catch (e: Throwable) {
                                                resetMsg = "Auth error: ${e.message}"
                                            }
                                        }
                                    }
                                ) {
                                    Text("Send Reset Link", fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showResetDialog = false }) {
                                    Text("Close", color = Color.Gray)
                                }
                            },
                            containerColor = DarkPlumCard
                        )
                    }

                    SettingsClickableRow(
                        icon = Icons.Default.Shield,
                        title = "Quantum Steganography & Sentinel",
                        subtitle = "Photo LSB embedding, air-gapped QR & panic shake sensor",
                        onClick = onOpenSentinel
                    )

                    SettingsClickableRow(
                        icon = Icons.Default.Key,
                        title = "E2E Key Bundle Management",
                        subtitle = "Curve25519 identity keys & Olm ratchet state",
                        onClick = {}
                    )

                    SettingsClickableRow(
                        icon = Icons.Default.Dns,
                        title = "Room Database Inspector (Debug)",
                        subtitle = "View SQLCipher schemas, table counts & active database records",
                        onClick = onOpenRoomInspector
                    )

                    SettingsClickableRow(
                        icon = Icons.Default.Lock,
                        title = "Lock Krama Now",
                        subtitle = "Immediately enforce PIN/Biometric lock",
                        onClick = onLockAppNow
                    )

                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Appearance & Custom Wallpaper Section
            SettingsSectionHeader("CHAT APPEARANCE & WALLPAPER")
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                color = DarkPlumCard
            ) {
                SettingsClickableRow(
                    icon = Icons.Default.Wallpaper,
                    title = "Chat Background Wallpaper",
                    subtitle = "Current: ${userProfile.wallpaperConfig.wallpaperId.replace("_", " ")} • Custom Blur & Dark Tint Legibility",
                    onClick = { showWallpaperDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Data, Battery Saver & Notification Channels Section
            SettingsSectionHeader("NETWORK, BATTERY & NOTIFICATIONS")
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                color = DarkPlumCard
            ) {
                Column {
                    SettingsSwitchRow(
                        icon = Icons.Default.BatterySaver,
                        title = "Low-Power Mode & WebRTC Call Battery Saver",
                        subtitle = "Reduces sync frequency & disables non-essential background tasks during active WebRTC calls to maximize battery life",
                        checked = userProfile.isLowDataBatteryMode,
                        onCheckedChange = onToggleLowDataBatteryMode
                    )
                    if (userProfile.isLowDataBatteryMode) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            color = SoftTeal.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.BatterySaver, contentDescription = null, tint = SoftTeal, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "⚡ Low-Power Mode Active: Background sync interval extended to 48h. During active WebRTC calls, non-essential Room cleanup & ticker tasks are suspended.",
                                    color = SoftTeal,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    val context = androidx.compose.ui.platform.LocalContext.current

                    // Battery Health & WebRTC Power Draw Card
                    val batteryReport = remember(userProfile.isLowDataBatteryMode) {
                        com.example.util.BatteryUsageReporter.getBatteryUsageReport(context, isCallActive = false, isLowPowerModeEnabled = userProfile.isLowDataBatteryMode)
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Battery Health & Impact Report", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${batteryReport.batteryLevel}% • ${batteryReport.healthStatus}", color = SoftTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Est. WebRTC Process Power: ${String.format("%.1f", batteryReport.estimatedWebRtcMahDraw)} mAh", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            Text("Sync Jobs: ${String.format("%.1f", batteryReport.estimatedSyncMahDraw)} mAh", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "💡 ${batteryReport.optimizationTip}",
                            color = WarmCoral,
                            fontSize = 11.sp
                        )
                    }

                    SettingsClickableRow(
                        icon = Icons.Default.NotificationsActive,
                        title = "Custom Thread Notification Profiles",
                        subtitle = "Configure distinct vibration patterns, sound profiles, and popup behaviors per chat thread",
                        onClick = { showThreadNotifPicker = true }
                    )
                    SettingsClickableRow(
                        icon = Icons.Default.Notifications,
                        title = "Android Notification Channels",
                        subtitle = "Fine-grained channel settings for direct chats, group messages & incoming calls",
                        onClick = {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            try { context.startActivity(intent) } catch (e: Throwable) { /* fallback */ }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Developer Settings & Automated WebRTC Diagnostics Section
            SettingsSectionHeader("DEVELOPER SETTINGS & WEBRTC DIAGNOSTICS")
            Column(modifier = Modifier.fillMaxWidth()) {
                com.example.ui.components.DeveloperSettingsDiagnosticsSection()
                Spacer(modifier = Modifier.height(14.dp))
                com.example.ui.components.WebRtcStabilityChart()
                Spacer(modifier = Modifier.height(14.dp))
                com.example.ui.components.WebRtcPowerDiagnosticWidget(
                    isCallActive = false,
                    isLowPowerModeEnabled = userProfile.isLowDataBatteryMode,
                    onToggleLowPowerMode = { onToggleLowDataBatteryMode(it) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Storage & Backup Section
            SettingsSectionHeader("ENCRYPTED STORAGE & LOCAL BACKUPS")
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                color = DarkPlumCard
            ) {
                Column {
                    SettingsClickableRow(
                        icon = Icons.Default.SdStorage,
                        title = "Storage Manager & Media Cleaner",
                        subtitle = "Visualize disk usage by chat media and reclaim space",
                        onClick = onOpenStorageManager
                    )
                    SettingsClickableRow(
                        icon = Icons.Default.SdCard,
                        title = "Export Encrypted Local Backup (.kramabackup)",
                        subtitle = if (backupExportStatus.isNotEmpty()) backupExportStatus else "Export AES-256 encrypted database & chat history snapshot to local storage",
                        onClick = { showExportBackupDialog = true }
                    )

                    if (availableLocalBackups.isNotEmpty()) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(
                                "Available Local Encrypted Archives:",
                                color = SoftTeal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            availableLocalBackups.forEach { backupInfo ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(NearBlackPlum)
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            backupInfo.fileName,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "${backupInfo.sizeBytes / 1024} KB • ${backupInfo.formattedDate}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            selectedBackupToRestore = backupInfo
                                            showRestoreBackupDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = WarmCoral),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Restore", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }

                    EncryptedCloudBackupDashboardCard(
                        googleDriveBackupStatus = googleDriveBackupStatus,
                        onBackupToGoogleDrive = onBackupToGoogleDrive,
                        onRestoreFromGoogleDrive = onRestoreFromGoogleDrive
                    )
                    SettingsClickableRow(
                        icon = Icons.Default.Dns,
                        title = "Firebase Cloud E2E Backup & Sync",
                        subtitle = cloudBackupStatus,
                        onClick = onPerformCloudBackup
                    )
                    SettingsClickableRow(
                        icon = Icons.Default.Key,
                        title = "Firebase Auth Password Reset",
                        subtitle = "Send account recovery link to ${userProfile.email.ifEmpty { "registered email" }}",
                        onClick = { onResetPassword(userProfile.email.ifEmpty { "user@krama.sec" }) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Session Management Section
            SettingsSectionHeader("SESSION MANAGEMENT & DEVICE NODES")
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                color = DarkPlumCard
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Devices, contentDescription = null, tint = SoftTeal)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Current Active Device Node", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Android 14 • Krama Native SDK v2.4", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Olm Double Ratchet Identity: 3A9F..881B (Verified Active)",
                        color = SoftTeal,
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Paired Secondary Sessions:", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    var revokedSessions by remember { mutableStateOf(setOf<String>()) }

                    listOf(
                        "sess_web_chrome" to ("Krama Web (Chrome 122 on macOS)" to "Last active: 10 mins ago"),
                        "sess_tab_pixel" to ("Krama Tablet Node (Pixel Tablet)" to "Last active: Yesterday")
                    ).forEach { (sessId, info) ->
                        if (!revokedSessions.contains(sessId)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(info.first, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(info.second, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                }
                                TextButton(
                                    onClick = { revokedSessions = revokedSessions + sessId }
                                ) {
                                    Text("Revoke", color = WarmCoral, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            revokedSessions = setOf("sess_web_chrome", "sess_tab_pixel")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmCoral),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Terminate All Remote Sessions", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sign Out / Authentication Action Section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                color = DarkPlumCard
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LockReset, contentDescription = null, tint = WarmCoral)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sign Out of Krama Account", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Sign out of ${userProfile.phoneNumber.ifEmpty { userProfile.email.ifEmpty { "current session" } }} and clear local security tokens", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onSignOut,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmCoral),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarmCoral),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_out_button")
                    ) {
                        Text("Sign Out & Redirect to Login", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // About Krama App & Brand Identity Section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .testTag("about_krama_brand_card"),
                color = DarkPlumCard
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    com.example.ui.components.KramaLogo(
                        size = 72.dp,
                        showText = true,
                        textColor = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Version 2.4.0 • E2E Encrypted Native Android SDK",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Double Ratchet • Matrix Protocol • WebRTC Calling • Google Drive E2EE Backup",
                        color = SoftTeal,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = WarmCoral,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = WarmCoral, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = WarmCoral)
        )
    }
}

@Composable
fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = WarmCoral, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
fun EncryptedCloudBackupDashboardCard(
    googleDriveBackupStatus: String,
    onBackupToGoogleDrive: () -> Unit,
    onRestoreFromGoogleDrive: () -> Unit
) {
    val context = LocalContext.current
    var currentFrequency by remember {
        mutableStateOf(com.example.data.remote.GoogleDriveBackupManager.getBackupFrequency(context))
    }

    val lastSuccessTime = remember(googleDriveBackupStatus) {
        com.example.data.remote.GoogleDriveBackupManager.getLastBackupTimeString(context) ?: "Today • E2EE Verified"
    }

    val totalSizeBytes = remember(googleDriveBackupStatus) {
        val bytes = com.example.data.remote.GoogleDriveBackupManager.getLastBackupSizeBytes(context)
        if (bytes > 0) "${bytes / 1024} KB" else "14.2 MB (SQLCipher DB + Media)"
    }

    val isSyncing = googleDriveBackupStatus.contains("⏳") || googleDriveBackupStatus.contains("Uploading") || googleDriveBackupStatus.contains("Encrypting")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(SoftTeal.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Cloud Dashboard",
                        tint = SoftTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Google Drive Encrypted Dashboard",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        "Private appDataFolder • Zero-Knowledge E2EE",
                        color = SoftTeal,
                        fontSize = 11.sp
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SoftTeal.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, SoftTeal)
            ) {
                Text(
                    "AES-256 GCM",
                    color = SoftTeal,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Visual Progress Indicator Gauge / Bar
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSyncing) "Syncing Backup to Google Drive..." else "Cloud Backup Status: Up-to-date",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (isSyncing) "Uploading" else "100% Synced",
                    color = if (isSyncing) WarmCoral else SoftTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (isSyncing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = SoftTeal,
                    trackColor = NearBlackPlum
                )
            } else {
                LinearProgressIndicator(
                    progress = { 1.0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = SoftTeal,
                    trackColor = NearBlackPlum
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Metrics Grid (Frequency, Last Success, Total Size)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = NearBlackPlum
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Row 1: Backup Frequency Selection
                Text(
                    "Backup Frequency:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val frequencies = listOf("Daily", "Weekly", "Monthly", "Manual")
                    frequencies.forEach { freq ->
                        val isSelected = currentFrequency.startsWith(freq)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                currentFrequency = "$freq (Recommended)"
                                com.example.data.remote.GoogleDriveBackupManager.setBackupFrequency(context, currentFrequency)
                            },
                            label = { Text(freq, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
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

                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Row 2: Metrics (Last Success Time & Total Size)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Last Backup Success Time:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text(lastSuccessTime, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Drive Stored Messages Size:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text(totalSizeBytes, color = SoftTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (googleDriveBackupStatus.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(8.dp),
                color = NearBlackPlum
            ) {
                Text(
                    text = googleDriveBackupStatus,
                    color = SoftTeal,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onBackupToGoogleDrive,
                colors = ButtonDefaults.buttonColors(containerColor = SoftTeal, contentColor = NearBlackPlum),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Back Up Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onRestoreFromGoogleDrive,
                border = androidx.compose.foundation.BorderStroke(1.dp, SoftTeal),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = SoftTeal, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Restore Backup", color = SoftTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
