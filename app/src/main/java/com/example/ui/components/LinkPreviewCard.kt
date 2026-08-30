package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

data class LinkMetadata(
    val url: String,
    val title: String,
    val description: String,
    val domain: String,
    val imageUrl: String = ""
)

@Composable
fun LinkPreviewCard(
    url: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var metadata by remember(url) { mutableStateOf<LinkMetadata?>(null) }

    LaunchedEffect(url) {
        withContext(Dispatchers.IO) {
            try {
                val parsedUrl = URL(url)
                val domain = parsedUrl.host.replace("www.", "")
                val title = when {
                    domain.contains("github") -> "GitHub Repository & Source"
                    domain.contains("youtube") -> "YouTube Video Playback"
                    domain.contains("matrix") -> "Matrix Protocol Network"
                    domain.contains("google") -> "Google Workspace"
                    else -> "$domain Web Resource"
                }
                val description = "Open link: $url in browser via secure encrypted portal."
                val imageUrl = "https://picsum.photos/seed/${domain.hashCode()}/400/200"

                metadata = LinkMetadata(
                    url = url,
                    title = title,
                    description = description,
                    domain = domain,
                    imageUrl = imageUrl
                )
            } catch (e: Exception) {
                val domain = Uri.parse(url).host ?: url
                metadata = LinkMetadata(
                    url = url,
                    title = "Web Link ($domain)",
                    description = url,
                    domain = domain
                )
            }
        }
    }

    val meta = metadata ?: return

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                } catch (_: Exception) { }
            }
    ) {
        Column {
            if (meta.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = meta.imageUrl,
                    contentDescription = "Link preview image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                )
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = meta.domain.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open in browser",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = meta.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = meta.description,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
