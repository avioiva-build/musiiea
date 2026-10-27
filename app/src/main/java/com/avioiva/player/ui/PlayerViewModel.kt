package com.avioiva.player.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avioiva.player.AvioivaApplication
import com.avioiva.player.data.Album
import com.avioiva.player.data.Folder
import com.avioiva.player.data.Playlist
import com.avioiva.player.data.Track
import com.avioiva.player.playback.PlayerConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Single shared ViewModel: the media library, playlists and the playback
 * connection, all exposed as observable state for Compose.
 */
class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val avioiva = app as AvioivaApplication

    val connection: PlayerConnection = avioiva.playerConnection
    val playlists = avioiva.playlistStore
    val sleepTimer = avioiva.sleepTimer
    val equalizer = avioiva.equalizerManager

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    /** The Track matching what the player is currently playing (for artwork). */
    val currentTrack: StateFlow<Track?> =
        combine(_tracks, connection.currentId) { list, id ->
            id?.let { idString -> list.firstOrNull { it.id.toString() == idString } }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Tracks of a playlist resolved against the current library. */
    fun resolve(playlist: Playlist): List<Track> = tracksByIds(playlist.trackIds)

    fun tracksByIds(ids: List<Long>): List<Track> {
        val byId = _tracks.value.associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            _scanning.value = true
            val scanned = withContext(Dispatchers.IO) {
                com.avioiva.player.data.MusicLibrary.scan(getApplication())
            }
            _tracks.value = scanned
            _albums.value = com.avioiva.player.data.MusicLibrary.albums(scanned)
            _folders.value = com.avioiva.player.data.MusicLibrary.folders(scanned)
            _scanning.value = false
        }
    }

    // --------------------- playback convenience ---------------------

    fun play(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isNotEmpty()) connection.playQueue(tracks, startIndex)
    }

    fun playShuffled(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        connection.setShuffle(true)
        connection.playQueue(tracks, 0)
    }

    fun togglePlayPause() = connection.togglePlayPause()
    fun seekNext() = connection.seekNext()
    fun seekPrevious() = connection.seekPrevious()
    fun seekTo(positionMs: Long) = connection.seekTo(positionMs)
    fun seekToIndex(index: Int) = connection.seekToIndex(index)
    fun setShuffle(enabled: Boolean) = connection.setShuffle(enabled)
    fun cycleRepeatMode() = connection.cycleRepeatMode()
    fun setVolume(value: Float) = connection.setVolume(value)
    fun playNext(track: Track) = connection.playNext(track)
    fun addToQueue(track: Track) = connection.addToQueue(track)

    fun startSleepTimer(minutes: Int, fadeOut: Boolean) =
        connection.startSleepTimer(minutes * 60_000L, fadeOut)

    fun cancelSleepTimer() = connection.cancelSleepTimer()
}
