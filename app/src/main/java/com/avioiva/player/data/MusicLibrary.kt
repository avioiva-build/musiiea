package com.avioiva.player.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.File

/**
 * Reads the device audio library through MediaStore (fully offline, no
 * permissions beyond the audio read permission) and derives the
 * album / folder groupings used by the browse tabs.
 */
object MusicLibrary {

    /** Files shorter than this are treated as system sounds, not songs. */
    private const val MIN_DURATION_MS = 30_000L

    fun contentUri(trackId: Long): Uri =
        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackId)

    fun scan(context: Context): List<Track> {
        val tracks = mutableListOf<Track>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.SIZE,
        )
        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(MIN_DURATION_MS.toString())
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder,
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataCol) ?: continue
                    val rawTitle = cursor.getString(titleCol)
                    val rawArtist = cursor.getString(artistCol)
                    val rawAlbum = cursor.getString(albumCol)

                    tracks += Track(
                        id = cursor.getLong(idCol),
                        title = if (rawTitle.isNullOrBlank()) File(path).nameWithoutExtension else rawTitle,
                        artist = if (rawArtist.isNullOrBlank() || rawArtist == MediaStore.UNKNOWN_STRING) "Unknown artist" else rawArtist,
                        album = if (rawAlbum.isNullOrBlank() || rawAlbum == MediaStore.UNKNOWN_STRING) "Unknown album" else rawAlbum,
                        albumId = cursor.getLong(albumIdCol),
                        durationMs = cursor.getLong(durationCol),
                        path = path,
                        folderPath = File(path).parent ?: "/",
                        year = cursor.getInt(yearCol),
                        // TRACK is disc*1000 + number for multi-disc albums.
                        trackNumber = cursor.getInt(trackCol) % 1000,
                        sizeBytes = cursor.getLong(sizeCol),
                    )
                }
            }
        } catch (_: Throwable) {
            // Permission revoked mid-scan or a broken provider; show what we have.
        }
        return tracks
    }

    fun albums(tracks: List<Track>): List<Album> =
        tracks
            .groupBy { it.albumId }
            .map { (_, list) ->
                val sorted = list.sortedWith(
                    compareBy({ it.trackNumber }, { it.title.lowercase() })
                )
                val first = sorted.first()
                Album(
                    id = first.albumId,
                    name = first.album,
                    artist = sorted.map { it.artist }.distinct().let {
                        if (it.size == 1) it.first() else "Various artists"
                    },
                    year = sorted.mapNotNull { t -> t.year.takeIf { y -> y > 0 } }.minOrNull() ?: 0,
                    tracks = sorted,
                )
            }
            .sortedWith(compareBy<Album> { it.name.lowercase() })

    fun folders(tracks: List<Track>): List<Folder> =
        tracks
            .groupBy { it.folderPath }
            .map { (path, list) ->
                Folder(path, list.sortedWith(compareBy { it.title.lowercase() }))
            }
            .sortedWith(compareBy { it.path.lowercase() })
}
