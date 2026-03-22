package com.stockmarket.tutorials.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stockmarket.tutorials.data.model.AccessType
import com.stockmarket.tutorials.data.model.Video
import com.stockmarket.tutorials.ui.theme.*
import com.stockmarket.tutorials.ui.viewmodel.AdminViewModel
import com.stockmarket.tutorials.ui.viewmodel.OperationStatus
import java.text.SimpleDateFormat
import java.util.*

/**
 * Admin screen for managing videos.
 * Lists all videos with options to edit, delete, and manage access.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVideosScreen(
    adminViewModel: AdminViewModel,
    onAddVideo: () -> Unit,
    onBack: () -> Unit
) {
    val allVideos by adminViewModel.allVideos.collectAsState()
    val isLoading by adminViewModel.isLoadingVideos.collectAsState()
    val operationStatus by adminViewModel.operationStatus.collectAsState()

    var showDeleteDialog by remember { mutableStateOf<Video?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(operationStatus) {
        when (operationStatus) {
            is OperationStatus.Success -> {
                snackbarHostState.showSnackbar(
                    (operationStatus as OperationStatus.Success).message
                )
                adminViewModel.clearOperationStatus()
            }
            is OperationStatus.Error -> {
                snackbarHostState.showSnackbar(
                    (operationStatus as OperationStatus.Error).message
                )
                adminViewModel.clearOperationStatus()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Manage Videos",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${allVideos.size} videos",
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
                actions = {
                    IconButton(onClick = onAddVideo) {
                        Icon(
                            Icons.Default.VideoCall,
                            contentDescription = "Add Video",
                            tint = Green400
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddVideo,
                containerColor = Green600,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.VideoCall, contentDescription = "Add Video")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
        } else if (allVideos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No videos yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Upload your first video tutorial",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
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
                items(allVideos, key = { it.id }) { video ->
                    AdminVideoCard(
                        video = video,
                        onDelete = { showDeleteDialog = video }
                    )
                }
            }
        }

        // Delete Video Dialog
        showDeleteDialog?.let { video ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = {
                    Text(
                        "Delete Video",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text("Are you sure you want to delete \"${video.title}\"? This action can be undone by reactivating the video.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            adminViewModel.deleteVideo(video.id)
                            showDeleteDialog = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = RedBearish)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("Cancel")
                    }
                },
                containerColor = DarkCard,
                titleContentColor = TextPrimary,
                textContentColor = TextSecondary
            )
        }
    }
}

@Composable
fun AdminVideoCard(
    video: Video,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (video.isActive) DarkCard else DarkCard.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Thumbnail placeholder
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    DarkCardElevated,
                                    Green900.copy(alpha = 0.3f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(video.category),
                        contentDescription = null,
                        tint = Green400,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // Status
                        if (!video.isActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(
                                containerColor = RedBearish.copy(alpha = 0.2f),
                                contentColor = RedBearish
                            ) {
                                Text(
                                    "INACTIVE",
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Category + Access Type
                    Row {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Green800.copy(alpha = 0.5f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = video.category.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = Green300
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when (video.accessType) {
                                        AccessType.PUBLIC -> BlueInfo.copy(alpha = 0.15f)
                                        AccessType.ASSIGNED -> OrangeWarning.copy(alpha = 0.15f)
                                        AccessType.ADMIN_ONLY -> GoldAccent.copy(alpha = 0.15f)
                                    }
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = video.accessType.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = when (video.accessType) {
                                    AccessType.PUBLIC -> BlueInfo
                                    AccessType.ASSIGNED -> OrangeWarning
                                    AccessType.ADMIN_ONLY -> GoldAccent
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Assigned to ${video.assignedUserIds.size} users • ${video.assignedGroups.size} groups",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Upload date
                Text(
                    text = "Uploaded: ${
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            .format(Date(video.uploadedAt))
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                // Delete button
                if (video.isActive) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = RedBearish
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
