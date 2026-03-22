package com.stockmarket.tutorials.data.model

/**
 * Tracks user's watch progress for a video.
 * Stored in Firestore under "users/{uid}/watchProgress" subcollection.
 */
data class WatchProgress(
    val videoId: String = "",
    val userId: String = "",
    val lastPositionMs: Long = 0L,
    val totalWatchedMs: Long = 0L,
    val completedPercentage: Float = 0f,
    val isCompleted: Boolean = false,
    val lastWatchedAt: Long = System.currentTimeMillis()
) {
    constructor() : this(videoId = "")
}

/**
 * Tracks user activity for admin monitoring.
 */
data class UserActivity(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val lastLogin: Long = 0L,
    val totalVideosWatched: Int = 0,
    val totalWatchTimeMs: Long = 0L,
    val deviceId: String? = null,
    val isActive: Boolean = true
) {
    constructor() : this(uid = "")
}
