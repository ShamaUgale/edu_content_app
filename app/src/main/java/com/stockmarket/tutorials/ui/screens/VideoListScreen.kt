package com.stockmarket.tutorials.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stockmarket.tutorials.data.model.VideoCategory
import com.stockmarket.tutorials.ui.theme.*
import com.stockmarket.tutorials.ui.viewmodel.AuthViewModel
import com.stockmarket.tutorials.ui.viewmodel.VideoViewModel

/**
 * Video listing screen filtered by category.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoListScreen(
    authViewModel: AuthViewModel,
    videoViewModel: VideoViewModel,
    categoryName: String,
    onVideoClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val videos by videoViewModel.filteredVideos.collectAsState()
    val isLoading by videoViewModel.isLoading.collectAsState()
    val watchProgressMap by videoViewModel.watchProgressMap.collectAsState()

    val category = VideoCategory.fromString(categoryName)

    // Load and filter videos
    LaunchedEffect(currentUser, category) {
        currentUser?.let { user ->
            videoViewModel.loadVideosForUser(user)
            videoViewModel.filterVideosByCategory(category)
            videoViewModel.loadWatchProgress(user.uid)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${videos.size} videos",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Green500)
            }
        } else if (videos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = getCategoryIcon(category),
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No videos in ${category.displayName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(videos, key = { it.id }) { video ->
                    VideoCard(
                        video = video,
                        progress = watchProgressMap[video.id]?.completedPercentage,
                        onClick = { onVideoClick(video.id) }
                    )
                }
            }
        }
    }
}
