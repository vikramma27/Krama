package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.UserProfile
import com.example.ui.components.NativeCameraCaptureView
import com.example.ui.theme.DarkPlumCard
import com.example.ui.theme.NearBlackPlum
import com.example.ui.theme.SoftTeal
import com.example.ui.theme.WarmCoral
import kotlinx.coroutines.delay

enum class UsernameCheckStatus {
    IDLE, CHECKING, AVAILABLE, TAKEN, TOO_SHORT
}

@Composable
fun ProfileEditScreen(
    userProfile: UserProfile,
    onSaveProfile: (name: String, username: String, avatarUrl: String, statusText: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(userProfile.name) }
    var username by remember { mutableStateOf(userProfile.username.removePrefix("@")) }
    var statusText by remember { mutableStateOf(userProfile.statusText) }
    var avatarUrl by remember { mutableStateOf(userProfile.avatarUrl) }

    var checkStatus by remember { mutableStateOf(UsernameCheckStatus.IDLE) }
    var isTakingCameraPhoto by remember { mutableStateOf(false) }

    // Real-time username availability check effect
    LaunchedEffect(username) {
        val trimmed = username.trim()
        if (trimmed.length < 3) {
            checkStatus = UsernameCheckStatus.TOO_SHORT
            return@LaunchedEffect
        }

        checkStatus = UsernameCheckStatus.CHECKING
        delay(400) // Debounce delay simulating real-time Matrix directory lookup

        val takenUsernames = setOf("admin", "matrix", "support", "root", "alex", "john")
        checkStatus = if (takenUsernames.contains(trimmed.lowercase()) && trimmed.lowercase() != userProfile.username.removePrefix("@").lowercase()) {
            UsernameCheckStatus.TAKEN
        } else {
            UsernameCheckStatus.AVAILABLE
        }
    }

    val presetAvatars = listOf(
        "https://picsum.photos/id/1025/300/300",
        "https://picsum.photos/id/1027/300/300",
        "https://picsum.photos/id/1005/300/300",
        "https://picsum.photos/id/1012/300/300"
    )

    if (isTakingCameraPhoto) {
        NativeCameraCaptureView(
            onPhotoCaptured = { uri ->
                avatarUrl = uri.toString()
                isTakingCameraPhoto = false
            },
            onClose = { isTakingCameraPhoto = false }
        )
        return
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("profile_edit_screen"),
        color = NearBlackPlum
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("profile_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Edit Profile",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                IconButton(
                    onClick = {
                        if (checkStatus != UsernameCheckStatus.TAKEN) {
                            onSaveProfile(name, username, avatarUrl, statusText)
                            onBack()
                        }
                    },
                    modifier = Modifier.testTag("save_profile_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Save", tint = WarmCoral)
                }
            }

            // Avatar Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(WarmCoral)
                        .border(3.dp, WarmCoral, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl.isNotEmpty()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = name.take(1).ifEmpty { "U" },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 42.sp
                        )
                    }

                    // Camera Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(WarmCoral)
                            .clickable { isTakingCameraPhoto = true }
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Take Photo", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { isTakingCameraPhoto = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkPlumCard),
                        modifier = Modifier.testTag("take_photo_profile_button")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = WarmCoral, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Take Photo", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Avatar Presets
                Text("OR SELECT AN AVATAR PRESET", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    presetAvatars.forEach { url ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(2.dp, if (avatarUrl == url) WarmCoral else Color.Transparent, CircleShape)
                                .clickable { avatarUrl = url }
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = "Avatar Option",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Form Inputs
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                color = DarkPlumCard
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("DISPLAY NAME", color = WarmCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmCoral),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("display_name_input")
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text("MATRIX USERNAME", color = WarmCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it.filter { char -> char.isLetterOrDigit() || char == '_' } },
                        leadingIcon = {
                            Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = WarmCoral)
                        },
                        trailingIcon = {
                            when (checkStatus) {
                                UsernameCheckStatus.CHECKING -> CircularProgressIndicator(modifier = Modifier.size(20.dp), color = WarmCoral, strokeWidth = 2.dp)
                                UsernameCheckStatus.AVAILABLE -> Icon(Icons.Default.CheckCircle, contentDescription = "Available", tint = SoftTeal)
                                UsernameCheckStatus.TAKEN -> Icon(Icons.Default.Cancel, contentDescription = "Taken", tint = Color(0xFFD32F2F))
                                else -> {}
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (checkStatus == UsernameCheckStatus.TAKEN) Color(0xFFD32F2F) else WarmCoral
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input")
                    )

                    // Real-time username status badge
                    Spacer(modifier = Modifier.height(6.dp))
                    when (checkStatus) {
                        UsernameCheckStatus.CHECKING -> {
                            Text("Checking @$username availability on Matrix homeserver...", color = SoftTeal, fontSize = 12.sp)
                        }
                        UsernameCheckStatus.AVAILABLE -> {
                            Text("✓ @$username is available!", color = SoftTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        UsernameCheckStatus.TAKEN -> {
                            Text("❌ @$username is already taken in the Matrix directory", color = Color(0xFFE53935), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        UsernameCheckStatus.TOO_SHORT -> {
                            Text("Username must be at least 3 characters", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                        else -> {}
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text("STATUS / BIO", color = WarmCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = statusText,
                        onValueChange = { statusText = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmCoral),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("status_text_input")
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text("REGISTERED PHONE NUMBER", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(userProfile.phoneNumber, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Profile Action Button
            Button(
                onClick = {
                    if (checkStatus != UsernameCheckStatus.TAKEN) {
                        onSaveProfile(name, username, avatarUrl, statusText)
                        onBack()
                    }
                },
                enabled = checkStatus != UsernameCheckStatus.TAKEN,
                colors = ButtonDefaults.buttonColors(containerColor = WarmCoral),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_profile_full_button")
            ) {
                Text("Save Profile Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
