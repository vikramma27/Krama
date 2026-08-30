# Krama Firebase Setup Guide

Complete setup guide for configuring Firebase backend services for the Krama messaging app.

## Project Information

- **Firebase Project ID:** `karmaapp-2bab2`
- **Android Package Name:** `com.aistudio.krama.messenger`
- **Project Number:** (See Firebase Console)

---

## Step 1: Download google-services.json

### Option A: Using Firebase CLI (Recommended)

If you have Firebase CLI installed and authenticated:

```bash
# Install Firebase CLI if not already installed
npm install -g firebase-tools

# Login to Firebase
firebase login

# Set the project as active
firebase use karmaapp-2bab2

# Download the Android SDK config
firebase apps:sdkconfig ANDROID --app app/google-services.json --project karmaapp-2bab2
```

### Option B: Manual Download from Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select project: **karmaapp-2bab2**
3. Click the gear icon ⚙️ → **Project Settings**
4. Scroll to "Your apps" section
5. If no Android app exists:
   - Click "Add app" → Select Android icon
   - Package name: `com.aistudio.krama.messenger`
   - App nickname: `Krama Messenger`
   - Optional: Add SHA-1 fingerprints for signing certs
   - Click "Register app"
6. Click "Download google-services.json"
7. Place the file in: `app/google-services.json`

**Important:** Do NOT commit this file to version control. It's already in `.gitignore`.

---

## Step 2: Enable Authentication Methods

### 2.1 Email/Password Authentication

1. Firebase Console → **Authentication** → **Sign-in method**
2. Click "Email/Password"
3. Enable:
   - ✅ **Email/Password** (Required)
   - ⬜ **Email link (passwordless sign-in)** (Optional - recommended)
4. Click "Save"

### 2.2 Google Sign-in

1. Firebase Console → **Authentication** → **Sign-in method**
2. Click "Google"
3. Enable: ✅ **Enable**
4. Select your **Web Client ID** from Google Cloud Console
5. Click "Save"

**Note:** You need to create OAuth 2.0 credentials in Google Cloud Console first:
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. APIs & Services → Credentials
3. Create OAuth 2.0 Client ID for Web application
4. Copy the Client ID to Firebase Console

### 2.3 Phone Authentication

1. Firebase Console → **Authentication** → **Sign-in method**
2. Click "Phone"
3. Enable: ✅ **Enable**
4. Select supported countries
5. Click "Save"

**For Development - Add Test Phone Numbers:**

1. In Phone sign-in settings, scroll to "Phone numbers for testing"
2. Add test numbers:
   ```
   Phone number: +1 555-000-0000
   Verification code: 123456
   ```
3. Click "Save"

This prevents SMS quota consumption during development.

---

## Step 3: Set Up Firestore Database

### 3.1 Create Database

1. Firebase Console → **Firestore Database** → **Create database**
2. Select **Start in production mode** (recommended for security)
3. Choose a location close to your users
4. Click "Enable"

### 3.2 Configure Security Rules

1. Firebase Console → **Firestore Database** → **Rules**
2. Copy the contents of `firestore.rules` from this project
3. Paste into the Firebase Console rules editor
4. Click "Publish"

**Current Rules Location:** `firestore.rules`

### 3.3 Create Indexes

Firestore will prompt you to create indexes when needed. To create manually:

1. Firebase Console → **Firestore Database** → **Indexes**
2. Add composite indexes for:
   - `conversations`: participants (ASC), lastMessageTime (DESC)
   - `messages`: conversationId (ASC), createdAt (DESC)

---

## Step 4: Firebase Storage (DEPRECATED)

### ⚠️ Important: Firebase Storage is No Longer Used

Media files are now stored via:
- **Matrix media repository** (primary) - E2E encrypted
- **PixEdge via Telegram** (fallback) - for files >10MB

You can skip this step unless you need Firebase Storage for avatars/thumbnails.

### 4.1 Enable Firebase Storage (Optional)

If you still want Firebase Storage for user avatars:

1. Firebase Console → **Storage** → **Get started**
2. Select "Start in production mode"
3. Choose a location
4. Click "Done"

### 4.2 Configure Storage Rules

1. Firebase Console → **Storage** → **Rules**
2. Copy the contents of `storage.rules` from this project
3. Paste into the rules editor
4. Click "Publish"

Note: The rules are configured to deny all writes to `/conversations/`, `/users/`, and `/tmp/` paths since these now use Matrix or PixEdge.

---

## Step 5: Enable Cloud Messaging

1. Firebase Console → **Messaging** → **Get started**
2. Android app is automatically configured via google-services.json
3. No additional setup needed for basic push notifications

---

## Step 6: Configure Google Sign-in (SHA-1 Fingerprints)

### For Debug Builds

Run this command to get your debug SHA-1:

```bash
keytool -list -v -alias androiddebugkey -keystore "%USERPROFILE%\.android\debug.keystore" -storepass android -keypass android
```

### For Release Builds

```bash
keytool -list -v -alias upload -keystore "path/to/your/keystore.jks" -storepass YOUR_STORE_PASSWORD -keypass YOUR_KEY_PASSWORD
```

### Add to Firebase Console

1. Firebase Console → Project Settings → Your apps → Select Android app
2. Scroll to "SHA certificate fingerprints"
3. Click "Add fingerprint"
4. Paste your SHA-1 fingerprint
5. Click "Save"
6. Download updated `google-services.json`

---

## Step 7: Set Up Authentication Emulator (Development Only)

For local development without consuming Firebase quotas:

### 7.1 Install Firebase Emulator Suite

```bash
npm install -g firebase-tools
firebase init emulators
```

### 7.2 Configure Emulators

Select:
- ✅ Authentication Emulator
- ✅ Firestore Emulator
- ⬜ Storage Emulator (optional - not needed)

### 7.3 Connect App to Emulators

In your app code (Kotlin), add:

```kotlin
if (BuildConfig.DEBUG) {
    // Connect to local emulators
    FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
    FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080)
}
```

**Note:** The IP `10.0.2.2` is the Android emulator's host loopback address.

---

## Step 8: Verify Configuration

After setup, verify your configuration:

### Check Authentication

1. Run the app
2. Try signing in with Email/Password
3. Verify user appears in Firebase Console → Authentication → Users

### Check Firestore

1. Create a test user
2. Check if user profile is created in Firestore Console → Database

### Check Media Upload (Matrix/PixEdge)

1. Upload a test image
2. Verify it appears in Matrix media repository or PixEdge
3. For large files (>10MB), verify PixEdge fallback works

---

## Step 9: Production Checklist

Before deploying to production:

- [ ] **google-services.json** is properly configured
- [ ] All authentication methods tested
- [ ] Firestore security rules published
- [ ] Firebase Storage rules published (if using avatars)
- [ ] SHA-1 fingerprints added for release builds
- [ ] Firebase Console shows no warnings
- [ ] Tested on both debug and release builds
- [ ] Backup configuration tested (if enabled)
- [ ] PixEdge deployed and configured (for large file fallback)

---

## Troubleshooting

### "google-services.json is missing" error

1. Verify file exists at: `app/google-services.json`
2. Ensure package name in file matches: `com.aistudio.krama.messenger`
3. Re-download from Firebase Console if needed

### "This app is not authorized to use Firebase Authentication" error

1. Verify SHA-1 fingerprint is added in Firebase Console
2. Check that google-services.json is the latest version
3. Ensure authentication methods are enabled

### "Quota exceeded" error

1. Check Firebase Console → Usage
2. Add billing account (Blaze plan) for pay-as-you-go
3. Or wait for quota reset (daily/monthly depending on service)

### Firestore permission denied

1. Check security rules in Firebase Console
2. Verify user is authenticated
3. Check that document paths match the rules

---

## Firebase Services Used in Krama

| Service | Purpose | Free Tier Limits | Status |
|---------|---------|-----------------|--------|
| **Authentication** | User identity | 10K MAU, 10K SMS/month (phone) | ✅ Active |
| **Firestore** | User profiles, messages | 1GB storage, 50K reads/day | ✅ Active |
| **Realtime Database** | Online presence, typing | 100 simultaneous connections | ✅ Active |
| **Cloud Storage** | Media files | 5GB storage | ⚠️ Deprecated |
| **Cloud Messaging** | Push notifications | Unlimited (no free tier) | ✅ Active |
| **Analytics** | Usage analytics | Unlimited | ✅ Active |

### Media Storage Migration

Media storage has been migrated from Firebase Storage to:
- **Matrix media repository** (primary) - E2E encrypted, free
- **PixEdge via Telegram** (fallback) - for files >10MB

This reduces Firebase Storage usage to near zero and provides E2E encryption for media.

---

## Cost Optimization Tips

1. **Use Firestore bundles** for frequent queries
2. **Enable offline persistence** to reduce reads
3. **Use pagination** for message history
4. **Delete old media** from local cache
5. **Monitor usage** in Firebase Console
6. **Set up billing alerts** to avoid surprises

---

## Support

For Firebase-related issues:
- [Firebase Documentation](https://firebase.google.com/docs)
- [Firebase Support](https://firebase.google.com/support)
- [Stack Overflow](https://stackoverflow.com/questions/tagged/firebase)
