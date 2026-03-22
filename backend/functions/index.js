/**
 * Firebase Cloud Functions for StockMarket Tutorials
 * ================================================
 * 
 * These functions handle secure server-side operations:
 * 1. User creation (admin-only) - creates Firebase Auth + Firestore profile
 * 2. User deactivation with auth disable
 * 3. Signed URL generation with expiration
 * 4. Session validation
 * 
 * IMPORTANT: These functions should be deployed to Firebase to enable
 * secure admin operations without the admin being signed out.
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();
const auth = admin.auth();

// ==========================================
// MIDDLEWARE: Verify Admin Role
// ==========================================

/**
 * Verifies that the calling user has admin role.
 * @param {string} uid - Firebase Auth UID of the caller
 * @returns {Promise<boolean>}
 */
async function verifyAdmin(uid) {
    const userDoc = await db.collection("users").doc(uid).get();
    if (!userDoc.exists) return false;
    const userData = userDoc.data();
    return userData.role === "ADMIN" && userData.isActive === true;
}

// ==========================================
// FUNCTION: Create User (Admin Only)
// ==========================================

/**
 * Creates a new user with Firebase Auth account and Firestore profile.
 * This is a callable function that can only be invoked by authenticated admins.
 * 
 * This solves the problem of admin being signed out when creating users
 * client-side with createUserWithEmailAndPassword().
 */
exports.createUser = functions.https.onCall(async (data, context) => {
    // Verify authentication
    if (!context.auth) {
        throw new functions.https.HttpsError(
            "unauthenticated",
            "Must be authenticated to create users."
        );
    }

    // Verify admin role
    const isAdmin = await verifyAdmin(context.auth.uid);
    if (!isAdmin) {
        throw new functions.https.HttpsError(
            "permission-denied",
            "Only administrators can create users."
        );
    }

    // Validate input
    const { email, password, displayName, role, groups } = data;

    if (!email || !password || !displayName) {
        throw new functions.https.HttpsError(
            "invalid-argument",
            "Email, password, and displayName are required."
        );
    }

    if (password.length < 8) {
        throw new functions.https.HttpsError(
            "invalid-argument",
            "Password must be at least 8 characters."
        );
    }

    try {
        // Create Firebase Auth user
        const userRecord = await auth.createUser({
            email: email,
            password: password,
            displayName: displayName,
            disabled: false,
        });

        // Create Firestore user profile
        const userProfile = {
            uid: userRecord.uid,
            email: email,
            displayName: displayName,
            role: role || "USER",
            isActive: true,
            boundDeviceId: null,
            assignedVideoIds: [],
            assignedGroups: groups || [],
            lastLogin: 0,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            createdBy: context.auth.uid,
        };

        await db.collection("users").doc(userRecord.uid).set(userProfile);

        return {
            success: true,
            uid: userRecord.uid,
            message: `User ${displayName} created successfully.`,
        };
    } catch (error) {
        console.error("Error creating user:", error);
        throw new functions.https.HttpsError(
            "internal",
            error.message || "Failed to create user."
        );
    }
});

// ==========================================
// FUNCTION: Deactivate User (Admin Only)
// ==========================================

/**
 * Deactivates a user by disabling their Firebase Auth account
 * and marking them inactive in Firestore.
 */
exports.deactivateUser = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Must be authenticated.");
    }

    const isAdmin = await verifyAdmin(context.auth.uid);
    if (!isAdmin) {
        throw new functions.https.HttpsError("permission-denied", "Admin access required.");
    }

    const { uid } = data;
    if (!uid) {
        throw new functions.https.HttpsError("invalid-argument", "User UID is required.");
    }

    // Prevent self-deactivation
    if (uid === context.auth.uid) {
        throw new functions.https.HttpsError(
            "failed-precondition",
            "Cannot deactivate your own account."
        );
    }

    try {
        // Disable Firebase Auth account
        await auth.updateUser(uid, { disabled: true });

        // Update Firestore profile
        await db.collection("users").doc(uid).update({
            isActive: false,
        });

        // Revoke all refresh tokens (force sign out)
        await auth.revokeRefreshTokens(uid);

        return {
            success: true,
            message: "User deactivated successfully.",
        };
    } catch (error) {
        console.error("Error deactivating user:", error);
        throw new functions.https.HttpsError("internal", error.message);
    }
});

// ==========================================
// FUNCTION: Reactivate User (Admin Only)
// ==========================================

exports.reactivateUser = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Must be authenticated.");
    }

    const isAdmin = await verifyAdmin(context.auth.uid);
    if (!isAdmin) {
        throw new functions.https.HttpsError("permission-denied", "Admin access required.");
    }

    const { uid } = data;
    if (!uid) {
        throw new functions.https.HttpsError("invalid-argument", "User UID is required.");
    }

    try {
        await auth.updateUser(uid, { disabled: false });
        await db.collection("users").doc(uid).update({
            isActive: true,
        });

        return {
            success: true,
            message: "User reactivated successfully.",
        };
    } catch (error) {
        console.error("Error reactivating user:", error);
        throw new functions.https.HttpsError("internal", error.message);
    }
});

// ==========================================
// FUNCTION: Generate Signed URL (Authenticated Users)
// ==========================================

/**
 * Generates a time-limited signed URL for video streaming.
 * Validates that the user has access to the requested video.
 */
exports.getSignedVideoUrl = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Must be authenticated.");
    }

    const { videoId } = data;
    if (!videoId) {
        throw new functions.https.HttpsError("invalid-argument", "Video ID is required.");
    }

    try {
        // Get user profile
        const userDoc = await db.collection("users").doc(context.auth.uid).get();
        if (!userDoc.exists || !userDoc.data().isActive) {
            throw new functions.https.HttpsError("permission-denied", "Account not active.");
        }
        const user = userDoc.data();

        // Get video document
        const videoDoc = await db.collection("videos").doc(videoId).get();
        if (!videoDoc.exists || !videoDoc.data().isActive) {
            throw new functions.https.HttpsError("not-found", "Video not found.");
        }
        const video = videoDoc.data();

        // Check access permissions
        let hasAccess = false;

        switch (video.accessType) {
            case "PUBLIC":
                hasAccess = true;
                break;
            case "ASSIGNED":
                hasAccess =
                    (video.assignedUserIds && video.assignedUserIds.includes(context.auth.uid)) ||
                    (video.assignedGroups && user.assignedGroups &&
                        video.assignedGroups.some(g => user.assignedGroups.includes(g))) ||
                    (user.assignedVideoIds && user.assignedVideoIds.includes(videoId));
                break;
            case "ADMIN_ONLY":
                hasAccess = user.role === "ADMIN";
                break;
        }

        if (!hasAccess) {
            throw new functions.https.HttpsError(
                "permission-denied",
                "You do not have access to this video."
            );
        }

        // Generate signed URL with 30-minute expiration
        const bucket = admin.storage().bucket();
        const file = bucket.file(video.videoStoragePath);

        const [signedUrl] = await file.getSignedUrl({
            action: "read",
            expires: Date.now() + 30 * 60 * 1000, // 30 minutes
        });

        return {
            success: true,
            url: signedUrl,
            expiresIn: 30 * 60, // seconds
        };
    } catch (error) {
        if (error instanceof functions.https.HttpsError) throw error;
        console.error("Error generating signed URL:", error);
        throw new functions.https.HttpsError("internal", "Failed to generate video URL.");
    }
});

// ==========================================
// FUNCTION: Unbind Device (Admin Only)
// ==========================================

exports.unbindDevice = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Must be authenticated.");
    }

    const isAdmin = await verifyAdmin(context.auth.uid);
    if (!isAdmin) {
        throw new functions.https.HttpsError("permission-denied", "Admin access required.");
    }

    const { uid } = data;
    if (!uid) {
        throw new functions.https.HttpsError("invalid-argument", "User UID is required.");
    }

    try {
        await db.collection("users").doc(uid).update({
            boundDeviceId: null,
        });

        return {
            success: true,
            message: "Device unbound successfully.",
        };
    } catch (error) {
        console.error("Error unbinding device:", error);
        throw new functions.https.HttpsError("internal", error.message);
    }
});

// ==========================================
// FUNCTION: Validate Session (Triggered on Auth)
// ==========================================

/**
 * Validates user session on every authentication state change.
 * Checks if the user account is still active.
 */
exports.validateSession = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        return { valid: false, reason: "Not authenticated." };
    }

    try {
        const userDoc = await db.collection("users").doc(context.auth.uid).get();
        if (!userDoc.exists) {
            return { valid: false, reason: "User profile not found." };
        }

        const user = userDoc.data();
        if (!user.isActive) {
            return { valid: false, reason: "Account has been deactivated." };
        }

        // Check session expiration (24 hours)
        const sessionTimeout = 24 * 60 * 60 * 1000;
        if (user.lastLogin && Date.now() - user.lastLogin > sessionTimeout) {
            return { valid: false, reason: "Session expired. Please log in again." };
        }

        return { valid: true };
    } catch (error) {
        console.error("Error validating session:", error);
        return { valid: false, reason: "Session validation failed." };
    }
});

// ==========================================
// FUNCTION: Track User Activity
// ==========================================

/**
 * Callable function to get activity data for all users (admin dashboard).
 */
exports.getUserActivity = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Must be authenticated.");
    }

    const isAdmin = await verifyAdmin(context.auth.uid);
    if (!isAdmin) {
        throw new functions.https.HttpsError("permission-denied", "Admin access required.");
    }

    try {
        const usersSnapshot = await db.collection("users").get();
        const activities = [];

        for (const doc of usersSnapshot.docs) {
            const user = doc.data();

            // Get watch progress for this user
            const progressSnapshot = await db
                .collection("users")
                .doc(doc.id)
                .collection("watchProgress")
                .get();

            let totalWatchTimeMs = 0;
            let videosWatched = 0;

            progressSnapshot.docs.forEach((progressDoc) => {
                const progress = progressDoc.data();
                totalWatchTimeMs += progress.totalWatchedMs || 0;
                if (progress.isCompleted) videosWatched++;
            });

            activities.push({
                uid: user.uid,
                email: user.email,
                displayName: user.displayName,
                lastLogin: user.lastLogin || 0,
                totalVideosWatched: videosWatched,
                totalWatchTimeMs: totalWatchTimeMs,
                deviceId: user.boundDeviceId || null,
                isActive: user.isActive,
            });
        }

        return { success: true, activities: activities };
    } catch (error) {
        console.error("Error getting user activity:", error);
        throw new functions.https.HttpsError("internal", error.message);
    }
});

// ==========================================
// FIRESTORE TRIGGER: Auto-cleanup on user deletion
// ==========================================

exports.onUserDeleted = functions.auth.user().onDelete(async (user) => {
    try {
        // Delete user's Firestore profile
        await db.collection("users").doc(user.uid).delete();

        // Delete user's watch progress
        const progressSnapshot = await db
            .collection("users")
            .doc(user.uid)
            .collection("watchProgress")
            .get();

        const batch = db.batch();
        progressSnapshot.docs.forEach((doc) => {
            batch.delete(doc.ref);
        });
        await batch.commit();

        console.log(`Cleaned up data for deleted user: ${user.uid}`);
    } catch (error) {
        console.error(`Error cleaning up user ${user.uid}:`, error);
    }
});
