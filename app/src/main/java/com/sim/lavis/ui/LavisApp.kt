package com.sim.lavis.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sim.lavis.playback.PlayerManager
import com.sim.lavis.ui.components.LavisIcons
import com.sim.lavis.ui.screens.MiniPlayer
import com.sim.lavis.ui.screens.NowPlayingScreen
import com.sim.lavis.ui.screens.PlaylistDetailScreen
import com.sim.lavis.ui.screens.PlaylistsScreen
import com.sim.lavis.ui.screens.SingerDetailScreen
import com.sim.lavis.ui.screens.SingersScreen
import com.sim.lavis.ui.screens.WrappedScreen
import com.sim.lavis.ui.theme.TextFaint

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("playlists", "Library", LavisIcons.Library),
    Tab("singers", "Singers", LavisIcons.Person),
    Tab("wrapped", "Wrapped", LavisIcons.BarChart)
)

private const val PLAYER_ROUTE = "player"

@Composable
fun LavisApp(playerManager: PlayerManager) {
    val navController = rememberNavController()
    val libraryViewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory)
    val wrappedViewModel: WrappedViewModel = viewModel(factory = WrappedViewModel.Factory)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val onPlayer = backStackEntry?.destination?.route == PLAYER_ROUTE

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // Screens handle the status bar themselves so the player can draw behind it.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(
                visible = !onPlayer,
                enter = fadeIn(),
                exit = fadeOut(tween(120))
            ) {
                Column {
                    MiniPlayer(playerManager) { navController.navigate(PLAYER_ROUTE) }
                    BottomTabs(navController, backStackEntry?.destination?.route)
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "playlists",
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(180)) }
        ) {
            composable("playlists") {
                PlaylistsScreen(libraryViewModel) { playlist ->
                    navController.navigate("playlist/${Uri.encode(playlist)}")
                }
            }
            composable("playlist/{name}") { entry ->
                val name = Uri.decode(entry.arguments?.getString("name") ?: "")
                PlaylistDetailScreen(
                    playlistName = name,
                    viewModel = libraryViewModel,
                    playerManager = playerManager,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("singers") {
                SingersScreen(libraryViewModel) { id, name ->
                    navController.navigate("singer/$id/${Uri.encode(name)}")
                }
            }
            composable("singer/{id}/{name}") { entry ->
                val id = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                val name = Uri.decode(entry.arguments?.getString("name") ?: "")
                SingerDetailScreen(
                    singerId = id,
                    singerName = name,
                    viewModel = libraryViewModel,
                    playerManager = playerManager,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("wrapped") {
                WrappedScreen(wrappedViewModel)
            }
            composable(
                PLAYER_ROUTE,
                enterTransition = { slideInVertically(tween(320)) { it } + fadeIn(tween(200)) },
                exitTransition = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(200)) },
                popExitTransition = { slideOutVertically(tween(280)) { it } + fadeOut(tween(280)) }
            ) {
                NowPlayingScreen(playerManager) { navController.popBackStack() }
            }
        }
    }
}

@Composable
private fun BottomTabs(navController: NavHostController, currentRoute: String?) {
    // Detail pages keep their parent tab highlighted.
    val selectedTab = when {
        currentRoute == null -> null
        currentRoute.startsWith("playlist") -> "playlists"
        currentRoute.startsWith("singer") -> "singers"
        else -> currentRoute
    }
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab.route,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo("playlists") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = TextFaint,
                    unselectedTextColor = TextFaint
                )
            )
        }
    }
}
