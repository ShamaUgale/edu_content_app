package com.stockmarket.tutorials.data.model

/**
 * Represents a video tutorial in the system.
 * Stored in Firestore under "videos" collection.
 */
data class Video(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: VideoCategory = VideoCategory.BASICS,
    val thumbnailUrl: String = "",
    val videoStoragePath: String = "",  // Firebase Storage path
    val durationSeconds: Long = 0L,
    val uploadedAt: Long = System.currentTimeMillis(),
    val uploadedBy: String = "",
    val isActive: Boolean = true,
    val accessType: AccessType = AccessType.ASSIGNED,  // How access is controlled
    val assignedUserIds: List<String> = emptyList(),
    val assignedGroups: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val orderIndex: Int = 0
) {
    // No-arg constructor for Firestore deserialization
    constructor() : this(id = "")
}

enum class VideoCategory(val displayName: String) {
    BASICS("Basics"),
    INTRADAY("Intraday Trading"),
    OPTIONS("Options Trading"),
    STRATEGY("Strategy"),
    TECHNICAL_ANALYSIS("Technical Analysis"),
    FUNDAMENTAL_ANALYSIS("Fundamental Analysis"),
    RISK_MANAGEMENT("Risk Management"),
    ADVANCED("Advanced");

    companion object {
        fun fromString(value: String): VideoCategory {
            return entries.find { it.name.equals(value, ignoreCase = true) }
                ?: BASICS
        }

        fun all(): List<VideoCategory> = entries.toList()
    }
}

enum class AccessType {
    PUBLIC,     // All users can access
    ASSIGNED,   // Only assigned users/groups
    ADMIN_ONLY; // Only admins

    companion object {
        fun fromString(value: String): AccessType {
            return entries.find { it.name.equals(value, ignoreCase = true) }
                ?: ASSIGNED
        }
    }
}
