package com.avioiva.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.avioiva.player.data.Track
import com.avioiva.player.ui.PlayerViewModel
import com.avioiva.player.ui.components.AddToPlaylistDialog
import com.avioiva.player.ui.components.Artwork
import com.avioiva.player.ui.components.Waveform
import com.avioiva.player.util.Format
import com.avioiva.player.util.YTMusic

/**
 * Full-screen "Now Playing", modelled on the user's reference design:
 * big rounded artwork, title/artist with a heart + 3-dot menu, a waveform
 * seek bar plus slider with times, rewind / play / forward controls and a
 * bottom row with the equalizer shortcut, a Lyrics pill and the queue.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    vm: PlayerViewModel,
    onClose: () -> Unit,
    onOpenEqualizer: () -> Unit = {},
) {
    val connection = vm.connection
    val context = LocalContext.current

    val isPlaying by connection.isPlaying.collectAsStateWithLifecycle()
    val title by connection.currentTitle.collectAsStateWithLifecycle()
    val artist by connection.currentArtist.collectAsStateWithLifecycle()
    val durationMs by connection.durationMs.collectAsStateWithLifecycle()
    val positionMs by connection.positionMs.collectAsStateWithLifecycle()
    val shuffle by connection.shuffleEnabled.collectAsStateWithLifecycle()
    val repeatMode by connection.repeatMode.collectAsStateWithLifecycle()
    val queue by connection.queue.collectAsStateWithLifecycle()
    val queueIndex by connection.queueIndex.collectAsStateWithLifecycle()
    val currentTrack by vm.currentTrack.collectAsStateWithLifecycle()
    val timerState by vm.sleepTimer.state.collectAsStateWithLifecycle()
    val playlists by vm.playlists.playlists.collectAsStateWithLifecycle()
    val favorites by vm.favorites.ids.collectAsStateWithLifecycle()

    val isFavorite = currentTrack?.id?.toString()?.let { favorites.contains(it) } == true

    var queueOpen by remember { mutableStateOf(false) }
    var lyricsOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var sleepDialogOpen by remember { mutableStateOf(false) }
    var addToPlaylistTrack by remember { mutableStateOf<Track?>(null) }
    var scrubPosition by remember { mutableFloatStateOf(-1f) }

    BackHandler(onBack = onClose)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ---- top row: minimize + sleep-timer status ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close")
            }
            Spacer(Modifier.weight(1f))
            if (timerState.active) {
                Text(
                    text = "Sleep in ${Format.time(timerState.remainingMs)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(48.dp))
        }

        // ---- artwork ----
        Artwork(
            track = currentTrack,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            cornerRadius = 28.dp,
        )

        Spacer(Modifier.height(24.dp))

        // ---- title / artist + heart + menu ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title.ifEmpty { "Nothing playing" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artist.ifEmpty { "—" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = { currentTrack?.let { vm.toggleFavorite(it.id.toString()) } },
                enabled = currentTrack != null,
            ) {
                Icon(
                    if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Add to playlist") },
                        onClick = {
                            addToPlaylistTrack = currentTrack
                            menuOpen = false
                        },
                        enabled = currentTrack != null,
                    )
                    DropdownMenuItem(
                        text = { Text(if (shuffle) "Shuffle: on" else "Shuffle: off") },
                        onClick = {
                            connection.setShuffle(!shuffle)
                            menuOpen = false
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                when (repeatMode) {
                                    Player.REPEAT_MODE_ALL -> "Repeat: all"
                                    Player.REPEAT_MODE_ONE -> "Repeat: one"
                                    else -> "Repeat: off"
                                }
                            )
                        },
                        onClick = {
                            connection.cycleRepeatMode()
                            menuOpen = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Sleep timer…") },
                        onClick = {
                            sleepDialogOpen = true
                            menuOpen = false
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // ---- waveform ----
        Waveform(
            seed = currentTrack?.id?.toString() ?: title,
            progress = if (durationMs > 0) {
                (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
            } else 0f,
            onSeek = { fraction ->
                if (durationMs > 0) connection.seekTo((fraction * durationMs).toLong())
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
        )

        Spacer(Modifier.height(4.dp))

        // ---- precise seek slider + times ----
        Slider(
            value = when {
                scrubPosition >= 0f -> scrubPosition
                durationMs > 0 -> positionMs.toFloat().coerceIn(0f, durationMs.toFloat())
                else -> 0f
            },
            onValueChange = { scrubPosition = it },
            onValueChangeFinished = {
                if (scrubPosition >= 0f) connection.seekTo(scrubPosition.toLong())
                scrubPosition = -1f
            },
            valueRange = 0f..(if (durationMs > 0) durationMs.toFloat() else 1f),
            enabled = durationMs > 0,
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = Format.time(if (scrubPosition >= 0f) scrubPosition.toLong() else positionMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = Format.time(durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(8.dp))

        // ---- transport controls: rewind / play / forward ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { connection.seekPrevious() }) {
                Icon(
                    Icons.Rounded.FastRewind,
                    contentDescription = "Previous",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp),
                )
            }
            FilledIconButton(
                onClick = { vm.togglePlayPause() },
                modifier = Modifier.size(76.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(40.dp),
                )
            }
            IconButton(onClick = { connection.seekNext() }) {
                Icon(
                    Icons.Rounded.FastForward,
                    contentDescription = "Next",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---- bottom row: equalizer / YT Music / lyrics / queue ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onOpenEqualizer) {
                Icon(
                    Icons.Rounded.Tune,
                    contentDescription = "Equalizer",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PillButton(
                icon = { Icon(Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp)) },
                label = "YT Music",
                enabled = title.isNotEmpty(),
                onClick = { YTMusic.openSearch(context, "$title $artist") },
            )
            PillButton(
                icon = {
                    Icon(
                        Icons.Rounded.FormatQuote,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                label = "Lyrics",
                onClick = { lyricsOpen = true },
            )
            IconButton(onClick = { queueOpen = true }) {
                Icon(
                    Icons.Rounded.QueueMusic,
                    contentDescription = "Queue",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
    }

    // ---- up-next queue sheet ----
    if (queueOpen) {
        ModalBottomSheet(onDismissRequest = { queueOpen = false }) {
            Text(
                text = "Up next  ·  ${queue.size} tracks",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            LazyColumn(modifier = Modifier.padding(bottom = 32.dp)) {
                itemsIndexed(queue, key = { index, entry -> "${entry.mediaId}-$index" }) { index, entry ->
                    val isCurrent = index == queueIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                connection.seekToIndex(index)
                            }
                            .background(
                                if (isCurrent) MaterialTheme.colorScheme.surfaceContainerHigh
                                else MaterialTheme.colorScheme.surface
                            )
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(28.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = entry.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }

    // ---- lyrics sheet ----
    if (lyricsOpen) {
        ModalBottomSheet(onDismissRequest = { lyricsOpen = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            ) {
                Text("Lyrics", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = title.ifEmpty { "Nothing playing" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artist.ifEmpty { "—" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "No lyrics are stored for this track.\n\n" +
                        "MUSIIEA works fully offline - it never downloads content " +
                        "or reads online lyrics databases.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    // ---- sleep timer dialog ----
    if (sleepDialogOpen) {
        SleepTimerDialog(
            active = timerState.active,
            onStart = { minutes, fade ->
                vm.startSleepTimer(minutes, fade)
                sleepDialogOpen = false
            },
            onCancelTimer = {
                vm.cancelSleepTimer()
                sleepDialogOpen = false
            },
            onDismiss = { sleepDialogOpen = false },
        )
    }

    // ---- add-to-playlist dialog ----
    addToPlaylistTrack?.let { track ->
        AddToPlaylistDialog(
            playlists = playlists,
            onAddTo = { vm.playlists.addTrack(it.id, track.id) },
            onCreateAndAdd = { name ->
                vm.playlists.create(name)
                // create() appends a new playlist; add the track to it.
                val fresh = vm.playlists.playlists.value.lastOrNull()
                if (fresh != null) vm.playlists.addTrack(fresh.id, track.id)
            },
            onDismiss = { addToPlaylistTrack = null },
        )
    }
}

/** Rounded "pill" action button (Lyrics / YT Music). */
@Composable
private fun PillButton(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (enabled) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
    ) {
        Row(
            modifier = Modifier
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SleepTimerDialog(
    active: Boolean,
    onStart: (minutes: Int, fadeOut: Boolean) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit,
) {
    var fadeOut by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep timer") },
        text = {
            Column {
                Text(
                    "Playback pauses automatically after:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                val rows = listOf(
                    listOf(5, 10, 15),
                    listOf(30, 45, 60),
                    listOf(90),
                )
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        row.forEach { minutes ->
                            OutlinedButton(
                                onClick = { onStart(minutes, fadeOut) },
                                shape = RoundedCornerShape(20.dp),
                            ) {
                                Text("$minutes min")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = fadeOut, onCheckedChange = { fadeOut = it })
                    Spacer(Modifier.width(12.dp))
                    Text("Fade out volume at the end")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            if (active) {
                TextButton(onClick = onCancelTimer) { Text("Stop timer") }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
