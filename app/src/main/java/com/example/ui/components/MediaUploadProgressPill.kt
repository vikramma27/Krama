package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import com.example.ui.theme.SoftTeal
import com.example.ui.theme.WarmCoral

/**
 * Non-intrusive floating progress overlay displaying real-time feedback for
 * media uploads (status stories, chat attachments, voice notes) with full
 * edge case support (network dropout, cancellation, retry).
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
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("media_upload_progress_pill")
    ) {
        activeTask?.let { task ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkPlumCard.copy(alpha = 0.96f),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WarmCoral.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
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
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (task.status) {
                                            UploadStatus.COMPLETED -> SoftTeal.copy(alpha = 0.2f)
                                            UploadStatus.PAUSED_NO_NETWORK, UploadStatus.FAILED -> Color(0xFF38232A)
                                            else -> WarmCoral.copy(alpha = 0.2f)
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
                                        UploadStatus.PAUSED_NO_NETWORK, UploadStatus.FAILED -> WarmCoral
                                        else -> WarmCoral
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = task.fileName,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                val statusDetail = when (task.status) {
                                    UploadStatus.UPLOADING -> "${(task.progressPercent * 100).toInt()}% • ${String.format("%.1f", task.speedKbps / 1024f)} MB/s"
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
                                        UploadStatus.PAUSED_NO_NETWORK, UploadStatus.FAILED -> WarmCoral
                                        else -> Color.LightGray
                                    },
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (task.status == UploadStatus.FAILED || task.status == UploadStatus.PAUSED_NO_NETWORK) {
                                IconButton(
                                    onClick = { MediaUploadManager.instance.retryUpload(context, task.taskId) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Retry Upload",
                                        tint = SoftTeal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (task.status == UploadStatus.UPLOADING || task.status == UploadStatus.PAUSED_NO_NETWORK) {
                                IconButton(
                                    onClick = { MediaUploadManager.instance.cancelUpload(context, task.taskId) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = "Cancel Upload",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (task.status == UploadStatus.UPLOADING || task.status == UploadStatus.PAUSED_NO_NETWORK) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { task.progressPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = if (task.status == UploadStatus.PAUSED_NO_NETWORK) Color.Gray else WarmCoral,
                            trackColor = Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}
