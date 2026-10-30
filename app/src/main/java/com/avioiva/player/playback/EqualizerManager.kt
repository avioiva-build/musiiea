package com.avioiva.player.playback

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Owns the audio effects chain:
 *  - [Equalizer]    multi-band EQ with device-provided presets (usually 5 bands)
 *  - [BassBoost]    low-frequency amplifier
 *  - [Treble]       high-shelf lift/cut via a dedicated second [Equalizer]
 *                   instance (top band full offset, band below it half)
 *  - [Virtualizer]  spatial "3D surround" widening
 *
 * All of them are attached to ExoPlayer's audio session, so they only affect
 * MUSIIEA's output. Settings persist in SharedPreferences and are
 * re-applied automatically whenever a new audio session is created.
 */
class EqualizerManager(context: Context) {

    data class State(
        val enabled: Boolean = false,
        val attached: Boolean = false,
        val bandLevels: List<Int> = List(DEFAULT_BAND_COUNT) { 0 },
        val bandLabels: List<String> = defaultBandLabels(),
        val bandRange: IntRange = -1500..1500,
        val presets: List<String> = emptyList(),
        val activePreset: Int = -1,
        val bassEnabled: Boolean = false,
        val bassStrength: Int = 500,
        val trebleEnabled: Boolean = false,
        val trebleValue: Int = 500,   // 0..1000 slider; 500 = flat (0 dB)
        val surroundEnabled: Boolean = false,
        val surroundStrength: Int = 500,
    )

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var trebleEq: Equalizer? = null
    private var virtualizer: Virtualizer? = null

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<State> = _state.asStateFlow()

    /**
     * Attach (or re-attach) the effect chain to an audio session.
     * Called by [PlaybackService] whenever ExoPlayer reports an audio session id.
     */
    @Synchronized
    fun attach(audioSessionId: Int) {
        if (audioSessionId == 0) return
        releaseEffects()

        try {
            val eq = Equalizer(PRIORITY, audioSessionId)
            equalizer = eq

            val bandCount = eq.numberOfBands.toInt()
            val levelRange = eq.bandLevelRange
            val low = levelRange[0].toInt()
            val high = levelRange[1].toInt()
            val labels = (0 until bandCount).map { band ->
                formatFrequency(eq.getCenterFreq(band.toShort()) / 1000)
            }
            val presetNames = (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) }

            val current = _state.value
            val savedLevels = current.bandLevels

            if (current.activePreset in presetNames.indices) {
                // Restore a preset and sync the sliders to its curve.
                eq.usePreset(current.activePreset.toShort())
                val synced = (0 until bandCount).map { eq.getBandLevel(it.toShort()).toInt() }
                _state.value = current.copy(
                    bandLevels = synced,
                    bandLabels = labels,
                    bandRange = low..high,
                    presets = presetNames,
                    attached = true,
                )
            } else {
                val levels =
                    if (savedLevels.size == bandCount) savedLevels
                    else List(bandCount) { 0 }
                levels.forEachIndexed { band, level ->
                    eq.setBandLevel(band.toShort(), level.coerceIn(low, high).toShort())
                }
                _state.value = current.copy(
                    bandLevels = levels,
                    bandLabels = labels,
                    bandRange = low..high,
                    presets = presetNames,
                    attached = true,
                )
            }

            eq.setEnabled(_state.value.enabled)
        } catch (_: Throwable) {
            // No effect engine available on this device.
            releaseEffects()
            return
        }

        try {
            val bb = BassBoost(PRIORITY, audioSessionId)
            bassBoost = bb
            bb.setStrength(_state.value.bassStrength.coerceIn(0, 1000).toShort())
            bb.setEnabled(_state.value.bassEnabled)
        } catch (_: Throwable) {
            bassBoost = null
        }

        try {
            val vz = Virtualizer(PRIORITY, audioSessionId)
            virtualizer = vz
            vz.setStrength(_state.value.surroundStrength.coerceIn(0, 1000).toShort())
            vz.setEnabled(_state.value.surroundEnabled)
        } catch (_: Throwable) {
            virtualizer = null
        }

        try {
            // Android ships no dedicated Treble effect, so a second Equalizer
            // instance acts as a high-shelf (see [applyTrebleTo]). Effects on
            // the same session chain together, and this instance is fully
            // independent of the main EQ's band sliders.
            val te = Equalizer(PRIORITY, audioSessionId)
            trebleEq = te
            applyTrebleTo(te)
            te.setEnabled(_state.value.trebleEnabled)
        } catch (_: Throwable) {
            trebleEq = null
        }
    }

    /** Detach and free all effect instances. */
    @Synchronized
    fun releaseEffects() {
        try { equalizer?.enabled = false } catch (_: Throwable) {}
        try { equalizer?.release() } catch (_: Throwable) {}
        try { bassBoost?.enabled = false } catch (_: Throwable) {}
        try { bassBoost?.release() } catch (_: Throwable) {}
        try { trebleEq?.enabled = false } catch (_: Throwable) {}
        try { trebleEq?.release() } catch (_: Throwable) {}
        try { virtualizer?.enabled = false } catch (_: Throwable) {}
        try { virtualizer?.release() } catch (_: Throwable) {}
        equalizer = null
        bassBoost = null
        trebleEq = null
        virtualizer = null
        _state.value = _state.value.copy(attached = false)
    }

    // ----------------------- UI-facing setters -----------------------

    fun setEnabled(value: Boolean) {
        _state.value = _state.value.copy(enabled = value)
        try { equalizer?.enabled = value } catch (_: Throwable) {}
        persist()
    }

    /** Apply a device preset and sync the band sliders to its curve. */
    fun applyPreset(index: Int) {
        val eq = equalizer
        if (eq != null) {
            try {
                eq.usePreset(index.toShort())
                val levels = (0 until eq.numberOfBands.toInt())
                    .map { eq.getBandLevel(it.toShort()).toInt() }
                _state.value = _state.value.copy(bandLevels = levels, activePreset = index)
                persist()
                return
            } catch (_: Throwable) {
            }
        }
        _state.value = _state.value.copy(activePreset = index)
        persist()
    }

    /** Set a single band level (milli-bel); marks the EQ as "Custom". */
    fun setBandLevel(band: Int, level: Int) {
        val range = _state.value.bandRange
        val clamped = level.coerceIn(range.first, range.last)
        val levels = _state.value.bandLevels.toMutableList()
        if (band in levels.indices) levels[band] = clamped
        _state.value = _state.value.copy(bandLevels = levels, activePreset = -1)
        try { equalizer?.setBandLevel(band.toShort(), clamped.toShort()) } catch (_: Throwable) {}
        persist()
    }

    fun setBassEnabled(value: Boolean) {
        _state.value = _state.value.copy(bassEnabled = value)
        try { bassBoost?.enabled = value } catch (_: Throwable) {}
        persist()
    }

    fun setBassStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _state.value = _state.value.copy(bassStrength = clamped)
        try { bassBoost?.setStrength(clamped.toShort()) } catch (_: Throwable) {}
        persist()
    }

    fun setTrebleEnabled(value: Boolean) {
        _state.value = _state.value.copy(trebleEnabled = value)
        try { trebleEq?.enabled = value } catch (_: Throwable) {}
        persist()
    }

    /** Slider value 0..1000; 500 = flat, endpoints = +/- [TREBLE_MAX_MB]. */
    fun setTrebleValue(value: Int) {
        val clamped = value.coerceIn(0, 1000)
        _state.value = _state.value.copy(trebleValue = clamped)
        trebleEq?.let { te -> try { applyTrebleTo(te) } catch (_: Throwable) {} }
        persist()
    }

    /**
     * Shapes [te] into a high-shelf "treble" curve: the top band gets the
     * full offset and the band below it half, which lifts/cuts the highs
     * without touching the rest of the spectrum.
     */
    private fun applyTrebleTo(te: Equalizer) {
        try {
            val count = te.numberOfBands.toInt()
            if (count < 2) return
            val range = te.bandLevelRange
            val low = range[0].toInt()
            val high = range[1].toInt()
            val offset = ((_state.value.trebleValue - 500) * TREBLE_MAX_MB) / 500
            val top = (count - 1).toShort()
            te.setBandLevel(top, offset.coerceIn(low, high).toShort())
            te.setBandLevel((top - 1).toShort(), (offset / 2).coerceIn(low, high).toShort())
        } catch (_: Throwable) {
        }
    }

    fun setSurroundEnabled(value: Boolean) {
        _state.value = _state.value.copy(surroundEnabled = value)
        try { virtualizer?.enabled = value } catch (_: Throwable) {}
        persist()
    }

    fun setSurroundStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _state.value = _state.value.copy(surroundStrength = clamped)
        try { virtualizer?.setStrength(clamped.toShort()) } catch (_: Throwable) {}
        persist()
    }

    // ----------------------- persistence -----------------------

    private fun loadState(): State {
        val levels = prefs.getString(KEY_LEVELS, null)
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
            ?: List(DEFAULT_BAND_COUNT) { 0 }
        return State(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            bandLevels = levels,
            activePreset = prefs.getInt(KEY_PRESET, -1),
            bassEnabled = prefs.getBoolean(KEY_BASS_ENABLED, false),
            bassStrength = prefs.getInt(KEY_BASS_STRENGTH, 500),
            trebleEnabled = prefs.getBoolean(KEY_TREBLE_ENABLED, false),
            trebleValue = prefs.getInt(KEY_TREBLE_VALUE, 500),
            surroundEnabled = prefs.getBoolean(KEY_SURROUND_ENABLED, false),
            surroundStrength = prefs.getInt(KEY_SURROUND_STRENGTH, 500),
        )
    }

    private fun persist() {
        val s = _state.value
        prefs.edit()
            .putBoolean(KEY_ENABLED, s.enabled)
            .putString(KEY_LEVELS, s.bandLevels.joinToString(","))
            .putInt(KEY_PRESET, s.activePreset)
            .putBoolean(KEY_BASS_ENABLED, s.bassEnabled)
            .putInt(KEY_BASS_STRENGTH, s.bassStrength)
            .putBoolean(KEY_TREBLE_ENABLED, s.trebleEnabled)
            .putInt(KEY_TREBLE_VALUE, s.trebleValue)
            .putBoolean(KEY_SURROUND_ENABLED, s.surroundEnabled)
            .putInt(KEY_SURROUND_STRENGTH, s.surroundStrength)
            .apply()
    }

    private fun formatFrequency(hz: Int): String =
        if (hz >= 1000) {
            if (hz % 1000 == 0) "${hz / 1000} kHz"
            else String.format(Locale.US, "%.1f kHz", hz / 1000f)
        } else {
            "$hz Hz"
        }

    private companion object {
        const val PREFS_NAME = "avioiva_prefs"
        const val PRIORITY = 0
        const val DEFAULT_BAND_COUNT = 5

        const val KEY_ENABLED = "eq.enabled"
        const val KEY_LEVELS = "eq.levels"
        const val KEY_PRESET = "eq.preset"
        const val KEY_BASS_ENABLED = "bass.enabled"
        const val KEY_BASS_STRENGTH = "bass.strength"
        const val KEY_TREBLE_ENABLED = "treble.enabled"
        const val KEY_TREBLE_VALUE = "treble.value"
        const val KEY_SURROUND_ENABLED = "surround.enabled"
        const val KEY_SURROUND_STRENGTH = "surround.strength"

        /** Max treble shelf offset in milli-bel: +/-600 mB = +/-6 dB. */
        const val TREBLE_MAX_MB = 600

        fun defaultBandLabels() = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
    }
}
