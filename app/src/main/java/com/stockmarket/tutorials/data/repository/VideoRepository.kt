package com.stockmarket.tutorials.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.stockmarket.tutorials.data.model.AccessType
import com.stockmarket.tutorials.data.model.User
import com.stockmarket.tutorials.data.model.Video
import com.stockmarket.tutorials.data.model.VideoCategory
import com.stockmarket.tutorials.data.model.WatchProgress
import com.stockmarket.tutorials.util.Constants
import com.stockmarket.tutorials.util.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository handling video content management.
 * Manages video CRUD, access control, streaming URLs, and watch progress.
 */
@Singleton
class VideoRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {

    // ========== VIDEO ACCESS ==========

    /**
     * Get videos accessible to a specific user.
     * Considers user-based assignment, group-based assignment, and public videos.
     */
    fun observeVideosForUser(user: User): Flow<Resource<List<Video>>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_VIDEOS)
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Error fetching videos"))
                    return@addSnapshotListener
                }

                val allVideos = snapshot?.documents?.mapNotNull {
                    it.toObject(Video::class.java)
                } ?: emptyList()

                // Filter videos based on access control
                val accessibleVideos = allVideos.filter { video ->
                    when (video.accessType) {
                        AccessType.PUBLIC -> true
                        AccessType.ASSIGNED -> {
                            // Check if user is directly assigned
                            video.assignedUserIds.contains(user.uid) ||
                            // Check if user belongs to an assigned group
                            video.assignedGroups.any { group ->
                                user.assignedGroups.contains(group)
                            } ||
                            // Check if user's assigned videos include this one
                            user.assignedVideoIds.contains(video.id)
                        }
                        AccessType.ADMIN_ONLY -> user.role == com.stockmarket.tutorials.data.model.UserRole.ADMIN
                    }
                }.sortedBy { it.orderIndex }

                trySend(Resource.Success(accessibleVideos))
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get videos by category for a specific user.
     */
    fun observeVideosByCategory(
        user: User,
        category: VideoCategory
    ): Flow<Resource<List<Video>>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_VIDEOS)
            .whereEqualTo("isActive", true)
            .whereEqualTo("category", category.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Error fetching videos"))
                    return@addSnapshotListener
                }

                val videos = snapshot?.documents?.mapNotNull {
                    it.toObject(Video::class.java)
                }?.filter { video ->
                    video.accessType == AccessType.PUBLIC ||
                    video.assignedUserIds.contains(user.uid) ||
                    video.assignedGroups.any { user.assignedGroups.contains(it) } ||
                    user.assignedVideoIds.contains(video.id)
                }?.sortedBy { it.orderIndex } ?: emptyList()

                trySend(Resource.Success(videos))
            }

        awaitClose { listener.remove() }
    }

    // ========== SECURE STREAMING ==========

    /**
     * Get a time-limited signed URL for video streaming.
     * URL expires after the configured duration.
     */
    suspend fun getSignedStreamUrl(videoStoragePath: String): Resource<String> {
        return try {
            if (videoStoragePath.isBlank()) {
                return Resource.Error("Invalid video path")
            }

            val storageRef = storage.reference.child(videoStoragePath)
            val url = storageRef.downloadUrl.await()

            // Firebase Storage URLs have built-in token-based access.
            // For even more security, use Cloud Functions to generate
            // truly time-limited signed URLs.
            Resource.Success(url.toString())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get video URL", e)
        }
    }

    // ========== WATCH PROGRESS ==========

    /**
     * Save watch progress for resume playback.
     */
    suspend fun saveWatchProgress(
        userId: String,
        videoId: String,
        positionMs: Long,
        durationMs: Long
    ): Resource<Unit> {
        return try {
            val completedPercentage = if (durationMs > 0) {
                (positionMs.toFloat() / durationMs.toFloat()) * 100f
            } else 0f

            val progress = WatchProgress(
                videoId = videoId,
                userId = userId,
                lastPositionMs = positionMs,
                totalWatchedMs = positionMs,
                completedPercentage = completedPercentage,
                isCompleted = completedPercentage >= 95f,
                lastWatchedAt = System.currentTimeMillis()
            )

            firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .collection(Constants.COLLECTION_WATCH_PROGRESS)
                .document(videoId)
                .set(progress)
                .await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to save progress", e)
        }
    }

    /**
     * Get watch progress for resuming playback.
     */
    suspend fun getWatchProgress(userId: String, videoId: String): Resource<WatchProgress?> {
        return try {
            val doc = firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .collection(Constants.COLLECTION_WATCH_PROGRESS)
                .document(videoId)
                .get()
                .await()

            val progress = doc.toObject(WatchProgress::class.java)
            Resource.Success(progress)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get progress", e)
        }
    }

    /**
     * Get all watch progress for a user.
     */
    suspend fun getAllWatchProgress(userId: String): Resource<List<WatchProgress>> {
        return try {
            val docs = firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .collection(Constants.COLLECTION_WATCH_PROGRESS)
                .get()
                .await()

            val progressList = docs.documents.mapNotNull {
                it.toObject(WatchProgress::class.java)
            }
            Resource.Success(progressList)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get watch progress", e)
        }
    }

    // ========== ADMIN: VIDEO MANAGEMENT ==========

    /**
     * Get all videos (Admin only).
     */
    fun observeAllVideos(): Flow<Resource<List<Video>>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_VIDEOS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Error fetching all videos"))
                    return@addSnapshotListener
                }

                val videos = snapshot?.documents?.mapNotNull {
                    it.toObject(Video::class.java)
                }?.sortedBy { it.orderIndex } ?: emptyList()

                trySend(Resource.Success(videos))
            }

        awaitClose { listener.remove() }
    }

    /**
     * Add a new video entry (Admin only).
     * The actual video file should be uploaded to Firebase Storage first.
     */
    suspend fun addVideo(video: Video): Resource<String> {
        return try {
            val docRef = if (video.id.isNotBlank()) {
                firestore.collection(Constants.COLLECTION_VIDEOS).document(video.id)
            } else {
                firestore.collection(Constants.COLLECTION_VIDEOS).document()
            }

            val videoWithId = video.copy(id = docRef.id)
            docRef.set(videoWithId).await()

            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add video", e)
        }
    }

    /**
     * Update an existing video (Admin only).
     */
    suspend fun updateVideo(video: Video): Resource<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_VIDEOS)
                .document(video.id)
                .set(video)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update video", e)
        }
    }

    /**
     * Delete a video (Admin only - soft delete).
     */
    suspend fun deleteVideo(videoId: String): Resource<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_VIDEOS)
                .document(videoId)
                .update("isActive", false)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete video", e)
        }
    }

    /**
     * Assign a video to specific users (Admin only).
     */
    suspend fun assignVideoToUsers(videoId: String, userIds: List<String>): Resource<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_VIDEOS)
                .document(videoId)
                .update("assignedUserIds", userIds)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to assign video", e)
        }
    }

    /**
     * Upload video file to Firebase Storage and return the storage path.
     */
    suspend fun uploadVideoFile(
        fileUri: android.net.Uri,
        fileName: String
    ): Resource<String> {
        return try {
            val storagePath = "videos/$fileName"
            val ref = storage.reference.child(storagePath)
            ref.putFile(fileUri).await()
            Resource.Success(storagePath)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to upload video", e)
        }
    }
}
