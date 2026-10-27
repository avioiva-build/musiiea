package com.avioiva.player.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.avioiva.player.AvioivaApplication
import com.avioiva.player.MainActivity

/**
 * Foreground media playback service.
 *
 * Responsibilities:
 *  - owns the ExoPlayer instance (gapless album playback: the whole queue is
 *    prepared up front, so track transitions are pre-buffered and seamless,
 *    and MP3/FLAC encoder delay/padding is trimmed automatically)
 *  - publishes a MediaSession, which gives us the system media notification,
 *    lock-screen / quick-settings controls, and headset button handling
 *  - routes the audio session id to the [EqualizerManager]
 *
 * Music keeps playing when the app is backgrounded or the screen is locked.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as AvioivaApplication

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true) // pause when headphones are unplugged
            .setWakeMode(C.WAKE_MODE_LOCAL)    // keep the CPU awake with the screen off
            .build()

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                app.equalizerManager.attach(audioSessionId)
            }
        })
        // The session id may already be allocated before we subscribed.
        val sessionId = player.audioSessionId
        if (sessionId != C.AUDIO_SESSION_ID_UNSET) {
            app.equalizerManager.attach(sessionId)
        }

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        val shouldStop = player == null ||
            (player.playWhenReady.not() && player.mediaItemCount == 0) ||
            player.playbackState == Player.STATE_ENDED
        if (shouldStop && (player?.isPlaying != true)) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        (application as AvioivaApplication).equalizerManager.releaseEffects()
        super.onDestroy()
    }
}
