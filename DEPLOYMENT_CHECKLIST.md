# Deployment Checklist

Pre-deployment, post-deployment, and verification checklists for Krama and PixEdge projects.

---

## Pre-Deployment Checklist

### PixEdge Deployment

- [ ] **Telegram Bot Created**
  - [ ] Bot token obtained from @BotFather
  - [ ] Bot username configured
  - [ ] Bot added to storage channel as admin

- [ ] **Storage Channel**
  - [ ] Private channel created
  - [ ] Bot added as admin with posting permissions
  - [ ] Channel ID obtained (format: `-1001234567890`)

- [ ] **Neon PostgreSQL**
  - [ ] Project created at console.neon.tech
  - [ ] Database schema initialized
  - [ ] Connection string tested

- [ ] **Upstash Redis**
  - [ ] Database created at console.upstash.com
  - [ ] REST URL and Token copied
  - [ ] Connection verified

- [ ] **Environment Variables Configured in Vercel**
  - [ ] `NEXT_PUBLIC_BASE_URL` set
  - [ ] `NEXTAUTH_SECRET` set (32+ random chars)
  - [ ] `NEXTAUTH_URL` set
  - [ ] `DATABASE_URL` set
  - [ ] `TELEGRAM_BOT_TOKEN` set
  - [ ] `TELEGRAM_CHAT_ID` set
  - [ ] `UPSTASH_REDIS_REST_URL` set
  - [ ] `UPSTASH_REDIS_REST_TOKEN` set

- [ ] **MTProto Configuration (Optional)**
  - [ ] API credentials obtained from my.telegram.org
  - [ ] Session string generated
  - [ ] `TELEGRAM_API_ID` set
  - [ ] `TELEGRAM_API_HASH` set
  - [ ] `TELEGRAM_SESSION_STRING` set

- [ ] **OAuth Providers (Optional)**
  - [ ] Google OAuth Client ID/Secret configured
  - [ ] GitHub OAuth Client ID/Secret configured
  - [ ] Callback URLs verified

### Krama Deployment

- [ ] **Firebase Project**
  - [ ] Project created: `karmaapp-2bab2`
  - [ ] `google-services.json` downloaded
  - [ ] Package name verified: `com.aistudio.krama.messenger`

- [ ] **Firebase Authentication**
  - [ ] Email/Password enabled
  - [ ] Google Sign-in enabled
  - [ ] Phone Sign-in enabled (if needed)

- [ ] **Cloud Firestore**
  - [ ] Database created in production mode
  - [ ] Security rules deployed from `firestore.rules`
  - [ ] Composite indexes created

- [ ] **Cloud Messaging**
  - [ ] FCM configured
  - [ ] VAPID key set (if needed)

- [ ] **Build Configuration**
  - [ ] `google-services.json` in `app/` directory
  - [ ] `.env` file configured with PixEdge settings (if using)
  - [ ] Release keystore configured (for release builds)

---

## Post-Deployment Checklist

### PixEdge Verification

- [ ] **Basic Functionality**
  - [ ] Homepage loads at deployment URL
  - [ ] User registration works
  - [ ] Email/password login works
  - [ ] OAuth login works (if configured)

- [ ] **Media Upload**
  - [ ] Small file upload works (<20MB)
  - [ ] File appears in Telegram channel
  - [ ] Upload returns valid URL
  - [ ] URL is accessible and serves file

- [ ] **Dashboard**
  - [ ] List uploads shows uploaded files
  - [ ] Delete functionality works
  - [ ] API key generation works

- [ ] **Telegram Bot**
  - [ ] `/start` command works
  - [ ] `/help` command works
  - [ ] Upload via bot works
  - [ ] Account linking works

- [ ] **MTProto (if configured)**
  - [ ] Large file upload works (>20MB)
  - [ ] 2GB upload succeeds
  - [ ] Streaming works for large files

### Krama Verification

- [ ] **Authentication**
  - [ ] Email/password sign-in works
  - [ ] Google sign-in works
  - [ ] Phone sign-in works
  - [ ] Auth state persists correctly

- [ ] **Messaging**
  - [ ] Can create conversations
  - [ ] Messages send successfully
  - [ ] E2E encryption is working
  - [ ] Message history loads

- [ ] **Media Sharing**
  - [ ] Small image upload works
  - [ ] Small video upload works
  - [ ] Large file fallback to PixEdge works
  - [ ] Media downloads correctly

- [ ] **Push Notifications**
  - [ ] Notifications arrive when app is in background
  - [ ] Tap notification opens correct chat
  - [ ] Notification content is appropriate

---

## Verification Procedures

### Test Matrix

| Test | Expected Result | Pass/Fail |
|------|----------------|-----------|
| PixEdge: Upload 5MB image | Returns URL, appears in Telegram | |
| PixEdge: Upload 100MB video | Uses MTProto, returns URL | |
| PixEdge: Bot `/start` | Welcome message displayed | |
| Krama: Send text message | Message appears in Matrix | |
| Krama: Send 5MB image | Uploaded via Matrix | |
| Krama: Send 15MB video | Fallback to PixEdge | |
| Krama: Receive push notification | Notification shows message | |

### Smoke Tests

#### PixEdge Smoke Test

```bash
# 1. Register new account
curl -X POST https://your-domain.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"testpass123"}'

# 2. Login and get session
curl -X POST https://your-domain.com/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"testpass123"}'

# 3. Upload test image
curl -X POST https://your-domain.com/api/v1/upload \
  -F "file=@test.png" \
  -H "Cookie: next-auth.session-token=..."

# 4. Verify upload
curl https://your-domain.com/api/v1/list \
  -H "Cookie: next-auth.session-token=..."
```

#### Krama Smoke Test

1. Install debug APK on Android device
2. Sign in with test account
3. Send text message to test contact
4. Share a small image (<5MB)
5. Share a large image (>10MB)
6. Verify large image uses PixEdge
7. Check push notification arrives

---

## Rollback Procedures

### PixEdge Rollback

1. **Revert Vercel Deployment**
   ```
   Vercel Dashboard → Deployments → Select previous → "..." → "Promote to Production"
   ```

2. **Restore Database (if needed)**
   ```
   Neon Dashboard → Backups → Select backup → Restore
   ```

### Krama Rollback

1. **Revert GitHub Actions Build**
   - No action needed (APK remains in artifact)
   - Install previous APK from artifacts

2. **Restore Firebase (if needed)**
   ```
   Firebase Console → Firestore → Backups → Restore
   ```

---

## Monitoring

### Key Metrics to Watch

| Service | Metric | Warning Threshold |
|---------|--------|------------------|
| Vercel | Bandwidth usage | >80% of free tier |
| Neon | Storage usage | >400MB |
| Upstash | Commands/day | >8K/day |
| Firebase | Auth users | >8K MAU |
| Firebase | Firestore reads | >40K/day |

### Uptime Checks

Set up monitoring for:
- [ ] PixEdge API health: `https://your-domain.com/api/health`
- [ ] PixEdge homepage
- [ ] Telegram bot responsiveness

---

## Security Checklist

- [ ] No secrets in git history
- [ ] `.env` files not committed
- [ ] `google-services.json` not committed
- [ ] MTProto session string not committed
- [ ] API keys rotated after deployment
- [ ] Telegram bot token is secure
- [ ] Database password is strong

---

## Cost Verification

### Expected Costs: $0/month

Verify no unexpected charges:

1. **Vercel**: Free tier active (no overages)
2. **Neon**: Within 0.5GB limit
3. **Upstash**: Within free tier
4. **Firebase**: Within Spark plan limits
5. **Telegram**: Free (no paid features used)

---

## Documentation Updates

After deployment, update:

- [ ] README.md with actual deployment URLs
- [ ] API_INTEGRATION.md if endpoints changed
- [ ] ARCHITECTURE.md if architecture changed
- [ ] Project docs with any configuration changes

---

## Emergency Contacts

| Service | Support | Link |
|---------|---------|------|
| Vercel | Support team | vercel.com/support |
| Neon | Documentation | console.neon.tech/docs |
| Upstash | Discord | upstash.com/discord |
| Firebase | Support page | firebase.google.com/support |
| Telegram | Bot Father | @BotFather |

---

## Sign-off

| Role | Name | Date | Signature |
|------|------|------|-----------|
| Deployer | | | |
| Reviewer | | | |
| Project Lead | | | |
