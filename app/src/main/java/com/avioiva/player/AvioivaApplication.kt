package com.avioiva.player

import android.app.Application
import com.avioiva.player.data.PlaylistStore
import com.avioiva.player.playback.EqualizerManager
import com.avioiva.player.playback.PlayerConnection
import com.avioiva.player.playback.SleepTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Holds the app-wide singletons. Everything is lazy: the playback service and
 * its controller are only created when the user actually starts interacting,
 * which keeps cold start fast and the memory footprint small.
 */
class AvioivaApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val playlistStore: PlaylistStore by lazy { PlaylistStore(this, applicationScope) }
    val equalizerManager: EqualizerManager by lazy { EqualizerManager(this) }
    val sleepTimer: SleepTimer by lazy { SleepTimer(applicationScope) }
    val playerConnection: PlayerConnection by lazy {
        PlayerConnection(this, sleepTimer)
    }
}
