package com.stockmarket.tutorials.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.stockmarket.tutorials.data.model.User
import com.stockmarket.tutorials.data.model.UserRole
import com.stockmarket.tutorials.util.Constants
import com.stockmarket.tutorials.util.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository handling all authentication and user management operations.
 * Uses Firebase Auth for authentication and Firestore for user data.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    val currentFirebaseUser: FirebaseUser?
        get() = auth.currentUser

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    /**
     * Sign in with email and password.
     * Also validates device binding and updates last login timestamp.
     */
    suspend fun signIn(email: String, password: String, deviceId: String): Resource<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: return Resource.Error("Authentication failed")

            // Fetch user profile from Firestore
            val userDoc = firestore.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .get()
                .await()

            if (!userDoc.exists()) {
                auth.signOut()
                return Resource.Error("User profile not found. Contact admin.")
            }

            val user = userDoc.toObject(User::class.java)
                ?: return Resource.Error("Failed to parse user data")

            // Check if user is active
            if (!user.isActive) {
                auth.signOut()
                return Resource.Error("Account has been deactivated. Contact admin.")
            }

            // Device binding check
            if (user.boundDeviceId != null && user.boundDeviceId != deviceId) {
                auth.signOut()
                return Resource.Error("Your account is bound to a different device. Contact admin.")
            }

            // Update last login and bind device
            firestore.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .update(
                    mapOf(
                        "lastLogin" to System.currentTimeMillis(),
                        "boundDeviceId" to deviceId
                    )
                ).await()

            Resource.Success(user.copy(lastLogin = System.currentTimeMillis(), boundDeviceId = deviceId))
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Sign in failed", e)
        }
    }

    /**
     * Sign out the current user.
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Get the current user's profile from Firestore.
     */
    suspend fun getCurrentUserProfile(): Resource<User> {
        return try {
            val uid = auth.currentUser?.uid ?: return Resource.Error("Not authenticated")

            val userDoc = firestore.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .get()
                .await()

            val user = userDoc.toObject(User::class.java)
                ?: return Resource.Error("User profile not found")

            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to fetch user profile", e)
        }
    }

    /**
     * Observe current user profile changes in real-time.
     */
    fun observeCurrentUserProfile(): Flow<Resource<User>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Resource.Error("Not authenticated"))
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(Constants.COLLECTION_USERS)
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Error observing user"))
                    return@addSnapshotListener
                }

                val user = snapshot?.toObject(User::class.java)
                if (user != null) {
                    trySend(Resource.Success(user))
                } else {
                    trySend(Resource.Error("User profile not found"))
                }
            }

        awaitClose { listener.remove() }
    }

    // ========== ADMIN OPERATIONS ==========

    /**
     * Create a new user (Admin only).
     * Creates both Firebase Auth account and Firestore profile.
     */
    suspend fun createUser(
        email: String,
        password: String,
        displayName: String,
        role: UserRole,
        assignedGroups: List<String>
    ): Resource<User> {
        return try {
            // NOTE: In production, this should be done via a Cloud Function
            // to prevent the admin from being signed out.
            // For now, we'll create the auth account and immediately
            // create the Firestore document.

            val currentAdminUser = auth.currentUser
            val adminEmail = currentAdminUser?.email ?: ""
            val adminPassword = "" // Would need to be passed or use Cloud Functions

            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: return Resource.Error("Failed to create user")

            val newUser = User(
                uid = uid,
                email = email,
                displayName = displayName,
                role = role,
                isActive = true,
                assignedGroups = assignedGroups,
                createdAt = System.currentTimeMillis(),
                createdBy = currentAdminUser?.uid ?: "admin"
            )

            firestore.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .set(newUser)
                .await()

            // Sign back in as admin if possible
            // In production, use Cloud Functions to avoid this issue
            if (adminEmail.isNotEmpty()) {
                // Re-auth would be needed here
            }

            Resource.Success(newUser)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create user", e)
        }
    }

    /**
     * Get all users (Admin only).
     */
    fun observeAllUsers(): Flow<Resource<List<User>>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_USERS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Error fetching users"))
                    return@addSnapshotListener
                }

                val users = snapshot?.documents?.mapNotNull {
                    it.toObject(User::class.java)
                } ?: emptyList()

                trySend(Resource.Success(users))
            }

        awaitClose { listener.remove() }
    }

    /**
     * Update a user's profile (Admin only).
     */
    suspend fun updateUser(user: User): Resource<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_USERS)
                .document(user.uid)
                .set(user)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update user", e)
        }
    }

    /**
     * Deactivate a user (Admin only - soft delete).
     */
    suspend fun deactivateUser(uid: String): Resource<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .update("isActive", false)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to deactivate user", e)
        }
    }

    /**
     * Unbind a user's device (Admin only - allows login from new device).
     */
    suspend fun unbindDevice(uid: String): Resource<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .update("boundDeviceId", null)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to unbind device", e)
        }
    }
}
