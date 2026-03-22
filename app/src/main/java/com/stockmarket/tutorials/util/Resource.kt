package com.stockmarket.tutorials.util

/**
 * Result wrapper for repository operations.
 * Provides a type-safe way to handle success, error, and loading states.
 */
sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val exception: Exception? = null) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
}
