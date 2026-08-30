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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.AIModelStatus
import com.example.ai.AISettingsViewModel
import com.example.data.local.entity.MessageEntity
import com.example.ui.theme.DarkPlumCard
import com.example.ui.theme.NearBlackPlum
import com.example.ui.theme.SoftTeal
import com.example.ui.theme.WarmCoral

@Composable
fun AIScreen(
    viewModel: AISettingsViewModel,
    recentMessages: List<MessageEntity> = emptyList(),
    onRevokeConsent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val progress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val isGlobalAiEnabled by viewModel.isGlobalAiEnabled.collectAsStateWithLifecycle()
    val privacySettings by viewModel.privacySettings.collectAsStateWithLifecycle()
    val activeCapabilities by viewModel.activeCapabilities.collectAsStateWithLifecycle()
    val queryResult by viewModel.queryResult.collectAsStateWithLifecycle()
    val isProcessingQuery by viewModel.isProcessingQuery.collectAsStateWithLifecycle()

    var userPrompt by remember { mutableStateOf("") }
    var showPrivacyControls by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("ai_screen"),
        color = NearBlackPlum
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(WarmCoral.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "AI Assistant",
                            tint = WarmCoral,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Krama Local AI Engine",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "100% On-Device • Encrypted • Zero Data Transfer",
                            color = SoftTeal,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(
                    onClick = { showPrivacyControls = !showPrivacyControls },
                    modifier = Modifier.testTag("ai_privacy_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Privacy Settings",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Model Download & Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkPlumCard),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = viewModel.modelSpec.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Base Model (${viewModel.modelSpec.modelSizeMb} MB • GGUF Q4)",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when (downloadState) {
                                        AIModelStatus.READY -> SoftTeal.copy(alpha = 0.2f)
                                        AIModelStatus.DOWNLOADING -> WarmCoral.copy(alpha = 0.2f)
                                        else -> Color.Gray.copy(alpha = 0.2f)
                                    }
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = when (downloadState) {
                                    AIModelStatus.READY -> "READY"
                                    AIModelStatus.DOWNLOADING -> "DOWNLOADING ${(progress * 100).toInt()}%"
                                    AIModelStatus.PAUSED -> "PAUSED"
                                    AIModelStatus.VERIFYING -> "VERIFYING CHECKSUM"
                                    AIModelStatus.ERROR -> "ERROR"
                                    AIModelStatus.NOT_INSTALLED -> "NOT INSTALLED"
                                },
                                color = when (downloadState) {
                                    AIModelStatus.READY -> SoftTeal
                                    AIModelStatus.DOWNLOADING -> WarmCoral
                                    else -> Color.White
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (downloadState == AIModelStatus.DOWNLOADING || downloadState == AIModelStatus.VERIFYING) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = WarmCoral,
                            trackColor = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = statusMessage,
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (downloadState) {
                            AIModelStatus.NOT_INSTALLED, AIModelStatus.ERROR -> {
                                Button(
                                    onClick = { viewModel.startModelDownload() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("download_ai_model_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = WarmCoral),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Download Model (${viewModel.modelSpec.modelSizeMb} MB)")
                                }
                            }

                            AIModelStatus.DOWNLOADING -> {
                                Button(
                                    onClick = { viewModel.pauseModelDownload() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Pause")
                                }
                                Button(
                                    onClick = { viewModel.cancelModelDownload() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = WarmCoral),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Cancel")
                                }
                            }

                            AIModelStatus.PAUSED -> {
                                Button(
                                    onClick = { viewModel.resumeModelDownload() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = SoftTeal),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Resume")
                                }
                            }

                            AIModelStatus.READY -> {
                                Button(
                                    onClick = { viewModel.deleteModel() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("delete_ai_model_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38232A)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = WarmCoral, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Delete Model Storage", color = WarmCoral)
                                }
                            }

                            else -> {}
                        }
                    }
                }
            }

            // Interactive On-Device AI Playground Query Section
            if (downloadState == AIModelStatus.READY) {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkPlumCard),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "💬 Natural Language Query Assistant",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Supports English, Tamil Unicode, & Tanglish (e.g., \"vanakkam, meeting time enna?\")",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = userPrompt,
                            onValueChange = { userPrompt = it },
                            placeholder = { Text("Ask local AI or search messages...", color = Color.Gray, fontSize = 13.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_query_input_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WarmCoral,
                                unfocusedBorderColor = Color.DarkGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (userPrompt.isNotBlank()) {
                                            viewModel.processQuery(userPrompt, recentMessages)
                                        }
                                    },
                                    enabled = !isProcessingQuery && userPrompt.isNotBlank(),
                                    modifier = Modifier.testTag("send_ai_query_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Send Query",
                                        tint = if (userPrompt.isNotBlank()) WarmCoral else Color.Gray
                                    )
                                }
                            }
                        )

                        val currentResult = queryResult
                        if (currentResult != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NearBlackPlum)
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = currentResult,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // AI Capabilities Toggles Section
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "⚡ Modular AI Capabilities",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            CapabilityToggleCard("MEMORY_SEARCH", "Memory Search", "Locate specific statements across encrypted message history.", Icons.Default.Search, activeCapabilities["MEMORY_SEARCH"] == true) { viewModel.toggleCapability("MEMORY_SEARCH", it) }
            CapabilityToggleCard("CONVERSATION_SUMMARY", "Conversation Summary & Timeline", "Generate structured summaries and key milestones.", Icons.Default.Analytics, activeCapabilities["CONVERSATION_SUMMARY"] == true) { viewModel.toggleCapability("CONVERSATION_SUMMARY", it) }
            CapabilityToggleCard("REMINDER_EXTRACTION", "Reminder & To-Do Extraction", "Detect action items and automatically suggest tasks.", Icons.Default.NotificationsActive, activeCapabilities["REMINDER_EXTRACTION"] == true) { viewModel.toggleCapability("REMINDER_EXTRACTION", it) }
            CapabilityToggleCard("CALENDAR_SYNC", "Calendar Event Suggestions", "Extract meeting invites and convert to calendar events.", Icons.Default.CalendarMonth, activeCapabilities["CALENDAR_SYNC"] == true) { viewModel.toggleCapability("CALENDAR_SYNC", it) }
            CapabilityToggleCard("SCAM_DETECTION", "Phishing & Scam Warning", "Analyze incoming links and requests for financial scams.", Icons.Default.Security, activeCapabilities["SCAM_DETECTION"] == true) { viewModel.toggleCapability("SCAM_DETECTION", it) }
            CapabilityToggleCard("TRANSLATION", "Tamil & Tanglish Transliteration", "Normalize Tanglish (Tamil in Latin script) & English.", Icons.Default.Language, activeCapabilities["TRANSLATION"] == true) { viewModel.toggleCapability("TRANSLATION", it) }

            // Optional Downloadable Heavy Modules
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "📦 Optional Downloadable Modules",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            OptionalModuleCard("Voice Transcription & Understanding", "Audio OCR for voice messages (Whisper Tiny)", 350, Icons.Default.Mic)
            OptionalModuleCard("Document & Image OCR", "Extract text from shared documents and receipts", 200, Icons.Default.Description)
            OptionalModuleCard("Image & Visual Understanding", "Local vision encoder for photo understanding", 500, Icons.Default.Image)
            OptionalModuleCard("Video Understanding", "Local frame extraction and action summary", 800, Icons.Default.Videocam)

            // Privacy Settings Drawer
            AnimatedVisibility(visible = showPrivacyControls) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkPlumCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🛡️ On-Device Privacy Controls",
                            color = WarmCoral,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        PrivacySwitchRow("Process data on device only", privacySettings.processDataOnDeviceOnly) { viewModel.updatePrivacySettings(privacySettings.copy(processDataOnDeviceOnly = it)) }
                        PrivacySwitchRow("Never upload conversations to cloud", privacySettings.neverUploadConversations) { viewModel.updatePrivacySettings(privacySettings.copy(neverUploadConversations = it)) }
                        PrivacySwitchRow("Include voice notes (off by default)", privacySettings.includeVoiceNotes) { viewModel.updatePrivacySettings(privacySettings.copy(includeVoiceNotes = it)) }
                        PrivacySwitchRow("Include shared media (off by default)", privacySettings.includeMedia) { viewModel.updatePrivacySettings(privacySettings.copy(includeMedia = it)) }
                        PrivacySwitchRow("Include documents (off by default)", privacySettings.includeDocuments) { viewModel.updatePrivacySettings(privacySettings.copy(includeDocuments = it)) }

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRevokeConsent,
                            colors = ButtonDefaults.buttonColors(containerColor = WarmCoral),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Revoke AI Consent & Wipe AI State", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun CapabilityToggleCard(
    id: String,
    title: String,
    description: String,
    icon: ImageVector,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = DarkPlumCard),
        shape = RoundedCornerShape(14.dp)
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
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isEnabled) SoftTeal.copy(alpha = 0.2f) else Color.DarkGray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = if (isEnabled) SoftTeal else Color.Gray, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(text = description, color = Color.Gray, fontSize = 11.sp, lineHeight = 14.sp)
                }
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = SoftTeal,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.DarkGray
                ),
                modifier = Modifier.testTag("toggle_capability_$id")
            )
        }
    }
}

@Composable
fun OptionalModuleCard(
    title: String,
    description: String,
    sizeMb: Int,
    icon: ImageVector
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = DarkPlumCard),
        shape = RoundedCornerShape(14.dp)
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
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(WarmCoral.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = WarmCoral, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(text = "$description ($sizeMb MB)", color = Color.Gray, fontSize = 11.sp)
                }
            }

            IconButton(onClick = {}) {
                Icon(imageVector = Icons.Default.Download, contentDescription = "Download Module", tint = Color.White)
            }
        }
    }
}

@Composable
fun PrivacySwitchRow(
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SoftTeal
            )
        )
    }
}
