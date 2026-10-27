package com.avioiva.player.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache

/**
 * Loads embedded cover art (works for MP3 ID3, FLAC METADATA_BLOCK_PICTURE,
 * M4A covr atoms...) with a small memory cache and downsampling so even huge
 * 4 MB covers don't blow up the heap.
 */
object AlbumArtLoader {

    private const val TARGET_SIZE = 512

    @Volatile
    private var cache: LruCache<String, Bitmap>? = null

    private fun cache(): LruCache<String, Bitmap> {
        cache?.let { return it }
        synchronized(this) {
            cache?.let { return it }
            // Roughly 1/8 of the app's available heap, in kilobytes.
            val maxKb = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
            return LruCache<String, Bitmap>(maxKb).also { cache = it }
        }
    }

    /** Embedded cover for [track], or null when the file has none. */
    fun load(track: Track): Bitmap? {
        cache().get(track.path)?.let { return it }

        var retriever: MediaMetadataRetriever? = null
        return try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(track.path)
            val bytes = retriever.embeddedPicture
            val bitmap = bytes?.let { decodeScaled(it) }
            if (bitmap != null) {
                cache().put(track.path, bitmap)
            }
            bitmap
        } catch (_: Throwable) {
            null
        } finally {
            try {
                retriever?.release()
            } catch (_: Throwable) {
            }
        }
    }

    private fun decodeScaled(bytes: ByteArray): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sampleSize = 1
        while (
            bounds.outWidth / (sampleSize * 2) >= TARGET_SIZE &&
            bounds.outHeight / (sampleSize * 2) >= TARGET_SIZE
        ) {
            sampleSize *= 2
        }
        BitmapFactory.decodeByteArray(
            bytes, 0, bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sampleSize },
        )
    } catch (_: Throwable) {
        null
    }
}
