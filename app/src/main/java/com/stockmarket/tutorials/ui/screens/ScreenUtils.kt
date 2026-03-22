package com.stockmarket.tutorials.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.stockmarket.tutorials.data.model.VideoCategory

/**
 * Shared UI utility functions used across multiple screens.
 */

/**
 * Returns the appropriate Material icon for a video category.
 */
fun getCategoryIcon(category: VideoCategory): ImageVector {
    return when (category) {
        VideoCategory.BASICS -> Icons.Default.MenuBook
        VideoCategory.INTRADAY -> Icons.Default.Speed
        VideoCategory.OPTIONS -> Icons.Default.CallSplit
        VideoCategory.STRATEGY -> Icons.Default.Psychology
        VideoCategory.TECHNICAL_ANALYSIS -> Icons.Default.BarChart
        VideoCategory.FUNDAMENTAL_ANALYSIS -> Icons.Default.Analytics
        VideoCategory.RISK_MANAGEMENT -> Icons.Default.Shield
        VideoCategory.ADVANCED -> Icons.Default.School
    }
}

/**
 * Formats seconds into a human-readable duration string (H:MM:SS or M:SS).
 */
fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%d:%02d", minutes, secs)
    }
}
