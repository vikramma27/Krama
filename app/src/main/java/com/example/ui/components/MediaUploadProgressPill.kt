package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularConnectedNoInternet0Bar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.media.MediaUploadManager
import com.example.media.MediaUploadTask
import com.example.media.UploadStatus
import com.example.ui.theme.DarkPlumCard
import com.example.ui.theme.NearBlackPlum
import com.example.ui.theme.QuantumTeal
import com.example.ui.theme.SoftTeal
import com.example.ui.theme.StellarCoral
import com.example.ui.theme.WhiteOak

/**
 * Premium media upload progress indicator with sophisticated animations and
 * micro-interactions. Provides clear, accessible feedback for media uploads
 * with elegant design that stands out from standard implementations.
 */
@Composable
fun MediaUploadProgressPill(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uploadTasks by MediaUploadManager.instance.uploadTasks.collectAsStateWithLifecycle()

    val activeTask = uploadTasks.firstOrNull()

    AnimatedVisibility(
        visible = activeTask != null,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(initialOffsetY = { -it }, animationSpec = tween(250)),
        exit = fadeOut(animationSpec = tween(250)) + slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(250)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp) // Increased padding for premium feel
            .testTag("media_upload_progress_pill")
    ) {
        activeTask?.let { task ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DarkPlumCard.copy(alpha = 0.98f),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, QuantumTeal.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp) // Larger, more touch-friendly icon container
                                    .clip(CircleShape)
                                    .background(
                                        when (task.status) {
                                            UploadStatus.COMPLETED -> SoftTeal.copy(alpha = 0.15f)
                                            UploadStatus.PAUSED_NO_NETWORK, UploadStatus.FAILED -> StellarCoral.copy(alpha = 0.15f)
                                            else -> QuantumTeal.copy(alpha = 0.15f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (task.status) {
                                        UploadStatus.COMPLETED -> Icons.Default.CheckCircle
                                        UploadStatus.PAUSED_NO_NETWORK -> Icons.Default.SignalCellularConnectedNoInternet0Bar
                                        else -> Icons.Default.CloudUpload
                                    },
                                    contentDescription = null,
                                    tint = when (task.status) {
                                        UploadStatus.COMPLETED -> SoftTeal
                                        UploadStatus.PAUSED_NO_NETWORK, UploadStatus.FAILED -> StellarCoral
                                        else -> QuantumTeal
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                // File name with premium typography
                                Text(
                                    text = task.fileName,
                                    color = NearBlackPlum,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Status detail with clear, readable text
                                val statusDetail = when (task.status) {
                                    UploadStatus.UPLOADING -> {
                                        val progressPercent = (task.progressPercent * 100).toInt()
                                        val speedMbps = task.speedKbps / 1024f
                                        "$progressPercent% • ${String.format("%.1f", speedMbps)} MB/s"
                                    }
                                    UploadStatus.PAUSED_NO_NETWORK -> "Offline • Waiting to resume"
                                    UploadStatus.COMPLETED -> "Upload Complete"
                                    UploadStatus.FAILED -> task.errorMessage ?: "Upload failed"
                                    UploadStatus.CANCELLED -> "Cancelled"
                                    else -> ""
                                }

                                Text(
                                    text = statusDetail,
                                    color = when (task.status) {
                                        UploadStatus.COMPLETED -> SoftTeal
                                        UploadStatus.PAUSED_NO_NETWORK, UploadStatus.FAILED -> StellarCoral
                                        else -> NearBlackPlum.copy(alpha = 0.7f)
                                    },
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Retry button with modern styling
                            if (task.status == UploadStatus.FAILED || task.status == UploadStatus.PAUSED_NO_NETWORK) {
                                IconButton(
                                    onClick = { MediaUploadManager.instance.retryUpload(context, task.taskId) },
                                    modifier = Modifier.size(48.dp), // Larger touch target
                                    containerColor = QuantumTeal.copy(alpha = 0.1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Retry Upload",
                                        tint = QuantumTeal,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Cancel button with modern styling
                            if (task.status == UploadStatus.UPLOADING || task.status == UploadStatus.PAUSED_NO_NETWORK) {
                                IconButton(
                                    onClick = { MediaUploadManager.instance.cancelUpload(context, task.taskId) },
                                    modifier = Modifier.size(48.dp), // Larger touch target
                                    containerColor = StellarCoral.copy(alpha = 0.1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = "Cancel Upload",
                                        tint = StellarCoral,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Enhanced progress bar with modern styling
                    if (task.status == UploadStatus.UPLOADING || task.status == UploadStatus.PAUSED_NO_NETWORK) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Progress percentage text
                            Text(
                                text = "${(task.progressPercent * 100).toInt()}%",
                                color = QuantumTeal,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(40.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // Progress bar with gradient effect
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp) // Thicker progress bar
                                    .background(
                                        when (task.status) {
                                            UploadStatus.PAUSED_NO_NETWORK -> Color.Gray.copy(alpha = 0.3f)
                                            else -> QuantumTeal
                                        }
                                    )
                                    .clip(RoundedCornerShape(3.dp))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(task.progressPercent)
                                        .height(6.dp)
                                        .background(
                                            when (task.status) {
                                                UploadStatus.PAUSED_NO_NETWORK -> Color.Gray
                                                else -> QuantumTeal
                                            }
                                        )
                                        .clip(RoundedCornerShape(3.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}