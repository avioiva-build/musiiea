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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avioiva.player.playback.PlayerConnection
import com.avioiva.player.ui.PlayerViewModel
import com.avioiva.player.ui.components.Artwork
import com.avioiva.player.util.Format
import androidx.media3.common.Player

/**
 * Full-screen "Now Playing": artwork, seek bar, transport controls
 * (shuffle / prev / play / next / repeat-one), volume slider, sleep timer
 * and the "Up next" queue sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(vm: PlayerViewModel, onClose: () -> Unit) {
    val connection = vm.connection

    val isPlaying by connection.isPlaying.collectAsStateWithLifecycle()
    val title by connection.currentTitle.collectAsStateWithLifecycle()
    val artist by connection.currentArtist.collectAsStateWithLifecycle()
    val durationMs by connection.durationMs.collectAsStateWithLifecycle()
    val positionMs by connection.positionMs.collectAsStateWithLifecycle()
    val shuffle by connection.shuffleEnabled.collectAsStateWithLifecycle()
    val repeatMode by connection.repeatMode.collectAsStateWithLifecycle()
    val volume by connection.volume.collectAsStateWithLifecycle()
    val queue by connection.queue.collectAsStateWithLifecycle()
    val queueIndex by connection.queueIndex.collectAsStateWithLifecycle()
    val currentTrack by vm.currentTrack.collectAsStateWithLifecycle()
    val timerState by vm.sleepTimer.state.collectAsStateWithLifecycle()

    var queueOpen by remember { mutableStateOf(false) }
    var sleepDialogOpen by remember { mutableStateOf(false) }
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
        // ---- header row ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Close")
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = if (timerState.active) {
                    "Sleep in ${Format.time(timerState.remainingMs)}"
                } else {
                    "Now playing"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { queueOpen = true }) {
                Icon(Icons.Rounded.QueueMusic, contentDescription = "Queue")
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---- artwork ----
        Artwork(
            track = currentTrack,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            cornerRadius = 24.dp,
        )

        Spacer(Modifier.height(24.dp))

        // ---- titles ----
        Text(
            text = title.ifEmpty { "Nothing playing" },
            style = MaterialTheme.typography.headlineSmall,
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

        Spacer(Modifier.height(20.dp))

        // ---- seek bar ----
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

        // ---- transport controls ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { connection.setShuffle(!shuffle) }) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (shuffle) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { connection.seekPrevious() }) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    modifier = Modifier.size(36.dp),
                )
            }
            FilledIconButton(
                onClick = { vm.togglePlayPause() },
                modifier = Modifier.size(72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(36.dp),
                )
            }
            IconButton(onClick = { connection.seekNext() }) {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    modifier = Modifier.size(36.dp),
                )
            }
            IconButton(onClick = { connection.cycleRepeatMode() }) {
                Icon(
                    if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne
                    else Icons.Rounded.Repeat,
                    contentDescription = "Repeat",
                    tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---- volume ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.VolumeDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = volume.coerceIn(0f, 1f),
                onValueChange = { connection.setVolume(it) },
                valueRange = 0f..1f,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            )
            Icon(
                Icons.Rounded.VolumeUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(4.dp))

        // ---- sleep timer ----
        TextButton(onClick = { sleepDialogOpen = true }) {
            Icon(
                if (timerState.active) Icons.Rounded.TimerOff else Icons.Rounded.Timer,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (timerState.active) {
                    "Stop sleep timer (${Format.time(timerState.remainingMs)} left)"
                } else {
                    "Sleep timer"
                }
            )
        }
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
