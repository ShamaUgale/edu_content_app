package com.stockmarket.tutorials.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.stockmarket.tutorials.data.model.Video
import com.stockmarket.tutorials.ui.theme.*
import com.stockmarket.tutorials.ui.viewmodel.AuthViewModel
import com.stockmarket.tutorials.ui.viewmodel.VideoViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * Secure video player screen with:
 * - ExoPlayer streaming (no download)
 * - Watermark overlay (username + timestamp)
 * - Resume playback from last position
 * - Progress saving
 * - No caching of video files
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    authViewModel: AuthViewModel,
    videoViewModel: VideoViewModel,
    videoId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val videos by videoViewModel.videos.collectAsState()

    val video = remember(videos, videoId) {
        videos.find { it.id == videoId }
    }

    var streamUrl by remember { mutableStateOf<String?>(null) }
    var isLoadingUrl by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var resumePosition by remember { mutableStateOf(0L) }

    // Fetch stream URL and resume position
    LaunchedEffect(video) {
        video?.let { v ->
            isLoadingUrl = true
            try {
                // Get resume position
                currentUser?.let { user ->
                    val progress = videoViewModel.getVideoProgress(user.uid, v.id)
                    resumePosition = progress?.lastPositionMs ?: 0L
                }

                // Get signed streaming URL
                val url = videoViewModel.getStreamUrl(v.videoStoragePath)
                if (url != null) {
                    streamUrl = url
                } else {
                    error = "Failed to load video. Please try again."
                }
            } catch (e: Exception) {
                error = e.message ?: "Failed to load video"
            }
            isLoadingUrl = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = video?.title ?: "Video Player",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
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
                    containerColor = Color.Black,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoadingUrl -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Green500)
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = RedBearish,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error ?: "",
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                streamUrl != null -> {
                    // Video Player with Watermark
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    ) {
                        SecureVideoPlayer(
                            url = streamUrl!!,
                            resumePositionMs = resumePosition,
                            onProgressUpdate = { positionMs, durationMs ->
                                currentUser?.let { user ->
                                    videoViewModel.saveProgress(
                                        user.uid,
                                        videoId,
                                        positionMs,
                                        durationMs
                                    )
                                }
                            }
                        )

                        // ====== WATERMARK OVERLAY ======
                        WatermarkOverlay(
                            email = currentUser?.email ?: "",
                            displayName = currentUser?.displayName ?: ""
                        )
                    }
                }
            }

            // Video Details
            video?.let { v ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground)
                        .padding(16.dp)
                ) {
                    Text(
                        text = v.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(v.category),
                            contentDescription = null,
                            tint = Green400,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = v.category.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Green400
                        )

                        if (v.durationSeconds > 0) {
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatDuration(v.durationSeconds),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }

                    if (v.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = v.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Secure ExoPlayer composable.
 * - Streams only (no caching)
 * - Saves progress periodically
 * - Resumes from last position
 */
@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun SecureVideoPlayer(
    url: String,
    resumePositionMs: Long = 0L,
    onProgressUpdate: (positionMs: Long, durationMs: Long) -> Unit
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                // Configure for streaming only - no caching
                val dataSourceFactory = DefaultHttpDataSource.Factory()
                    .setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(15000)
                    .setReadTimeoutMs(15000)

                val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(url))

                setMediaSource(mediaSource)
                prepare()

                // Resume from last position
                if (resumePositionMs > 0) {
                    seekTo(resumePositionMs)
                }

                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    // Save progress periodically
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(10000) // Save every 10 seconds
            if (exoPlayer.isPlaying) {
                onProgressUpdate(exoPlayer.currentPosition, exoPlayer.duration)
            }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            // Save final position
            if (exoPlayer.duration > 0) {
                onProgressUpdate(exoPlayer.currentPosition, exoPlayer.duration)
            }
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Watermark overlay displaying user's email and timestamp.
 * Semi-transparent, rotated to discourage screen capture.
 */
@Composable
fun WatermarkOverlay(
    email: String,
    displayName: String
) {
    val currentTime = remember { mutableStateOf("") }

    // Update timestamp
    LaunchedEffect(Unit) {
        while (true) {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            currentTime.value = sdf.format(Date())
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.15f)
    ) {
        // Diagonal watermarks across the video
        Column(
            modifier = Modifier
                .fillMaxSize()
                .rotate(-25f)
                .offset(x = (-30).dp, y = (-20).dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(5) {
                Text(
                    text = "$email  •  ${currentTime.value}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Center watermark
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = displayName,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(0.3f)
            )
            Text(
                text = email,
                color = Color.White,
                fontSize = 11.sp,
                modifier = Modifier.alpha(0.25f)
            )
        }
    }
}
