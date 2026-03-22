package com.stockmarket.tutorials.ui.navigation

/**
 * Navigation routes for the app.
 */
sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Dashboard : Screen("dashboard")
    data object VideoList : Screen("video_list/{category}") {
        fun createRoute(category: String) = "video_list/$category"
    }
    data object VideoPlayer : Screen("video_player/{videoId}") {
        fun createRoute(videoId: String) = "video_player/$videoId"
    }
    data object AdminPanel : Screen("admin_panel")
    data object AdminUsers : Screen("admin_users")
    data object AdminVideos : Screen("admin_videos")
    data object AdminAddUser : Screen("admin_add_user")
    data object AdminAddVideo : Screen("admin_add_video")
}
