# Krama - Privacy-First Encrypted Messaging App

A modern, privacy-focused Android messaging application with end-to-end encryption, offline-first architecture, and a distinctive Material 3 design.

## Features

- **End-to-End Encryption**: Built on Matrix protocol with SQLCipher for local storage
- **Privacy-First Design**: App lock, biometric authentication, privacy shields, and FLAG_SECURE
- **Offline-First**: Full functionality without internet, with automatic sync when online
- **Modern UI**: Material 3 design with distinctive color palette and smooth animations
- **Free & Open Source**: No server costs, no data harvesting, completely free to run

## Tech Stack

- **Language**: Kotlin 1.9+
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: Clean Architecture (Presentation / Domain / Data layers)
- **Backend**: Firebase (Auth, Firestore, Cloud Messaging, Analytics) + Matrix SDK
- **Local Storage**: Room Database with SQLCipher encryption
- **Background Processing**: WorkManager
- **Dependency Injection**: Hilt

## Prerequisites

Before building the app, you need to set up:

1. **Firebase Project** (Already configured: `karmaapp-2bab2`)
   - Download `google-services.json` and place it in `app/` directory
   - See [FIREBASE_SETUP.md](./FIREBASE_SETUP.md) for complete setup instructions
   - Enable authentication methods: Email/Password, Google Sign-in, Phone
   - Configure Firestore database with security rules

2. **Matrix Server** (Optional but recommended)
   - Use a public Matrix server like [matrix.org](https://matrix.org/)
   - Or host your own using [Synapse](https://github.com/matrix-org/synapse) or [Conduwuit](https://github.com/girlbossceo/conduwuit)

3. **Gemini API Key** (Optional)
   - For AI features, get an API key from [Google AI Studio](https://aistudio.google.com/app/apikey)
   - Add to your local `.env` file or GitHub Secrets

## Building

### Local Development

1. Clone the repository
2. Place your `google-services.json` in `app/` directory
3. Create a `.env` file with your configuration (see `.env.example`)
4. Build the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```

### GitHub Actions (Automatic Builds)

The repository includes CI/CD that automatically builds APKs on:
- Push to `main`/`master` branches
- Pull requests
- Version tags (e.g., `v1.0.0`)

APKs are uploaded as build artifacts (30-day retention).
Releases are created automatically when you push version tags.

**Note**: The CI build creates a placeholder `google-services.json` if not present, so builds succeed without the actual Firebase config. For production deployments, add `google-services.json` to GitHub Secrets.

## Firebase Configuration

### Quick Setup

1. **Download google-services.json**
   - Firebase Console → Project Settings → Your apps → Download google-services.json
   - Place in: `app/google-services.json`

2. **Enable Authentication**
   - Authentication → Sign-in method
   - Enable: Email/Password, Google, Phone

3. **Create Firestore Database**
   - Firestore → Create database (Production mode)
   - Copy rules from `firestore.rules`
   - Publish rules

4. **Configure Storage (Optional)**
   - Storage → Get started
   - Copy rules from `storage.rules`
   - Publish rules

For detailed instructions, see [FIREBASE_SETUP.md](./FIREBASE_SETUP.md).

### Firebase Services Used

| Service | Purpose | Configuration |
|---------|---------|---------------|
| **Authentication** | User identity | Email/Password, Google, Phone |
| **Firestore** | User profiles, messages | Rules in `firestore.rules` |
| **Realtime Database** | Presence, typing | Included in google-services.json |
| **Cloud Storage** | Media files | Rules in `storage.rules` |
| **Cloud Messaging** | Push notifications | Auto-configured |

## Configuration

### Environment Variables

Create `app/.env` from `.env.example`:

```bash
# Gemini AI API (optional)
GEMINI_API_KEY=your_api_key_here
```

### Matrix Configuration

The app uses Matrix for E2E encrypted messaging. Configure in `MatrixConfig`:

```kotlin
// In your data layer
val homeserverUrl = "https://matrix.org" // or your custom server
```

## Project Structure

```
app/src/main/java/com/example/
├── KramaApplication.kt      # Application class with initialization
├── MainActivity.kt          # Main activity with Compose UI
├── data/                    # Data layer
│   ├── local/             # Room database, SQLCipher
│   ├── remote/            # Firebase, Matrix SDK
│   └── repository/         # Repository implementations
├── domain/                  # Business logic layer
│   ├── model/             # Domain models
│   ├── repository/         # Repository interfaces
│   └── engine/            # Lifecycle, Network, Recovery engines
├── ui/                      # Presentation layer
│   ├── components/         # Reusable Compose components
│   ├── screens/           # Screen composables
│   ├── theme/             # Material 3 theme
│   └── viewmodel/         # ViewModels
└── service/                # Background services
```

## Backend Architecture

The app uses a hybrid backend approach for zero-cost operation:

1. **Firebase** (Free Tier)
   - Authentication
   - Cloud Firestore for metadata
   - Cloud Messaging for push notifications
   - Analytics

2. **Matrix** (Public Server / Self-Hosted)
   - End-to-end encrypted messaging
   - VoIP signaling
   - Room management

3. **Local Storage**
   - Room + SQLCipher for encrypted local data
   - SharedPreferences (encrypted)
   - File-based storage for media

This architecture ensures:
- No server costs (using free tiers)
- Full offline functionality
- End-to-end encryption
- No vendor lock-in

## Cost Analysis

For ~15 users:
- Firebase Free Tier: Sufficient (100GB Firestore, 1GB Storage, etc.)
- Matrix Public Server: Free
- GitHub Actions: Free (2000 min/month)
- Google Play: $25 one-time fee

**Total Monthly Cost: $0**

## Deployment

### Debug Build
```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### Release Build
```bash
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

For release builds, set these environment variables:
```bash
export KEYSTORE_PATH=/path/to/keystore.jks
export STORE_PASSWORD=your_store_password
export KEY_PASSWORD=your_key_password
```

### Google Play

1. Create a Google Play Developer account ($25 one-time)
2. Upload the release APK to Play Console
3. Configure app signing (Google can manage keys for free)
4. Submit for review

## License

This project is licensed under the MIT License - see LICENSE file for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## Support

For issues and questions, please open a GitHub issue.