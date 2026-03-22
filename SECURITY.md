# Security Considerations - StockMarket Tutorials

## 🔒 Security Architecture Overview

This document outlines the security measures implemented in the StockMarket Tutorials application and recommendations for further hardening.

---

## 1. Anti-Piracy Protections

### 1.1 FLAG_SECURE - Screenshot & Screen Recording Prevention
**Implementation**: `MainActivity.kt`

```kotlin
window.setFlags(
    WindowManager.LayoutParams.FLAG_SECURE,
    WindowManager.LayoutParams.FLAG_SECURE
)
```

- Applied in `onCreate()` and re-applied in `onResume()`
- Prevents:
  - Screenshots via system screenshot
  - Screen recording via system recorder
  - Screen sharing / casting
  - Third-party recording apps (most)
- **Limitation**: Some root-level recording tools may bypass this on rooted devices (hence root detection)

### 1.2 Watermark Overlay
**Implementation**: `VideoPlayerScreen.kt` → `WatermarkOverlay()`

- Semi-transparent watermark displayed over video content
- Contains:
  - User's email address
  - User's display name
  - Real-time timestamp (updates every second)
- Rotated diagonally to make removal difficult
- Multiple watermark positions (center + diagonal pattern)
- **Purpose**: Discourages screen-to-screen recording (filming the screen with another device)

### 1.3 No Video Download
**Implementation**: `VideoPlayerScreen.kt` → `SecureVideoPlayer()`

- ExoPlayer configured for streaming only
- `DefaultHttpDataSource` used without disk cache
- No `CacheDataSource` configured
- Video URLs are time-limited signed URLs (30-minute expiration)

---

## 2. Device Security

### 2.1 Root Detection
**Implementation**: `SecurityUtils.kt` → `isDeviceRooted()`

Multiple heuristic checks:
- **Binary check**: Scans for `su` binary in known paths
- **SU execution check**: Attempts to detect `su` via `which`
- **Root app detection**: Checks for known root management apps (Magisk, SuperSU, etc.)
- **System property check**: Detects `ro.debuggable=1` or `ro.secure=0`
- **Filesystem check**: Detects writable `/system` partition

**Recommendation for production**:
- Consider using [SafetyNet Attestation API](https://developer.android.com/training/safetynet/attestation) or [Play Integrity API](https://developer.android.com/google/play/integrity) for more robust device attestation
- Add certificate pinning for API calls
- Consider commercial solutions like Promon SHIELD or DexGuard for enterprise-grade protection

### 2.2 Emulator Detection
**Implementation**: `SecurityUtils.kt` → `isEmulator()`

Checks `Build` properties:
- `FINGERPRINT`, `MODEL`, `MANUFACTURER`, `BRAND`, `DEVICE`
- `PRODUCT` (sdk variants)
- `HARDWARE` (goldfish, ranchu)
- `HOST`, `BOARD`

### 2.3 Device Binding (1 User = 1 Device)
**Implementation**: `AuthRepository.kt` → `signIn()`

- On first login, device fingerprint is stored with user profile
- Subsequent logins from different devices are rejected
- Admin can unbind a device to allow re-binding
- Device fingerprint uses `ANDROID_ID` + hardware identifiers

---

## 3. Authentication & Access Control

### 3.1 No Public Signup
- Registration is completely disabled for end users
- Only admins can create user accounts
- Firebase Auth + Firestore used together for role validation

### 3.2 Role-Based Access Control (RBAC)
| Role | Permissions |
|------|-----------|
| ADMIN | Full access: user mgmt, video mgmt, all content |
| USER | View assigned videos only, track own progress |

### 3.3 Session Management
- Sessions expire after 24 hours (configurable)
- Last login timestamp tracked in Firestore
- Cloud Function `validateSession` can be called to verify session
- `revokeRefreshTokens()` used when deactivating users

### 3.4 Video Access Control
Three access levels per video:
- **PUBLIC**: All authenticated users
- **ASSIGNED**: Only users/groups explicitly assigned
- **ADMIN_ONLY**: Only admin users

Access is enforced at:
1. **Client-side**: Repository filters videos by access rules
2. **Server-side**: Cloud Function `getSignedVideoUrl` validates access before generating URL
3. **Database-level**: Firestore rules restrict read/write operations

---

## 4. Network Security

### 4.1 HTTPS Enforcement
**Implementation**: `network_security_config.xml`

```xml
<base-config cleartextTrafficPermitted="false">
```

- All cleartext (HTTP) traffic is blocked
- Only HTTPS connections allowed

### 4.2 Signed URLs
**Implementation**: Cloud Function `getSignedVideoUrl`

- Video streaming URLs are generated server-side
- Each URL has a 30-minute expiration
- Access is validated before URL generation
- URLs cannot be reused after expiration

**Recommendation**: Implement URL signing with user-specific tokens for additional traceability

### 4.3 Firebase Security Rules
- **Firestore**: Role-based rules with admin verification
- **Storage**: Read-only for authenticated users, no direct writes
- **Default deny-all** for unmatched paths

---

## 5. Code Protection

### 5.1 ProGuard / R8 Obfuscation
**Implementation**: `proguard-rules.pro`

- Code shrinking enabled in release builds
- Aggressive repackaging (`-repackageclasses 'a'`)
- Class hierarchy flattening
- Method overloading
- 5 optimization passes
- Debug log stripping in release builds

### 5.2 Backup Prevention
**Implementation**: `AndroidManifest.xml` + `data_extraction_rules.xml`

```xml
android:allowBackup="false"
android:fullBackupContent="false"
```

- Prevents data extraction via ADB backup
- Excludes all domains from cloud backup and device transfer
- Prevents credential/data leakage through backup exploits

---

## 6. Security Threat Matrix

| Threat | Mitigation | Strength |
|--------|-----------|----------|
| Screenshot capture | FLAG_SECURE | ⭐⭐⭐⭐ |
| Screen recording | FLAG_SECURE | ⭐⭐⭐⭐ |
| Video download | Streaming-only + signed URLs | ⭐⭐⭐⭐ |
| URL sharing | Time-limited signed URLs | ⭐⭐⭐⭐⭐ |
| Credential sharing | Device binding | ⭐⭐⭐⭐ |
| Rooted device bypass | Root detection | ⭐⭐⭐ |
| Emulator piracy | Emulator detection | ⭐⭐⭐ |
| Reverse engineering | ProGuard/R8 obfuscation | ⭐⭐⭐ |
| Camera recording screen | Watermark overlay | ⭐⭐⭐ |
| ADB data extraction | Backup disabled | ⭐⭐⭐⭐ |
| Unauthorized access | Firebase rules + RBAC | ⭐⭐⭐⭐⭐ |
| Session hijacking | Session expiration + token revocation | ⭐⭐⭐⭐ |
| Network interception | HTTPS-only + signed URLs | ⭐⭐⭐⭐ |

---

## 7. Recommendations for Production Hardening

### High Priority
1. **Play Integrity API**: Replace basic root/emulator detection with Google's Play Integrity API for more reliable device attestation
2. **Certificate Pinning**: Implement SSL certificate pinning to prevent MITM attacks
3. **DRM (Widevine)**: Implement Google Widevine DRM for hardware-level content protection
4. **Server-side signed URLs**: Always use Cloud Functions for URL generation (never generate client-side)

### Medium Priority
5. **Rate Limiting**: Add rate limiting to Cloud Functions to prevent abuse
6. **Firebase App Check**: Enable App Check to verify authentic app instances
7. **Custom Claims**: Use Firebase Auth custom claims instead of Firestore lookups for role checking
8. **Audit Logging**: Implement comprehensive audit trails for all admin operations

### Low Priority (but Nice to Have)
9. **Tamper Detection**: Detect if the APK has been modified (signature verification)
10. **Debugging Detection**: Detect if a debugger is attached
11. **Frida/Xposed Detection**: Detect common hooking frameworks
12. **Geofencing**: Restrict app usage to specific geographic regions
13. **Biometric Lock**: Add fingerprint/face unlock for app access

---

## 8. Compliance Notes

- **Data Privacy**: User data stored in Firestore. Ensure compliance with local data protection laws (GDPR, etc.)
- **Content Rights**: Ensure proper licensing for all video content
- **Terms of Service**: Users should agree to anti-piracy terms before accessing content
- **Data Retention**: Implement data retention policies and user data deletion mechanisms

---

## 9. Incident Response

If content leakage is suspected:
1. **Identify source**: Watermark on leaked content reveals the user's email and timestamp
2. **Deactivate user**: Admin → Manage Users → Deactivate
3. **Revoke access**: Cloud Function `deactivateUser` also revokes refresh tokens
4. **Audit trail**: Check Firestore for last login, device info, and watch activity
5. **Report**: Document the incident for legal/compliance purposes
