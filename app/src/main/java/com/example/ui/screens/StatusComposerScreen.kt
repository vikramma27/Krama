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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkPlumCard
import com.example.ui.theme.WarmCoral

@Composable
fun StatusComposerScreen(
    onPostStatus: (text: String, mediaUrl: String, bgColorHex: String, audience: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    val colorOptions = listOf("#3B2E7E", "#150F22", "#FF6B57", "#2FBF9C", "#30245E")
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    var selectedAudience by remember { mutableStateOf("All Contacts") }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("status_composer_screen"),
        color = Color(android.graphics.Color.parseColor(colorOptions[selectedColorIndex]))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        selectedColorIndex = (selectedColorIndex + 1) % colorOptions.size
                    }) {
                        Icon(Icons.Default.ColorLens, contentDescription = "Change Color", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    val context = androidx.compose.ui.platform.LocalContext.current
                    Button(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                val taskId = "upload_status_${System.currentTimeMillis()}"
                                com.example.media.MediaUploadManager.instance.startUpload(
                                    context = context,
                                    taskId = taskId,
                                    fileName = "status_story_${System.currentTimeMillis() / 1000}.jpg",
                                    mediaType = "STATUS_STORY",
                                    fileSizeBytes = 2_400_000L, // 2.4 MB story image
                                    simulatedDurationMs = 3500L
                                )
                                onPostStatus(textInput, "", colorOptions[selectedColorIndex], selectedAudience)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmCoral),
                        shape = CircleShape
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Post Story", color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Central Status Text Entry
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = {
                        Text(
                            "Type an encrypted status update...",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("status_text_input")
                )
            }

            // Bottom Audience bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Group, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Audience: $selectedAudience", color = Color.White, fontSize = 13.sp)
                }

                Text(
                    text = "24h Hard Expiry",
                    color = WarmCoral,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
