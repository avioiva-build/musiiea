package com.avioiva.player.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Hands the user over to YouTube Music.
 *
 * MUSIIEA itself is 100% offline (no INTERNET permission) and cannot stream
 * or log into YouTube accounts - embedding YouTube Music sign-in / streaming
 * in a third-party app is not permitted. Instead, this button deep-links into
 * the official YouTube Music app (or the web player in a browser) with the
 * current track pre-filled in the search box, so switching takes one tap.
 */
object YTMusic {

    private const val YT_MUSIC_PACKAGE = "com.google.android.apps.youtube.music"
    private const val SEARCH_URL = "https://music.youtube.com/search?q="

    /**
     * Opens YouTube Music searching for [query] (usually "title artist").
     * Tries the installed YouTube Music app first, then falls back to any
     * browser. Never throws - if nothing can handle the link, it no-ops.
     */
    fun openSearch(context: Context, query: String) {
        val url = SEARCH_URL + Uri.encode(query)

        // 1) The official YouTube Music app claims music.youtube.com app links.
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .setPackage(YT_MUSIC_PACKAGE)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        } catch (_: ActivityNotFoundException) {
            // YouTube Music app not installed - fall through to the browser.
        }

        // 2) Any browser opens the YouTube Music web player.
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
