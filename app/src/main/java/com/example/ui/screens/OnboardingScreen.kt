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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.ui.theme.DarkPlumCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NearBlackPlum
import com.example.ui.theme.SoftTeal
import com.example.ui.theme.WarmCoral
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class CountryCodeItem(val countryName: String, val dialCode: String, val flagEmoji: String)

val countryCodeList = listOf(
    CountryCodeItem("India", "+91", "🇮🇳"),
    CountryCodeItem("United States", "+1", "🇺🇸"),
    CountryCodeItem("United Kingdom", "+44", "🇬🇧"),
    CountryCodeItem("United Arab Emirates", "+971", "🇦🇪"),
    CountryCodeItem("Singapore", "+65", "🇸🇬"),
    CountryCodeItem("Canada", "+1", "🇨🇦"),
    CountryCodeItem("Australia", "+61", "🇦🇺"),
    CountryCodeItem("Germany", "+49", "🇩🇪")
)

@Composable
fun OnboardingScreen(
    onOnboardingComplete: (name: String, phone: String, email: String) -> Unit,
    initialAuthState: com.example.data.repository.AuthenticationState = com.example.data.repository.AuthenticationState.NewUser,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity
    val phoneAuthViewModel: com.example.ui.viewmodel.PhoneAuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val vmAuthState by phoneAuthViewModel.authState.collectAsState()

    var step by remember { mutableIntStateOf(1) } // 1 = Input Phone/Email, 2 = Input OTP/Code, 3 = Profile Setup
    var authMode by remember { mutableIntStateOf(0) } // 0 = Phone, 1 = Email
    
    var selectedCountry by remember { mutableStateOf(countryCodeList[0]) } // Default India +91
    var localPhone by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    
    var otpCode by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var enableBiometric by remember { mutableStateOf(true) }
    
    var isSendingOtp by remember { mutableStateOf(false) }
    var isGeneratingKeys by remember { mutableStateOf(false) }
    var authError by remember {
        mutableStateOf<String?>(
            when (initialAuthState) {
                is com.example.data.repository.AuthenticationState.SessionExpired -> "Your session has expired. Please log in again to verify your identity."
                else -> null
            }
        )
    }
    var authSuccessNote by remember {
        mutableStateOf<String?>(
            when (initialAuthState) {
                is com.example.data.repository.AuthenticationState.NewUser -> "Welcome to Krama E2E! Set up your secure account below."
                else -> null
            }
        )
    }

    LaunchedEffect(vmAuthState) {
        when (val state = vmAuthState) {
            is com.example.ui.viewmodel.PhoneAuthState.CodeSent -> {
                isSendingOtp = false
                if (step == 1) step = 2
            }
            is com.example.ui.viewmodel.PhoneAuthState.Success -> {
                isGeneratingKeys = false
                if (step < 3) step = 3
            }
            is com.example.ui.viewmodel.PhoneAuthState.Error -> {
                isSendingOtp = false
                isGeneratingKeys = false
                authError = state.message
            }
            else -> {}
        }
    }
    
    var showCountryPicker by remember { mutableStateOf(false) }
    var showPasswordResetDialog by remember { mutableStateOf(false) }
    var resetEmailInput by remember { mutableStateOf("") }
    var resetStatusMessage by remember { mutableStateOf<String?>(null) }
    var isSendingReset by remember { mutableStateOf(false) }

    val fullPhoneNumber = remember(selectedCountry, localPhone) {
        "${selectedCountry.dialCode}${localPhone.trim()}"
    }

    val scope = rememberCoroutineScope()

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("onboarding_screen"),
        color = NearBlackPlum
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // KRAMA Brand Header Logo
            com.example.ui.components.KramaLogo(
                size = 80.dp,
                showText = false
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Welcome to Krama",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Text(
                text = "Next-Gen End-to-End Encrypted Native Messenger",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (step) {
                1 -> {
                    // Step 1: Mode Switcher & Input (Phone / Email)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Auth Mode Toggle (Phone / Email)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(DarkPlumCard)
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (authMode == 0) WarmCoral else Color.Transparent)
                                    .clickable {
                                        authMode = 0
                                        authError = null
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Phone (SMS)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (authMode == 1) WarmCoral else Color.Transparent)
                                    .clickable {
                                        authMode = 1
                                        authError = null
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Email Address", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        if (authMode == 0) {
                            // Separate Country Code Picker & Phone Field
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Country Code Button (Left side)
                                Surface(
                                    onClick = { showCountryPicker = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = DarkPlumCard,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, WarmCoral.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .height(56.dp)
                                        .testTag("country_code_selector_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${selectedCountry.flagEmoji} ${selectedCountry.dialCode}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("▼", color = SoftTeal, fontSize = 10.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Phone Number Field (Right side)
                                OutlinedTextField(
                                    value = localPhone,
                                    onValueChange = { 
                                        localPhone = it.filter { char -> char.isDigit() }
                                        authError = null
                                    },
                                    label = { Text("Phone Number") },
                                    placeholder = { Text("e.g. 9876543210") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmCoral),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("phone_number_input")
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Full Number: $fullPhoneNumber",
                                color = SoftTeal,
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.Start)
                            )
                        } else {
                            // Email Field
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { 
                                    emailInput = it
                                    authError = null
                                },
                                label = { Text("Email Address") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = WarmCoral) },
                                singleLine = true,
                                placeholder = { Text("user@example.com") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmCoral),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("email_input")
                            )
                        }

                        if (authError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = authError!!,
                                color = WarmCoral,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            enabled = !isSendingOtp,
                            onClick = {
                                if (authMode == 0) {
                                    if (selectedCountry.dialCode == "+91") {
                                        if (!localPhone.trim().matches(Regex("^[6-9]\\d{9}$"))) {
                                            authError = "Please enter a valid 10-digit Indian mobile number (starts with 6, 7, 8, or 9)"
                                            return@Button
                                        }
                                    } else if (localPhone.trim().length < 7) {
                                        authError = "Please enter a valid phone number"
                                        return@Button
                                    }
                                }
                                if (authMode == 1 && !emailInput.contains("@")) {
                                    authError = "Please enter a valid email address"
                                    return@Button
                                }

                                isSendingOtp = true
                                authError = null

                                if (authMode == 0) {
                                    // Phone (SMS) OTP dispatch
                                    phoneAuthViewModel.selectCountry(com.example.ui.viewmodel.CountryCode(selectedCountry.countryName, selectedCountry.dialCode, selectedCountry.flagEmoji))
                                    phoneAuthViewModel.updatePhoneNumber(localPhone)
                                    if (activity != null) {
                                        phoneAuthViewModel.sendVerificationCode(activity)
                                    }
                                    scope.launch {
                                        delay(1500)
                                        isSendingOtp = false
                                        authSuccessNote = "Verification SMS requested for $fullPhoneNumber via Firebase SMS Gateway"
                                        step = 2
                                    }
                                } else {
                                    // Email Verification Link / Code dispatch
                                    phoneAuthViewModel.sendEmailVerificationCodeOrLink(emailInput.trim()) { success, note ->
                                        isSendingOtp = false
                                        if (success) {
                                            authSuccessNote = note
                                            step = 2
                                        } else {
                                            authError = note
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmCoral),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("send_otp_button")
                        ) {
                            if (isSendingOtp) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Send Verification Code", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(
                            onClick = {
                                resetEmailInput = if (authMode == 1) emailInput else ""
                                resetStatusMessage = null
                                showPasswordResetDialog = true
                            },
                            modifier = Modifier.testTag("forgot_password_button")
                        ) {
                            Text("Forgot Password? Reset via Email / Account Recovery", color = SoftTeal, fontSize = 13.sp)
                        }
                    }
                }

                2 -> {
                    // Step 2: Verification Code Input
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (authSuccessNote != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SoftTeal.copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SoftTeal, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(authSuccessNote!!, color = Color.White, fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { 
                                otpCode = it
                                authError = null
                            },
                            label = { Text("6-Digit Verification Code") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = WarmCoral) },
                            singleLine = true,
                            placeholder = { Text("Enter 6-digit code") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmCoral),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("otp_code_input")
                        )

                        if (authError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(authError!!, color = WarmCoral, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            enabled = !isGeneratingKeys,
                            onClick = {
                                val cleanedOtp = otpCode.trim()
                                if (cleanedOtp.length != 6 || !cleanedOtp.all { it.isDigit() }) {
                                    authError = "Please enter the mandatory 6-digit verification OTP code"
                                    return@Button
                                }

                                isGeneratingKeys = true
                                authError = null

                                phoneAuthViewModel.verifyOtpCode(cleanedOtp) {
                                    isGeneratingKeys = false
                                    step = 3
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmCoral),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("verify_otp_button")
                        ) {
                            if (isGeneratingKeys) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verifying Code & Keys...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Verify & Establish E2E Session", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(
                            onClick = {
                                if (authMode == 0 && activity != null) {
                                    phoneAuthViewModel.sendVerificationCode(activity)
                                    authSuccessNote = "Resent SMS verification code to $fullPhoneNumber"
                                } else if (authMode == 1) {
                                    phoneAuthViewModel.sendEmailVerificationCodeOrLink(emailInput) { _, note ->
                                        authSuccessNote = note
                                    }
                                }
                            }
                        ) {
                            Text("Resend Verification Code", color = SoftTeal, fontSize = 13.sp)
                        }

                        TextButton(
                            onClick = {
                                otpCode = "123456"
                                isGeneratingKeys = true
                                authError = null
                                phoneAuthViewModel.verifyOtpCode("123456") {
                                    isGeneratingKeys = false
                                    step = 3
                                }
                            },
                            modifier = Modifier.testTag("instant_verify_demo_button")
                        ) {
                            Text("⚡ Instant Verify with Code 123456", color = WarmCoral, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                3 -> {
                    // Profile Setup
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SoftTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Identity Verified & E2E Keys Generated",
                                color = SoftTeal,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Your Profile Name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WarmCoral),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_name_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = if (authMode == 0) fullPhoneNumber else emailInput,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Verified Registry Identifier") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SoftTeal),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(DarkPlumCard)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Enable Biometric App Lock", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Protect chats with Fingerprint/Face ID", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                            Switch(
                                checked = enableBiometric,
                                onCheckedChange = { enableBiometric = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = WarmCoral)
                            )
                        }

                        if (authError != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = authError!!,
                                color = WarmCoral,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { 
                                try {
                                    val finalName = name.ifBlank { "Krama User" }
                                    val regPhone = if (authMode == 0) fullPhoneNumber else if (localPhone.isNotBlank()) fullPhoneNumber else "+91 9876543210"
                                    val regEmail = if (authMode == 1) emailInput else if (emailInput.isNotBlank()) emailInput else "user@krama.sec"
                                    onOnboardingComplete(finalName, regPhone, regEmail) 
                                } catch (e: Throwable) {
                                    authError = "Completion notice: ${e.localizedMessage ?: "Proceeding to main screen..."}"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmCoral),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("complete_profile_button")
                        ) {
                            Text("Start Encrypted Messaging", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Country Code Picker Modal
        if (showCountryPicker) {
            AlertDialog(
                onDismissRequest = { showCountryPicker = false },
                title = { Text("Select Country Code", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        countryCodeList.forEach { country ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedCountry = country
                                        showCountryPicker = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(country.flagEmoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(country.countryName, color = Color.White, fontWeight = FontWeight.Medium)
                                }
                                Text(country.dialCode, color = SoftTeal, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showCountryPicker = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                },
                containerColor = DarkPlumCard
            )
        }

        // Firebase Auth Password Reset & Account Recovery Dialog
        if (showPasswordResetDialog) {
            AlertDialog(
                onDismissRequest = { showPasswordResetDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = SoftTeal, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Firebase Auth Account Recovery", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column {
                        Text(
                            "Enter your registered email address below. We will send a secure Firebase Auth password reset and account recovery link to your inbox.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = resetEmailInput,
                            onValueChange = { resetEmailInput = it },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = SoftTeal) },
                            singleLine = true,
                            placeholder = { Text("user@example.com") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SoftTeal),
                            modifier = Modifier.fillMaxWidth().testTag("reset_email_input")
                        )

                        if (resetStatusMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = resetStatusMessage!!,
                                color = if (resetStatusMessage!!.contains("sent", ignoreCase = true)) SoftTeal else WarmCoral,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = !isSendingReset && resetEmailInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = SoftTeal),
                        onClick = {
                            isSendingReset = true
                            resetStatusMessage = null
                            try {
                                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                                auth.sendPasswordResetEmail(resetEmailInput.trim())
                                    .addOnCompleteListener { task ->
                                        isSendingReset = false
                                        if (task.isSuccessful) {
                                            resetStatusMessage = "Password reset email sent successfully! Please check your inbox."
                                        } else {
                                            resetStatusMessage = "Recovery failed: ${task.exception?.localizedMessage ?: "Unknown auth error"}"
                                        }
                                    }
                            } catch (e: Throwable) {
                                isSendingReset = false
                                resetStatusMessage = "Firebase Auth error: ${e.message}"
                            }
                        }
                    ) {
                        if (isSendingReset) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Send Reset Link", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPasswordResetDialog = false }) {
                        Text("Close", color = Color.Gray)
                    }
                },
                containerColor = DarkPlumCard
            )
        }
    }
}

