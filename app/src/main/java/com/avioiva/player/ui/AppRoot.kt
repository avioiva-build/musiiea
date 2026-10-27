package com.avioiva.player.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avioiva.player.data.Album
import com.avioiva.player.data.Folder
import com.avioiva.player.data.Playlist
import com.avioiva.player.data.Track
import com.avioiva.player.ui.components.AddToPlaylistDialog
import com.avioiva.player.ui.components.Artwork
import com.avioiva.player.ui.screens.AlbumsScreen
import com.avioiva.player.ui.screens.AllSongsScreen
import com.avioiva.player.ui.screens.AlbumDetailScreen
import com.avioiva.player.ui.screens.EqualizerScreen
import com.avioiva.player.ui.screens.FoldersScreen
import com.avioiva.player.ui.screens.FolderDetailScreen
import com.avioiva.player.ui.screens.NowPlayingScreen
import com.avioiva.player.ui.screens.PlaylistsScreen
import com.avioiva.player.ui.screens.PlaylistDetailScreen

private enum class Tab(val label: String) {
    Albums("Albums"),
    Folders("Folders"),
    Playlists("Playlists"),
    Equalizer("Equalizer"),
}

/** Detail screens pushed on top of the tabs. */
private sealed interface Detail {
    data class AlbumDetail(val album: Album) : Detail
    data class FolderDetail(val folder: Folder) : Detail
    data class PlaylistDetail(val playlistId: String) : Detail
    data object AllSongs : Detail
}

@Composable
fun AppRoot(vm: PlayerViewModel = viewModel()) {
    val context = LocalContext.current

    // ---------------- permission gate ----------------
    var granted by remember {
        mutableStateOf(hasAudioPermission(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        granted = hasAudioPermission(context)
        if (granted) vm.refreshLibrary()
    }

    LaunchedEffect(Unit) {
        if (granted) {
            vm.refreshLibrary()
        } else {
            permissionLauncher.launch(neededPermissions())
        }
    }

    if (!granted) {
        PermissionScreen(onGrant = { permissionLauncher.launch(neededPermissions()) })
        return
    }

    // ---------------- main chrome ----------------
    var tab by rememberSaveable { mutableStateOf(Tab.Albums) }
    var detail by remember { mutableStateOf<Detail?>(null) }
    var nowPlayingOpen by remember { mutableStateOf(false) }
    var addToPlaylistTrack by remember { mutableStateOf<Track?>(null) }

    val playlists by vm.playlists.playlists.collectAsStateWithLifecycle()

    fun openDetail(d: Detail?) { detail = d }

    Scaffold(
        bottomBar = {
            Column {
                MiniPlayerBar(vm = vm, onOpen = { nowPlayingOpen = true })
                NavigationBar {
                    Tab.entries.forEach { current ->
                        NavigationBarItem(
                            selected = tab == current,
                            onClick = {
                                tab = current
                                detail = null
                            },
                            icon = {
                                Icon(
                                    imageVector = when (current) {
                                        Tab.Albums -> Icons.Rounded.Album
                                        Tab.Folders -> Icons.Rounded.Folder
                                        Tab.Playlists -> Icons.Rounded.PlaylistPlay
                                        Tab.Equalizer -> Icons.Rounded.Equalizer
                                    },
                                    contentDescription = current.label,
                                )
                            },
                            label = { Text(current.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (tab) {
                Tab.Albums -> AlbumsScreen(
                    vm = vm,
                    onOpenAllSongs = { openDetail(Detail.AllSongs) },
                    onOpenAlbum = { openDetail(Detail.AlbumDetail(it)) },
                )
                Tab.Folders -> FoldersScreen(
                    vm = vm,
                    onOpenFolder = { openDetail(Detail.FolderDetail(it)) },
                )
                Tab.Playlists -> PlaylistsScreen(
                    vm = vm,
                    onOpenPlaylist = { openDetail(Detail.PlaylistDetail(it.id)) },
                )
                Tab.Equalizer -> EqualizerScreen(vm = vm)
            }

            // Detail overlays sit on top of the tab content.
            detail?.let { d ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    when (d) {
                        is Detail.AlbumDetail -> AlbumDetailScreen(
                            album = d.album,
                            vm = vm,
                            onBack = { openDetail(null) },
                            onAddToPlaylist = { addToPlaylistTrack = it },
                        )
                        is Detail.FolderDetail -> FolderDetailScreen(
                            folder = d.folder,
                            vm = vm,
                            onBack = { openDetail(null) },
                            onAddToPlaylist = { addToPlaylistTrack = it },
                        )
                        is Detail.PlaylistDetail -> PlaylistDetailScreen(
                            playlistId = d.playlistId,
                            vm = vm,
                            onBack = { openDetail(null) },
                            onAddToPlaylist = { addToPlaylistTrack = it },
                        )
                        Detail.AllSongs -> AllSongsScreen(
                            vm = vm,
                            onBack = { openDetail(null) },
                            onAddToPlaylist = { addToPlaylistTrack = it },
                        )
                    }
                }
            }
        }
    }

    // ---------------- now playing overlay ----------------
    AnimatedVisibility(
        visible = nowPlayingOpen,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
        NowPlayingScreen(vm = vm, onClose = { nowPlayingOpen = false })
    }

    // ---------------- global dialogs ----------------
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

@Composable
private fun MiniPlayerBar(vm: PlayerViewModel, onOpen: () -> Unit) {
    val queue by vm.connection.queue.collectAsStateWithLifecycle()
    val isPlaying by vm.connection.isPlaying.collectAsStateWithLifecycle()
    val title by vm.connection.currentTitle.collectAsStateWithLifecycle()
    val artist by vm.connection.currentArtist.collectAsStateWithLifecycle()
    val currentTrack by vm.currentTrack.collectAsStateWithLifecycle()

    if (queue.isEmpty()) return

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onOpen)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(track = currentTrack, modifier = Modifier.size(44.dp), cornerRadius = 6.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title.ifEmpty { "Nothing playing" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = { vm.togglePlayPause() }) {
                Icon(
                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                )
            }
            IconButton(onClick = { vm.seekNext() }) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Next")
            }
        }
    }
}

@Composable
private fun PermissionScreen(onGrant: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "AVIOIVA Player needs your music library",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Grant access to audio files so AVIOIVA can list and play the " +
                    "songs stored on this device. Everything stays offline - " +
                    "the app has no internet permission at all.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            androidx.compose.material3.Button(onClick = onGrant) {
                Text("Grant access")
            }
        }
    }
}

// ---------------- helpers ----------------

private fun hasAudioPermission(context: android.content.Context): Boolean =
    neededPermissions().any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

private fun neededPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
