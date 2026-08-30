# Krama - Privacy-First Encrypted Messaging

A modern Android messaging application with end-to-end encryption, offline-first architecture, and zero server costs.

## Features

- **End-to-End Encryption**: Matrix protocol with Olm/Megolm encryption
- **Privacy-First Design**: App lock, biometric auth, privacy shields, FLAG_SECURE
- **Offline-First**: Full functionality without internet, automatic sync
- **Modern UI**: Material 3 design with distinctive purple/teal palette
- **Free Forever**: $0/month operating cost

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Krama System                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                        Firebase (Auth & Metadata)                      │   │
│   │   ┌──────────────┐  ┌──────────────┐  ┌───────────────────────┐  │   │
│   │   │   Firebase   │  │   Cloud      │  │    Cloud Messaging    │  │   │
│   │   │   Auth      │  │   Firestore  │  │    (Push Notifications)│  │   │
│   │   └──────────────┘  └──────────────┘  └───────────────────────┘  │   │
│   │          │                 │                      │                  │   │
│   │          └─────────────────┴──────────────────────┘                 │   │
│   │                            │                                        │   │
│   └────────────────────────────┼────────────────────────────────────────┘   │
│                                │                                             │
│   ┌────────────────────────────┼────────────────────────────────────────┐   │
│   │                            ▼                                         │   │
│   │   ┌─────────────────────────────────────────────────────────────┐  │   │
│   │   │                    Matrix.org (E2E Encrypted)               │  │   │
│   │   │                                                              │  │   │
│   │   │   ┌──────────────┐  ┌──────────────┐  ┌────────────────┐  │  │   │
│   │   │   │   Messaging  │  │    Media     │  │   VoIP        │  │  │   │
│   │   │   │   (E2E)     │  │   (<10MB)    │  │   Signaling   │  │  │   │
│   │   │   └──────────────┘  └──────────────┘  └────────────────┘  │  │   │
│   │   └─────────────────────────────────────────────────────────────┘  │   │
│   │                            │                                         │   │
│   └────────────────────────────┼────────────────────────────────────────┘   │
│                                │                                              │
│   ┌────────────────────────────┼────────────────────────────────────────┐   │
│   │                            ▼                                         │   │
│   │   ┌─────────────────────────────────────────────────────────────┐  │   │
│   │   │                   PixEdge (Fallback Media)                   │  │   │
│   │   │                                                              │  │   │
│   │   │   ┌──────────────┐  ┌──────────────┐  ┌────────────────┐  │  │   │
│   │   │   │  Next.js     │  │   Neon       │  │   Upstash     │  │  │   │
│   │   │   │  (Vercel)   │  │  PostgreSQL  │  │   Redis       │  │  │   │
│   │   │   └──────────────┘  └──────────────┘  └────────────────┘  │  │   │
│   │   │                            │                              │  │   │
│   │   │                   ┌────────▼────────┐                    │  │   │
│   │   │                   │    Telegram     │                    │  │   │
│   │   │                   │   (Storage)     │                    │  │   │
│   │   │                   └─────────────────┘                    │  │   │
│   │   └─────────────────────────────────────────────────────────────┘  │   │
│   │                                                                       │   │
│   │                        Krama Android App                              │   │
│   │   ┌──────────────────────────────────────────────────────────────┐   │   │
│   │   │  UI (Jetpack Compose) │ Domain │ Data (Room + SQLCipher)  │   │   │
│   │   └──────────────────────────────────────────────────────────────┘   │   │
│   │                                                                       │   │
└───┼───────────────────────────────────────────────────────────────────────┘   │
    │                                                                           │
    └───────────────────────────────────────────────────────────────────────────┘
```

---

## Data Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Media Upload Flow                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   User selects media                                                          │
│          │                                                                   │
│          ▼                                                                   │
│   ┌─────────────────┐                                                       │
│   │ Is file > 10MB? │                                                       │
│   └────────┬────────┘                                                       │
│            │                                                                  │
│      YES   │   NO                                                            │
│      ┌─────┴─────┐                                                          │
│      ▼           ▼                                                           │
│   ┌──────┐  ┌────────────────────────────────────────┐                       │
│   │PixEdge│  │ Matrix Upload                          │                       │
│   │Upload │  │ (E2E Encrypted)                       │                       │
│   └──┬───┘  └─────────────────┬──────────────────────┘                       │
│      │                        │                                               │
│      │                        ▼                                               │
│      │              ┌─────────────────┐                                     │
│      │              │ Upload success? │                                     │
│      │              └────────┬────────┘                                     │
│      │                   YES │ │ NO                                         │
│      │              ┌────────┘ └──┐                                        │
│      │              ▼             ▼                                         │
│      │         ┌────────┐    ┌─────────┐                                    │
│      │         │ Done   │    │ PixEdge │                                    │
│      │         └────────┘    │Fallback │                                    │
│      │                        └────┬────┘                                    │
│      └─────────────────────────────┼────────────────────────────────────────┘
│                                    │                                          │
│                                    ▼                                          │
│                            Send message with URL                             │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology | Purpose |
|-------|------------|---------|
| **UI** | Jetpack Compose + Material 3 | Modern declarative UI |
| **Language** | Kotlin 1.9+ | Android development |
| **Architecture** | Clean Architecture | Separation of concerns |
| **Local DB** | Room + SQLCipher | Encrypted local storage |
| **Auth** | Firebase Auth | User authentication |
| **Metadata** | Cloud Firestore | User profiles, settings |
| **Messaging** | Matrix SDK | E2E encrypted messaging |
| **Media** | Matrix + PixEdge | Encrypted media storage |
| **Push** | FCM | Push notifications |
| **DI** | Hilt | Dependency injection |
| **Async** | Kotlin Coroutines + Flow | Reactive programming |

---

## Project Structure

```
app/src/main/java/com/example/krama/
├── KramaApplication.kt       # App initialization
├── MainActivity.kt           # Main entry point
├── data/
│   ├── local/              # Room + SQLCipher
│   ├── remote/             # Firebase, Matrix, PixEdge
│   └── repository/         # Repository implementations
├── domain/
│   ├── model/             # Domain models
│   ├── repository/         # Repository interfaces
│   └── engine/            # Business logic engines
├── ui/
│   ├── components/        # Reusable UI components
│   ├── screens/           # Screen composables
│   ├── theme/             # Material 3 theming
│   └── viewmodel/         # ViewModels
└── service/               # Background services
```

---

## Prerequisites

### Firebase Setup

1. Download `google-services.json` from [Firebase Console](https://console.firebase.google.com/)
2. Place it in `app/google-services.json`
3. Enable authentication methods:
   - Email/Password
   - Google Sign-in
   - Phone (optional)

### Matrix Server

Uses [matrix.org](https://matrix.org/) by default. For self-hosting:

```kotlin
// In MatrixConfig
val homeserverUrl = "https://your-matrix-server.com"
```

### PixEdge (Optional)

For large file fallback (>10MB):

1. Deploy PixEdge following [PIXEDGE_DEPLOYMENT.md](../PixEdge/PIXEDGE_DEPLOYMENT.md)
2. Configure in `.env`:

```bash
PIXEDGE_API_URL=https://your-pixedge.vercel.app
PIXEDGE_API_KEY=your_api_key
```

---

## Building

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

### GitHub Actions

CI/CD automatically builds APKs on:
- Push to `main`/`master`
- Pull requests
- Version tags (`v1.0.0`)

---

## Cost Analysis

### Monthly Operating Cost: **$0**

| Service | Free Tier | Usage |
|---------|-----------|-------|
| Firebase Auth | 10K MAU | ~15 users ✓ |
| Cloud Firestore | 1GB / 50K reads/day | ~15 users ✓ |
| Cloud Messaging | Unlimited | Push notifications ✓ |
| Matrix (Public) | Free | Messaging ✓ |
| PixEdge (Telegram) | Free | Large file storage ✓ |
| GitHub Actions | 2000 min/month | CI/CD ✓ |

**One-time costs:**
- Google Play Developer Account: $25

---

## Configuration

### Environment Variables

Create `app/.env` from `.env.example`:

```bash
# Gemini AI (optional)
GEMINI_API_KEY=your_key

# PixEdge (optional - for files >10MB)
PIXEDGE_API_URL=https://your-pixedge.vercel.app
PIXEDGE_API_KEY=your_api_key
```

### Firebase Services

| Service | Purpose | Status |
|---------|---------|--------|
| Authentication | User identity | ✅ Active |
| Firestore | Metadata storage | ✅ Active |
| Cloud Messaging | Push notifications | ✅ Active |
| Analytics | Usage tracking | ✅ Active |
| Storage | **Deprecated** | ❌ Use Matrix/PixEdge |

---

## Security

### End-to-End Encryption

- Matrix Olm/Megolm protocol
- Identity keys for key verification
- Forward secrecy via session keys

### Local Storage

- SQLCipher AES-256 encryption
- Android Keystore for key management
- Encrypted SharedPreferences

### Privacy Features

- App lock (PIN/Biometric)
- FLAG_SECURE for screenshots
- Privacy indicators
- Message auto-destruct

---

## License

MIT License - see LICENSE file for details.

---

## Support

For issues and questions:
- [GitHub Issues](https://github.com/org-calm-moon-46812842/Krama/issues)
