package com.avioiva.player.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Offline playlist storage. Playlists live in a small JSON file inside the
 * app's private directory - no database, no cloud, nothing leaves the device.
 */
class PlaylistStore(context: Context, private val scope: CoroutineScope) {

    private val file = File(context.applicationContext.filesDir, FILE_NAME)

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) {
            _playlists.value = load()
        }
    }

    fun create(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        update { it + Playlist(UUID.randomUUID().toString(), trimmed) }
    }

    fun rename(id: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        update { list -> list.map { if (it.id == id) it.copy(name = trimmed) else it } }
    }

    fun delete(id: String) = update { list -> list.filterNot { it.id == id } }

    fun addTrack(playlistId: String, trackId: Long) = update { list ->
        list.map {
            if (it.id == playlistId && trackId !in it.trackIds) {
                it.copy(trackIds = it.trackIds + trackId)
            } else {
                it
            }
        }
    }

    fun removeTrack(playlistId: String, trackId: Long) = update { list ->
        list.map {
            if (it.id == playlistId) it.copy(trackIds = it.trackIds - trackId) else it
        }
    }

    private fun update(transform: (List<Playlist>) -> List<Playlist>) {
        val next = transform(_playlists.value)
        _playlists.value = next
        scope.launch(Dispatchers.IO) { save(next) }
    }

    private fun load(): List<Playlist> = try {
        if (!file.exists()) {
            emptyList()
        } else {
            val array = JSONArray(file.readText())
            (0 until array.length()).mapNotNull { index ->
                val obj = array.optJSONObject(index) ?: return@mapNotNull null
                val ids = mutableListOf<Long>()
                val idsArray = obj.optJSONArray("trackIds")
                if (idsArray != null) {
                    for (j in 0 until idsArray.length()) ids += idsArray.getLong(j)
                }
                Playlist(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    name = obj.optString("name", "Playlist"),
                    trackIds = ids,
                )
            }
        }
    } catch (_: Throwable) {
        emptyList()
    }

    private fun save(list: List<Playlist>) = try {
        val array = JSONArray()
        list.forEach { playlist ->
            val ids = JSONArray()
            playlist.trackIds.forEach { ids.put(it) }
            array.put(
                JSONObject()
                    .put("id", playlist.id)
                    .put("name", playlist.name)
                    .put("trackIds", ids)
            )
        }
        file.writeText(array.toString(2))
    } catch (_: Throwable) {
        // Disk full or IO failure; keep the in-memory copy authoritative.
    }

    private companion object {
        const val FILE_NAME = "playlists.json"
    }
}
