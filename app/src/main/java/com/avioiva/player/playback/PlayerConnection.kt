package com.avioiva.player.playback

import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.avioiva.player.data.MusicLibrary
import com.avioiva.player.data.Track
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Bridge between the UI and the playback service, built on a Media3
 * [MediaController]. Exposes the player state as StateFlows so Compose can
 * observe it directly, and provides all playback commands.
 *
 * MediaController is documented thread-safe, so it is also safe to call its
 * methods from the sleep-timer coroutine.
 */
class PlayerConnection(
    context: Context,
    private val sleepTimer: SleepTimer,
) {

    data class QueueEntry(
        val mediaId: String,
        val title: String,
        val artist: String,
    )

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controller: MediaController? = null
    private var tickerJob: Job? = null

    // --------------------- observable player state ---------------------

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentId = MutableStateFlow<String?>(null)
    val currentId: StateFlow<String?> = _currentId.asStateFlow()

    private val _currentTitle = MutableStateFlow("")
    val currentTitle: StateFlow<String> = _currentTitle.asStateFlow()

    private val _currentArtist = MutableStateFlow("")
    val currentArtist: StateFlow<String> = _currentArtist.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _queue = MutableStateFlow<List<QueueEntry>>(emptyList())
    val queue: StateFlow<List<QueueEntry>> = _queue.asStateFlow()

    private val _queueIndex = MutableStateFlow(-1)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    init {
        val future = MediaController.Builder(
            appContext,
            SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java)),
        ).buildAsync()

        Futures.addCallback(
            future,
            object : FutureCallback<MediaController> {
                override fun onSuccess(result: MediaController) {
                    controller = result
                    result.addListener(playerListener, MoreExecutors.directExecutor())
                    refreshFrom(result)
                    startTicker()
                }

                override fun onFailure(t: Throwable) {
                    controller = null
                }
            },
            MoreExecutors.directExecutor(),
        )
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            refreshFrom(player)
        }
    }

    private fun refreshFrom(player: Player) {
        _isPlaying.value = player.isPlaying
        _currentId.value = player.currentMediaItem?.mediaId
        _currentTitle.value = player.mediaMetadata.title?.toString().orEmpty()
        _currentArtist.value = player.mediaMetadata.artist?.toString().orEmpty()
        _durationMs.value = player.duration.takeIf { it > 0 } ?: 0L
        _positionMs.value = player.currentPosition.coerceAtLeast(0L)
        _shuffleEnabled.value = player.shuffleModeEnabled
        _repeatMode.value = player.repeatMode
        _volume.value = player.volume

        val entries = ArrayList<QueueEntry>(player.mediaItemCount)
        for (i in 0 until player.mediaItemCount) {
            val item = player.getMediaItemAt(i)
            entries += QueueEntry(
                mediaId = item.mediaId,
                title = item.mediaMetadata.title?.toString().orEmpty(),
                artist = item.mediaMetadata.artist?.toString().orEmpty(),
            )
        }
        _queue.value = entries
        _queueIndex.value = player.currentMediaItemIndex
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (true) {
                controller?.let { c ->
                    if (c.isPlaying) {
                        _positionMs.value = c.currentPosition.coerceAtLeast(0L)
                        val d = c.duration
                        if (d > 0) _durationMs.value = d
                    }
                }
                delay(250)
            }
        }
    }

    // --------------------- commands ---------------------

    fun playQueue(tracks: List<Track>, startIndex: Int = 0) {
        val c = controller ?: return
        if (tracks.isEmpty()) return
        c.setMediaItems(tracks.map { it.toMediaItem() }, startIndex.coerceIn(0, tracks.size - 1), 0L)
        c.prepare()
        c.play()
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun seekNext() { controller?.seekToNextMediaItem() }
    fun seekPrevious() { controller?.seekToPreviousMediaItem() }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun seekToIndex(index: Int, positionMs: Long = 0L) {
        controller?.seekTo(index, positionMs)
    }

    fun setShuffle(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
    }

    /** OFF -> ALL -> ONE -> OFF */
    fun cycleRepeatMode() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setVolume(value: Float) {
        controller?.volume = value.coerceIn(0f, 1f)
    }

    /** Insert [track] right after the current one ("Play next"). */
    fun playNext(track: Track) {
        val c = controller ?: return
        c.addMediaItems(c.currentMediaItemIndex + 1, listOf(track.toMediaItem()))
    }

    /** Append [track] to the end of the queue ("Add to queue"). */
    fun addToQueue(track: Track) {
        controller?.addMediaItem(track.toMediaItem())
    }

    // --------------------- sleep timer ---------------------

    fun startSleepTimer(durationMs: Long, fadeOut: Boolean) {
        sleepTimer.start(
            durationMs = durationMs,
            fadeOut = fadeOut,
            onFadeStart = { fadeOutVolume(SleepTimer.FADE_MS) },
            onExpire = { pauseAndRestoreVolume() },
        )
    }

    fun cancelSleepTimer() = sleepTimer.cancel()

    private var savedVolume = -1f

    private fun fadeOutVolume(fadeMs: Long) {
        val c = controller ?: return
        savedVolume = c.volume
        scope.launch {
            val start = System.currentTimeMillis()
            while (true) {
                val t = (System.currentTimeMillis() - start).toFloat() / fadeMs
                if (t >= 1f) break
                c.volume = savedVolume * (1f - t)
                delay(100)
            }
        }
    }

    private fun pauseAndRestoreVolume() {
        controller?.pause()
        if (savedVolume >= 0f) controller?.volume = savedVolume
        savedVolume = -1f
    }

    // --------------------- helpers ---------------------

    private fun Track.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(MusicLibrary.contentUri(id))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .build()
            )
            .build()
}
