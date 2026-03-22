# StockMarket Tutorials - Secure Android Application

A **security-focused Android application** for delivering stock market video tutorials with strict access control, anti-piracy protections, and role-based content management.

---

## 📁 Project Structure

```
stock_market_content_app/
├── app/
│   ├── build.gradle.kts              # App dependencies & config
│   ├── proguard-rules.pro            # R8/ProGuard obfuscation rules
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/
│       │   ├── values/
│       │   │   ├── strings.xml
│       │   │   ├── colors.xml
│       │   │   └── themes.xml
│       │   └── xml/
│       │       ├── network_security_config.xml
│       │       └── data_extraction_rules.xml
│       └── java/com/stockmarket/tutorials/
│           ├── StockMarketApp.kt         # @HiltAndroidApp
│           ├── MainActivity.kt           # FLAG_SECURE, root/emulator check
│           ├── data/
│           │   ├── model/
│           │   │   ├── User.kt           # User model with roles
│           │   │   ├── Video.kt          # Video model with categories
│           │   │   └── WatchProgress.kt  # Watch progress tracking
│           │   └── repository/
│           │       ├── AuthRepository.kt  # Firebase Auth + device binding
│           │       └── VideoRepository.kt # Video access + streaming
│           ├── di/
│           │   └── AppModule.kt          # Hilt DI module
│           ├── ui/
│           │   ├── navigation/
│           │   │   ├── Screen.kt         # Route definitions
│           │   │   └── AppNavigation.kt  # NavHost with auth routing
│           │   ├── screens/
│           │   │   ├── LoginScreen.kt        # Secure login
│           │   │   ├── SecurityBlockScreen.kt # Root/emulator block
│           │   │   ├── DashboardScreen.kt    # Main dashboard
│           │   │   ├── VideoListScreen.kt    # Category video list
│           │   │   ├── VideoPlayerScreen.kt  # ExoPlayer + watermark
│           │   │   ├── AdminPanelScreen.kt   # Admin dashboard
│           │   │   ├── AdminUsersScreen.kt   # User management
│           │   │   ├── AdminAddUserScreen.kt # Add user form
│           │   │   ├── AdminVideosScreen.kt  # Video management
│           │   │   └── AdminAddVideoScreen.kt # Add video form
│           │   ├── viewmodel/
│           │   │   ├── AuthViewModel.kt      # Auth state management
│           │   │   ├── VideoViewModel.kt     # Video + progress state
│           │   │   └── AdminViewModel.kt     # Admin operations
│           │   └── theme/
│           │       ├── Color.kt              # Finance-themed palette
│           │       ├── Theme.kt              # Dark Material3 theme
│           │       └── Type.kt               # Typography
│           └── util/
│               ├── SecurityUtils.kt          # Root/emulator/recording detection
│               ├── Resource.kt               # Result wrapper
│               └── Constants.kt              # App constants
├── backend/
│   ├── firebase.json                  # Firebase project config
│   ├── firestore.rules                # Firestore security rules
│   ├── storage.rules                  # Storage security rules
│   ├── firestore.indexes.json
│   └── functions/
│       ├── package.json
│       └── index.js                   # Cloud Functions (user mgmt, signed URLs)
├── build.gradle.kts                   # Root build file
├── settings.gradle.kts
├── gradle.properties
├── SETUP.md                           # ← This file
└── SECURITY.md                        # Security documentation
```

---

## ⚙️ Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| Architecture | MVVM (ViewModel + Repository) |
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| DI | Hilt (Dagger) |
| Auth | Firebase Authentication |
| Database | Cloud Firestore |
| Storage | Firebase Storage |
| Video Player | ExoPlayer (Media3) |
| Obfuscation | ProGuard / R8 |

---

## 🚀 Setup Instructions

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Firebase CLI (`npm install -g firebase-tools`)
- Node.js 18+
- A Firebase project

### Step 1: Firebase Project Setup

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project (or use existing)
3. Enable the following services:
   - **Authentication** → Enable Email/Password sign-in
   - **Cloud Firestore** → Create database in **production mode**
   - **Cloud Storage** → Initialize storage
   - **Cloud Functions** → Upgrade to Blaze plan (required for Cloud Functions)

### Step 2: Connect Android App to Firebase

1. In Firebase Console → **Project Settings** → **Add app** → **Android**
2. Enter package name: `com.stockmarket.tutorials`
3. Download `google-services.json`
4. Place it in: `app/google-services.json`

### Step 3: Deploy Firebase Rules & Functions

```bash
# Navigate to backend directory
cd backend

# Login to Firebase
firebase login

# Initialize Firebase (select your project)
firebase use --add

# Install function dependencies
cd functions
npm install
cd ..

# Deploy everything
firebase deploy
```

This deploys:
- Firestore security rules
- Storage security rules
- Cloud Functions (user creation, signed URLs, etc.)

### Step 4: Create Initial Admin User

Since no self-registration is allowed, you need to create the first admin manually:

```bash
# Using Firebase CLI or Firebase Console:

# 1. Create a user in Firebase Auth:
#    Go to Firebase Console → Authentication → Add User
#    Enter: admin@yourdomain.com / YourStrongPassword123!

# 2. Get the UID from the Auth console

# 3. Create the admin's Firestore document:
#    Go to Firestore → Create Collection "users"
#    Document ID = {the UID from step 2}
#    Fields:
```

```json
{
  "uid": "<UID_FROM_AUTH>",
  "email": "admin@yourdomain.com",
  "displayName": "Admin",
  "role": "ADMIN",
  "isActive": true,
  "boundDeviceId": null,
  "assignedVideoIds": [],
  "assignedGroups": ["admin"],
  "lastLogin": 0,
  "createdAt": <SERVER_TIMESTAMP>,
  "createdBy": "system"
}
```

### Step 5: Build & Run the App

```bash
# Open the project in Android Studio
# Sync Gradle files
# Build and run on a physical device (emulator will be blocked!)

# For release build:
./gradlew assembleRelease
```

> ⚠️ **Note**: The app blocks emulators by design. Use a physical Android device for testing, or temporarily disable the emulator check in `MainActivity.kt` during development.

### Step 6: Upload Videos

1. Upload video files to **Firebase Storage** under the `videos/` path
2. Note the storage path (e.g., `videos/options_trading_basics.mp4`)
3. In the app, log in as admin → Admin Panel → Manage Videos → Add Video
4. Enter the storage path and assign access permissions

---

## 📱 App Features

### User Features
- 🔐 Secure login with admin-provided credentials
- 📊 Dashboard with categories and assigned videos
- 🎬 Secure video streaming with ExoPlayer
- ▶️ Resume playback from last position
- 📈 Watch progress tracking

### Admin Features
- 👥 Full user management (create, edit, deactivate)
- 📹 Video management (add, edit, delete, assign)
- 🔗 Device binding management (unbind users)
- 📊 User activity tracking (via Cloud Functions)

### Security Features
- 🛡️ FLAG_SECURE on all screens (blocks screenshots/recording)
- 🔍 Root detection (multi-heuristic)
- 🖥️ Emulator detection
- 🔒 Device binding (1 user = 1 device)
- ⏱️ Session expiration (24 hours)
- 🏷️ Watermark overlay (username + timestamp)
- 🔐 HTTPS-only enforcement
- 📦 ProGuard/R8 code obfuscation
- 🗄️ No data backup allowed
- 🌐 Signed URLs with expiration for video streaming

---

## 🔧 Configuration

### Session Timeout
Edit `Constants.kt`:
```kotlin
const val SESSION_TIMEOUT_MS = 24 * 60 * 60 * 1000L  // 24 hours
```

### Signed URL Expiration
Edit Cloud Functions `index.js`:
```javascript
expires: Date.now() + 30 * 60 * 1000, // 30 minutes
```

### Video Categories
Edit `Video.kt` → `VideoCategory` enum to add/remove categories.

---

## 📋 Development Notes

### Disabling Security Checks for Development

During development, you may want to temporarily disable:

1. **Emulator detection**: Comment out the emulator check in `MainActivity.kt`
2. **FLAG_SECURE**: Comment out `window.setFlags(...)` to enable screenshots
3. **Root detection**: Comment out root check in `MainActivity.kt`

> ⚠️ **NEVER deploy to production with security checks disabled!**

### Testing Admin Operations

For testing user creation without Cloud Functions:
- The app includes client-side user creation in `AuthRepository.kt`
- For production, always use Cloud Functions (prevents admin sign-out issue)
