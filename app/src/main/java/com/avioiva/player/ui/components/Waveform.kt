package com.avioiva.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.sin
import kotlin.random.Random

/**
 * Music-style waveform seek bar, like the one on the reference design.
 *
 * The bar heights are pseudo-random but deterministic per [seed] (the track
 * id), so every track gets its own stable "fingerprint" waveform without
 * decoding any audio. Bars left of the playhead use [activeColor via
 * colorScheme.primary]; the rest are dimmed. Tap or drag anywhere on it to
 * seek - [onSeek] reports the new fraction 0..1 when the gesture ends.
 */
@Composable
fun Waveform(
    seed: String,
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    barCount: Int = 56,
) {
    val bars = remember(seed) {
        val rnd = Random(seed.hashCode())
        List(barCount) { i ->
            val envelope = 0.55f + 0.45f * sin(i / 3.1f)      // slow musical swell
            val jitter = 0.35f + 0.65f * rnd.nextFloat()      // per-bar variation
            (envelope * jitter).coerceIn(0.12f, 1f)
        }
    }

    // While a drag is in flight we show the dragged fraction, not playback.
    var dragFraction by remember { mutableStateOf(-1f) }
    val shown = if (dragFraction >= 0f) dragFraction else progress.coerceIn(0f, 1f)

    val activeColor = MaterialTheme.colorScheme.primary
    val idleColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onSeek((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        if (dragFraction >= 0f) onSeek(dragFraction)
                        dragFraction = -1f
                    },
                    onDragCancel = { dragFraction = -1f },
                ) { change, _ ->
                    change.consume()
                    dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                }
            },
    ) {
        val w = size.width
        val h = size.height
        val gap = w * 0.004f
        val barWidth = (w - gap * (barCount - 1)) / barCount
        val activeUntil = shown * barCount

        bars.forEachIndexed { index, amplitude ->
            val barHeight = h * amplitude
            val x = index * (barWidth + gap)
            drawRoundRect(
                color = if (index + 0.5f < activeUntil) activeColor else idleColor,
                topLeft = Offset(x, (h - barHeight) / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}
