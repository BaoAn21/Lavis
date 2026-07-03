package com.sim.lavis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.sim.lavis.playback.PlayerManager
import com.sim.lavis.ui.components.DimText
import com.sim.lavis.ui.components.asciiBar
import com.sim.lavis.ui.components.formatTime
import com.sim.lavis.ui.theme.TermAmber
import com.sim.lavis.ui.theme.TermCyan
import com.sim.lavis.ui.theme.TermGray
import com.sim.lavis.ui.theme.TermSurface

/** Compact bar above the tab row; tap to open the full player. */
@Composable
fun MiniPlayer(playerManager: PlayerManager, onOpen: () -> Unit) {
    val state by playerManager.state.collectAsState()
    if (state.currentSongId == null) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TermSurface)
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = (if (state.isPlaying) "▶ " else "‖ ") + state.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            DimText(state.subtitle, modifier = Modifier)
        }
        Text(
            text = "[|<]",
            style = MaterialTheme.typography.labelLarge,
            color = TermCyan,
            modifier = Modifier.clickable { playerManager.previous() }.padding(4.dp)
        )
        Text(
            text = if (state.isPlaying) "[||]" else "[>>]",
            style = MaterialTheme.typography.labelLarge,
            color = TermAmber,
            modifier = Modifier.clickable { playerManager.togglePlayPause() }.padding(4.dp)
        )
        Text(
            text = "[>|]",
            style = MaterialTheme.typography.labelLarge,
            color = TermCyan,
            modifier = Modifier.clickable { playerManager.next() }.padding(4.dp)
        )
    }
}

@Composable
fun NowPlayingScreen(playerManager: PlayerManager, onBack: () -> Unit) {
    val state by playerManager.state.collectAsState()
    val fraction = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            text = "[..] now-playing",
            style = MaterialTheme.typography.titleLarge,
            color = TermCyan,
            modifier = Modifier.padding(vertical = 12.dp).clickable(onClick = onBack)
        )

        Spacer(modifier = Modifier.height(48.dp))

        DimText("track ${state.queueIndex + 1}/${state.queueSize}")
        Text(
            text = state.title.ifEmpty { "nothing playing" },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = state.subtitle,
            style = MaterialTheme.typography.titleMedium,
            color = TermAmber,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = asciiBar(fraction, width = 24),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DimText(formatTime(state.positionMs))
            DimText(formatTime(state.durationMs))
        }
        Slider(
            value = fraction,
            onValueChange = { playerManager.seekToFraction(it) },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = TermSurface
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "[|<]",
                style = MaterialTheme.typography.headlineSmall,
                color = TermCyan,
                modifier = Modifier.clickable { playerManager.previous() }.padding(8.dp)
            )
            Text(
                text = if (state.isPlaying) "[ || ]" else "[ > ]",
                style = MaterialTheme.typography.headlineSmall,
                color = TermAmber,
                modifier = Modifier.clickable { playerManager.togglePlayPause() }.padding(8.dp)
            )
            Text(
                text = "[>|]",
                style = MaterialTheme.typography.headlineSmall,
                color = TermCyan,
                modifier = Modifier.clickable { playerManager.next() }.padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = if (state.shuffle) "[shuffle:on]" else "[shuffle:off]",
                style = MaterialTheme.typography.labelLarge,
                color = if (state.shuffle) TermAmber else TermGray,
                modifier = Modifier.clickable { playerManager.toggleShuffle() }.padding(8.dp)
            )
            val repeatLabel = when (state.repeatMode) {
                Player.REPEAT_MODE_ALL -> "[repeat:all]"
                Player.REPEAT_MODE_ONE -> "[repeat:one]"
                else -> "[repeat:off]"
            }
            Text(
                text = repeatLabel,
                style = MaterialTheme.typography.labelLarge,
                color = if (state.repeatMode != Player.REPEAT_MODE_OFF) TermAmber else TermGray,
                modifier = Modifier.clickable { playerManager.cycleRepeat() }.padding(8.dp)
            )
        }
    }
}
