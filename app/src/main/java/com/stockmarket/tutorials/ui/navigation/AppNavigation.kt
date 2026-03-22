package com.stockmarket.tutorials.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stockmarket.tutorials.ui.screens.*
import com.stockmarket.tutorials.ui.viewmodel.AdminViewModel
import com.stockmarket.tutorials.ui.viewmodel.AuthState
import com.stockmarket.tutorials.ui.viewmodel.AuthViewModel
import com.stockmarket.tutorials.ui.viewmodel.VideoViewModel

/**
 * Main navigation host for the app.
 * Routes are determined by authentication state.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()

    // Navigate based on auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.NotAuthenticated -> {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            is AuthState.AuthenticatedUser, is AuthState.AuthenticatedAdmin -> {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            is AuthState.Initial -> { /* Loading */ }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        // Login Screen
        composable(
            route = Screen.Login.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Dashboard Screen
        composable(
            route = Screen.Dashboard.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            val videoViewModel: VideoViewModel = hiltViewModel()
            DashboardScreen(
                authViewModel = authViewModel,
                videoViewModel = videoViewModel,
                onCategoryClick = { category ->
                    navController.navigate(Screen.VideoList.createRoute(category.name))
                },
                onVideoClick = { videoId ->
                    navController.navigate(Screen.VideoPlayer.createRoute(videoId))
                },
                onAdminPanelClick = {
                    navController.navigate(Screen.AdminPanel.route)
                },
                onLogout = {
                    authViewModel.signOut()
                }
            )
        }

        // Video List by Category
        composable(
            route = Screen.VideoList.route,
            arguments = listOf(
                navArgument("category") { type = NavType.StringType }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: "BASICS"
            val videoViewModel: VideoViewModel = hiltViewModel()
            VideoListScreen(
                authViewModel = authViewModel,
                videoViewModel = videoViewModel,
                categoryName = category,
                onVideoClick = { videoId ->
                    navController.navigate(Screen.VideoPlayer.createRoute(videoId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Video Player
        composable(
            route = Screen.VideoPlayer.route,
            arguments = listOf(
                navArgument("videoId") { type = NavType.StringType }
            ),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
            val videoViewModel: VideoViewModel = hiltViewModel()
            VideoPlayerScreen(
                authViewModel = authViewModel,
                videoViewModel = videoViewModel,
                videoId = videoId,
                onBack = { navController.popBackStack() }
            )
        }

        // Admin Panel
        composable(
            route = Screen.AdminPanel.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
        ) {
            val adminViewModel: AdminViewModel = hiltViewModel()
            AdminPanelScreen(
                adminViewModel = adminViewModel,
                onManageUsers = { navController.navigate(Screen.AdminUsers.route) },
                onManageVideos = { navController.navigate(Screen.AdminVideos.route) },
                onBack = { navController.popBackStack() }
            )
        }

        // Admin - Manage Users
        composable(
            route = Screen.AdminUsers.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
        ) {
            val adminViewModel: AdminViewModel = hiltViewModel()
            AdminUsersScreen(
                adminViewModel = adminViewModel,
                onAddUser = { navController.navigate(Screen.AdminAddUser.route) },
                onBack = { navController.popBackStack() }
            )
        }

        // Admin - Add User
        composable(
            route = Screen.AdminAddUser.route,
            enterTransition = { slideInVertically(initialOffsetY = { it }) + fadeIn() },
            exitTransition = { slideOutVertically(targetOffsetY = { it }) + fadeOut() }
        ) {
            val adminViewModel: AdminViewModel = hiltViewModel()
            AdminAddUserScreen(
                adminViewModel = adminViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // Admin - Manage Videos
        composable(
            route = Screen.AdminVideos.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
        ) {
            val adminViewModel: AdminViewModel = hiltViewModel()
            AdminVideosScreen(
                adminViewModel = adminViewModel,
                onAddVideo = { navController.navigate(Screen.AdminAddVideo.route) },
                onBack = { navController.popBackStack() }
            )
        }

        // Admin - Add Video
        composable(
            route = Screen.AdminAddVideo.route,
            enterTransition = { slideInVertically(initialOffsetY = { it }) + fadeIn() },
            exitTransition = { slideOutVertically(targetOffsetY = { it }) + fadeOut() }
        ) {
            val adminViewModel: AdminViewModel = hiltViewModel()
            AdminAddVideoScreen(
                adminViewModel = adminViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
