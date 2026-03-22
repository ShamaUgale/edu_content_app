package com.stockmarket.tutorials.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockmarket.tutorials.data.model.User
import com.stockmarket.tutorials.data.model.UserRole
import com.stockmarket.tutorials.data.model.Video
import com.stockmarket.tutorials.data.model.VideoCategory
import com.stockmarket.tutorials.data.model.AccessType
import com.stockmarket.tutorials.data.repository.AuthRepository
import com.stockmarket.tutorials.data.repository.VideoRepository
import com.stockmarket.tutorials.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Admin Panel operations.
 * Handles user management, video management, and activity tracking.
 */
@HiltViewModel
class AdminViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val videoRepository: VideoRepository
) : ViewModel() {

    // Users list
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    // All videos
    private val _allVideos = MutableStateFlow<List<Video>>(emptyList())
    val allVideos: StateFlow<List<Video>> = _allVideos.asStateFlow()

    // Operation status
    private val _operationStatus = MutableStateFlow<OperationStatus>(OperationStatus.Idle)
    val operationStatus: StateFlow<OperationStatus> = _operationStatus.asStateFlow()

    // Loading states
    private val _isLoadingUsers = MutableStateFlow(false)
    val isLoadingUsers: StateFlow<Boolean> = _isLoadingUsers.asStateFlow()

    private val _isLoadingVideos = MutableStateFlow(false)
    val isLoadingVideos: StateFlow<Boolean> = _isLoadingVideos.asStateFlow()

    init {
        loadUsers()
        loadAllVideos()
    }

    // ========== USER MANAGEMENT ==========

    fun loadUsers() {
        viewModelScope.launch {
            _isLoadingUsers.value = true
            authRepository.observeAllUsers().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _users.value = result.data
                        _isLoadingUsers.value = false
                    }
                    is Resource.Error -> {
                        _operationStatus.value = OperationStatus.Error(result.message)
                        _isLoadingUsers.value = false
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }

    fun createUser(
        email: String,
        password: String,
        displayName: String,
        role: UserRole,
        groups: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Loading("Creating user...")
            when (val result = authRepository.createUser(email, password, displayName, role, groups)) {
                is Resource.Success -> {
                    _operationStatus.value = OperationStatus.Success("User created successfully")
                }
                is Resource.Error -> {
                    _operationStatus.value = OperationStatus.Error(result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Loading("Updating user...")
            when (val result = authRepository.updateUser(user)) {
                is Resource.Success -> {
                    _operationStatus.value = OperationStatus.Success("User updated successfully")
                }
                is Resource.Error -> {
                    _operationStatus.value = OperationStatus.Error(result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun deactivateUser(uid: String) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Loading("Deactivating user...")
            when (val result = authRepository.deactivateUser(uid)) {
                is Resource.Success -> {
                    _operationStatus.value = OperationStatus.Success("User deactivated")
                }
                is Resource.Error -> {
                    _operationStatus.value = OperationStatus.Error(result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun unbindDevice(uid: String) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Loading("Unbinding device...")
            when (val result = authRepository.unbindDevice(uid)) {
                is Resource.Success -> {
                    _operationStatus.value = OperationStatus.Success("Device unbound successfully")
                }
                is Resource.Error -> {
                    _operationStatus.value = OperationStatus.Error(result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    // ========== VIDEO MANAGEMENT ==========

    fun loadAllVideos() {
        viewModelScope.launch {
            _isLoadingVideos.value = true
            videoRepository.observeAllVideos().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _allVideos.value = result.data
                        _isLoadingVideos.value = false
                    }
                    is Resource.Error -> {
                        _operationStatus.value = OperationStatus.Error(result.message)
                        _isLoadingVideos.value = false
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }

    fun addVideo(
        title: String,
        description: String,
        category: VideoCategory,
        videoStoragePath: String,
        accessType: AccessType = AccessType.ASSIGNED,
        assignedUserIds: List<String> = emptyList(),
        assignedGroups: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Loading("Adding video...")
            val video = Video(
                title = title,
                description = description,
                category = category,
                videoStoragePath = videoStoragePath,
                accessType = accessType,
                assignedUserIds = assignedUserIds,
                assignedGroups = assignedGroups,
                uploadedAt = System.currentTimeMillis()
            )
            when (val result = videoRepository.addVideo(video)) {
                is Resource.Success -> {
                    _operationStatus.value = OperationStatus.Success("Video added successfully")
                }
                is Resource.Error -> {
                    _operationStatus.value = OperationStatus.Error(result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateVideo(video: Video) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Loading("Updating video...")
            when (val result = videoRepository.updateVideo(video)) {
                is Resource.Success -> {
                    _operationStatus.value = OperationStatus.Success("Video updated successfully")
                }
                is Resource.Error -> {
                    _operationStatus.value = OperationStatus.Error(result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun deleteVideo(videoId: String) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Loading("Deleting video...")
            when (val result = videoRepository.deleteVideo(videoId)) {
                is Resource.Success -> {
                    _operationStatus.value = OperationStatus.Success("Video deleted successfully")
                }
                is Resource.Error -> {
                    _operationStatus.value = OperationStatus.Error(result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun assignVideoToUsers(videoId: String, userIds: List<String>) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Loading("Assigning video...")
            when (val result = videoRepository.assignVideoToUsers(videoId, userIds)) {
                is Resource.Success -> {
                    _operationStatus.value = OperationStatus.Success("Video assigned successfully")
                }
                is Resource.Error -> {
                    _operationStatus.value = OperationStatus.Error(result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun clearOperationStatus() {
        _operationStatus.value = OperationStatus.Idle
    }
}

sealed class OperationStatus {
    data object Idle : OperationStatus()
    data class Loading(val message: String) : OperationStatus()
    data class Success(val message: String) : OperationStatus()
    data class Error(val message: String) : OperationStatus()
}
