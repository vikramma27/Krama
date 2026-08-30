package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NearBlackPlum
import com.example.ui.theme.SoftTeal
import com.example.ui.theme.WarmCoral
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class GameType { NONE, LUDO, UNO, BUSINESS }
enum class GameConnectionState { CONNECTED, RECONNECTING, PAUSED }

data class TogetherPointsState(
    val level: Int = 12,
    val points: Int = 3450,
    val conversationHours: Int = 128,
    val gameHours: Int = 42,
    val streakDays: Int = 24
)

data class GameHistoryItem(
    val gameType: GameType,
    val date: String,
    val winner: String,
    val durationMins: Int,
    val scoreSummary: String
)

data class AchievementItem(
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayTogetherBottomSheet(
    partnerName: String,
    onDismiss: () -> Unit,
    onLaunchGame: (GameType) -> Unit,
    onShareGameMemory: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val togetherPoints = remember { TogetherPointsState() }

    val history = remember {
        listOf(
            GameHistoryItem(GameType.LUDO, "Today", "Vikram", 14, "Won 4-2"),
            GameHistoryItem(GameType.UNO, "Yesterday", partnerName, 9, "Won by Wild +4"),
            GameHistoryItem(GameType.BUSINESS, "3 days ago", "Vikram", 22, "Built 3 Hotels"),
            GameHistoryItem(GameType.LUDO, "25 July", partnerName, 18, "Won 4-3"),
            GameHistoryItem(GameType.UNO, "22 July", "Vikram", 8, "Perfect UNO Call")
        )
    }

    val achievements = remember {
        listOf(
            AchievementItem("First Match", "Played your first game together", "🎮", true),
            AchievementItem("100 Games Club", "Completed 100 game matches", "💯", true),
            AchievementItem("Perfect UNO", "Called UNO and won on the same turn", "🃏", true),
            AchievementItem("Triple Six", "Rolled 6 three times in Ludo", "🎲", true),
            AchievementItem("Property Tycoon", "Bought 10 properties in Business", "🏢", true),
            AchievementItem("Comeback King", "Won a game from last place", "👑", false)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🎮 Play Together",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = Color(0xFFE11D48),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "❤️ $partnerName",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E293B),
                contentColor = WarmCoral,
                modifier = Modifier.clip(RoundedCornerShape(16.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Games", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Memories & Stats", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Couple Points", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> {
                    // Games Selection Grid
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        GameLauncherCard(
                            title = "🎲 Ludo Classic & Quick",
                            subtitle = "Dice rolls, piece jumps, kill explosions, home celebrations",
                            badge = "Popular",
                            accentColor = Color(0xFF10B981),
                            onClick = {
                                onLaunchGame(GameType.LUDO)
                                onDismiss()
                            }
                        )

                        GameLauncherCard(
                            title = "🃏 UNO Attack & Wilds",
                            subtitle = "Draw cards, flip, wild color picker, +4 explosions & voice callouts",
                            badge = "Fast 5m",
                            accentColor = Color(0xFF38BDF8),
                            onClick = {
                                onLaunchGame(GameType.UNO)
                                onDismiss()
                            }
                        )

                        GameLauncherCard(
                            title = "🏢 Business Tycoon",
                            subtitle = "Monopoly-style property purchases, rents, auctions & trade memory",
                            badge = "Strategic",
                            accentColor = Color(0xFFF59E0B),
                            onClick = {
                                onLaunchGame(GameType.BUSINESS)
                                onDismiss()
                            }
                        )
                    }
                }
                1 -> {
                    // Memories & Relationship Game Diary
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("542", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text("Games Played", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("284 - 258", color = SoftTeal, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text("Vikram vs $partnerName", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("24 Days", color = WarmCoral, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text("Daily Streak", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                    }
                                }
                            }
                        }

                        item {
                            Text("❤️ Recent Match Diary", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        items(history) { item ->
                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when(item.gameType) {
                                            GameType.LUDO -> "🎲"
                                            GameType.UNO -> "🃏"
                                            GameType.BUSINESS -> "🏢"
                                            else -> "🎮"
                                        },
                                        fontSize = 22.sp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${item.gameType.name} • ${item.date}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("${item.winner} ${item.scoreSummary} (${item.durationMins} mins)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    }
                                    TextButton(onClick = {
                                        onShareGameMemory("🎮 Memory: ${item.winner} won ${item.gameType.name} on ${item.date} (${item.scoreSummary})!")
                                        onDismiss()
                                    }) {
                                        Text("Share", color = SoftTeal, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Couple Points & Achievements
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Brush.horizontalGradient(listOf(Color(0xFF818CF8), Color(0xFFC084FC))))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("✨ Couple Level ${togetherPoints.level}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text("${togetherPoints.points} pts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { 0.75f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(CircleShape),
                                        color = Color.White,
                                        trackColor = Color.White.copy(alpha = 0.3f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("550 pts to Level ${togetherPoints.level + 1}! Points earned automatically from conversation time & gaming together.", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
                                }
                            }
                        }

                        item {
                            Text("🏆 Relationship Achievements", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        items(achievements) { ach ->
                            Surface(
                                color = if (ach.isUnlocked) Color(0xFF1E293B) else Color(0xFF0F172A),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, if (ach.isUnlocked) SoftTeal.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(ach.icon, fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ach.title, color = if (ach.isUnlocked) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(ach.description, color = Color(0xFF94A3B8), fontSize = 10.sp)
                                    }
                                    if (ach.isUnlocked) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Unlocked", tint = SoftTeal, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun GameLauncherCard(
    title: String,
    subtitle: String,
    badge: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(color = accentColor, shape = RoundedCornerShape(8.dp)) {
                        Text(badge, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = accentColor, modifier = Modifier.size(28.dp))
        }
    }
}

// Reusable GameContainer Component using Material 3 Expressive (2026) Design System
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameContainer(
    gameType: GameType,
    partnerName: String,
    onCloseGame: () -> Unit,
    onShareMatchResult: (String) -> Unit,
    content: @Composable (turnEngine: TurnEngine, sessionState: GameSessionData) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Initialize TurnEngine for managing game session state and Firebase Realtime Database sync
    val turnEngine = remember(gameType, partnerName) {
        TurnEngine(
            sessionId = "session_${gameType.name.lowercase()}_pair",
            myPlayerId = "Vikram",
            partnerPlayerId = partnerName,
            gameType = gameType,
            scope = scope
        )
    }

    DisposableEffect(turnEngine) {
        onDispose {
            turnEngine.cleanup()
        }
    }

    val sessionState by turnEngine.sessionState.collectAsState()
    val isConnected by turnEngine.isConnected.collectAsState()
    val isReconnecting by turnEngine.isReconnecting.collectAsState()

    var showExitDialog by remember { mutableStateOf(false) }
    var isCallPiPExpanded by remember { mutableStateOf(true) }
    var isMicMuted by remember { mutableStateOf(false) }
    var activeEmojiReactions by remember { mutableStateOf(listOf<String>()) }

    val isMyTurn = sessionState.currentPlayerId == "Vikram"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // M3 Glassmorphic Top Bar: Header & Scoreboard
            Surface(
                color = Color(0xFF1E293B).copy(alpha = 0.95f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showExitDialog = true }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Pause / Exit", tint = Color.White)
                            }
                            Text(
                                text = when (gameType) {
                                    GameType.LUDO -> "🎲 Ludo Match"
                                    GameType.UNO -> "🃏 UNO Battle"
                                    GameType.BUSINESS -> "🏢 Business Tycoon"
                                    else -> "🎮 Game Session"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        // Connection Status Indicator
                        Surface(
                            color = when {
                                isReconnecting || !isConnected -> Color(0xFFF59E0B)
                                else -> Color(0xFF10B981)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.clickable {
                                // Toggle test reconnect trigger
                                turnEngine.simulateConnectionLoss(!isReconnecting)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isReconnecting || !isConnected) "⚡ Reconnecting..." else "🟢 Connected",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Turn Engine Scoreboard & Turn Timer Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // My Player Card
                        Surface(
                            color = if (isMyTurn) WarmCoral else Color(0xFF334155),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .border(
                                    width = if (isMyTurn) 2.dp else 0.dp,
                                    color = if (isMyTurn) Color.Yellow else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("❤️ You (${sessionState.myScore})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                if (isMyTurn) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("⏱ ${sessionState.turnTimerSeconds}s", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }

                        Text("VS", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                        // Partner Player Card
                        Surface(
                            color = if (!isMyTurn) SoftTeal else Color(0xFF334155),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .border(
                                    width = if (!isMyTurn) 2.dp else 0.dp,
                                    color = if (!isMyTurn) SoftTeal else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("❤️ $partnerName (${sessionState.partnerScore})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                if (!isMyTurn) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("⏱ ${sessionState.turnTimerSeconds}s", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // In-Call Picture-in-Picture Floating Video Bar (Voice/Video call stays active while playing!)
            PipInCallFloatingBar(
                partnerName = partnerName,
                isMuted = isMicMuted,
                isExpanded = isCallPiPExpanded,
                onToggleMute = { isMicMuted = !isMicMuted },
                onToggleExpand = { isCallPiPExpanded = !isCallPiPExpanded }
            )

            // Main Interactive Game Canvas Container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                content(turnEngine, sessionState)

                // Floating Emoji Reaction Overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = activeEmojiReactions.isNotEmpty(),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = activeEmojiReactions.lastOrNull() ?: "💖",
                            fontSize = 64.sp
                        )
                    }
                }

                // Network Listener Reconnecting Overlay
                if (isReconnecting || !isConnected) {
                    ReconnectingOverlay(
                        moveCount = sessionState.moveCount,
                        onForceResume = {
                            turnEngine.simulateConnectionLoss(false)
                        }
                    )
                }
            }

            // Expressive Bottom Action Bar
            Surface(
                color = Color(0xFF1E293B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        Toast.makeText(context, if (isMicMuted) "🎙 Mic unmuted" else "🎙 Mic muted", Toast.LENGTH_SHORT).show()
                        isMicMuted = !isMicMuted
                    }) {
                        Icon(
                            imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mic Toggle",
                            tint = if (isMicMuted) Color(0xFFEF4444) else SoftTeal
                        )
                    }

                    // Quick Emoji Launch Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    ) {
                        listOf("😍", "😂", "😮", "🎲", "🔥", "🏆").forEach { emoji ->
                            Surface(
                                color = Color(0xFF334155),
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable {
                                        activeEmojiReactions = activeEmojiReactions + emoji
                                        scope.launch {
                                            delay(1500)
                                            activeEmojiReactions = activeEmojiReactions.drop(1)
                                        }
                                        turnEngine.executeAction("EMOJI", emoji)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(emoji, fontSize = 16.sp)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            turnEngine.finishGame("Vikram")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmCoral)
                    ) {
                        Text("Finish", fontSize = 11.sp)
                    }
                }
            }
        }

        // Post-Match Celebration Modal
        if (sessionState.isGameOver) {
            PostMatchCelebrationModal(
                gameType = gameType,
                winnerName = sessionState.winnerId.ifEmpty { "Vikram" },
                partnerName = partnerName,
                matchDurationMins = (sessionState.moveCount * 0.3).toInt().coerceAtLeast(4),
                turnsCount = sessionState.moveCount,
                onRematch = {
                    turnEngine.executeAction("REMATCH", "Started new match")
                },
                onShareMemory = { summary ->
                    onShareMatchResult(summary)
                    onCloseGame()
                },
                onClose = {
                    onCloseGame()
                }
            )
        }

        // M3 Exit Confirmation Dialog
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                containerColor = Color(0xFF1E293B),
                title = { Text("Pause or Return to Chat?", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("Your match state (Move ${sessionState.moveCount}) will be preserved automatically so you can resume anytime!", color = Color(0xFF94A3B8)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showExitDialog = false
                            onCloseGame()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmCoral)
                    ) {
                        Text("Return to Chat")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false }) {
                        Text("Continue Playing", color = SoftTeal)
                    }
                }
            )
        }
    }
}

@Composable
fun ActiveGameContainerOverlay(
    gameType: GameType,
    partnerName: String,
    onCloseGame: () -> Unit,
    onShareMatchResult: (String) -> Unit
) {
    GameContainer(
        gameType = gameType,
        partnerName = partnerName,
        onCloseGame = onCloseGame,
        onShareMatchResult = onShareMatchResult
    ) { turnEngine, sessionState ->
        val isMyTurn = sessionState.currentPlayerId == "Vikram"

        when (gameType) {
            GameType.LUDO -> LudoGameCanvasView(
                isMyTurn = isMyTurn,
                partnerName = partnerName,
                onTurnFinished = {
                    turnEngine.switchTurn("Ludo Move Executed")
                },
                onGameOver = {
                    turnEngine.finishGame("Vikram")
                }
            )
            GameType.UNO -> UnoGameCanvasView(
                isMyTurn = isMyTurn,
                partnerName = partnerName,
                onTurnFinished = {
                    turnEngine.switchTurn("UNO Card Played")
                },
                onGameOver = {
                    turnEngine.finishGame("Vikram")
                }
            )
            GameType.BUSINESS -> BusinessGameCanvasView(
                isMyTurn = isMyTurn,
                partnerName = partnerName,
                onTurnFinished = {
                    turnEngine.switchTurn("Property Action Executed")
                },
                onGameOver = {
                    turnEngine.finishGame("Vikram")
                }
            )
            else -> {}
        }
    }
}

// 📶 Network Reconnecting Overlay with M3 Glassmorphism
@Composable
fun ReconnectingOverlay(
    moveCount: Int,
    onForceResume: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .border(1.dp, Color(0xFFF59E0B).copy(alpha = alphaPulse), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("⚡ Connection Lost", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                CircularProgressIndicator(
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Saving match state... Move $moveCount preserved.",
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Waiting for network reconnection to seamlessly resume match without progress loss.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onForceResume,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                ) {
                    Text("Resume Match Now", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 📞 Picture-in-Picture In-Call Floating Control Bar
@Composable
fun PipInCallFloatingBar(
    partnerName: String,
    isMuted: Boolean,
    isExpanded: Boolean,
    onToggleMute: () -> Unit,
    onToggleExpand: () -> Unit
) {
    Surface(
        color = Color(0xFF0F172A).copy(alpha = 0.9f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .border(1.dp, SoftTeal.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(WarmCoral),
                    contentAlignment = Alignment.Center
                ) {
                    Text(partnerName.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("📞 Voice Call Active", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("Talking to $partnerName • 00:14:22", color = SoftTeal, fontSize = 9.sp)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute",
                        tint = if (isMuted) Color(0xFFEF4444) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand Call",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}


// 🎲 Ludo Interactive Game View
@Composable
fun LudoGameCanvasView(
    isMyTurn: Boolean,
    partnerName: String,
    onTurnFinished: () -> Unit,
    onGameOver: () -> Unit
) {
    var diceValue by remember { mutableIntStateOf(6) }
    var isRolling by remember { mutableStateOf(false) }
    var myPositions by remember { mutableStateOf(listOf(0, 3, 7, 0)) }
    var partnerPositions by remember { mutableStateOf(listOf(0, 5, 0, 0)) }
    var actionLog by remember { mutableStateOf("Roll the dice to make your move!") }
    val scope = rememberCoroutineScope()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("🎲 Ludo Board", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)

            // Board Representation
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F172A))
                    .border(2.dp, SoftTeal, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Render 4 Quad Home Yards
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFEF4444).copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                            Text("Red Home\n(You)", color = Color.White, fontSize = 10.sp, textAlign = TextAlign.Center)
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF3B82F6).copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                            Text("Blue Home\n($partnerName)", color = Color.White, fontSize = 10.sp, textAlign = TextAlign.Center)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF10B981).copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                            Text("Green Home", color = Color.White, fontSize = 10.sp, textAlign = TextAlign.Center)
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFF59E0B).copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                            Text("Yellow Home", color = Color.White, fontSize = 10.sp, textAlign = TextAlign.Center)
                        }
                    }
                }

                // Center Home Victory Box
                Surface(
                    color = WarmCoral,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🏆 HOME", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                }
            }

            Text(actionLog, color = Color(0xFF94A3B8), fontSize = 12.sp, textAlign = TextAlign.Center)

            // Dice Roller
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    color = Color(0xFF334155),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .size(64.dp)
                        .clickable(enabled = isMyTurn && !isRolling) {
                            scope.launch {
                                isRolling = true
                                repeat(6) {
                                    diceValue = Random.nextInt(1, 7)
                                    delay(80)
                                }
                                isRolling = false
                                actionLog = "Rolled a $diceValue! Piece moved forward $diceValue cells."
                                if (diceValue == 6) {
                                    actionLog += " 🎉 Triple Six Chance!"
                                } else {
                                    delay(1000)
                                    onTurnFinished()
                                }
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = when(diceValue) {
                                1 -> "⚀"
                                2 -> "⚁"
                                3 -> "⚂"
                                4 -> "⚃"
                                5 -> "⚄"
                                else -> "⚅"
                            },
                            fontSize = 36.sp,
                            color = Color.White
                        )
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            isRolling = true
                            repeat(6) {
                                diceValue = Random.nextInt(1, 7)
                                delay(80)
                            }
                            isRolling = false
                            actionLog = "Rolled $diceValue! Executed move."
                            delay(800)
                            onTurnFinished()
                        }
                    },
                    enabled = isMyTurn && !isRolling,
                    colors = ButtonDefaults.buttonColors(containerColor = WarmCoral)
                ) {
                    Text(if (isMyTurn) "🎲 Roll Dice" else "Waiting for $partnerName...", fontSize = 12.sp)
                }
            }
        }
    }
}

// 🃏 UNO Interactive Game View
@Composable
fun UnoGameCanvasView(
    isMyTurn: Boolean,
    partnerName: String,
    onTurnFinished: () -> Unit,
    onGameOver: () -> Unit
) {
    var topCardColor by remember { mutableStateOf("Blue") }
    var topCardNumber by remember { mutableStateOf("7") }
    var myCardsCount by remember { mutableIntStateOf(4) }
    var partnerCardsCount by remember { mutableIntStateOf(3) }
    var showUnoFlash by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf("Match color or number!") }
    val scope = rememberCoroutineScope()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🃏 UNO Card Battle", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text("$partnerName has $partnerCardsCount cards", color = Color(0xFF38BDF8), fontSize = 11.sp)
            }

            // Top Discard Pile Card
            Box(
                modifier = Modifier
                    .size(120.dp, 160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        when (topCardColor) {
                            "Red" -> Color(0xFFEF4444)
                            "Blue" -> Color(0xFF3B82F6)
                            "Green" -> Color(0xFF10B981)
                            else -> Color(0xFFF59E0B)
                        }
                    )
                    .border(3.dp, Color.White, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(topCardNumber, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 36.sp)
                    Text(topCardColor, color = Color.White, fontSize = 12.sp)
                }
            }

            if (showUnoFlash) {
                Surface(
                    color = Color(0xFFEF4444),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("⚡ UNO CALLED BY VIKRAM!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(8.dp))
                }
            }

            Text(actionMessage, color = Color(0xFF94A3B8), fontSize = 12.sp, textAlign = TextAlign.Center)

            // Playable Cards Hand Row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        topCardColor = listOf("Red", "Blue", "Green", "Yellow").random()
                        topCardNumber = (1..9).random().toString()
                        myCardsCount = (myCardsCount - 1).coerceAtLeast(0)
                        actionMessage = "Played $topCardColor $topCardNumber!"
                        if (myCardsCount == 1) {
                            showUnoFlash = true
                        }
                        if (myCardsCount == 0) {
                            onGameOver()
                        } else {
                            onTurnFinished()
                        }
                    },
                    enabled = isMyTurn,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("🃏 Play Card", fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        topCardColor = "Red"
                        topCardNumber = "+4"
                        actionMessage = "🔥 +4 Wild Attack Used on $partnerName!"
                        onTurnFinished()
                    },
                    enabled = isMyTurn,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("🔥 +4 Attack", fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        myCardsCount++
                        actionMessage = "Drew 1 card from deck."
                        onTurnFinished()
                    },
                    enabled = isMyTurn,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569))
                ) {
                    Text("📥 Draw", fontSize = 11.sp)
                }
            }
        }
    }
}

// 🏢 Business (Monopoly-style) Game View
@Composable
fun BusinessGameCanvasView(
    isMyTurn: Boolean,
    partnerName: String,
    onTurnFinished: () -> Unit,
    onGameOver: () -> Unit
) {
    var myCash by remember { mutableIntStateOf(1500) }
    var partnerCash by remember { mutableIntStateOf(1420) }
    var currentTile by remember { mutableStateOf("Park Avenue") }
    var actionLog by remember { mutableStateOf("Landed on Park Avenue! Available to purchase.") }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("💼 You", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("$$myCash", color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("💼 $partnerName", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("$$partnerCash", color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // Current Property Tile Card
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFFF59E0B), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏢 $currentTile", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Property Price: $320 • Rent: $45", color = Color(0xFFFCD34D), fontSize = 12.sp)
                }
            }

            Text(actionLog, color = Color(0xFF94A3B8), fontSize = 12.sp, textAlign = TextAlign.Center)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (myCash >= 320) {
                            myCash -= 320
                            actionLog = "Bought $currentTile for $320!"
                            onTurnFinished()
                        }
                    },
                    enabled = isMyTurn && myCash >= 320,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Text("Buy Property ($320)", fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        val tiles = listOf("Broadway", "Times Square", "Wall Street", "Fifth Avenue")
                        currentTile = tiles.random()
                        actionLog = "Walked to $currentTile!"
                        onTurnFinished()
                    },
                    enabled = isMyTurn,
                    colors = ButtonDefaults.buttonColors(containerColor = WarmCoral)
                ) {
                    Text("🎲 Roll & Walk", fontSize = 11.sp)
                }
            }
        }
    }
}

// Winner Post Match Modal
@Composable
fun PostMatchCelebrationModal(
    gameType: GameType,
    winnerName: String,
    partnerName: String,
    matchDurationMins: Int,
    turnsCount: Int,
    onRematch: () -> Unit,
    onShareMemory: (String) -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.85f),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(24.dp)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, SoftTeal, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎉 MATCH FINISHED!", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("🏆 Winner: $winnerName", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("+150 Together Points Earned! ✨", color = SoftTeal, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("⏱ Match Duration: $matchDurationMins mins", color = Color.LightGray, fontSize = 12.sp)
                            Text("🔄 Total Turns: $turnsCount", color = Color.LightGray, fontSize = 12.sp)
                            Text("💬 Call Status: Voice Call continued smoothly", color = Color.LightGray, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onRematch,
                            colors = ButtonDefaults.buttonColors(containerColor = WarmCoral),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Play Again 🔄", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                onShareMemory("🏆 Play Together: $winnerName won ${gameType.name} in $matchDurationMins mins! GG ❤️")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftTeal),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Share Memory 💬", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onClose) {
                        Text("Return to Chat", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PlayTogetherLauncherCard(
    partnerName: String,
    onOpenGames: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenGames)
            .border(1.dp, SoftTeal.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = WarmCoral.copy(alpha = 0.2f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🎮", fontSize = 22.sp)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🎮 Play Together with $partnerName",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "Ludo, UNO, Business Tycoon • Relationship Memories & Couple Points",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Open", tint = SoftTeal)
        }
    }
}
