# Krama System Architecture

Complete system architecture documentation for the Krama messaging project.

## Overview

Krama is a privacy-first encrypted messaging app that uses a hybrid backend approach to achieve zero monthly costs while maintaining end-to-end encryption and reliable media delivery.

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              Krama System Architecture                          │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌─────────────────┐                         ┌─────────────────┐              │
│  │  Krama Android  │                         │  PixEdge Web     │              │
│  │  Client App     │                         │  Dashboard       │              │
│  └────────┬────────┘                         └────────┬────────┘              │
│           │                                           │                        │
│           │                                           │                        │
│  ┌────────▼──────────────────────────────────────────▼────────┐              │
│  │                    Firebase (Auth & Metadata)              │              │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐│              │
│  │  │   Auth      │  │  Firestore  │  │  Cloud Messaging     ││              │
│  │  │ (Identity)  │  │ (Metadata)  │  │  (Push Notifications)││              │
│  │  └─────────────┘  └─────────────┘  └─────────────────────┘│              │
│  └───────────────────────────────────────────────────────────┘              │
│                                    │                                           │
│                                    │                                           │
│  ┌─────────────────────────────────▼─────────────────────────────────┐      │
│  │                        Matrix.org (E2E Encrypted)                     │      │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────────┐ │      │
│  │  │  Messaging  │  │   Media     │  │   VoIP Signaling            │ │      │
│  │  │  (E2E Enc)  │  │  (Primary)  │  │                             │ │      │
│  │  └─────────────┘  └─────────────┘  └─────────────────────────────┘ │      │
│  └─────────────────────────────────────────────────────────────────────┘      │
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐      │
│  │                    PixEdge (Fallback Media Storage)                    │      │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────────┐ │      │
│  │  │  Next.js    │  │   Neon      │  │   Upstash Redis             │ │      │
│  │  │  (API/UI)   │  │  PostgreSQL │  │   (Metadata)                │ │      │
│  │  └─────────────┘  └─────────────┘  └─────────────────────────────┘ │      │
│  │                                    │                                   │      │
│  │                          ┌─────────▼─────────┐                        │      │
│  │                          │   Telegram        │                        │      │
│  │                          │   (Storage)       │                        │      │
│  │                          └───────────────────┘                        │      │
│  └─────────────────────────────────────────────────────────────────────┘      │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## Component Roles

### Krama Android App

The mobile client built with Kotlin and Jetpack Compose.

| Component | Technology | Purpose |
|-----------|------------|---------|
| UI Layer | Jetpack Compose | Modern declarative UI |
| Domain Layer | Kotlin | Business logic |
| Data Layer | Room + SQLCipher | Local encrypted storage |
| Networking | Retrofit + OkHttp | API communication |
| Messaging | Matrix SDK | E2E encrypted messaging |
| Background | WorkManager | Reliable background tasks |

### Firebase Services

Used for authentication and metadata storage (free tier).

```
Firebase Architecture:
┌──────────────────────────────────────────────────────────────┐
│                      Firebase Console                        │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────────┐    ┌─────────────────┐                 │
│  │   Firebase      │    │   Cloud         │                 │
│  │   Auth         │    │   Firestore     │                 │
│  │                 │    │                 │                 │
│  │  • Email/Pass   │    │  • User profiles│                 │
│  │  • Google       │    │  • Conversations│                 │
│  │  • Phone        │    │  • Settings     │                 │
│  │                 │    │  • Contacts     │                 │
│  └────────┬────────┘    └────────┬────────┘                 │
│           │                      │                          │
│           │                      │                          │
│           ▼                      ▼                          │
│  ┌────────────────────────────────────────────┐            │
│  │            Firebase App SDK                  │            │
│  │     (google-services.json)                   │            │
│  └────────────────────────────────────────────┘            │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

| Service | Free Tier | Purpose |
|---------|-----------|---------|
| Authentication | 10K MAU | User identity management |
| Firestore | 1GB storage | Metadata and user profiles |
| Cloud Messaging | Unlimited | Push notifications |

### Matrix.org

Primary messaging and media transport with end-to-end encryption.

```
Matrix Architecture:
┌──────────────────────────────────────────────────────────────┐
│                      Matrix Network                          │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────────┐         ┌─────────────────┐            │
│  │  Krama Client   │◀──────▶│  Matrix Server   │            │
│  │                 │         │  (matrix.org or  │            │
│  │  • E2E Encrypt  │         │   self-hosted)   │            │
│  │  • Message Sync │         │                 │            │
│  │  • Media Upload │         │  • User accounts│            │
│  └─────────────────┘         │  • Room state   │            │
│                              │  • Media repo   │            │
│                              │  • History      │            │
│                              └────────┬────────┘            │
│                                       │                      │
│                                       ▼                      │
│                              ┌─────────────────┐            │
│                              │  Matrix Media   │            │
│                              │  Repository     │            │
│                              │                 │            │
│                              │  • Thumbnails   │            │
│                              │  • Files <10MB  │            │
│                              └─────────────────┘            │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

| Feature | Implementation |
|---------|----------------|
| E2E Encryption | Olm/Megolm (Matrix spec) |
| Key Exchange | Interactive key verification |
| Media Storage | Matrix media repository |
| File Limit | 10MB (recommended) |

### PixEdge

Fallback media storage using Telegram as a free, unlimited backend.

```
PixEdge Architecture:
┌──────────────────────────────────────────────────────────────┐
│                       PixEdge Platform                       │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────────┐    ┌─────────────────┐                 │
│  │   Krama Client  │    │   Web Browser   │                 │
│  │   (API Upload)  │    │   (Dashboard)  │                 │
│  └────────┬────────┘    └────────┬────────┘                 │
│           │                      │                           │
│           └──────────┬───────────┘                           │
│                      │                                       │
│                      ▼                                       │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                  Next.js Application                  │   │
│  │                                                       │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌───────────┐ │   │
│  │  │   /api/v1    │  │   /dashboard │  │  /upload  │ │   │
│  │  │   (REST API) │  │   (User UI)  │  │  (Forms)  │ │   │
│  │  └──────┬───────┘  └──────────────┘  └───────────┘ │   │
│  │         │                                              │   │
│  │  ┌──────▼───────────────────────────────────────┐     │   │
│  │  │              Service Layer                    │     │   │
│  │  │                                               │     │   │
│  │  │  ┌─────────────┐  ┌─────────────────────────┐│     │   │
│  │  │  │ gramjs      │  │ Telegram Bot API        ││     │   │
│  │  │  │ (MTProto)   │  │ (20MB limit)            ││     │   │
│  │  │  │ ≤2GB        │  │                         ││     │   │
│  │  │  └─────────────┘  └─────────────────────────┘│     │   │
│  │  │         │                    │                │     │   │
│  │  └─────────┼────────────────────┼────────────────┘     │   │
│  │            │                    │                     │   │
│  └────────────┼────────────────────┼─────────────────────┘   │
│               │                    │                           │
│               ▼                    ▼                           │
│  ┌────────────────────┐  ┌─────────────────────┐             │
│  │   Telegram API     │  │   Telegram Bot      │             │
│  │   (MTProto)        │  │   (Bot API)         │             │
│  └────────────────────┘  └─────────────────────┘             │
│               │                    │                           │
│               └────────────────────┘                           │
│                            │                                   │
│                            ▼                                   │
│               ┌─────────────────────────┐                     │
│               │   Telegram Storage      │                     │
│               │   Channel (Private)     │                     │
│               │                         │                     │
│               │   • Unlimited storage  │                     │
│               │   • CDN delivery        │                     │
│               │   • 2GB max file size  │                     │
│               └─────────────────────────┘                     │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐     │
│  │                  Data Layer                           │     │
│  │  ┌─────────────────┐  ┌─────────────────┐           │     │
│  │  │  Neon PostgreSQL│  │  Upstash Redis  │           │     │
│  │  │  (Auth/Sessions)│  │  (Media Meta)   │           │     │
│  │  └─────────────────┘  └─────────────────┘           │     │
│  └─────────────────────────────────────────────────────┘     │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

## Data Flow

### Message Flow

```
User A types message                    User B receives message
      │                                        ▲
      │                                        │
      ▼                                        │
┌───────────────────────────────────────────────┐
│ 1. Compose message in Krama                    │
│ 2. Encrypt with Matrix E2E                    │
│ 3. Send via Matrix SDK                        │
└───────────────────────────────────────────────┘
                │
                ▼
┌───────────────────────────────────────────────┐
│ Matrix Server                                 │
│ • Verifies sender identity                    │
│ • Encrypts for recipients                     │
│ • Stores encrypted message                    │
│ • Pushes to recipient devices                │
└───────────────────────────────────────────────┘
                │
                ▼
┌───────────────────────────────────────────────┐
│ Firebase Cloud Messaging                      │
│ • Sends push notification to User B           │
│ • Contains message preview (encrypted)        │
└───────────────────────────────────────────────┘
                │
                ▼
┌───────────────────────────────────────────────┐
│ User B's Krama App                            │
│ • Receives push notification                  │
│ • Fetches message from Matrix                 │
│ • Decrypts with local keys                    │
│ • Displays in UI                              │
└───────────────────────────────────────────────┘
```

### Media Upload Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Media Upload Decision Flow                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  User selects media                                                  │
│         │                                                             │
│         ▼                                                             │
│  ┌─────────────────┐                                                │
│  │ Is file >10MB?  │                                                │
│  └────────┬────────┘                                                │
│           │                                                          │
│     YES   │   NO                                                     │
│     ┌─────┴─────┐                                                   │
│     ▼           ▼                                                    │
│  ┌──────┐   ┌──────────────────────────────────────┐                │
│  │PixEdge│   │ Try Matrix upload                    │                │
│  │Upload │   └────────────────┬───────────────────┘                 │
│  └──┬───┘                    │                                        │
│     │                        ▼                                        │
│     │              ┌─────────────────┐                               │
│     │              │ Upload success? │                               │
│     │              └────────┬────────┘                               │
│     │                   YES │ │ NO                                    │
│     │              ┌────────┘ └────────┐                             │
│     │              ▼                   ▼                             │
│     │         ┌────────┐          ┌─────────┐                        │
│     │         │ Done   │          │PixEdge  │                        │
│     │         └────────┘          │Fallback │                        │
│     │                             └────┬────┘                        │
│     │                                  │                              │
│     └──────────────────────────────────┘                              │
│                           │                                           │
│                           ▼                                           │
│                   ┌───────────────┐                                   │
│                   │ Send message  │                                   │
│                   │ with media URL│                                   │
│                   └───────────────┘                                   │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

### Authentication Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Firebase Authentication Flow                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌───────────┐     ┌───────────┐     ┌───────────┐                  │
│  │   Krama   │     │ Firebase  │     │  Matrix   │                  │
│  │   App     │     │   Auth    │     │   Server  │                  │
│  └─────┬─────┘     └─────┬─────┘     └─────┬─────┘                  │
│        │                  │                  │                       │
│        │  1. Sign in      │                  │                       │
│        │─────────────────▶│                  │                       │
│        │                  │                  │                       │
│        │  2. Verify cred  │                  │                       │
│        │                  │                  │                       │
│        │  3. Create session                 │                       │
│        │◀─────────────────│                  │                       │
│        │                  │                  │                       │
│        │  4. Get Firebase token             │                       │
│        │─────────────────▶│                  │                       │
│        │                  │                  │                       │
│        │  5. Link account │                  │                       │
│        │───────────────────────────────────▶│                       │
│        │                  │                  │                       │
│        │  6. Success       │                  │                       │
│        │◀───────────────────────────────────│                       │
│        │                  │                  │                       │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

## Security Architecture

### End-to-End Encryption

```
┌─────────────────────────────────────────────────────────────────────┐
│                    E2E Encryption Architecture                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────────┐          ┌─────────────────┐                   │
│  │    User A      │          │    User B      │                    │
│  │   Krama App    │          │   Krama App    │                    │
│  │                 │          │                 │                    │
│  │ ┌─────────────┐ │          │ ┌─────────────┐ │                   │
│  │ │ Identity    │ │          │ │ Identity    │ │                   │
│  │ │ Keys        │ │◀───────▶│ │ Keys        │ │                   │
│  │ │ (Ed25519)   │ │  Verify  │ │ (Ed25519)   │ │                   │
│  │ └─────────────┘ │          │ └─────────────┘ │                   │
│  │                 │          │                 │                    │
│  │ ┌─────────────┐ │          │ ┌─────────────┐ │                   │
│  │ │ One-time    │ │◀───────▶│ │ One-time    │ │                   │
│  │ │ Keys (Olm)  │ │  Exchange│ │ Keys (Olm)  │ │                   │
│  │ └─────────────┘ │          │ └─────────────┘ │                   │
│  │                 │          │                 │                    │
│  │ ┌─────────────┐ │          │ ┌─────────────┐ │                   │
│  │ │ Session     │ │◀───────▶│ │ Session     │ │                   │
│  │ │ Keys        │ │  Encrypt │ │ Keys        │ │                   │
│  │ └─────────────┘ │          │ └─────────────┘ │                   │
│  └────────┬────────┘          └────────┬────────┘                   │
│           │                            │                             │
│           │     Encrypted Message      │                             │
│           └────────────────────────────┘                             │
│                                                                      │
│                         Matrix Server                                 │
│                    (Cannot decrypt messages)                         │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Local Storage Security

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Local Storage Security                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Krama Android Device                                                │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │                                                             │    │
│  │  ┌─────────────────┐      ┌─────────────────┐              │    │
│  │  │   Room DB       │      │ SharedPrefs     │              │    │
│  │  │                 │      │                 │              │    │
│  │  │ SQLCipher       │      │ EncryptedPrefs  │              │    │
│  │  │ AES-256        │      │ AES-256         │              │    │
│  │  │                 │      │                 │              │    │
│  │  │ • Messages      │      │ • Settings      │              │    │
│  │  │ • Contacts      │      │ • Auth tokens   │              │    │
│  │  │ • Encryption    │      │ • User prefs    │              │    │
│  │  │   keys          │      │                 │              │    │
│  │  └─────────────────┘      └─────────────────┘              │    │
│  │                                                             │    │
│  │  ┌─────────────────────────────────────────────────────┐   │    │
│  │  │            Android Keystore                          │   │    │
│  │  │                                                       │   │    │
│  │  │  • Master key (hardware-backed)                      │   │    │
│  │  │  • SQLCipher key derivation                         │   │    │
│  │  │  • Secure key storage                               │   │    │
│  │  └─────────────────────────────────────────────────────┘   │    │
│  │                                                             │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

## Backup and Recovery

### PixEdge Data

```
┌─────────────────────────────────────────────────────────────────────┐
│                    PixEdge Backup Architecture                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────┐      ┌─────────────┐      ┌─────────────┐         │
│  │  Telegram   │      │   Neon      │      │  Upstash    │         │
│  │  Channel    │      │  PostgreSQL │      │   Redis     │         │
│  │  (Backup)   │      │  (Backup)   │      │  (Backup)   │         │
│  └──────┬──────┘      └──────┬──────┘      └──────┬──────┘         │
│         │                     │                     │                 │
│         │         Periodic exports via              │                 │
│         │         Neon console / pg_dump            │                 │
│         │                     │                     │                 │
│         │                     ▼                     │                 │
│         │              ┌─────────────┐              │                 │
│         │              │   GitHub    │              │                 │
│         │              │   (Backup)  │              │                 │
│         │              └─────────────┘              │                 │
│         │                     │                     │                 │
│         │         Automated daily backups           │                 │
│         │                                                                      │
│                                                                      │
│  Recovery Procedures:                                                  │
│  1. Telegram: Always available (Telegram's infrastructure)          │
│  2. Neon: Point-in-time recovery from Neon dashboard                │
│  3. Redis: Data can be rebuilt from Telegram + Neon                 │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

## Scalability Considerations

### Current Limits (Free Tier)

| Service | Limit | Usage in Krama |
|---------|-------|----------------|
| Firebase Auth | 10K MAU | ~15 users ✓ |
| Firestore | 50K reads/day | ~15 users ✓ |
| Neon PostgreSQL | 0.5GB | ~1000 users ✓ |
| Upstash Redis | 10K commands/day | ~100 users ✓ |
| Vercel | 100GB bandwidth | ~1000 users ✓ |
| Telegram | Unlimited | Unlimited ✓ |

### Scaling Strategy

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Scaling Path                                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Phase 1: Free Tier (Current)                                       │
│  • 15-50 users                                                      │
│  • All free services                                                │
│  • $0/month                                                         │
│                                                                      │
│  Phase 2: Low-Cost (~$10/month)                                    │
│  • 50-500 users                                                     │
│  • Upgrade Neon to 3GB ($7/month)                                  │
│  • Upgrade Upstash Redis ($3/month)                                 │
│                                                                      │
│  Phase 3: Production (~$50/month)                                   │
│  • 500-5000 users                                                   │
│  • Vercel Pro ($20/month)                                          │
│  • Larger Neon/Redis plans ($30/month)                             │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

## Disaster Recovery

### PixEdge Recovery

1. **Telegram Channel**: Media files are permanently stored in Telegram
2. **Neon PostgreSQL**: Automated daily backups, point-in-time recovery
3. **Upstash Redis**: Data can be rebuilt from Telegram metadata

### Krama Recovery

1. **Firebase**: Automated daily backups via Firebase
2. **Matrix**: History stored on Matrix server
3. **Local Data**: Encrypted SQLite backup on device

### Recovery Time Objectives

| Component | RTO | RPO |
|-----------|-----|-----|
| PixEdge API | 5 min | 0 (Telegram is live) |
| PixEdge Database | 1 hour | 24 hours |
| Firebase Auth | 0 (managed) | 0 (managed) |
| Firestore | 0 (managed) | 24 hours |
| Matrix | Depends on server | Depends on server |

---

## Cost Summary

### Monthly Operating Costs

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Monthly Cost Breakdown                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Krama Backend (Firebase):                                          │
│  ├── Firebase Auth:        $0.00 (free tier)                        │
│  ├── Cloud Firestore:      $0.00 (free tier)                      │
│  ├── Cloud Messaging:      $0.00 (free tier)                       │
│  └── Firebase Analytics:  $0.00 (free tier)                       │
│                                                                      │
│  Matrix.org:                                                        │
│  └── Public Server:    $0.00 (free tier - public server)           │
│                                                                      │
│  PixEdge Backend:                                                   │
│  ├── Vercel Hosting:     $0.00 (free tier)                         │
│  ├── Neon PostgreSQL:     $0.00 (0.5GB free)                       │
│  ├── Upstash Redis:       $0.00 (free tier)                        │
│  └── Telegram Storage:    $0.00 (unlimited)                       │
│                                                                      │
│  ┌─────────────────────────────────────────┐                        │
│  │  TOTAL MONTHLY COST:         $0.00     │                        │
│  └─────────────────────────────────────────┘                        │
│                                                                      │
│  One-time costs:                                                    │
│  ├── Google Play Developer: $25.00 (one-time)                      │
│  └── Domain (optional):      $10-15/year                            │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Dependencies Diagram

```
Krama Android App
├── Firebase
│   ├── com.google.firebase:firebase-auth
│   ├── com.google.firebase:firebase-firestore
│   └── com.google.firebase:firebase-messaging
├── Matrix
│   └── org.matrix.android:matrix-sdk
├── Local Storage
│   ├── androidx.room:room-runtime (SQLite)
│   └── net.zetetic:android-database-sqlcipher
├── Networking
│   ├── com.squareup.okhttp3:okhttp
│   └── com.squareup.retrofit2:retrofit
└── UI
    └── androidx.compose:compose-bom

PixEdge Web App
├── Next.js 15
├── NextAuth.js
│   ├── @auth/core
│   └── @auth/prisma-adapter
├── Database
│   ├── @neondatabase/serverless
│   └── @prisma/client
├── Cache
│   └── @upstash/redis
├── Telegram
│   ├── telegram (Bot API)
│   └── gramjs (MTProto)
└── Utilities
    ├── bcryptjs
    └── jsonwebtoken
```
