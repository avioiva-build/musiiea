package com.avioiva.player.data

/** A single audio file from the device library. */
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val path: String,
    val folderPath: String,
    val year: Int,
    val trackNumber: Int,
    val sizeBytes: Long,
)

/** An album: all tracks sharing the same ALBUM_ID in MediaStore. */
data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val year: Int,
    val tracks: List<Track>,
) {
    val trackCount: Int get() = tracks.size
    val totalDurationMs: Long get() = tracks.sumOf { it.durationMs }
    val firstTrack: Track? get() = tracks.firstOrNull()
}

/** A directory containing one or more tracks (folder browsing). */
data class Folder(
    val path: String,
    val tracks: List<Track>,
) {
    val name: String get() = path.substringAfterLast('/')
    val trackCount: Int get() = tracks.size
}

/** A user-created playlist. Stores track IDs so it survives library rescans. */
data class Playlist(
    val id: String,
    val name: String,
    val trackIds: List<Long> = emptyList(),
)
