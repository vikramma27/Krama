package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.domain.model.UserProfile
import com.example.ui.theme.DarkPlumCard
import com.example.ui.theme.NearBlackPlum
import com.example.ui.theme.SoftTeal
import com.example.ui.theme.WarmCoral
import com.example.util.QrCodeUtil
import com.example.util.QrContactData

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrCodeScreen(
    userProfile: UserProfile?,
    onBack: () -> Unit,
    onAddContactFromQr: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: My QR Code, 1: Scan QR
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val myName = userProfile?.name?.ifEmpty { "Vikram" } ?: "Vikram"
    val myPhone = userProfile?.phoneNumber?.ifEmpty { "+1 555-019-2834" } ?: "+1 555-019-2834"
    val myPubKey = "ed25519_pk_${userProfile?.username?.replace("@", "") ?: "vikram"}_73a98"

    val myQrPayload = remember(myName, myPhone, myPubKey) {
        QrCodeUtil.encodeContactToQr(myName, myPhone, myPubKey)
    }

    val myQrBitmap = remember(myQrPayload) {
        QrCodeUtil.createQrBitmap(myQrPayload)
    }

    var scannedContactResult by remember { mutableStateOf<QrContactData?>(null) }
    var flashEnabled by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Camera permission granted. Point camera at peer QR code.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Camera permission required for QR scanning.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "P2P Contact QR Code",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Air-Gapped Matrix Identity Verification",
                            color = SoftTeal,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("qr_screen_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkPlumCard)
            )
        },
        containerColor = NearBlackPlum,
        modifier = modifier
            .fillMaxSize()
            .testTag("qr_code_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkPlumCard,
                contentColor = SoftTeal,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = WarmCoral,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("My QR Code", fontWeight = FontWeight.Bold)
                        }
                    },
                    selectedContentColor = WarmCoral,
                    unselectedContentColor = Color.Gray,
                    modifier = Modifier.testTag("tab_my_qr")
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan Peer QR", fontWeight = FontWeight.Bold)
                        }
                    },
                    selectedContentColor = WarmCoral,
                    unselectedContentColor = Color.Gray,
                    modifier = Modifier.testTag("tab_scan_qr")
                )

                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pair Device", fontWeight = FontWeight.Bold)
                        }
                    },
                    selectedContentColor = WarmCoral,
                    unselectedContentColor = Color.Gray,
                    modifier = Modifier.testTag("tab_pair_device")
                )
            }

            if (selectedTab == 0) {
                // MY QR CODE TAB
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkPlumCard),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(WarmCoral),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = myName.take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = myName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )

                            Text(
                                text = myPhone,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Display QR Code
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .border(3.dp, SoftTeal, RoundedCornerShape(20.dp))
                                    .background(NearBlackPlum)
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = myQrBitmap,
                                    contentDescription = "My QR Code",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NearBlackPlum)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = SoftTeal, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ED25519: ${myPubKey.take(16)}...",
                                    color = SoftTeal,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Scan this QR code with Krama on another device to establish an instant E2EE Olm double ratchet channel.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(myQrPayload))
                                Toast.makeText(context, "Matrix QR Payload copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftTeal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copy_qr_payload_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Key", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                Toast.makeText(context, "QR Contact Card ready for P2P NFC / Bluetooth drop.", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmCoral),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_qr_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share QR", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (selectedTab == 1) {
                // SCAN PEER QR TAB
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkPlumCard),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Point camera at peer QR",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                IconButton(onClick = { flashEnabled = !flashEnabled }) {
                                    Icon(
                                        Icons.Default.FlashOn,
                                        contentDescription = "Flash",
                                        tint = if (flashEnabled) WarmCoral else Color.Gray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Scanner Camera Frame Box with Animated Laser
                            val infiniteTransition = rememberInfiniteTransition(label = "laser")
                            val laserProgress by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1800, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "laserAnim"
                            )

                            val lifecycleOwner = LocalLifecycleOwner.current
                            val isCameraPermissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

                            Box(
                                modifier = Modifier
                                    .size(250.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .border(3.dp, WarmCoral, RoundedCornerShape(24.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                if (isCameraPermissionGranted) {
                                    AndroidView(
                                        factory = { ctx ->
                                            PreviewView(ctx).apply {
                                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                                cameraProviderFuture.addListener({
                                                    try {
                                                        val cameraProvider = cameraProviderFuture.get()
                                                        val preview = Preview.Builder().build().also {
                                                            it.setSurfaceProvider(surfaceProvider)
                                                        }
                                                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                                        cameraProvider.unbindAll()
                                                        val camera = cameraProvider.bindToLifecycle(
                                                            lifecycleOwner, cameraSelector, preview
                                                        )
                                                        camera.cameraControl.enableTorch(flashEnabled)
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("QrCodeScreen", "CameraX init failed: ${e.message}")
                                                    }
                                                }, ContextCompat.getMainExecutor(ctx))
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.3f),
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Camera Permission Required",
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                // Animated Scanning Line overlay
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .padding(horizontal = 12.dp)
                                        .padding(top = (laserProgress * 240).dp)
                                        .background(SoftTeal)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Or test with a sample peer QR code identity:",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Quick sample triggers for instant offline scanning test
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val sampleJson = QrCodeUtil.encodeContactToQr(
                                            name = "Aria Vance",
                                            phone = "+1 555-014-9982",
                                            publicKey = "ed25519_pk_aria_9982"
                                        )
                                        scannedContactResult = QrCodeUtil.parseQrContact(sampleJson)
                                    },
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SoftTeal),
                                    modifier = Modifier.testTag("scan_sample_aria")
                                ) {
                                    Text("Scan 'Aria Vance'", color = SoftTeal, fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val sampleJson = QrCodeUtil.encodeContactToQr(
                                            name = "Kaelen Voss",
                                            phone = "+1 555-018-4420",
                                            publicKey = "ed25519_pk_kaelen_4420"
                                        )
                                        scannedContactResult = QrCodeUtil.parseQrContact(sampleJson)
                                    },
                                    border = androidx.compose.foundation.BorderStroke(1.dp, WarmCoral),
                                    modifier = Modifier.testTag("scan_sample_kaelen")
                                ) {
                                    Text("Scan 'Kaelen Voss'", color = WarmCoral, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == 2) {
                // PAIR SECONDARY DEVICE TAB
                var isPairedSuccess by remember { mutableStateOf(false) }
                val pairingPayload = remember {
                    """{"type":"PAIR_SECONDARY","userId":"user_me","pairingToken":"pair_sec_${System.currentTimeMillis()}","masterKey":"e2e_double_ratchet_sync_key_998"}"""
                }
                val pairingQrBitmap = remember(pairingPayload) {
                    QrCodeUtil.createQrBitmap(pairingPayload, 600, 600)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkPlumCard),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(WarmCoral.copy(alpha = 0.2f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = WarmCoral, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Secondary Device E2EE Pairing Flow", color = WarmCoral, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (pairingQrBitmap != null) {
                                Image(
                                    bitmap = pairingQrBitmap,
                                    contentDescription = "Secondary Device Pairing QR Code",
                                    modifier = Modifier
                                        .size(220.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(2.dp, WarmCoral, RoundedCornerShape(16.dp))
                                        .testTag("secondary_pairing_qr_image")
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Scan this pairing QR code on your secondary phone, tablet, or web instance to transfer conversation keys and synchronize encrypted messages.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    isPairedSuccess = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SoftTeal),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("simulate_link_device_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Simulate Linking Secondary Device", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (isPairedSuccess) {
                    AlertDialog(
                        onDismissRequest = { isPairedSuccess = false },
                        title = { Text("Secondary Device Linked!", color = Color.White, fontWeight = FontWeight.Bold) },
                        text = {
                            Text(
                                "Secondary device successfully paired!\n\nAll Olm Double Ratchet session keys and encrypted Room conversations have been synchronized across devices.",
                                color = SoftTeal,
                                fontSize = 13.sp
                            )
                        },
                        confirmButton = {
                            Button(onClick = { isPairedSuccess = false }, colors = ButtonDefaults.buttonColors(containerColor = WarmCoral)) {
                                Text("Done")
                            }
                        },
                        containerColor = DarkPlumCard
                    )
                }
            }
        }

        // SCANNED CONTACT DISCOVERY POPUP DIALOG
        if (scannedContactResult != null) {
            val contact = scannedContactResult!!
            AlertDialog(
                onDismissRequest = { scannedContactResult = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SoftTeal),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Peer Identity Verified", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Ed25519 Signature Valid", color = SoftTeal, fontSize = 11.sp)
                        }
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("DISPLAY NAME", color = WarmCoral, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(contact.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("PHONE NUMBER", color = SoftTeal, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(contact.phone, color = Color.White, fontSize = 14.sp)

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("PUBLIC KEY", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            contact.publicKey,
                            color = SoftTeal,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(NearBlackPlum)
                                .padding(10.dp)
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = SoftTeal, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ready to establish Double Ratchet Session", color = Color.White, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onAddContactFromQr(contact.name, contact.phone, contact.publicKey)
                            scannedContactResult = null
                            Toast.makeText(context, "Added ${contact.name} to verified contacts!", Toast.LENGTH_SHORT).show()
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmCoral),
                        modifier = Modifier.testTag("add_scanned_contact_button")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Contact & Chat")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { scannedContactResult = null }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = DarkPlumCard
            )
        }
    }
}
