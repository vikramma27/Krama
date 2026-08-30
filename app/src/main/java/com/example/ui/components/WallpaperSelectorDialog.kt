package com.example.ui.components

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.horizontalScroll
import com.example.ui.theme.AmberGold
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.domain.model.ChatWallpaperConfig
import com.example.ui.theme.DarkPlumCard
import com.example.ui.theme.NearBlackPlum
import com.example.ui.theme.ReceivedBubbleDark
import com.example.ui.theme.SentBubbleDark
import com.example.ui.theme.SoftTeal
import com.example.ui.theme.WarmCoral

data class WallpaperPreset(
    val id: String,
    val title: String,
    val backgroundBrush: Brush
)

val WALLPAPER_PRESETS = listOf(
    WallpaperPreset(
        id = "DEFAULT",
        title = "Dark Plum",
        backgroundBrush = Brush.verticalGradient(
            colors = listOf(NearBlackPlum, Color(0xFF1E1028), DarkPlumCard)
        )
    ),
    WallpaperPreset(
        id = "EMERALD_MATRIX",
        title = "Emerald Matrix",
        backgroundBrush = Brush.verticalGradient(
            colors = listOf(Color(0xFF041C15), Color(0xFF082E23), Color(0xFF0F4D3C))
        )
    ),
    WallpaperPreset(
        id = "CYBER_VIOLET",
        title = "Cyber Violet",
        backgroundBrush = Brush.linearGradient(
            colors = listOf(Color(0xFF1B0B2E), Color(0xFF2E1045), Color(0xFF4A1259))
        )
    ),
    WallpaperPreset(
        id = "OCEAN_TEAL",
        title = "Ocean Teal",
        backgroundBrush = Brush.verticalGradient(
            colors = listOf(Color(0xFF081820), Color(0xFF0E2A38), Color(0xFF16425B))
        )
    ),
    WallpaperPreset(
        id = "WARM_SUNSET",
        title = "Warm Sunset",
        backgroundBrush = Brush.verticalGradient(
            colors = listOf(Color(0xFF2A101A), Color(0xFF3D1624), Color(0xFF5E2135))
        )
    ),
    WallpaperPreset(
        id = "DARK_NEBULA",
        title = "Dark Nebula",
        backgroundBrush = Brush.radialGradient(
            colors = listOf(Color(0xFF231834), Color(0xFF120B1F), Color(0xFF08040D))
        )
    )
)

@Composable
fun WallpaperSelectorDialog(
    currentConfig: ChatWallpaperConfig,
    onDismiss: () -> Unit,
    onSaveConfig: (ChatWallpaperConfig) -> Unit
) {
    var selectedWallpaperId by remember { mutableStateOf(currentConfig.wallpaperId) }
    var blurRadiusDp by remember { mutableFloatStateOf(currentConfig.blurRadiusDp) }
    var darkTintOpacity by remember { mutableFloatStateOf(currentConfig.darkTintOpacity) }
    var selectedAccentColor by remember { mutableStateOf(currentConfig.accentColorHex) }
    var selectedPattern by remember { mutableStateOf(currentConfig.backgroundPattern) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .testTag("wallpaper_dialog"),
            color = NearBlackPlum
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SoftTeal.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Wallpaper, contentDescription = null, tint = SoftTeal)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Custom Chat Wallpaper",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live Preview Canvas Card
                Text(
                    text = "LIVE WALLPAPER PREVIEW",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val activePreset = WALLPAPER_PRESETS.find { it.id == selectedWallpaperId }
                    ?: WALLPAPER_PRESETS.first()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(activePreset.backgroundBrush)
                ) {
                    // Blur & Tint Overlay
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .then(if (blurRadiusDp > 0f) Modifier.blur(blurRadiusDp.dp) else Modifier)
                            .background(Color.Black.copy(alpha = darkTintOpacity))
                    )

                    // Mock Sample Chat Bubbles over wallpaper
                    Column(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Received Sample
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(ReceivedBubbleDark.copy(alpha = 0.92f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "How does this wallpaper look?",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }

                        // Sent Sample
                        Box(
                            modifier = Modifier
                                .align(Alignment.End)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SentBubbleDark.copy(alpha = 0.95f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "Super legible & customized! ✨",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Preset Selector
                Text(
                    text = "CHOOSE WALLPAPER STYLE",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(WALLPAPER_PRESETS) { preset ->
                        val isSelected = preset.id == selectedWallpaperId
                        Card(
                            modifier = Modifier
                                .size(width = 80.dp, height = 75.dp)
                                .clickable { selectedWallpaperId = preset.id }
                                .then(
                                    if (isSelected) Modifier.border(
                                        2.dp,
                                        SoftTeal,
                                        RoundedCornerShape(12.dp)
                                    ) else Modifier
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkPlumCard)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(preset.backgroundBrush),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = SoftTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = preset.title,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Blur Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BlurOn, contentDescription = null, tint = SoftTeal, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Blur Effect: ${blurRadiusDp.toInt()} dp",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Slider(
                    value = blurRadiusDp,
                    onValueChange = { blurRadiusDp = it },
                    valueRange = 0f..20f,
                    colors = SliderDefaults.colors(
                        thumbColor = SoftTeal,
                        activeTrackColor = SoftTeal,
                        inactiveTrackColor = DarkPlumCard
                    ),
                    modifier = Modifier.testTag("wallpaper_blur_slider")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Dark Tint Opacity Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Opacity, contentDescription = null, tint = WarmCoral, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Dark Tint Legibility Overlay: ${(darkTintOpacity * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Slider(
                    value = darkTintOpacity,
                    onValueChange = { darkTintOpacity = it },
                    valueRange = 0.1f..0.85f,
                    colors = SliderDefaults.colors(
                        thumbColor = WarmCoral,
                        activeTrackColor = WarmCoral,
                        inactiveTrackColor = DarkPlumCard
                    ),
                    modifier = Modifier.testTag("wallpaper_opacity_slider")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Accent Color Selector
                Text("Chat Accent Color", color = SoftTeal, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        "#26A69A" to SoftTeal,
                        "#FF6B6B" to WarmCoral,
                        "#9C27B0" to Color(0xFF9C27B0),
                        "#FFB300" to AmberGold,
                        "#2ECC71" to Color(0xFF2ECC71),
                        "#2196F3" to Color(0xFF2196F3)
                    ).forEach { (hex, color) ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedAccentColor == hex) 3.dp else 0.dp,
                                    color = if (selectedAccentColor == hex) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedAccentColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedAccentColor == hex) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Background Pattern Selector
                Text("Canvas Background Pattern", color = WarmCoral, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("DOTS", "GRID", "STARS", "GEOMETRIC", "NONE").forEach { pattern ->
                        FilterChip(
                            selected = selectedPattern == pattern,
                            onClick = { selectedPattern = pattern },
                            label = { Text(pattern, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SoftTeal,
                                selectedLabelColor = NearBlackPlum
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.LightGray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            onSaveConfig(
                                ChatWallpaperConfig(
                                    wallpaperId = selectedWallpaperId,
                                    blurRadiusDp = blurRadiusDp,
                                    darkTintOpacity = darkTintOpacity,
                                    accentColorHex = selectedAccentColor,
                                    backgroundPattern = selectedPattern
                                )
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftTeal),
                        modifier = Modifier.testTag("save_wallpaper_button")
                    ) {
                        Text("Apply Customization", color = NearBlackPlum, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
