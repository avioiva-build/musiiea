package com.avioiva.player.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sleep timer: counts down while music plays and pauses playback when it
 * reaches zero. Supports a gentle volume fade during the final seconds -
 * great for falling asleep to music or audiobooks.
 *
 * The timer lives at application scope (not in the UI), so it keeps working
 * while the app is in the background, and it cooperates with the fade helpers
 * in [PlayerConnection].
 */
class SleepTimer(private val scope: CoroutineScope) {

    data class State(
        val remainingMs: Long = 0L,
        val totalMs: Long = 0L,
        val fadeOut: Boolean = true,
    ) {
        val active: Boolean get() = remainingMs > 0L
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var job: Job? = null

    fun start(
        durationMs: Long,
        fadeOut: Boolean,
        onFadeStart: () -> Unit,
        onExpire: () -> Unit,
    ) {
        cancel()
        if (durationMs <= 0L) return
        job = scope.launch {
            val endAt = System.currentTimeMillis() + durationMs
            var fading = false
            while (true) {
                val remaining = endAt - System.currentTimeMillis()
                if (remaining <= 0L) break
                _state.value = State(remainingMs = remaining, totalMs = durationMs, fadeOut = fadeOut)
                if (fadeOut && !fading && remaining <= FADE_MS) {
                    fading = true
                    try { onFadeStart() } catch (_: Throwable) {}
                }
                delay(250)
            }
            _state.value = State()
            try { onExpire() } catch (_: Throwable) {}
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _state.value = State()
    }

    companion object {
        /** Fade-out duration before the timer pauses playback. */
        const val FADE_MS = 5_000L
    }
}
