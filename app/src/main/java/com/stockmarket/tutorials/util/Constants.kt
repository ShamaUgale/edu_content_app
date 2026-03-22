package com.stockmarket.tutorials.util

/**
 * Constants used throughout the application.
 */
object Constants {
    // Firestore Collections
    const val COLLECTION_USERS = "users"
    const val COLLECTION_VIDEOS = "videos"
    const val COLLECTION_WATCH_PROGRESS = "watchProgress"

    // Session
    const val SESSION_TIMEOUT_MS = 24 * 60 * 60 * 1000L  // 24 hours
    const val SIGNED_URL_EXPIRATION_MINUTES = 30L

    // Video Player
    const val MIN_BUFFER_MS = 5000
    const val MAX_BUFFER_MS = 30000
    const val BUFFER_FOR_PLAYBACK_MS = 2500
    const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 5000

    // Preferences
    const val PREF_SESSION_TOKEN = "session_token"
    const val PREF_SESSION_EXPIRY = "session_expiry"
    const val PREF_USER_EMAIL = "user_email"
    const val PREF_USER_ROLE = "user_role"
    const val PREF_DEVICE_ID = "device_id"
}
