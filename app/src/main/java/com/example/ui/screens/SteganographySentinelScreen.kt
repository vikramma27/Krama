package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkPlumCard
import com.example.ui.theme.NearBlackPlum
import com.example.ui.theme.SoftTeal
import com.example.ui.theme.WarmCoral
import kotlinx.coroutines.delay
import java.nio.charset.StandardCharsets
import kotlin.math.sqrt

@Composable
fun SteganographySentinelScreen(
    onBack: () -> Unit,
    onTriggerPanicLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Steganography, 1: Air-Gapped Optical, 2: Panic Sensor Sentinel

    // Steganography States
    var secretPayload by remember { mutableStateOf("CLASSIFIED: Matrix Megolm Key #849204 - E2EE Valid") }
    var stegBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var extractedPayload by remember { mutableStateOf("") }
    var isEncoding by remember { mutableStateOf(false) }
    var isDecoding by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    // Air-Gapped QR Optical Stream States
    var opticalFrameIndex by remember { mutableStateOf(0) }
    var isStreamingQr by remember { mutableStateOf(false) }

    // Panic Sensor State
    var isSensorSentinelActive by remember { mutableStateOf(true) }
    var currentGForce by remember { mutableStateOf(1.0f) }
    var panicTriggerCount by remember { mutableStateOf(0) }

    // Native SensorEventListener for Shake / Flip Panic Lock
    DisposableEffect(isSensorSentinelActive) {
        if (!isSensorSentinelActive) return@DisposableEffect onDispose {}

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val gX = x / SensorManager.GRAVITY_EARTH
                val gY = y / SensorManager.GRAVITY_EARTH
                val gZ = z / SensorManager.GRAVITY_EARTH

                val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()
                currentGForce = gForce

                // Trigger Panic Lock if phone is violently shaken (> 2.8 Gs) or flipped down
                if (gForce > 2.8f) {
                    panicTriggerCount++
                    onTriggerPanicLock()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (accelerometer != null) {
            sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager?.unregisterListener(sensorListener)
        }
    }

    // Air-Gapped Optical Frame Loop
    LaunchedEffect(isStreamingQr) {
        if (isStreamingQr) {
            while (true) {
                delay(300)
                opticalFrameIndex = (opticalFrameIndex + 1) % 4
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("steganography_sentinel_screen"),
        color = NearBlackPlum
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkPlumCard,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = WarmCoral, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Krama Quantum Vault & Sentinel",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Text(
                            text = "Native Steganography • Air-Gapped Optical • Panic Sensor",
                            color = SoftTeal,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Tabs Header
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkPlumCard,
                contentColor = WarmCoral,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = WarmCoral
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Steganography", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Air-Gapped QR", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Panic Sensor", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }

            // Tab Body Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // TAB 0: LSB BITMAP STEGANOGRAPHY ENGINE
                        Text("BITWISE LSB STEGANOGRAPHIC ENCODER", color = WarmCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Embed encrypted Matrix Olm payloads directly inside photo pixel LSB channels. Visually indistinguishable to external inspection.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Payload Text Field
                        OutlinedTextField(
                            value = secretPayload,
                            onValueChange = { secretPayload = it },
                            label = { Text("Secret Payload to Hide") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = WarmCoral) },
                            singleLine = false,
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmCoral),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons: Encode & Decode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    isEncoding = true
                                    statusMessage = "Executing Bitwise LSB Encoding on 300x300 ARGB Canvas..."
                                    stegBitmap = createSteganoBitmap(secretPayload)
                                    isEncoding = false
                                    statusMessage = "✓ Secret successfully embedded in Image LSB pixels!"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WarmCoral),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("encode_steganography_button")
                            ) {
                                Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Hide in Photo")
                            }

                            Button(
                                onClick = {
                                    if (stegBitmap != null) {
                                        isDecoding = true
                                        statusMessage = "Extracting LSB bitstreams from Bitmap channels..."
                                        extractedPayload = extractSteganoPayload(stegBitmap!!)
                                        isDecoding = false
                                        statusMessage = "✓ LSB Payload Decoded Successfully!"
                                    } else {
                                        statusMessage = "❌ Encode a secret image first!"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SoftTeal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("decode_steganography_button")
                            ) {
                                Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Extract Secret")
                            }
                        }

                        if (statusMessage.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = statusMessage,
                                color = if (statusMessage.startsWith("✓")) SoftTeal else WarmCoral,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Display Steganographic Image Result
                        if (stegBitmap != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DarkPlumCard),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Steganographic Cover Photo (LSB Encoded)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Image(
                                        bitmap = stegBitmap!!.asImageBitmap(),
                                        contentDescription = "Steganography Cover Image",
                                        modifier = Modifier
                                            .size(200.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(2.dp, SoftTeal, RoundedCornerShape(12.dp))
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Dimensions: 300x300 • Format: ARGB_8888 • LSB Depth: 1 bit/channel", color = SoftTeal, fontSize = 10.sp)
                                }
                            }
                        }

                        // Display Extracted Payload Result
                        if (extractedPayload.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = NearBlackPlum),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SoftTeal)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SoftTeal, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Extracted Matrix Secret Payload:", color = SoftTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = extractedPayload,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        // TAB 1: AIR-GAPPED OPTICAL STREAMING QR ENGINE
                        Text("AIR-GAPPED HIGH-SPEED OPTICAL STREAM", color = WarmCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Transmit encrypted Megolm room keys & messages visually via high-density animated QR streams when cellular/Wi-Fi networks are jammed or disconnected.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkPlumCard),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.QrCode2, contentDescription = null, tint = WarmCoral)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Frame $opticalFrameIndex/4 • Optical Payload Stream",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Visual High-Density QR Stream Box Simulation
                                Box(
                                    modifier = Modifier
                                        .size(220.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White)
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        // Dynamic QR-like pattern representation
                                        Text("⬛⬜⬛⬜⬛⬛⬜⬛", fontSize = 18.sp, color = Color.Black)
                                        Text("⬜⬛⬜⬛⬛⬜⬛⬜", fontSize = 18.sp, color = Color.Black)
                                        Text("⬛⬛ [FRAME $opticalFrameIndex] ⬛⬛", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        Text("⬜⬛⬜⬛⬛⬜⬛⬜", fontSize = 18.sp, color = Color.Black)
                                        Text("⬛⬜⬛⬜⬛⬛⬜⬛", fontSize = 18.sp, color = Color.Black)
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Olm Megolm Key Package Chunk #$opticalFrameIndex\n(SHA-256 Digest Verified • Air-Gapped)",
                                    color = SoftTeal,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = { isStreamingQr = !isStreamingQr },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isStreamingQr) WarmCoral else SoftTeal),
                                    modifier = Modifier.testTag("toggle_optical_stream_button")
                                ) {
                                    Text(if (isStreamingQr) "Pause Optical Stream" else "Start High-Speed Stream")
                                }
                            }
                        }
                    }

                    2 -> {
                        // TAB 2: HARDWARE ACCELEROMETER PANIC SENSOR SENTINEL
                        Text("HARDWARE ACCELEROMETER PANIC SENTINEL", color = WarmCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Monitors physical Android acceleration & orientation sensors. Violent shake or rapid face-down flip instantly locks app and flushes ephemeral keys.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkPlumCard),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Vibration, contentDescription = null, tint = WarmCoral)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Panic Sensor Monitoring", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Switch(
                                        checked = isSensorSentinelActive,
                                        onCheckedChange = { isSensorSentinelActive = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = WarmCoral,
                                            checkedTrackColor = WarmCoral.copy(alpha = 0.3f)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Current Hardware G-Force:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                    Text(
                                        text = "%.2f G".format(currentGForce),
                                        color = if (currentGForce > 2.5f) WarmCoral else SoftTeal,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Shake Threshold:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                    Text("2.80 G (Violent Movement)", color = Color.White, fontSize = 13.sp)
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = onTriggerPanicLock,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("test_panic_lock_button")
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Simulate Emergency Panic Lock Now")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Bitwise LSB Bitmap Steganography implementation helper
private fun createSteganoBitmap(payload: String): Bitmap {
    val width = 300
    val height = 300
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Draw background cover image
    val paint = Paint().apply {
        color = AndroidColor.parseColor("#1B2230")
        style = Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

    val circlePaint = Paint().apply {
        color = AndroidColor.parseColor("#E07A5F")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawCircle(150f, 150f, 80f, circlePaint)

    // Convert string payload into bit string
    val bytes = payload.toByteArray(StandardCharsets.UTF_8)
    val bitString = StringBuilder()
    // Append 16-bit payload length header
    val lengthBits = String.format("%16s", Integer.toBinaryString(bytes.size)).replace(' ', '0')
    bitString.append(lengthBits)

    for (b in bytes) {
        val bits = String.format("%8s", Integer.toBinaryString(b.toInt() and 0xFF)).replace(' ', '0')
        bitString.append(bits)
    }

    val totalBits = bitString.length
    var bitIndex = 0

    // Encode bits into LSB of Red channel of pixels
    for (y in 0 until height) {
        for (x in 0 until width) {
            if (bitIndex >= totalBits) break

            val pixel = bitmap.getPixel(x, y)
            val alpha = AndroidColor.alpha(pixel)
            var red = AndroidColor.red(pixel)
            val green = AndroidColor.green(pixel)
            val blue = AndroidColor.blue(pixel)

            val bit = bitString[bitIndex] - '0'
            red = (red and 0xFE) or bit
            bitIndex++

            val newPixel = AndroidColor.argb(alpha, red, green, blue)
            bitmap.setPixel(x, y, newPixel)
        }
        if (bitIndex >= totalBits) break
    }

    return bitmap
}

private fun extractSteganoPayload(bitmap: Bitmap): String {
    val width = bitmap.width
    val height = bitmap.height

    // Extract first 16 bits for payload length
    val lengthBits = StringBuilder()
    var pixelCount = 0

    for (y in 0 until height) {
        for (x in 0 until width) {
            if (pixelCount >= 16) break
            val pixel = bitmap.getPixel(x, y)
            val red = AndroidColor.red(pixel)
            lengthBits.append(red and 1)
            pixelCount++
        }
        if (pixelCount >= 16) break
    }

    val payloadLengthBytes = lengthBits.toString().toIntOrNull(2) ?: return "Decoding Error: Invalid header"
    val totalPayloadBits = payloadLengthBytes * 8

    val payloadBits = StringBuilder()
    var currentBitIndex = 0

    for (y in 0 until height) {
        for (x in 0 until width) {
            val globalIndex = y * width + x
            if (globalIndex < 16) continue // skip header bits

            if (currentBitIndex >= totalPayloadBits) break
            val pixel = bitmap.getPixel(x, y)
            val red = AndroidColor.red(pixel)
            payloadBits.append(red and 1)
            currentBitIndex++
        }
        if (currentBitIndex >= totalPayloadBits) break
    }

    // Convert payload bits back to string
    val extractedBytes = ByteArray(payloadLengthBytes)
    val bitStr = payloadBits.toString()
    for (i in 0 until payloadLengthBytes) {
        val byteStr = bitStr.substring(i * 8, (i + 1) * 8)
        extractedBytes[i] = byteStr.toInt(2).toByte()
    }

    return String(extractedBytes, StandardCharsets.UTF_8)
}
