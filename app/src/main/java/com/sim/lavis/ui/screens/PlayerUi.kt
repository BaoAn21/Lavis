package com.sim.lavis.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.sim.lavis.playback.PlayerManager
import com.sim.lavis.ui.components.CoverArt
import com.sim.lavis.ui.components.LavisIcons
import com.sim.lavis.ui.components.formatTime
import com.sim.lavis.ui.components.rememberDecodedArtwork
import com.sim.lavis.ui.theme.TextFaint
import com.sim.lavis.ui.theme.TextMuted
import com.sim.lavis.ui.theme.coverAccent

/** Floating card above the tab bar; tap to open the full player. */
@Composable
fun MiniPlayer(playerManager: PlayerManager, onOpen: () -> Unit) {
    val state by playerManager.state.collectAsState()
    val artwork = rememberDecodedArtwork(state.artwork)
    val fraction = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f

    AnimatedVisibility(
        visible = state.currentSongId != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable(onClick = onOpen)
            ) {
                Row(
                    modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CoverArt(image = artwork, key = state.title, size = 44.dp, shape = RoundedCornerShape(10.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = state.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = state.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = playerManager::togglePlayPause) {
                        Icon(
                            imageVector = if (state.isPlaying) LavisIcons.Pause else LavisIcons.Play,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(onClick = playerManager::next) {
                        Icon(
                            imageVector = LavisIcons.SkipNext,
                            contentDescription = "Next",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                // Thin progress line along the bottom edge of the card.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(playerManager: PlayerManager, onBack: () -> Unit) {
    val state by playerManager.state.collectAsState()
    val artwork = rememberDecodedArtwork(state.artwork)
    val accent = coverAccent(state.title)

    // While dragging, the slider follows the finger; the seek is sent once on release,
    // instead of flooding the player with a seek per pixel.
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val playedFraction = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f
    val shownFraction = dragFraction ?: playedFraction
    val coverScale by animateFloatAsState(
        targetValue = if (state.isPlaying) 1f else 0.88f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "coverScale"
    )

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Backdrop: the cover blown up and blurred, or a soft glow in the song's accent color.
        if (artwork != null) {
            Image(
                bitmap = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(80.dp)
                    .alpha(0.55f)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.45f), Color.Transparent)))
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                        1f to MaterialTheme.colorScheme.background
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(LavisIcons.ExpandMore, contentDescription = "Close player", modifier = Modifier.size(32.dp))
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    if (state.queueSize > 0) {
                        Text(
                            text = "${state.queueIndex + 1} of ${state.queueSize}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(Modifier.width(48.dp))
            }

            // The cover is the largest square that fits the space left over, so the controls
            // below always stay on screen whatever the phone's aspect ratio.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                CoverArt(
                    image = artwork,
                    key = state.title,
                    size = Dp.Unspecified,
                    shape = RoundedCornerShape(24.dp),
                    iconFraction = 0.3f,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .scale(coverScale)
                        .shadow(32.dp, RoundedCornerShape(24.dp), ambientColor = accent, spotColor = accent)
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = state.title.ifEmpty { "Nothing playing" },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Text(
                    text = state.subtitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Slider(
                value = shownFraction.coerceIn(0f, 1f),
                onValueChange = { dragFraction = it },
                onValueChangeFinished = {
                    dragFraction?.let { playerManager.seekToFraction(it) }
                    dragFraction = null
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                ),
                thumb = {
                    Box(
                        Modifier
                            .size(14.dp)
                            .shadow(4.dp, CircleShape)
                            .background(Color.White, CircleShape)
                    )
                },
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        modifier = Modifier.height(4.dp),
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                        ),
                        thumbTrackGapSize = 0.dp,
                        drawStopIndicator = null
                    )
                }
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    formatTime((shownFraction * state.durationMs).toLong()),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
                Text(formatTime(state.durationMs), style = MaterialTheme.typography.labelMedium, color = TextMuted)
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModeButton(
                    icon = LavisIcons.Shuffle,
                    active = state.shuffle,
                    description = if (state.shuffle) "Shuffle on" else "Shuffle off",
                    onClick = playerManager::toggleShuffle
                )
                IconButton(onClick = playerManager::previous, modifier = Modifier.size(56.dp)) {
                    Icon(LavisIcons.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp))
                }
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .shadow(20.dp, CircleShape, ambientColor = MaterialTheme.colorScheme.primary, spotColor = MaterialTheme.colorScheme.primary)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = playerManager::togglePlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) LavisIcons.Pause else LavisIcons.Play,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }
                IconButton(onClick = playerManager::next, modifier = Modifier.size(56.dp)) {
                    Icon(LavisIcons.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp))
                }
                ModeButton(
                    icon = if (state.repeatMode == Player.REPEAT_MODE_ONE) LavisIcons.RepeatOne else LavisIcons.Repeat,
                    active = state.repeatMode != Player.REPEAT_MODE_OFF,
                    description = when (state.repeatMode) {
                        Player.REPEAT_MODE_ALL -> "Repeat all"
                        Player.REPEAT_MODE_ONE -> "Repeat one"
                        else -> "Repeat off"
                    },
                    onClick = playerManager::cycleRepeat
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

/** Shuffle / repeat toggle: tinted with a dot underneath when on. */
@Composable
private fun ModeButton(icon: ImageVector, active: Boolean, description: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = if (active) MaterialTheme.colorScheme.primary else TextFaint
            )
        }
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
        )
    }
}
