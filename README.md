# 📈 StockMarket Tutorials — Secure Video Learning Platform

A **security-first Android application** for delivering premium stock market video tutorials with strict access control, anti-piracy protections, and admin-managed content.

> **Built with:** Kotlin • Jetpack Compose • Firebase • ExoPlayer • Hilt

---

## ✨ Features

### 👤 For Users
- Secure login with admin-provided credentials (no public sign-up)
- Browse video tutorials by category (Basics, Intraday, Options, Technical Analysis, etc.)
- Stream videos securely — no downloads, no caching
- Resume playback from where you left off
- Track your watch progress across sessions

### 🛠️ For Admins
- Create and manage user accounts with role-based permissions
- Upload and manage video content with flexible access control
- Assign videos to individual users or groups
- Bind/unbind user devices (enforce 1-user-1-device policy)
- Monitor user activity and watch progress

### 🔒 Security
- **Screenshot & screen recording blocked** (`FLAG_SECURE`)
- **Watermark overlay** on videos (user email + live timestamp)
- **Root & emulator detection** — app won't run on compromised devices
- **Device binding** — each account locked to a single physical device
- **Signed streaming URLs** — time-limited (30 min), validated server-side
- **HTTPS-only** network policy
- **ProGuard/R8 code obfuscation** in release builds
- **Backup disabled** — prevents ADB data extraction
- **Firestore & Storage security rules** with role-based access control

---

## 🏗️ Architecture

```
MVVM + Repository Pattern
┌──────────────────────────────────────────────┐
│  UI Layer (Jetpack Compose)                  │
│  ├── 10 Screens (Login, Dashboard, Player…)  │
│  ├── Navigation (Compose Navigation)         │
│  └── Theme (Material 3, Dark Mode)           │
├──────────────────────────────────────────────┤
│  ViewModel Layer                             │
│  ├── AuthViewModel                           │
│  ├── VideoViewModel                          │
│  └── AdminViewModel                          │
├──────────────────────────────────────────────┤
│  Data Layer                                  │
│  ├── AuthRepository (Firebase Auth + Firestore) │
│  ├── VideoRepository (Firestore + Storage)   │
│  └── Models (User, Video, WatchProgress)     │
├──────────────────────────────────────────────┤
│  Backend (Firebase)                          │
│  ├── Cloud Functions (Node.js)               │
│  ├── Firestore Security Rules                │
│  └── Storage Security Rules                  │
└──────────────────────────────────────────────┘
```

---

## 📋 Prerequisites

Before you begin, make sure you have:

| Tool | Version | Download |
|------|---------|----------|
| **Android Studio** | Hedgehog 2023.1.1+ | [Download](https://developer.android.com/studio) |
| **JDK** | 17 | Bundled with Android Studio |
| **Node.js** | 18+ | [Download](https://nodejs.org/) |
| **Firebase CLI** | Latest | `npm install -g firebase-tools` |
| **Physical Android device** | Android 7.0+ | Required (emulator is blocked by security) |

---

## 🚀 Setup Guide

### Step 1 — Clone the Repository

```bash
git clone https://github.com/ShamaUgale/edu_content_app.git
cd edu_content_app
```

### Step 2 — Create a Firebase Project

1. Go to **[Firebase Console](https://console.firebase.google.com/)** → Click **"Add project"**
2. Name your project (e.g., `stockmarket-tutorials`)
3. Enable **Google Analytics** (optional but recommended)
4. Once created, enable these services:

| Service | How to Enable |
|---------|--------------|
| **Authentication** | Console → Authentication → Get Started → Sign-in method → Enable **Email/Password** |
| **Cloud Firestore** | Console → Firestore Database → Create Database → Choose **Production mode** → Select a region |
| **Cloud Storage** | Console → Storage → Get Started → Choose **Production mode** |
| **Cloud Functions** | Requires **Blaze (pay-as-you-go)** plan. Console → Upgrade → Select Blaze |

### Step 3 — Connect the Android App to Firebase

1. In Firebase Console → **Project Settings** (gear icon) → **General** → scroll to **"Your apps"** → click **Android icon**
2. Register with these details:
   - **Package name:** `com.stockmarket.tutorials`
   - **App nickname:** StockMarket Tutorials  
   - **SHA-1:** (optional for now; needed for production signing)
3. Download the generated **`google-services.json`**
4. Place it in the project:
   ```
   edu_content_app/
   └── app/
       └── google-services.json   ← Place here
   ```

> ⚠️ **Important:** `google-services.json` is gitignored for security. Every developer needs their own copy from Firebase Console.

### Step 4 — Deploy Firebase Backend

```bash
# 1. Log in to Firebase CLI
firebase login

# 2. Navigate to the backend directory
cd backend

# 3. Select your Firebase project
firebase use --add
# → Select your project from the list
# → Give it an alias like "default"

# 4. Install Cloud Functions dependencies
cd functions
npm install
cd ..

# 5. Deploy everything (rules + functions)
firebase deploy
```

This deploys:
- ✅ Firestore security rules (role-based access control)
- ✅ Storage security rules (authenticated read-only)
- ✅ 7 Cloud Functions (user management, signed URLs, session validation)

You should see output like:
```
✔ Deploy complete!
✔ functions: Deployed 7 functions
✔ firestore: Released security rules
✔ storage: Released security rules
```

### Step 5 — Create the First Admin User

Since there's no public registration (by design), you must bootstrap the first admin **manually**:

**A. Create the Auth account:**
1. Firebase Console → **Authentication** → **Users** tab → **Add user**
2. Enter:
   - Email: `admin@yourcompany.com`
   - Password: `YourStrongPassword123!` (min 8 chars)
3. Click **Add user** — note the **User UID** shown in the table

**B. Create the Firestore user profile:**
1. Firebase Console → **Firestore Database** → **+ Start collection**
2. Collection ID: `users`
3. Document ID: paste the **User UID** from step A
4. Add these fields:

| Field | Type | Value |
|-------|------|-------|
| `uid` | string | *(paste the UID)* |
| `email` | string | `admin@yourcompany.com` |
| `displayName` | string | `Admin` |
| `role` | string | `ADMIN` |
| `isActive` | boolean | `true` |
| `boundDeviceId` | null | *(leave null)* |
| `assignedVideoIds` | array | `[]` *(empty)* |
| `assignedGroups` | array | `["admin"]` |
| `lastLogin` | number | `0` |
| `createdBy` | string | `system` |

5. Click **Save**

### Step 6 — Build & Run the App

1. Open the project in **Android Studio**
2. Wait for Gradle sync to complete (may take a few minutes on first load)
3. Connect a **physical Android device** via USB (enable USB debugging)
4. Click **▶ Run** or press `Shift+F10`

> 🚫 **Don't use an emulator** — the app detects and blocks emulators as a security measure.
>
> **For development only:** To temporarily allow emulators, comment out the emulator check in `MainActivity.kt`:
> ```kotlin
> // if (SecurityUtils.isEmulator()) {
> //     isSecurityBlocked = true
> //     return@setContent
> // }
> ```

### Step 7 — Upload Your First Video

1. Go to **Firebase Console** → **Storage** → click into the `videos/` folder (create it if needed)
2. Click **Upload file** → select your video (MP4 recommended, max 2GB)
3. Note the file path, e.g., `videos/intro_to_stocks.mp4`
4. Open the app → log in as admin → **Admin Panel** → **Manage Videos** → **+ Add Video**
5. Fill in:
   - **Title:** Introduction to Stocks
   - **Firebase Storage Path:** `videos/intro_to_stocks.mp4`
   - **Category:** Basics
   - **Access Type:** Assigned (or Public for everyone)
   - **Assigned Groups:** `premium` (or leave empty for Public)
6. Tap **Add Video** ✅

### Step 8 — Create a Regular User

1. In the app (logged in as admin) → **Admin Panel** → **Manage Users** → **+ Add User**
2. Fill in:
   - **Display Name:** John Doe
   - **Email:** john@example.com
   - **Password:** (min 8 characters)
   - **Role:** USER
   - **Groups:** `premium` (matches video assignment)
3. Tap **Create User**
4. Share the credentials securely with the user

---

## 📁 Project Structure

```
edu_content_app/
├── app/                                    # Android App
│   ├── build.gradle.kts                    # Dependencies & config
│   ├── proguard-rules.pro                  # Code obfuscation rules
│   └── src/main/
│       ├── AndroidManifest.xml             # App manifest
│       ├── res/                            # Resources (themes, strings, colors)
│       └── java/com/stockmarket/tutorials/
│           ├── MainActivity.kt             # Entry point + security checks
│           ├── StockMarketApp.kt           # Hilt application class
│           ├── data/
│           │   ├── model/                  # Data models (User, Video, WatchProgress)
│           │   └── repository/             # Firebase repositories (Auth, Video)
│           ├── di/                         # Hilt dependency injection
│           ├── ui/
│           │   ├── navigation/             # Navigation routes & graph
│           │   ├── screens/                # 10 Compose screens
│           │   ├── theme/                  # Material 3 dark theme
│           │   └── viewmodel/              # MVVM ViewModels
│           └── util/                       # Security utils, constants
│
├── backend/                                # Firebase Backend
│   ├── firebase.json                       # Firebase project config
│   ├── firestore.rules                     # Firestore security rules
│   ├── storage.rules                       # Storage security rules
│   └── functions/
│       ├── package.json
│       └── index.js                        # Cloud Functions (7 functions)
│
├── SETUP.md                                # Detailed setup guide
├── SECURITY.md                             # Security architecture docs
└── README.md                               # ← You are here
```

---

## 📱 Screens

| Screen | Description |
|--------|-------------|
| **Login** | Secure email/password login with error handling |
| **Security Block** | Blocks rooted devices and emulators |
| **Dashboard** | Stats banner, category chips, video cards with progress |
| **Video List** | Category-filtered video listing |
| **Video Player** | ExoPlayer streaming + watermark + resume playback |
| **Admin Panel** | Admin dashboard with user/video stats |
| **Admin Users** | User list with deactivate & device unbind actions |
| **Admin Add User** | Form to create new user with role & group assignment |
| **Admin Videos** | Video list with access badges & delete |
| **Admin Add Video** | Form to register new video with access control |

---

## 🔧 Configuration

| Setting | File | Default |
|---------|------|---------|
| Session timeout | `util/Constants.kt` | 24 hours |
| Signed URL expiry | `backend/functions/index.js` | 30 minutes |
| Video categories | `data/model/Video.kt` | 8 categories |
| Video buffer size | `util/Constants.kt` | 5MB / 15MB |
| App theme | `ui/theme/` | Dark mode only |

---

## 🔐 Security Deep Dive

For a complete breakdown of all security measures, threat analysis, and production hardening recommendations, see **[SECURITY.md](SECURITY.md)**.

---

## 📄 License

This project is proprietary software. All rights reserved.

---

## 🤝 Contributing

This is a private project. Contact the repository owner for access or collaboration requests.
