package com.stockmarket.tutorials.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stockmarket.tutorials.data.model.UserRole
import com.stockmarket.tutorials.data.model.Video
import com.stockmarket.tutorials.data.model.VideoCategory
import com.stockmarket.tutorials.ui.theme.*
import com.stockmarket.tutorials.ui.viewmodel.AuthViewModel
import com.stockmarket.tutorials.ui.viewmodel.VideoViewModel

/**
 * Main dashboard screen showing categories and assigned videos.
 * Adapts based on user role (shows admin access for admins).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel,
    videoViewModel: VideoViewModel,
    onCategoryClick: (VideoCategory) -> Unit,
    onVideoClick: (String) -> Unit,
    onAdminPanelClick: () -> Unit,
    onLogout: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val videos by videoViewModel.filteredVideos.collectAsState()
    val availableCategories by videoViewModel.availableCategories.collectAsState()
    val selectedCategory by videoViewModel.selectedCategory.collectAsState()
    val isLoading by videoViewModel.isLoading.collectAsState()
    val watchProgressMap by videoViewModel.watchProgressMap.collectAsState()

    // Load videos when user is available
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            videoViewModel.loadVideosForUser(user)
            videoViewModel.loadWatchProgress(user.uid)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSurface,
                shadowElevation = 4.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Logo
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(GradientGreenStart, GradientGreenEnd)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Welcome back,",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            Text(
                                text = currentUser?.displayName ?: "User",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        // Admin button (only for admins)
                        if (currentUser?.role == UserRole.ADMIN) {
                            IconButton(onClick = onAdminPanelClick) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin Panel",
                                    tint = GoldAccent
                                )
                            }
                        }

                        // Logout button
                        IconButton(onClick = onLogout) {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription = "Logout",
                                tint = TextMuted
                            )
                        }
                    }
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Stats Banner
                StatsCard(
                    totalVideos = videos.size,
                    completedVideos = watchProgressMap.values.count { it.isCompleted },
                    isAdmin = currentUser?.role == UserRole.ADMIN
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Categories Section
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // All category chip
                    item {
                        CategoryChip(
                            label = "All",
                            icon = Icons.Default.GridView,
                            isSelected = selectedCategory == null,
                            onClick = { videoViewModel.filterVideosByCategory(null) }
                        )
                    }

                    items(VideoCategory.all()) { category ->
                        CategoryChip(
                            label = category.displayName,
                            icon = getCategoryIcon(category),
                            isSelected = selectedCategory == category,
                            onClick = { videoViewModel.filterVideosByCategory(category) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Videos Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedCategory != null) selectedCategory!!.displayName
                               else "Your Videos",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "${videos.size} videos",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Green500)
                    }
                } else if (videos.isEmpty()) {
                    EmptyStateCard()
                } else {
                    videos.forEach { video ->
                        VideoCard(
                            video = video,
                            progress = watchProgressMap[video.id]?.completedPercentage,
                            onClick = { onVideoClick(video.id) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Bottom spacing for navigation bar
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StatsCard(
    totalVideos: Int,
    completedVideos: Int,
    isAdmin: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Green900, Green800, Green700)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.PlayCircle,
                    value = "$totalVideos",
                    label = "Available"
                )
                StatItem(
                    icon = Icons.Default.CheckCircle,
                    value = "$completedVideos",
                    label = "Completed"
                )
                StatItem(
                    icon = Icons.Default.TrendingUp,
                    value = if (totalVideos > 0) "${(completedVideos * 100 / totalVideos)}%" else "0%",
                    label = "Progress"
                )
            }
        }
    }
}

@Composable
private fun StatItem(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GoldAccent,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
fun CategoryChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = DarkCard,
            labelColor = TextSecondary,
            iconColor = TextMuted,
            selectedContainerColor = Green800,
            selectedLabelColor = TextPrimary,
            selectedLeadingIconColor = GoldAccent
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = DarkBorder,
            selectedBorderColor = Green600,
            enabled = true,
            selected = isSelected
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun VideoCard(
    video: Video,
    progress: Float?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Thumbnail area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                DarkCardElevated,
                                Green900.copy(alpha = 0.3f),
                                DarkCardElevated
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Play button overlay
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Green600.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Category badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Green800.copy(alpha = 0.9f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = video.category.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Green300,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Duration badge
                if (video.durationSeconds > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatDuration(video.durationSeconds),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Video info
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (video.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = video.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Progress bar
                if (progress != null && progress > 0f) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = if (progress >= 95f) GreenBullish else Green500,
                            trackColor = DarkBorder,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${progress.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (progress >= 95f) GreenBullish else TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.VideoLibrary,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No videos assigned yet",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Contact your administrator to get access to video tutorials.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// getCategoryIcon and formatDuration are defined in ScreenUtils.kt
