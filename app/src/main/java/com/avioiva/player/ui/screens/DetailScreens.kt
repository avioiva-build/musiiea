package com.avioiva.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avioiva.player.data.Album
import com.avioiva.player.data.Folder
import com.avioiva.player.data.Playlist
import com.avioiva.player.data.Track
import com.avioiva.player.ui.PlayerViewModel
import com.avioiva.player.ui.components.Artwork
import com.avioiva.player.ui.components.EmptyState
import com.avioiva.player.ui.components.TrackRow
import java.util.Locale

/** Shared header + list scaffolding for all detail screens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScaffold(
    vm: PlayerViewModel,
    title: String,
    subtitle: String,
    artworkTrack: Track?,
    tracks: List<Track>,
    sortOptions: List<String>,
    sortMode: Int,
    onSortSelected: (Int) -> Unit,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onPlayTrack: (Int) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onRemoveFromList: ((Track) -> Unit)? = null,
    headerExtra: @Composable () -> Unit = {},
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            SortMenu(sortOptions = sortOptions, sortMode = sortMode, onSelected = onSortSelected)
        }

        if (tracks.isEmpty()) {
            EmptyState("This list is empty")
            return@Column
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            artworkTrack?.let { track ->
                Artwork(
                    track = track,
                    modifier = Modifier.width(88.dp).aspectRatio(1f),
                    cornerRadius = 10.dp,
                )
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                headerExtra()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onPlay) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Play")
                    }
                    FilledTonalButton(onClick = onShuffle) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Shuffle")
                    }
                }
            }
        }

        val currentId by vm.connection.currentId.collectAsStateWithLifecycle()
        val isPlaying by vm.connection.isPlaying.collectAsStateWithLifecycle()

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                TrackRow(
                    track = track,
                    isCurrent = track.id.toString() == currentId,
                    isPlaying = isPlaying,
                    onClick = { onPlayTrack(index) },
                    onPlayNext = { vm.playNext(track) },
                    onAddToQueue = { vm.addToQueue(track) },
                    onAddToPlaylist = onAddToPlaylist,
                    onRemove = onRemoveFromList?.let { callback -> { callback(track) } },
                )
            }
        }
    }
}

@Composable
private fun SortMenu(sortOptions: List<String>, sortMode: Int, onSelected: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Text(
                text = sortOptions.getOrElse(sortMode) { sortOptions.first() },
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Icon(
            Icons.Rounded.ArrowDropDown,
            contentDescription = "Sort",
            modifier = Modifier.align(Alignment.BottomEnd),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            sortOptions.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(if (index == sortMode) "✓ $option" else option) },
                    onClick = {
                        onSelected(index)
                        open = false
                    },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Public detail screens
// ---------------------------------------------------------------------------

@Composable
fun AlbumDetailScreen(
    album: Album,
    vm: PlayerViewModel,
    onBack: () -> Unit,
    onAddToPlaylist: (Track) -> Unit,
) {
    var sortMode by rememberSaveable { mutableIntStateOf(0) }
    val sorted = remember(album, sortMode) {
        when (sortMode) {
            1 -> album.tracks.sortedBy { it.title.lowercase() }
            2 -> album.tracks.sortedByDescending { it.durationMs }
            else -> album.tracks // original track order
        }
    }
    DetailScaffold(
        vm = vm,
        title = album.name,
        subtitle = listOfNotNull(
            album.artist,
            album.year.takeIf { it > 0 }?.toString(),
            "${album.trackCount} songs",
        ).joinToString("  ·  "),
        artworkTrack = album.firstTrack,
        tracks = sorted,
        sortOptions = listOf("Track order", "Title", "Duration"),
        sortMode = sortMode,
        onSortSelected = { sortMode = it },
        onBack = onBack,
        onPlay = { vm.play(sorted) },
        onShuffle = { vm.playShuffled(sorted) },
        onPlayTrack = { index -> vm.play(sorted, index) },
        onAddToPlaylist = onAddToPlaylist,
    )
}

@Composable
fun FolderDetailScreen(
    folder: Folder,
    vm: PlayerViewModel,
    onBack: () -> Unit,
    onAddToPlaylist: (Track) -> Unit,
) {
    var sortMode by rememberSaveable { mutableIntStateOf(0) }
    val sorted = remember(folder, sortMode) {
        when (sortMode) {
            1 -> folder.tracks.sortedByDescending { it.durationMs }
            2 -> folder.tracks.sortedBy { it.artist.lowercase() }
            else -> folder.tracks // by title
        }
    }
    DetailScaffold(
        vm = vm,
        title = folder.name,
        subtitle = "${folder.path}  ·  ${folder.trackCount} songs",
        artworkTrack = folder.tracks.firstOrNull(),
        tracks = sorted,
        sortOptions = listOf("Title", "Duration", "Artist"),
        sortMode = sortMode,
        onSortSelected = { sortMode = it },
        onBack = onBack,
        onPlay = { vm.play(sorted) },
        onShuffle = { vm.playShuffled(sorted) },
        onPlayTrack = { index -> vm.play(sorted, index) },
        onAddToPlaylist = onAddToPlaylist,
    )
}

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    vm: PlayerViewModel,
    onBack: () -> Unit,
    onAddToPlaylist: (Track) -> Unit,
) {
    val allPlaylists by vm.playlists.playlists.collectAsStateWithLifecycle()
    val playlist = allPlaylists.firstOrNull { it.id == playlistId }

    if (playlist == null) {
        // Deleted while open - close the screen.
        BackHandler(onBack = onBack)
        EmptyState("Playlist not found")
        return
    }

    val tracks = remember(playlist, vm) { vm.resolve(playlist) }

    DetailScaffold(
        vm = vm,
        title = playlist.name,
        subtitle = "${tracks.size} songs  ·  playlist",
        artworkTrack = tracks.firstOrNull(),
        tracks = tracks,
        sortOptions = listOf("Added order"),
        sortMode = 0,
        onSortSelected = {},
        onBack = onBack,
        onPlay = { vm.play(tracks) },
        onShuffle = { vm.playShuffled(tracks) },
        onPlayTrack = { index -> vm.play(tracks, index) },
        onAddToPlaylist = onAddToPlaylist,
        onRemoveFromList = { track -> vm.playlists.removeTrack(playlist.id, track.id) },
        headerExtra = {
            Text(
                text = "Tap + from any song menu to add more",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        },
    )
}

@Composable
fun AllSongsScreen(
    vm: PlayerViewModel,
    onBack: () -> Unit,
    onAddToPlaylist: (Track) -> Unit,
) {
    val tracks by vm.tracks.collectAsStateWithLifecycle()
    var sortMode by rememberSaveable { mutableIntStateOf(0) }

    val sorted = remember(tracks, sortMode) {
        when (sortMode) {
            1 -> tracks.sortedBy { it.artist.lowercase() }
            2 -> tracks.sortedBy { it.album.lowercase() }
            3 -> tracks.sortedByDescending { it.durationMs }
            else -> tracks.sortedBy { it.title.lowercase() }
        }
    }

    DetailScaffold(
        vm = vm,
        title = "All songs",
        subtitle = String.format(Locale.US, "%d songs on this device", tracks.size),
        artworkTrack = null,
        tracks = sorted,
        sortOptions = listOf("Title", "Artist", "Album", "Duration"),
        sortMode = sortMode,
        onSortSelected = { sortMode = it },
        onBack = onBack,
        onPlay = { vm.play(sorted) },
        onShuffle = { vm.playShuffled(sorted) },
        onPlayTrack = { index -> vm.play(sorted, index) },
        onAddToPlaylist = onAddToPlaylist,
    )
}
