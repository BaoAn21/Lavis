package com.sim.lavis.ui.components

import android.graphics.BitmapFactory
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.sim.lavis.ui.theme.coverGradient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Thumbnails decoded once per session; `missing` remembers songs without embedded art. */
private object ThumbnailCache {
    val bitmaps = LruCache<String, ImageBitmap>(120)
    val missing: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()
}

private const val THUMB_PX = 256

/** The embedded cover of the audio file at [contentUri] (via MediaStore), or null if it has none. */
@Composable
fun rememberSongThumbnail(contentUri: String): ImageBitmap? {
    val context = LocalContext.current
    val bitmap by produceState(ThumbnailCache.bitmaps.get(contentUri), contentUri) {
        if (value != null || contentUri in ThumbnailCache.missing) return@produceState
        value = withContext(Dispatchers.IO) {
            try {
                context.contentResolver
                    .loadThumbnail(contentUri.toUri(), Size(THUMB_PX, THUMB_PX), null)
                    .asImageBitmap()
                    .also { ThumbnailCache.bitmaps.put(contentUri, it) }
            } catch (e: Exception) {
                ThumbnailCache.missing += contentUri
                null
            }
        }
    }
    return bitmap
}

/** Recently decoded player covers, keyed by content hash; lets the full player open without a flash. */
private val decodedArtwork = LruCache<Int, ImageBitmap>(6)

/** Decodes raw cover bytes (as extracted by the player) off the main thread. */
@Composable
fun rememberDecodedArtwork(bytes: ByteArray?): ImageBitmap? {
    val key = remember(bytes) { bytes?.contentHashCode() }
    val bitmap by produceState(key?.let { decodedArtwork.get(it) }, key) {
        val data = bytes
        if (key == null || data == null) {
            value = null
            return@produceState
        }
        value = decodedArtwork.get(key) ?: withContext(Dispatchers.Default) {
            BitmapFactory.decodeByteArray(data, 0, data.size)?.asImageBitmap()
        }?.also { decodedArtwork.put(key, it) }
    }
    return bitmap
}

/**
 * A square cover: [image] if there is one, otherwise a gradient derived from [key]
 * with a music-note glyph, so every song/playlist still looks intentional.
 */
@Composable
fun CoverArt(
    image: ImageBitmap?,
    key: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: Shape = RoundedCornerShape(10.dp),
    iconFraction: Float = 0.42f
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(coverGradient(key)),
        contentAlignment = Alignment.Center
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = LavisIcons.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxSize(iconFraction)
            )
        }
    }
}

/** Cover for a song row: the file's own art when available. */
@Composable
fun SongCover(contentUri: String, key: String, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    CoverArt(image = rememberSongThumbnail(contentUri), key = key, modifier = modifier, size = size)
}

/** Round gradient avatar with initials, for singers. */
@Composable
fun InitialsAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val initials = name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(coverGradient(name)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            style = TextStyle(fontSize = (size.value * 0.36f).sp, fontWeight = FontWeight.Bold)
        )
    }
}
