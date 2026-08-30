# Firebase Authentication Configuration

## Required Authentication Methods

Enable the following sign-in methods in Firebase Console > Authentication > Sign-in method:

### 1. Email/Password Authentication
- **Status:** Required
- **Enable:** Yes
- **Email links (passwordless):** Optional (recommended for better UX)

### 2. Google Sign-in
- **Status:** Required  
- **Enable:** Yes
- **Web Client ID:** Configure in Google Cloud Console
- **SHA-1 fingerprint:** Required for Android
  - Debug: `keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore -storepass android -keypass android -v`
  - Release: Your release keystore fingerprint

### 3. Phone Authentication
- **Status:** Required
- **Enable:** Yes
- **Supported countries:** Select all or specific countries based on user base
- **ReCAPTCHA verification:** Automatic (configured in google-services.json)

## Setup Steps

### Step 1: Enable Authentication Methods

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: `karmaapp-2bab2`
3. Navigate to Authentication > Sign-in method
4. Enable:
   - **Email/Password** (enable Email link optional)
   - **Google** (configure with your Web Client ID)
   - **Phone** (select supported countries)

### Step 2: Configure Google Sign-in (if not already done)

1. Go to Google Cloud Console > APIs & Services > Credentials
2. Create an OAuth 2.0 Client ID for Web application
3. Add the client ID to Firebase Console > Authentication > Sign-in method > Google

### Step 3: Add SHA Fingerprints

1. Go to Firebase Console > Project Settings > General
2. Scroll to "Your apps" section
3. Select your Android app: `com.aistudio.krama.messenger`
4. Add SHA certificate fingerprints:
   - Debug: Run `keytool -list -v -alias androiddebugkey -keystore %USERPROFILE%\.android\debug.keystore -storepass android`
   - Release: Use your release keystore fingerprint

### Step 4: Verify phone number for testing (Development only)

1. Firebase Console > Authentication > Sign-in method > Phone
2. Add test phone numbers and verification codes
3. This bypasses quota limits during development

## Security Considerations

### Rate Limiting
Firebase automatically applies rate limiting to prevent abuse:
- Phone: 5 SMS/ hour per country
- Email: 10,000 emails/day for password reset

### Quotas & Costs
- **Free Tier (Spark):** 10K SMS/month for phone auth
- **Pay-as-you-go (Blaze):** $0.01-0.06 per SMS depending on country

### Best Practices
1. Enable App Check to prevent abuse
2. Implement reCAPTCHA v3 for phone verification
3. Use Firebase Authentication session management
4. Enable multi-factor authentication for sensitive apps
