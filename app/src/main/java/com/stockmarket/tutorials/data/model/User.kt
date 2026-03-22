package com.stockmarket.tutorials.data.model

/**
 * Represents a user in the system.
 * Stored in Firestore under "users" collection.
 */
data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: UserRole = UserRole.USER,
    val isActive: Boolean = true,
    val boundDeviceId: String? = null,
    val assignedVideoIds: List<String> = emptyList(),
    val assignedGroups: List<String> = emptyList(),
    val lastLogin: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = ""
) {
    // No-arg constructor for Firestore deserialization
    constructor() : this(uid = "")
}

enum class UserRole {
    ADMIN,
    USER;

    companion object {
        fun fromString(value: String): UserRole {
            return when (value.uppercase()) {
                "ADMIN" -> ADMIN
                else -> USER
            }
        }
    }
}
