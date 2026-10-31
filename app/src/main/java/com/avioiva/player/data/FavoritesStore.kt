package com.avioiva.player.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tiny persistent "liked songs" store behind the heart button on the
 * Now Playing screen. Track ids live in SharedPreferences as a string set,
 * so favorites survive restarts without any database.
 */
class FavoritesStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _ids = MutableStateFlow(load())
    val ids: StateFlow<Set<String>> = _ids.asStateFlow()

    fun isFavorite(trackId: String): Boolean = _ids.value.contains(trackId)

    /** Toggles [trackId]; returns the new state (true = now a favorite). */
    fun toggle(trackId: String): Boolean {
        val updated = _ids.value.toMutableSet()
        val removed = updated.remove(trackId)
        if (!removed) updated.add(trackId)
        prefs.edit().putStringSet(KEY_IDS, updated).apply()
        _ids.value = updated
        return !removed
    }

    private fun load(): Set<String> =
        prefs.getStringSet(KEY_IDS, emptySet())?.toSet() ?: emptySet()

    private companion object {
        const val PREFS_NAME = "avioiva_favorites"
        const val KEY_IDS = "favorite.ids"
    }
}
