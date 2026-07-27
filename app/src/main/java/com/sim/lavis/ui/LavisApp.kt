package com.sim.lavis.ui

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sim.lavis.playback.PlayerManager
import com.sim.lavis.ui.screens.DownloadScreen
import com.sim.lavis.ui.screens.MiniPlayer
import com.sim.lavis.ui.screens.NowPlayingScreen
import com.sim.lavis.ui.screens.PlaylistDetailScreen
import com.sim.lavis.ui.screens.PlaylistsScreen
import com.sim.lavis.ui.screens.SingerDetailScreen
import com.sim.lavis.ui.screens.SingersScreen
import com.sim.lavis.ui.screens.WrappedScreen
import com.sim.lavis.ui.theme.TermGray

private data class Tab(val route: String, val label: String)

private val tabs = listOf(
    Tab("playlists", "[plst]"),
    Tab("singers", "[sngr]"),
    Tab("download", "[dnld]"),
    Tab("wrapped", "[wrap]")
)

@Composable
fun LavisApp(playerManager: PlayerManager) {
    val navController = rememberNavController()
    val libraryViewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory)
    val wrappedViewModel: WrappedViewModel = viewModel(factory = WrappedViewModel.Factory)
    val downloadViewModel: DownloadViewModel = viewModel(factory = DownloadViewModel.Factory)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(modifier = Modifier.navigationBarsPadding()) {
                MiniPlayer(playerManager) { navController.navigate("player") }
                BottomTabs(navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "playlists",
            modifier = Modifier.padding(innerPadding).fillMaxSize()
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
            composable("download") {
                DownloadScreen(downloadViewModel)
            }
            composable("wrapped") {
                WrappedScreen(wrappedViewModel)
            }
            composable("player") {
                NowPlayingScreen(playerManager) { navController.popBackStack() }
            }
        }
    }
}

@Composable
private fun BottomTabs(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEach { tab ->
            val selected = currentRoute == tab.route
            Text(
                text = tab.label,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) MaterialTheme.colorScheme.secondary else TermGray,
                modifier = Modifier.clickable {
                    navController.navigate(tab.route) {
                        popUpTo("playlists") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
