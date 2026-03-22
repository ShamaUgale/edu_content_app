package com.stockmarket.tutorials.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockmarket.tutorials.data.model.User
import com.stockmarket.tutorials.data.model.Video
import com.stockmarket.tutorials.data.model.VideoCategory
import com.stockmarket.tutorials.data.model.WatchProgress
import com.stockmarket.tutorials.data.repository.VideoRepository
import com.stockmarket.tutorials.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for video listing and management.
 * Handles loading videos, filtering by category, and tracking watch progress.
 */
@HiltViewModel
class VideoViewModel @Inject constructor(
    private val videoRepository: VideoRepository
) : ViewModel() {

    // Videos list
    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos.asStateFlow()

    // Filtered videos by category
    private val _filteredVideos = MutableStateFlow<List<Video>>(emptyList())
    val filteredVideos: StateFlow<List<Video>> = _filteredVideos.asStateFlow()

    // Selected category
    private val _selectedCategory = MutableStateFlow<VideoCategory?>(null)
    val selectedCategory: StateFlow<VideoCategory?> = _selectedCategory.asStateFlow()

    // Watch progress map
    private val _watchProgressMap = MutableStateFlow<Map<String, WatchProgress>>(emptyMap())
    val watchProgressMap: StateFlow<Map<String, WatchProgress>> = _watchProgressMap.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Available categories (from loaded videos)
    private val _availableCategories = MutableStateFlow<List<VideoCategory>>(emptyList())
    val availableCategories: StateFlow<List<VideoCategory>> = _availableCategories.asStateFlow()

    /**
     * Load videos accessible to the current user.
     */
    fun loadVideosForUser(user: User) {
        viewModelScope.launch {
            _isLoading.value = true
            videoRepository.observeVideosForUser(user).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _videos.value = result.data
                        _availableCategories.value = result.data
                            .map { it.category }
                            .distinct()
                            .sortedBy { it.ordinal }
                        filterVideosByCategory(_selectedCategory.value)
                        _isLoading.value = false
                    }
                    is Resource.Error -> {
                        _error.value = result.message
                        _isLoading.value = false
                    }
                    is Resource.Loading -> {
                        _isLoading.value = true
                    }
                }
            }
        }
    }

    /**
     * Filter videos by category.
     */
    fun filterVideosByCategory(category: VideoCategory?) {
        _selectedCategory.value = category
        _filteredVideos.value = if (category == null) {
            _videos.value
        } else {
            _videos.value.filter { it.category == category }
        }
    }

    /**
     * Load all watch progress for a user.
     */
    fun loadWatchProgress(userId: String) {
        viewModelScope.launch {
            when (val result = videoRepository.getAllWatchProgress(userId)) {
                is Resource.Success -> {
                    _watchProgressMap.value = result.data.associateBy { it.videoId }
                }
                is Resource.Error -> {
                    // Silently fail - progress is optional
                }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * Get video streaming URL.
     */
    suspend fun getStreamUrl(videoStoragePath: String): String? {
        return when (val result = videoRepository.getSignedStreamUrl(videoStoragePath)) {
            is Resource.Success -> result.data
            else -> null
        }
    }

    /**
     * Save watch progress.
     */
    fun saveProgress(userId: String, videoId: String, positionMs: Long, durationMs: Long) {
        viewModelScope.launch {
            videoRepository.saveWatchProgress(userId, videoId, positionMs, durationMs)
        }
    }

    /**
     * Get watch progress for a specific video.
     */
    suspend fun getVideoProgress(userId: String, videoId: String): WatchProgress? {
        return when (val result = videoRepository.getWatchProgress(userId, videoId)) {
            is Resource.Success -> result.data
            else -> null
        }
    }

    fun clearError() {
        _error.value = null
    }
}
