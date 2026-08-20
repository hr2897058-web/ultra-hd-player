package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.media.VideoItem
import com.example.ui.library.LibraryScreen
import com.example.ui.library.LibraryViewModel
import com.example.ui.player.PlayerScreen
import com.example.ui.theme.CyanNeon

sealed class Screen(val route: String) {
    object Library : Screen("library")
    object Player : Screen("player/{videoId}") {
        fun createRoute(videoId: Long) = "player/$videoId"
    }
}

@Composable
fun UltraPlayerNavGraph(
    navController: NavHostController = rememberNavController(),
    libraryViewModel: LibraryViewModel = viewModel(),
    onVideoSelected: (VideoItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedVideoForPlayback by remember { mutableStateOf<VideoItem?>(null) }

    NavHost(
        navController = navController,
        startDestination = Screen.Library.route,
        modifier = modifier
    ) {
        composable(Screen.Library.route) {
            LibraryScreen(
                viewModel = libraryViewModel,
                onVideoClick = { video ->
                    selectedVideoForPlayback = video
                    onVideoSelected(video)
                    navController.navigate(Screen.Player.createRoute(video.id))
                }
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(navArgument("videoId") { type = NavType.LongType })
        ) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getLong("videoId") ?: -1L
            var loadedVideo by remember { mutableStateOf(selectedVideoForPlayback) }

            LaunchedEffect(videoId) {
                if (loadedVideo == null || loadedVideo?.id != videoId) {
                    val found = libraryViewModel.findVideoById(videoId)
                    if (found != null) {
                        loadedVideo = found
                    }
                }
            }

            val currentVideo = loadedVideo
            if (currentVideo != null) {
                PlayerScreen(
                    video = currentVideo,
                    onBack = { navController.popBackStack() }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CyanNeon)
                }
            }
        }
    }
}
